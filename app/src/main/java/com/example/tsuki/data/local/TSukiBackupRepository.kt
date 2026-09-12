package com.example.tsuki.data.local

import android.content.Context
import android.net.Uri
import com.example.tsuki.data.recommendation.TSukiNeuroEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class TSukiBackupRepository(private val context: Context) {

    private val subRepo = TSukiSubscriptionRepository.getInstance(context)

    suspend fun exportSubscriptionsAsNewPipe(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val subs = subRepo.getAllSubscriptions().first()
            val fav = HomePreferences(context).favoriteChannels.first()
            val combined = if (subs.isNotEmpty()) subs else fav.mapNotNull { entry ->
                val p = entry.split("|")
                if (p[0].isBlank()) null else TSukiChannelSubscription(p[0], p.getOrNull(1) ?: p[0], p.getOrNull(2) ?: "")
            }
            val arr = JSONArray()
            combined.forEach { sub ->
                val url = when {
                    sub.channelId.startsWith("UC") -> "https://www.youtube.com/channel/${sub.channelId}"
                    sub.channelId.startsWith("@") -> "https://www.youtube.com/${sub.channelId}"
                    else -> "https://www.youtube.com/channel/${sub.channelId}"
                }
                val obj = JSONObject()
                obj.put("service_id", 0)
                obj.put("url", url)
                obj.put("name", sub.channelName.ifBlank { sub.channelId })
                arr.put(obj)
            }
            val root = JSONObject()
            root.put("app_version", "1.0")
            root.put("app_version_int", 1)
            root.put("subscriptions", arr)
            context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                OutputStreamWriter(out, Charsets.UTF_8).use { it.write(root.toString(2)) }
            } ?: return@withContext Result.failure(Exception("openOutputStream null"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importNewPipe(uri: Uri, onProgress: ((Int, Int) -> Unit)? = null): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() } ?: return@withContext Result.failure(Exception("read fail"))
            val obj = JSONObject(jsonString)
            val arr = obj.optJSONArray("subscriptions") ?: return@withContext Result.failure(Exception("no subs"))
            val toImport = mutableListOf<TSukiChannelSubscription>()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val url = item.optString("url")
                val name = item.optString("name")
                if (url.isBlank() || name.isBlank()) continue
                var id = ""
                when {
                    url.contains("/channel/") -> id = url.substringAfter("/channel/").substringBefore("/").substringBefore("?")
                    url.contains("/@") -> id = url.substringAfter("/@").substringBefore("/").substringBefore("?")
                    url.contains("/user/") -> id = url.substringAfter("/user/").substringBefore("/").substringBefore("?")
                    else -> id = url.substringAfterLast("/").substringBefore("?")
                }
                if (id.isBlank()) continue
                toImport.add(TSukiChannelSubscription(id, name, "", System.currentTimeMillis()))
            }
            val sem = Semaphore(5)
            val withAvatars = mutableListOf<TSukiChannelSubscription>()
            var done = 0
            supervisorScope {
                toImport.chunked(25).forEach { batch ->
                    val res = batch.map { sub ->
                        async(Dispatchers.IO) {
                            sem.withPermit {
                                val avatar = try { fetchAvatar(sub.channelId) } catch (_: Exception) { "" }
                                val r = sub.copy(channelThumbnail = avatar)
                                done += 1
                                onProgress?.invoke(done, toImport.size)
                                r
                            }
                        }
                    }.awaitAll()
                    withAvatars.addAll(res)
                }
            }
            withAvatars.forEach { subRepo.subscribe(it) }
            try { TSukiNeuroEngine.bootstrapFromSubscriptions(context, withAvatars.map { it.channelName }) } catch (_: Exception) {}
            Result.success(withAvatars.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importYouTubeCsv(uri: Uri, onProgress: ((Int, Int) -> Unit)? = null): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val toImport = mutableListOf<TSukiChannelSubscription>()
            context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input)).use { reader ->
                    var first = true
                    reader.forEachLine { line ->
                        if (first) { first = false; if (line.startsWith("Channel Id", ignoreCase = true)) return@forEachLine }
                        val parts = line.split(",", limit = 3)
                        if (parts.size < 3) return@forEachLine
                        val id = parts[0].trim().trimStart('\uFEFF')
                        val name = parts[2].trim().removeSurrounding("\"")
                        if (id.isNotEmpty() && name.isNotEmpty()) toImport.add(TSukiChannelSubscription(id, name, "", System.currentTimeMillis()))
                    }
                }
            } ?: return@withContext Result.failure(Exception("read fail"))
            val sem = Semaphore(5)
            val withAvatars = mutableListOf<TSukiChannelSubscription>()
            var done = 0
            supervisorScope {
                toImport.chunked(25).forEach { batch ->
                    val res = batch.map { sub ->
                        async(Dispatchers.IO) {
                            sem.withPermit {
                                val avatar = try { fetchAvatar(sub.channelId) } catch (_: Exception) { "" }
                                done += 1
                                onProgress?.invoke(done, toImport.size)
                                sub.copy(channelThumbnail = avatar)
                            }
                        }
                    }.awaitAll()
                    withAvatars.addAll(res)
                }
            }
            withAvatars.forEach { subRepo.subscribe(it) }
            try { TSukiNeuroEngine.bootstrapFromSubscriptions(context, withAvatars.map { it.channelName }) } catch (_: Exception) {}
            Result.success(withAvatars.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchAvatar(channelId: String): String = withContext(Dispatchers.IO) {
        try {
            val url = if (channelId.startsWith("@")) {
                "https://www.youtube.com/$channelId"
            } else {
                "https://www.youtube.com/channel/$channelId"
            }
            val extractor = org.schabi.newpipe.extractor.ServiceList.YouTube.getChannelExtractor(url)
            extractor.fetchPage()
            extractor.avatars.maxByOrNull { it.height }?.url ?: ""
        } catch (_: Exception) { "" }
    }

    suspend fun exportMaster(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val subs = subRepo.getAllSubscriptions().first()
            val fav = HomePreferences(context).favoriteChannels.first()
            val history = WatchHistoryManager.getInstance(context).getRecentHistory(200)
            val root = JSONObject()
            root.put("subscriptions", JSONArray().apply {
                (if (subs.isNotEmpty()) subs else fav.mapNotNull { e ->
                    val p = e.split("|")
                    if (p[0].isBlank()) null else TSukiChannelSubscription(p[0], p.getOrNull(1) ?: p[0], p.getOrNull(2) ?: "")
                }).forEach { sub ->
                    val o = JSONObject()
                    o.put("channelId", sub.channelId)
                    o.put("channelName", sub.channelName)
                    o.put("channelThumbnail", sub.channelThumbnail)
                    put(o)
                }
            })
            root.put("favoriteChannels", JSONArray(fav))
            root.put("watchHistory", JSONArray().apply {
                history.take(200).forEach { h ->
                    val o = JSONObject()
                    o.put("videoId", h.videoId)
                    o.put("title", h.title)
                    put(o)
                }
            })
            context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                OutputStreamWriter(out, Charsets.UTF_8).use { it.write(root.toString(2)) }
            } ?: return@withContext Result.failure(Exception("open fail"))
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun importMaster(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() } ?: return@withContext Result.failure(Exception("read fail"))
            val root = JSONObject(json)
            val arr = root.optJSONArray("subscriptions") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.optString("channelId")
                if (id.isBlank()) continue
                subRepo.subscribe(TSukiChannelSubscription(id, o.optString("channelName", id), o.optString("channelThumbnail", ""), System.currentTimeMillis()))
            }
            val favArr = root.optJSONArray("favoriteChannels")
            if (favArr != null) {
                val prefs = HomePreferences(context)
                for (i in 0 until favArr.length()) {
                    val s = favArr.optString(i)
                    if (s.isNotBlank()) prefs.addFavoriteChannel(s)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}

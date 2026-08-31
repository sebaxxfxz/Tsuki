package com.example.tsuki.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

data class UpdateInfo(
    val latestVersion: String,
    val isUpdateAvailable: Boolean,
    val changelog: String,
    val apkUrl: String?,
    val apkSizeMb: String,
    val releaseDate: String
)

object UpdateChecker {
    private const val API = "https://api.github.com/repos/sebaxxfxz/TSuki/releases/latest"

    suspend fun checkForUpdates(context: Context): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val current = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
            } catch (_: Exception) { "1.0" }
            val conn = (URL(API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "TSukiApp")
            }
            if (conn.responseCode != 200) return@withContext Result.failure(Exception("HTTP ${conn.responseCode}"))
            val raw = conn.inputStream.bufferedReader().use { it.readText() }
            val obj = JSONObject(raw)
            val tag = obj.optString("tag_name", "")
            val latest = tag.removePrefix("v").removePrefix("b").trim()
            val cur = current.removePrefix("v").removePrefix("b").trim()
            val newer = isNewer(latest, cur)
            val body = obj.optString("body", "Sin notas de versión.")
            val published = obj.optString("published_at", "")
            val date = formatDate(published)
            var url: String? = null
            var size = ""
            val assets = obj.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    val name = a.optString("name", "")
                    if (name.endsWith(".apk", true)) {
                        url = if (a.has("browser_download_url")) a.getString("browser_download_url") else null
                        val bytes = a.optLong("size", 0L)
                        if (bytes > 0) size = String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
                        break
                    }
                }
            }
            val info = UpdateInfo(latest, newer, body, url, size, date)
            Log.d("TSukiUpdate", "latest=$latest cur=$cur avail=$newer")
            Result.success(info)
        } catch (e: Exception) {
            Log.e("TSukiUpdate", "check failed", e)
            Result.failure(e)
        }
    }

    private fun isNewer(latest: String, current: String): Boolean {
        if (latest.isBlank() || latest == current) return false
        return try {
            val parseParts = { versionStr: String ->
                versionStr.split(".").mapNotNull { part ->
                    part.substringBefore("-").filter { it.isDigit() }.toIntOrNull()
                }
            }
            val l = parseParts(latest)
            val c = parseParts(current)
            val n = maxOf(l.size, c.size)
            for (i in 0 until n) {
                val lv = l.getOrElse(i) { 0 }
                val cv = c.getOrElse(i) { 0 }
                if (lv > cv) return true
                if (lv < cv) return false
            }
            false
        } catch (_: Exception) { latest != current }
    }

    private fun formatDate(iso: String): String {
        if (iso.isBlank()) return ""
        val normalized = iso.replace(Regex("\\.\\d+"), "")
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (pattern in patterns) {
            try {
                val inp = SimpleDateFormat(pattern, Locale.US)
                val d = inp.parse(normalized)
                if (d != null) {
                    val out = SimpleDateFormat("d 'de' MMMM, yyyy", Locale.forLanguageTag("es-ES"))
                    return out.format(d)
                }
            } catch (_: Exception) {}
        }
        return iso.take(10)
    }
}

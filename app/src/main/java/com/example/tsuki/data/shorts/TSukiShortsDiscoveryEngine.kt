package com.example.tsuki.data.shorts

import android.content.Context
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.network.ChannelRssClient
import com.example.tsuki.network.YouTubeExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

class TSukiShortsDiscoveryEngine private constructor(private val context: Context) {

    private val rssClient = ChannelRssClient()
    private val extractor = YouTubeExtractor()

    suspend fun getDiscoveryShorts(
        userSubs: Set<String> = emptySet(),
        topics: Set<String> = emptySet()
    ): List<MediaTrack> = withContext(Dispatchers.IO) {
        supervisorScope {
            val rssDeferred = async {
                if (userSubs.isEmpty()) return@async emptyList<MediaTrack>()
                val ids = userSubs.take(8)
                val lists = ids.map { id ->
                    async {
                        val clean = if (id.contains("|")) id.substringBefore("|") else id
                        val vids = try { rssClient.fetchChannelVideos(clean) } catch (_: Exception) { emptyList() }
                        vids.filter { TSukiShortsClassifier.isShort(it) }.take(4)
                    }
                }.awaitAll().flatten()
                lists
            }
            val topicList = topics.ifEmpty { setOf("shorts", "funny shorts", "music shorts") }.take(3)
            val searchDeferred = topicList.map { q ->
                async {
                    val query = "$q #shorts"
                    val res = try { extractor.searchVideos(query) } catch (_: Exception) { emptyList() }
                    res.filter { TSukiShortsClassifier.isShort(it) }.take(6)
                }
            }
            val rssShorts = try { rssDeferred.await() } catch (_: Exception) { emptyList() }
            val searchShorts = try { searchDeferred.awaitAll().flatten() } catch (_: Exception) { emptyList() }
            val combined = (rssShorts + searchShorts).distinctBy { it.id }
            deduplicateByTitle(combined)
        }
    }

    private fun deduplicateByTitle(videos: List<MediaTrack>): List<MediaTrack> {
        if (videos.size <= 1) return videos
        val result = mutableListOf<MediaTrack>()
        val tokens = videos.map { v ->
            v to v.title.lowercase().split(Regex("\\s+")).map { it.trim { c -> !c.isLetterOrDigit() } }.filter { it.length > 2 }.toSet()
        }
        val consumed = mutableSetOf<Int>()
        for (i in tokens.indices) {
            if (i in consumed) continue
            var best = tokens[i].first
            val bestTokens = tokens[i].second
            for (j in i + 1 until tokens.size) {
                if (j in consumed) continue
                val otherTokens = tokens[j].second
                if (bestTokens.isEmpty() || otherTokens.isEmpty()) continue
                val inter = bestTokens.intersect(otherTokens).size
                val union = bestTokens.union(otherTokens).size
                val sim = if (union > 0) inter.toDouble() / union else 0.0
                if (sim > 0.6) {
                    val other = tokens[j].first
                    if (other.viewCount > best.viewCount) best = other
                    consumed.add(j)
                }
            }
            result.add(best)
            consumed.add(i)
        }
        return result
    }

    companion object {
        @Volatile
        private var instance: TSukiShortsDiscoveryEngine? = null
        fun getInstance(context: Context): TSukiShortsDiscoveryEngine =
            instance ?: synchronized(this) {
                instance ?: TSukiShortsDiscoveryEngine(context.applicationContext).also { instance = it }
            }
    }
}

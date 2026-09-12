package com.example.tsuki.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

import java.util.LinkedHashMap

@Serializable
data class SponsorSegment(
    val category: String,
    val segment: List<Float>
)

class SponsorBlockClient private constructor() {

    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
    }
    private val cache = object : LinkedHashMap<String, List<Pair<Long, Long>>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<Pair<Long, Long>>>?): Boolean = size > 150
    }

    suspend fun getSkipSegments(videoId: String): List<Pair<Long, Long>> {
        synchronized(cache) { cache[videoId]?.let { return it } }
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://sponsor.ajay.app/api/skipSegments?videoID=$videoId&categories=[\"sponsor\",\"intro\",\"outro\",\"selfpromo\"]"
                val response = client.get(url)
                val responseText = response.bodyAsText()
                val segments = json.decodeFromString<List<SponsorSegment>>(responseText)

                val parsed = segments.mapNotNull { item ->
                    if (item.segment.size >= 2) {
                        val startMs = (item.segment[0] * 1000).toLong()
                        val endMs = (item.segment[1] * 1000).toLong()
                        Pair(startMs, endMs)
                    } else null
                }
                synchronized(cache) { cache[videoId] = parsed }
                parsed
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    companion object {
        @Volatile
        private var instance: SponsorBlockClient? = null

        fun getInstance(): SponsorBlockClient =
            instance ?: synchronized(this) {
                instance ?: SponsorBlockClient().also { instance = it }
            }
    }
}

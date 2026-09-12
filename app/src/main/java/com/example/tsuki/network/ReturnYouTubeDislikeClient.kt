package com.example.tsuki.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Immutable
@Serializable
data class RydVoteData(
    val id: String,
    val likes: Long = 0L,
    val dislikes: Long = 0L,
    val rating: Double = 0.0,
    val viewCount: Long = 0L
)

class ReturnYouTubeDislikeClient {

    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun getDislikes(videoId: String): RydVoteData? = withContext(Dispatchers.IO) {
        try {
            val url = "https://returnyoutubedislikeapi.com/votes?videoId=$videoId"
            val response = client.get(url)
            json.decodeFromString<RydVoteData>(response.bodyAsText())
        } catch (e: Exception) {
            null
        }
    }
}

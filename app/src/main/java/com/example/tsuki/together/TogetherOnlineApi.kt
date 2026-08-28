package com.example.tsuki.together

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

@Serializable data class TogetherOnlineCreateSessionRequest(val hostDisplayName: String, val settings: TogetherRoomSettings)
@Serializable data class TogetherOnlineCreateSessionResponse(val sessionId: String, val code: String, val hostKey: String, val guestKey: String, val wsUrl: String, val settings: TogetherRoomSettings)
@Serializable data class TogetherOnlineResolveRequest(val code: String)
@Serializable data class TogetherOnlineResolveResponse(val sessionId: String, val guestKey: String, val wsUrl: String, val settings: TogetherRoomSettings)

class TogetherOnlineApiException(message: String, val statusCode: Int? = null, cause: Throwable? = null) : Exception(message, cause)

class TogetherOnlineApi(private val baseUrl: String, private val bearerToken: String? = null) {
    private val root = baseUrl.trimEnd('/').let { if (it.endsWith("/v1")) it else "$it/v1" }
    private val codec = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }
    private val http = HttpClient(OkHttp) {
        engine { config { connectTimeout(15, TimeUnit.SECONDS); readTimeout(15, TimeUnit.SECONDS); writeTimeout(15, TimeUnit.SECONDS); retryOnConnectionFailure(true) } }
    }

    private suspend fun <T> retry(max: Int = 2, delayMs: Long = 800, block: suspend () -> T): T {
        var last: Throwable? = null
        for (i in 1..max) {
            try { return block() } catch (t: Throwable) {
                last = t
                val retryable = generateSequence(t) { it.cause }.lastOrNull()?.let { it is java.net.SocketTimeoutException || it is java.net.ConnectException || it is java.io.IOException } == true
                if (i < max && retryable) delay(delayMs * i) else throw t
            }
        }
        throw last!!
    }

    @Serializable private data class ErrBody(val ok: Boolean? = null, val error: String? = null, val code: String? = null)

    private fun errMsg(status: Int, body: String): String {
        val parsed = runCatching { codec.decodeFromString(ErrBody.serializer(), body) }.getOrNull()?.error?.trim()?.takeIf { it.isNotEmpty() }
        if (parsed != null) return parsed
        return when (status) {
            400 -> "Bad request"; 401 -> "Unauthorized"; 403 -> "Forbidden"; 404 -> "Session not found"; 429 -> "Too many requests"; in 500..599 -> "Server error ($status)"; else -> "Unexpected response ($status)"
        }
    }

    private fun token(): String = bearerToken?.trim()?.takeIf { it.isNotEmpty() } ?: throw TogetherOnlineApiException("Together token is missing")

    suspend fun createSession(hostDisplayName: String, settings: TogetherRoomSettings): TogetherOnlineCreateSessionResponse = retry {
        val tk = token()
        val payload = codec.encodeToString(TogetherOnlineCreateSessionRequest.serializer(), TogetherOnlineCreateSessionRequest(hostDisplayName, settings))
        val resp = http.post("$root/together/sessions") { header("Authorization", "Bearer $tk"); contentType(ContentType.Application.Json); setBody(payload) }
        val st = resp.status.value; val raw = resp.bodyAsText()
        if (st !in 200..299) throw TogetherOnlineApiException(errMsg(st, raw), st)
        codec.decodeFromString(TogetherOnlineCreateSessionResponse.serializer(), raw)
    }

    suspend fun resolveCode(code: String): TogetherOnlineResolveResponse = retry {
        val tk = token()
        val payload = codec.encodeToString(TogetherOnlineResolveRequest.serializer(), TogetherOnlineResolveRequest(code.trim()))
        val resp = http.post("$root/together/sessions/resolve") { header("Authorization", "Bearer $tk"); contentType(ContentType.Application.Json); setBody(payload) }
        val st = resp.status.value; val raw = resp.bodyAsText()
        if (st !in 200..299) throw TogetherOnlineApiException(errMsg(st, raw), st)
        codec.decodeFromString(TogetherOnlineResolveResponse.serializer(), raw)
    }

    suspend fun endSession(sessionId: String, hostKey: String) { retry {
        val sid = sessionId.trim(); val hk = hostKey.trim()
        require(sid.isNotEmpty()) { "Session ID is required" }; require(hk.isNotEmpty()) { "Host key is required" }
        val r = http.post("$root/together/sessions/$sid/end") { header("Authorization", "Bearer $hk") }
        val st = r.status.value
        if (st !in 200..299 && st != 404) throw TogetherOnlineApiException(errMsg(st, r.bodyAsText()), st)
    } }
}

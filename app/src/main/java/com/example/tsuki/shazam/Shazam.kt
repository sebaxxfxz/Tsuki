package com.example.tsuki.shazam

import com.example.tsuki.shazam.models.RecognitionResult
import com.example.tsuki.shazam.models.ShazamRequestJson
import com.example.tsuki.shazam.models.ShazamResponseJson
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

object Shazam {
    private val bgScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private const val MaxConc = 2
    private const val MinInterval = 1000L
    private const val MaxTry = 3
    private const val BaseDelay = 2000L
    private const val CacheTtl = 300000L
    private const val MaxQueue = 50

    private val active = AtomicInteger(0)
    private var lastTs = 0L
    private val gate = Mutex()
    private val queue = ConcurrentLinkedQueue<JobReq>()
    private val cache = ConcurrentHashMap<String, CacheEnt>()
    private var seqId = 0L
    @Volatile private var draining = false

    private val http by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(Json { isLenient = true; ignoreUnknownKeys = true; encodeDefaults = true }) }
            expectSuccess = false
        }
    }

    private val agents = listOf(
        "Dalvik/2.1.0 (Linux; U; Android 7.1.2; Pixel Build/N2G47H)",
        "Dalvik/2.1.0 (Linux; U; Android 8.0.0; SM-G950F Build/R16NW)",
        "Dalvik/2.1.0 (Linux; U; Android 9; SM-G960F Build/PPR1.180610.011)",
        "Dalvik/2.1.0 (Linux; U; Android 10; Pixel 3 Build/QQ1A.200205.002)",
        "Dalvik/2.1.0 (Linux; U; Android 11; SM-G991B Build/RP1A.200720.012)"
    )

    private val zones = listOf("Europe/Paris", "Europe/Berlin", "America/New_York", "America/Los_Angeles", "Asia/Tokyo", "Asia/Singapore")

    suspend fun recognize(signature: String, sampleDurationMs: Long): Result<RecognitionResult> {
        val key = keyOf(signature)
        cached(key)?.let { return Result.success(it) }
        return enqueue(signature, sampleDurationMs)
    }

    fun getPendingRequestsCount(): Int = queue.size
    fun getActiveRequestsCount(): Int = active.get()
    fun clearCache() { cache.clear() }
    fun cancelPendingRequests() { queue.clear() }
    fun cleanup() { cancelPendingRequests(); clearCache(); http.close() }

    private suspend fun enqueue(sig: String, dur: Long): Result<RecognitionResult> = gate.withLock {
        if (queue.size >= MaxQueue) return Result.failure(Exception("Request queue is full. Please wait."))
        val id = seqId++
        val req = JobReq(id, sig, dur)
        queue.offer(req)
        if (!draining) { draining = true; drain() }
        return req.await()
    }

    private suspend fun drain() {
        while (true) {
            val req = queue.poll() ?: break
            while (active.get() >= MaxConc) delay(100)
            active.incrementAndGet()
            bgScope.launch {
                try { req.done(execute(req.signature, req.sampleDurationMs)) } catch (e: Exception) { req.done(Result.failure(e)) } finally { active.decrementAndGet() }
            }
            throttle()
        }
        draining = false
    }

    private suspend fun execute(sig: String, dur: Long): Result<RecognitionResult> {
        var last: Exception? = null
        for (attempt in 0 until MaxTry) {
            try {
                throttle()
                val res = call(sig, dur)
                cache[keyOf(sig)] = CacheEnt(System.currentTimeMillis(), res)
                if (cache.size >= 100) evict()
                return Result.success(res)
            } catch (e: Exception) {
                last = e
                val is429 = e.message?.contains("429") == true || e.message?.contains("Too many requests", true) == true
                if (is429 && attempt < MaxTry - 1) { delay(BaseDelay * (1 shl attempt)); continue }
                throw e
            }
        }
        throw last ?: Exception("Recognition failed after $MaxTry attempts")
    }

    private suspend fun call(signature: String, sampleDurationMs: Long): RecognitionResult {
        val ts = System.currentTimeMillis() / 1000
        val u1 = UUID.randomUUID().toString().uppercase()
        val u2 = UUID.randomUUID().toString()
        val body = ShazamRequestJson(
            geolocation = ShazamRequestJson.Geo(Random.nextDouble() * 400 + 100, Random.nextDouble() * 180 - 90, Random.nextDouble() * 360 - 180),
            signature = ShazamRequestJson.Sig(sampleDurationMs, ts, signature),
            timestamp = ts,
            timezone = zones.random()
        )
        val resp = http.post("https://amp.shazam.com/discovery/v5/en/US/android/-/tag/$u1/$u2") {
            parameter("sync", "true"); parameter("webv3", "true"); parameter("sampling", "true"); parameter("connected", ""); parameter("shazamapiversion", "v3"); parameter("sharehub", "true"); parameter("video", "v3")
            header("User-Agent", agents.random()); header("Content-Language", "en_US"); contentType(ContentType.Application.Json); setBody(body)
        }
        if (!resp.status.isSuccess()) {
            when (resp.status.value) {
                429 -> throw Exception("Too many requests")
                404 -> throw Exception("No match found")
                in 500..599 -> throw Exception("Shazam service temporarily unavailable")
                else -> throw Exception("Recognition failed (error ${resp.status.value})")
            }
        }
        val parsed = resp.body<ShazamResponseJson>()
        return parsed.toResult() ?: throw Exception("No match found")
    }

    private suspend fun throttle() {
        val now = System.currentTimeMillis()
        val diff = now - lastTs
        if (diff < MinInterval) delay(MinInterval - diff)
        lastTs = System.currentTimeMillis()
    }

    private fun keyOf(s: String): String = s.hashCode().toString()
    private fun cached(k: String): RecognitionResult? {
        val e = cache[k] ?: return null
        if (System.currentTimeMillis() - e.ts > CacheTtl) { cache.remove(k); return null }
        return e.res
    }
    private fun evict() {
        val now = System.currentTimeMillis()
        val it = cache.entries.iterator()
        while (it.hasNext()) { val e = it.next(); if (now - e.value.ts > CacheTtl) it.remove() }
    }

    private fun ShazamResponseJson.toResult(): RecognitionResult? {
        val tr = track ?: return null
        val songMeta = tr.sections?.find { it?.type == "SONG" }?.metadata
        val album = songMeta?.find { it?.title == "Album" }?.text
        val label = songMeta?.find { it?.title == "Label" }?.text
        val released = songMeta?.find { it?.title == "Released" }?.text
        val lyrics = tr.sections?.find { it?.type == "LYRICS" }?.text
        val apple = tr.hub?.options?.firstOrNull { it?.providername?.contains("apple", true) == true }?.actions?.firstOrNull()
        val spotify = tr.hub?.providers?.find { it?.caption?.contains("spotify", true) == true }
        val video = tr.hub?.options?.find { it?.type?.contains("video", true) == true }?.actions?.firstOrNull()
        val vid = video?.uri?.let { u -> u.substringAfterLast("v=", "").takeIf { it.isNotEmpty() } ?: u.substringAfterLast("/", "").takeIf { it.isNotEmpty() && it.length == 11 } }
        return RecognitionResult(tr.key ?: tagid ?: "", tr.title ?: "", tr.subtitle ?: "", album, tr.images?.coverart, tr.images?.coverarthq, tr.genres?.primary, released, label, lyrics, tr.url, apple?.uri, spotify?.actions?.firstOrNull()?.uri, tr.isrc, vid)
    }

    private class JobReq(val id: Long, val signature: String, val sampleDurationMs: Long) {
        @Volatile private var out: Result<RecognitionResult>? = null
        @Volatile private var done = false
        suspend fun await(): Result<RecognitionResult> { while (!done) delay(50); return out ?: Result.failure(Exception("Result not received")) }
        fun done(r: Result<RecognitionResult>) { out = r; done = true }
    }
    private data class CacheEnt(val ts: Long, val res: RecognitionResult)
}

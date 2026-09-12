package com.example.tsuki.ui.player.canvas

import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import kotlin.coroutines.resume

object CanvasDiskCache {

    private const val MAX_BYTES = 256L * 1024 * 1024

    private lateinit var cacheDir: File

    private val downloadClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(java.time.Duration.ofSeconds(12))
            .readTimeout(java.time.Duration.ofSeconds(45))
            .callTimeout(java.time.Duration.ofMinutes(3))
            .build()
    }

    fun init(context: android.content.Context) {
        cacheDir = File(context.cacheDir, "canvas").apply { mkdirs() }
    }

    private fun targetFile(mediaId: String, url: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$mediaId|$url".toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(cacheDir, "${mediaId.take(12)}-${digest.take(24)}.mp4")
    }

    fun getCachedFile(mediaId: String, url: String): File? {
        if (!::cacheDir.isInitialized || url.isBlank()) return null
        val file = targetFile(mediaId, url)
        if (!file.exists() || file.length() < 4096L) return null
        return file.takeIf { isValidVideo(it) }
    }

    suspend fun ensureDownloaded(mediaId: String, url: String): File? = withContext(Dispatchers.IO) {
        if (!::cacheDir.isInitialized || url.isBlank()) return@withContext null
        if (url.contains(".m3u8")) return@withContext null
        val file = targetFile(mediaId, url)
        if (file.exists() && file.length() > 4096L && isValidVideo(file)) return@withContext file

        var attempt = 0
        val part = File(file.parentFile, file.name + ".part")
        while (attempt < 4) {
            attempt++
            try {
                val existingBytes = if (part.exists()) part.length() else 0L
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .apply {
                        if (existingBytes > 0) header("Range", "bytes=$existingBytes-")
                    }
                    .build()
                val completed = suspendCancellableCoroutine { continuation ->
                    val call = downloadClient.newCall(request)
                    continuation.invokeOnCancellation { call.cancel() }
                    call.enqueue(object : okhttp3.Callback {
                        override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                            continuation.resume(false)
                        }

                        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                            response.use { res ->
                                if (existingBytes > 0 && res.code == 416) {
                                    continuation.resume(true)
                                    return
                                }
                                val contentType = res.header("Content-Type") ?: ""
                                if (contentType.contains("mpegurl") || contentType.contains("text/") ||
                                    contentType.contains("image/") || contentType.contains("json")
                                ) {
                                    continuation.resume(false)
                                    return
                                }
                                try {
                                    val body = res.body ?: run { continuation.resume(false); return }
                                    val output = java.io.FileOutputStream(part, existingBytes > 0 && res.code == 206)
                                    body.byteStream().use { input ->
                                        output.use { out -> input.copyTo(out, 65536) }
                                    }
                                    continuation.resume(true)
                                } catch (_: Exception) {
                                    continuation.resume(false)
                                }
                            }
                        }
                    })
                }
                if (completed && part.exists() && part.length() > 4096L && isValidVideo(part)) {
                    if (!part.renameTo(file)) {
                        part.copyTo(file, overwrite = true)
                        part.delete()
                    }
                    trim()
                    return@withContext file
                }
                if (!completed && !part.exists()) continue
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("CanvasDiskCache", "download failed attempt $attempt: ${e.message}")
            }
            Thread.sleep(750L * attempt)
        }
        part.delete()
        null
    }

    private fun isValidVideo(file: File): Boolean {
        return try {
            val extractor = MediaExtractor()
            extractor.setDataSource(file.absolutePath)
            var found = false
            for (i in 0 until extractor.trackCount) {
                val format: MediaFormat = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/")) {
                    val decoderList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
                    found = decoderList.findDecoderForFormat(format) != null
                    break
                }
            }
            extractor.release()
            found
        } catch (_: Exception) {
            false
        }
    }

    private fun trim() {
        if (!::cacheDir.isInitialized) return
        val files = cacheDir.listFiles()?.filter { it.isFile } ?: return
        var total = files.sumOf { it.length() }
        if (total <= MAX_BYTES) return
        files.sortedBy { it.lastModified() }.forEach { file ->
            if (total <= MAX_BYTES) return
            total -= file.length()
            file.delete()
        }
    }

    fun clearAll() {
        if (!::cacheDir.isInitialized) return
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    fun sizeBytes(): Long =
        if (::cacheDir.isInitialized) cacheDir.listFiles()?.sumOf { it.length() } ?: 0L else 0L
}

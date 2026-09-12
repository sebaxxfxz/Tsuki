package com.example.tsuki.playback

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.domain.model.MediaType
import com.example.tsuki.network.YouTubeExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

class DownloadEngine(private val context: Context) {

    companion object {
        private const val TAG = "DownloadEngine"

        @Volatile
        private var instance: DownloadEngine? = null

        fun getInstance(context: Context): DownloadEngine =
            instance ?: synchronized(this) {
                instance ?: DownloadEngine(context.applicationContext).also { instance = it }
            }
    }

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
    private val _offlineTracks = kotlinx.coroutines.flow.MutableStateFlow<List<MediaTrack>>(emptyList())
    val offlineTracks: kotlinx.coroutines.flow.StateFlow<List<MediaTrack>> = _offlineTracks
    private val downloadedIds = java.util.concurrent.ConcurrentHashMap<String, Boolean>()
    private val activeDownloads = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Job>()

    init {
        scope.launch {
            val tracks = getDownloadedTracks()
            downloadedIds.clear()
            tracks.forEach { downloadedIds[it.videoId ?: it.id] = true }
            _offlineTracks.value = tracks
        }
    }

    fun refresh() {
        scope.launch {
            val tracks = getDownloadedTracks()
            downloadedIds.clear()
            tracks.forEach { downloadedIds[it.videoId ?: it.id] = true }
            _offlineTracks.value = tracks
        }
    }

    private val youtubeExtractor = YouTubeExtractor()
    private val httpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .callTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
        .dispatcher(okhttp3.Dispatcher().apply { maxRequests = 8; maxRequestsPerHost = 6 })
        .build()

    private suspend fun downloadUrlFast(url: String, targetFile: File, onProgress: (Int) -> Unit = {}): Boolean {
        val partFile = File(targetFile.parentFile, "${targetFile.name}.part")
        if (tryParallelSegments(url, partFile, onProgress)) return true
        return downloadToFile(url, targetFile, onProgress)
    }

    private suspend fun tryParallelSegments(url: String, partFile: File, onProgress: (Int) -> Unit): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val total = probeContentLength(url) ?: return@withContext false
            if (total < 2L * 1024L * 1024L) return@withContext false
            val segments = 4
            val segFiles = (0 until segments).map { File(partFile.parentFile, "${partFile.name}.seg$it") }
            segFiles.forEach { runCatching { it.delete() } }
            val totalRead = java.util.concurrent.atomic.AtomicLong(0L)
            val lastPercent = java.util.concurrent.atomic.AtomicInteger(-1)
            val sizePer = total / segments
            try {
                kotlinx.coroutines.coroutineScope {
                    (0 until segments).map { i ->
                        async(Dispatchers.IO) {
                            val start = i * sizePer
                            val end = if (i == segments - 1) total - 1 else (i + 1) * sizePer - 1
                            val req = Request.Builder().url(url)
                                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                                .addHeader("Range", "bytes=$start-$end").build()
                            httpClient.newCall(req).execute().use { resp ->
                                if (resp.code != 206) throw java.io.IOException("range unsupported:${resp.code}")
                                val b = resp.body ?: throw java.io.IOException("empty segment")
                                b.byteStream().use { input ->
                                    java.io.BufferedOutputStream(java.io.FileOutputStream(segFiles[i])).use { out ->
                                        val buf = ByteArray(128 * 1024)
                                        var n: Int
                                        while (input.read(buf).also { n = it } != -1) {
                                            out.write(buf, 0, n)
                                            val read = totalRead.addAndGet(n.toLong())
                                            val pct = ((read * 100) / total).toInt().coerceIn(0, 100)
                                            val prev = lastPercent.getAndSet(pct)
                                            if (prev < pct) onProgress(pct)
                                        }
                                        out.flush()
                                    }
                                }
                            }
                        }
                    }.forEach { it.await() }
                }
            } catch (_: Exception) {
                segFiles.forEach { runCatching { it.delete() } }
                return@withContext false
            }
            try {
                java.io.BufferedOutputStream(java.io.FileOutputStream(partFile)).use { out ->
                    val buf = ByteArray(128 * 1024)
                    segFiles.forEach { seg ->
                        seg.inputStream().use { input ->
                            var n: Int
                            while (input.read(buf).also { n = it } != -1) out.write(buf, 0, n)
                        }
                    }
                    out.flush()
                }
            } finally {
                segFiles.forEach { runCatching { it.delete() } }
            }
            if (!partFile.exists() || partFile.length() < total) {
                runCatching { partFile.delete() }
                return@withContext false
            }
            onProgress(100)
            true
        } catch (_: Exception) { false }
    }

    private fun probeContentLength(url: String): Long? {
        return try {
            val req = Request.Builder().url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .addHeader("Range", "bytes=0-0").build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.code != 206) return null
                val cr = resp.header("Content-Range") ?: return null
                cr.substringAfter("/").toLongOrNull()
            }
        } catch (_: Exception) { null }
    }

    @kotlinx.serialization.Serializable
    private data class DownloadMeta(
        val title: String,
        val artist: String,
        val artworkUrl: String? = null
    )

    private val metaJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    private val downloadDir: File
        get() = File(context.filesDir, "tsuki_offline_downloads").also {
            if (!it.exists()) it.mkdirs()
        }

    private val legacyDownloadDir: File
        get() = File(context.getExternalFilesDir(null) ?: context.filesDir, "tsuki_offline_downloads")

    private fun metaFile(videoId: String) = File(downloadDir, "$videoId.meta.json")

    private fun legacyMetaFile(videoId: String) = File(legacyDownloadDir, "$videoId.meta.json")

    private fun writeMeta(track: MediaTrack, videoId: String) {
        try {
            metaFile(videoId).writeText(
                metaJson.encodeToString(
                    DownloadMeta.serializer(),
                    DownloadMeta(track.title, track.artist, track.artworkUrl)
                )
            )
        } catch (_: Exception) {}
    }

    private fun readMeta(videoId: String): DownloadMeta? = try {
        (metaFile(videoId).takeIf { it.exists() } ?: legacyMetaFile(videoId).takeIf { it.exists() })?.let {
            metaJson.decodeFromString<DownloadMeta>(it.readText())
        }
    } catch (_: Exception) { null }

    suspend fun downloadTrack(
        track: MediaTrack,
        onProgress: (Int) -> Unit = {}
    ): MediaTrack? = withContext(Dispatchers.IO) {
        val videoId = track.videoId ?: track.id
        if (videoId.isBlank()) return@withContext null
        File(downloadDir, "$videoId.m4a").takeIf { it.exists() && it.length() > 0 }?.let {
            downloadedIds[videoId] = true
            return@withContext track.copy(isLocal = true, streamUrl = android.net.Uri.fromFile(it).toString(), mediaType = MediaType.LOCAL_AUDIO, isVideoItem = false)
        }
        File(downloadDir, "$videoId.webm").takeIf { it.exists() && it.length() > 0 }?.let {
            downloadedIds[videoId] = true
            return@withContext track.copy(isLocal = true, streamUrl = android.net.Uri.fromFile(it).toString(), mediaType = MediaType.LOCAL_AUDIO, isVideoItem = false)
        }
        val currentJob = coroutineContext[kotlinx.coroutines.Job]
        if (currentJob != null) {
            val busy = activeDownloads.putIfAbsent(videoId, currentJob)
            if (busy != null) {
                try {
                    kotlinx.coroutines.withTimeoutOrNull(60_000L) { busy.join() }
                } catch (_: Exception) {}
                val doneFile = File(downloadDir, "$videoId.m4a").takeIf { it.exists() && it.length() > 0 }
                    ?: File(downloadDir, "$videoId.webm").takeIf { it.exists() && it.length() > 0 }
                if (doneFile != null) {
                    downloadedIds[videoId] = true
                    return@withContext track.copy(isLocal = true, streamUrl = android.net.Uri.fromFile(doneFile).toString(), mediaType = MediaType.LOCAL_AUDIO, isVideoItem = false)
                }
                return@withContext null
            }
        }
        try {
            val detailed = try {
                kotlinx.coroutines.withTimeoutOrNull(30_000L) { youtubeExtractor.getStreamUrlsDetailed(videoId) }
            } catch (_: Exception) { null } ?: return@withContext null


            val downloadUrl = detailed.audioUrl ?: detailed.aacAudioUrl ?: detailed.videoUrl
                ?: return@withContext null
            val ext = if (
                downloadUrl.contains("audio%2Fwebm") || downloadUrl.contains("audio/webm") ||
                downloadUrl.contains("codec%3Dopus") || downloadUrl.contains("codec=opus")
            ) "webm" else "m4a"
            val targetFile = File(downloadDir, "$videoId.$ext")
            val partFile = File(downloadDir, "$videoId.$ext.part")

            if (!downloadUrlFast(downloadUrl, targetFile, onProgress)) return@withContext null

            if (!partFile.renameTo(targetFile)) {
                Log.e(TAG, "Could not finalize audio download for $videoId")
                partFile.delete()
                return@withContext null
            }

            writeMeta(track, videoId)
            downloadedIds[videoId] = true
            refresh()

            return@withContext track.copy(
                isLocal = true,
                streamUrl = android.net.Uri.fromFile(targetFile).toString(),
                mediaType = MediaType.LOCAL_AUDIO,
                isVideoItem = false
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            File(downloadDir, "$videoId.m4a.part").delete()
            File(downloadDir, "$videoId.webm.part").delete()
            File(downloadDir, "$videoId.tmp").delete()
            activeDownloads.remove(videoId)
            throw e
        } catch (e: Exception) {
            val fallbackId = track.videoId ?: track.id
            Log.e(TAG, "downloadTrack failed for $fallbackId", e)
            File(downloadDir, "$fallbackId.m4a.part").delete()
            File(downloadDir, "$fallbackId.webm.part").delete()
            File(downloadDir, "$fallbackId.tmp").delete()
            activeDownloads.remove(videoId)
            return@withContext null
        } finally {
            activeDownloads.remove(videoId)
        }
    }

    suspend fun downloadVideo(
        track: MediaTrack,
        quality: String = "720p",
        onProgress: (Int) -> Unit = {}
    ): MediaTrack? = withContext(Dispatchers.IO) {
        val videoId = track.videoId ?: track.id
        if (videoId.isBlank()) return@withContext null
        File(downloadDir, "$videoId.mp4").takeIf { it.exists() && it.length() > 0 }?.let {
            downloadedIds[videoId] = true
            return@withContext track.copy(isLocal = true, streamUrl = android.net.Uri.fromFile(it).toString(), mediaType = MediaType.STREAM_VIDEO, isVideoItem = true)
        }
        val currentJob = coroutineContext[kotlinx.coroutines.Job]
        if (currentJob != null) {
            val busy = activeDownloads.putIfAbsent(videoId, currentJob)
            if (busy != null) {
                try {
                    kotlinx.coroutines.withTimeoutOrNull(90_000L) { busy.join() }
                } catch (_: Exception) {}
                File(downloadDir, "$videoId.mp4").takeIf { it.exists() && it.length() > 0 }?.let {
                    downloadedIds[videoId] = true
                    return@withContext track.copy(isLocal = true, streamUrl = android.net.Uri.fromFile(it).toString(), mediaType = MediaType.STREAM_VIDEO, isVideoItem = true)
                }
                return@withContext null
            }
        }
        try {
            val streams = try {
                kotlinx.coroutines.withTimeoutOrNull(30_000L) { youtubeExtractor.getStreamUrlsDetailed(videoId) }
            } catch (_: Exception) { null } ?: return@withContext null
            val targetFile = File(downloadDir, "$videoId.mp4")

            val qualityMatch = streams?.availableQualities?.firstOrNull { it.label.contains(quality, ignoreCase = true) }?.url
            val progressiveUrl = streams?.progressiveVideoUrl
            val videoUrl = qualityMatch ?: streams?.videoUrl
            val audioUrl = streams?.aacAudioUrl ?: streams?.audioUrl

            if (progressiveUrl != null) {
                val downloaded = downloadToFile(progressiveUrl, targetFile, onProgress)
                if (downloaded) {
                    writeMeta(track, videoId)
                    downloadedIds[videoId] = true
                    refresh()
                    return@withContext track.copy(
                        isLocal = true,
                        streamUrl = android.net.Uri.fromFile(targetFile).toString(),
                        mediaType = MediaType.STREAM_VIDEO,
                        isVideoItem = true
                    )
                }
            }

            if (videoUrl != null && audioUrl != null && videoUrl != audioUrl) {
                val tempVideo = File(downloadDir, "$videoId.video.tmp")
                val tempAudio = File(downloadDir, "$videoId.audio.tmp")

                val videoOk = downloadToFile(videoUrl, tempVideo) { percent ->
                    onProgress((percent * 0.7f).toInt())
                }
                val audioOk = downloadToFile(audioUrl, tempAudio) { percent ->
                    onProgress(70 + (percent * 0.2f).toInt())
                }

                if (videoOk && audioOk) {
                    onProgress(92)
                    val muxSuccess = muxAudioVideo(tempVideo, tempAudio, targetFile)
                    tempVideo.delete()
                    tempAudio.delete()
                    if (muxSuccess) {
                        onProgress(100)
                        writeMeta(track, videoId)
                        downloadedIds[videoId] = true
                        refresh()
                        return@withContext track.copy(
                            isLocal = true,
                            streamUrl = android.net.Uri.fromFile(targetFile).toString(),
                            mediaType = MediaType.STREAM_VIDEO,
                            isVideoItem = true
                        )
                    } else {
                        targetFile.delete()
                    }
                } else {
                    tempVideo.delete()
                    tempAudio.delete()
                }
            }

            targetFile.delete()
            File(downloadDir, "$videoId.video.tmp").delete()
            File(downloadDir, "$videoId.audio.tmp").delete()
            File(downloadDir, "$videoId.mp4.part").delete()
            return@withContext null
        } catch (e: kotlinx.coroutines.CancellationException) {
            val cancelId = track.videoId ?: track.id
            File(downloadDir, "$cancelId.mp4.part").delete()
            File(downloadDir, "$cancelId.video.tmp").delete()
            File(downloadDir, "$cancelId.audio.tmp").delete()
            activeDownloads.remove(cancelId)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "downloadVideo failed for ${track.videoId ?: track.id}", e)
            val failId = track.videoId ?: track.id
            File(downloadDir, "$failId.mp4.part").delete()
            File(downloadDir, "$failId.video.tmp").delete()
            File(downloadDir, "$failId.audio.tmp").delete()
            activeDownloads.remove(failId)
            return@withContext null
        } finally {
            activeDownloads.remove(track.videoId ?: track.id)
        }
    }

    private fun downloadToFile(url: String, file: File, onProgress: (Int) -> Unit = {}): Boolean {
        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            return httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use false
                val body = response.body ?: return@use false
                val contentLength = body.contentLength()
                val partFile = File(file.parentFile, "${file.name}.part")
                body.byteStream().use { input ->
                    java.io.BufferedOutputStream(FileOutputStream(partFile)).use { output ->
                        val buffer = ByteArray(128 * 1024)
                        var bytesRead: Int
                        var totalRead = 0L
                        var lastPercent = -1
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (contentLength > 0) {
                                val percent = ((totalRead * 100) / contentLength).toInt().coerceIn(0, 100)
                                if (percent != lastPercent) {
                                    lastPercent = percent
                                    onProgress(percent)
                                }
                            }
                        }
                        output.flush()
                        if (contentLength > 0 && totalRead < contentLength) {
                            partFile.delete()
                            return@use false
                        }
                        if (contentLength <= 0 && totalRead <= 0) {
                            partFile.delete()
                            return@use false
                        }
                    }
                }
                if (!partFile.renameTo(file)) {
                    Log.e(TAG, "Could not finalize download to ${file.name}")
                    partFile.delete()
                    return@use false
                }
                return@use true
            }
        } catch (e: Exception) {
            Log.e(TAG, "downloadToFile failed for $url", e)
            File(file.parentFile, "${file.name}.part").delete()
            return false
        }
    }

    @SuppressLint("WrongConstant")
    private fun muxAudioVideo(videoFile: File, audioFile: File, outputFile: File): Boolean {
        val muxOutput = File(outputFile.parentFile, "${outputFile.name}.mux.tmp")
        var muxer: MediaMuxer? = null
        var videoExtractor: MediaExtractor? = null
        var audioExtractor: MediaExtractor? = null
        try {
            videoExtractor = MediaExtractor().apply { setDataSource(videoFile.absolutePath) }
            audioExtractor = MediaExtractor().apply { setDataSource(audioFile.absolutePath) }

            var videoTrackIndex = -1
            var videoFormat: MediaFormat? = null
            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    videoTrackIndex = i
                    videoFormat = format
                    break
                }
            }

            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until audioExtractor.trackCount) {
                val format = audioExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (videoTrackIndex == -1 || videoFormat == null) return false

            val videoMime = videoFormat.getString(MediaFormat.KEY_MIME) ?: ""
            val audioMime = if (audioTrackIndex != -1 && audioFormat != null) audioFormat.getString(MediaFormat.KEY_MIME) ?: "" else ""

            if (videoMime.contains("webm", true) || videoMime.contains("vp9", true) || videoMime.contains("vp8", true) ||
                audioMime.contains("opus", true) || audioMime.contains("vorbis", true)) {
                return false
            }

            if (muxOutput.exists()) muxOutput.delete()
            if (outputFile.exists()) outputFile.delete()
            muxer = try {
                MediaMuxer(muxOutput.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            } catch (e: Exception) {
                Log.e(TAG, "MediaMuxer init failed", e)
                return false
            }
            val muxerVideoTrack = try {
                muxer.addTrack(videoFormat)
            } catch (e: Exception) {
                Log.e(TAG, "muxer add video track failed", e)
                return false
            }
            val muxerAudioTrack = if (audioTrackIndex != -1 && audioFormat != null) {
                try {
                    muxer.addTrack(audioFormat)
                } catch (e: Exception) {
                    Log.e(TAG, "muxer add audio track failed", e)
                    -1
                }
            } else -1

            muxer.start()

            val videoMax = if (videoFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) videoFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) else 1024 * 1024
            val audioMax = if (audioFormat != null && audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) else 512 * 1024
            val buffer = ByteBuffer.allocateDirect(maxOf(videoMax, audioMax, 1024 * 1024))
            val bufferInfo = MediaCodec.BufferInfo()

            videoExtractor.selectTrack(videoTrackIndex)
            if (muxerAudioTrack != -1 && audioTrackIndex != -1) {
                audioExtractor.selectTrack(audioTrackIndex)
            }

            var videoDone = false
            var audioDone = (muxerAudioTrack == -1)
            var lastVideoPts = 0L
            var lastAudioPts = 0L

            while (!videoDone || !audioDone) {
                val videoTime = if (!videoDone) videoExtractor.sampleTime else Long.MAX_VALUE
                val audioTime = if (!audioDone) audioExtractor.sampleTime else Long.MAX_VALUE

                if (!videoDone && (audioDone || videoTime <= audioTime)) {
                    bufferInfo.offset = 0
                    bufferInfo.size = videoExtractor.readSampleData(buffer, 0)
                    if (bufferInfo.size < 0) {
                        videoDone = true
                    } else {
                        val pts = if (videoTime >= 0L) maxOf(lastVideoPts, videoTime) else lastVideoPts
                        lastVideoPts = pts
                        bufferInfo.presentationTimeUs = pts
                        bufferInfo.flags = videoExtractor.sampleFlags
                        muxer.writeSampleData(muxerVideoTrack, buffer, bufferInfo)
                        videoExtractor.advance()
                    }
                } else if (!audioDone) {
                    bufferInfo.offset = 0
                    bufferInfo.size = audioExtractor.readSampleData(buffer, 0)
                    if (bufferInfo.size < 0) {
                        audioDone = true
                    } else {
                        val pts = if (audioTime >= 0L) maxOf(lastAudioPts, audioTime) else lastAudioPts
                        lastAudioPts = pts
                        bufferInfo.presentationTimeUs = pts
                        bufferInfo.flags = audioExtractor.sampleFlags
                        muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)
                        audioExtractor.advance()
                    }
                }
            }

            muxer.stop()
            if (outputFile.exists()) outputFile.delete()
            if (!muxOutput.renameTo(outputFile)) {
                Log.e(TAG, "Could not finalize mux output to ${outputFile.name}")
                muxOutput.delete()
                return false
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "muxAudioVideo failed", e)
            if (muxOutput.exists()) muxOutput.delete()
            return false
        } finally {
            try { muxer?.release() } catch (_: Exception) {}
            try { videoExtractor?.release() } catch (_: Exception) {}
            try { audioExtractor?.release() } catch (_: Exception) {}
        }
    }

    fun isDownloaded(videoId: String): Boolean {
        if (videoId.isBlank()) return false
        if (downloadedIds.containsKey(videoId)) return true
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) return false
        if (legacyDownloadDir.absolutePath != downloadDir.absolutePath) {
            val legacyFiles = legacyDownloadDir.listFiles()
            if (legacyFiles != null && legacyFiles.any { it.nameWithoutExtension == videoId && it.length() > 0 }) return true
        }
        val files = downloadDir.listFiles() ?: return false
        return files.any { it.nameWithoutExtension == videoId && it.length() > 0 }
    }

    fun getDownloadedTracks(): List<MediaTrack> {
        val primary = downloadDir.listFiles()?.toList().orEmpty()
        val legacy = if (legacyDownloadDir.absolutePath != downloadDir.absolutePath) legacyDownloadDir.listFiles()?.toList().orEmpty() else emptyList()
        val allFiles = (primary + legacy).distinctBy { it.absolutePath }
        if (allFiles.isEmpty()) return emptyList()
        return allFiles.mapNotNull { file ->
            val videoId = file.nameWithoutExtension
            val meta = readMeta(videoId)
            val fileUri = android.net.Uri.fromFile(file).toString()
            if (file.extension == "m4a" || file.extension == "webm") {
                MediaTrack(
                    id = videoId,
                    title = meta?.title ?: "Descargado • $videoId",
                    artist = meta?.artist ?: "TSuki Offline",
                    artworkUrl = meta?.artworkUrl ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                    isLocal = true,
                    streamUrl = fileUri,
                    mediaType = MediaType.LOCAL_AUDIO,
                    videoId = videoId
                )
            } else if (file.extension == "mp4") {
                MediaTrack(
                    id = videoId,
                    title = meta?.title ?: "Descargado • $videoId",
                    artist = meta?.artist ?: "TSuki Offline",
                    artworkUrl = meta?.artworkUrl ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                    isLocal = true,
                    streamUrl = fileUri,
                    mediaType = MediaType.STREAM_VIDEO,
                    videoId = videoId,
                    isVideoItem = true
                )
            } else null
        }.distinctBy { "${it.videoId ?: it.id}_${it.isVideoItem}" }
    }

    fun deleteDownloadedTrack(videoId: String, onlyVideo: Boolean? = null): Boolean {
        if (videoId.isBlank()) return false
        var deleted = false
        val exts = when (onlyVideo) {
            true -> listOf("mp4")
            false -> listOf("m4a", "webm", "opus", "mp3")
            null -> listOf("m4a", "webm", "opus", "mp3", "mp4")
        }
        exts.forEach { ext ->
            val f1 = File(downloadDir, "$videoId.$ext")
            if (f1.exists()) { f1.delete(); deleted = true }
            val f2 = File(legacyDownloadDir, "$videoId.$ext")
            if (f2.exists()) { f2.delete(); deleted = true }
            File(downloadDir, "$videoId.$ext.part").delete()
            File(downloadDir, "$videoId.video.tmp").delete()
            File(downloadDir, "$videoId.audio.tmp").delete()
        }
        val stillExists = downloadDir.listFiles()?.any { it.nameWithoutExtension == videoId && it.length() > 0 } == true ||
            legacyDownloadDir.listFiles()?.any { it.nameWithoutExtension == videoId && it.length() > 0 } == true
        if (!stillExists) {
            metaFile(videoId).delete()
            legacyMetaFile(videoId).delete()
            downloadedIds.remove(videoId)
        }
        refresh()
        return deleted
    }

    fun getDownloadStorageSizeMb(): Float {
        var totalBytes = 0L
        downloadDir.listFiles()?.forEach { totalBytes += it.length() }
        legacyDownloadDir.listFiles()?.forEach { totalBytes += it.length() }
        return totalBytes / (1024f * 1024f)
    }

    data class DownloadBatchResult(
        val success: Int,
        val failed: Int,
        val skipped: Int
    )

    suspend fun downloadTracksBatch(
        tracks: List<MediaTrack>,
        onProgress: (current: Int, total: Int, trackTitle: String) -> Unit = { _, _, _ -> }
    ): DownloadBatchResult = withContext(Dispatchers.IO) {
        val pending = tracks.filterNot { isDownloaded(it.videoId ?: it.id) }
        val skipped = tracks.size - pending.size
        var success = 0
        var failed = 0
        try {
            pending.forEachIndexed { index, track ->
                onProgress(index + 1, pending.size, track.title)
                val res = try {
                    downloadTrack(track)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
                if (res != null) success++ else failed++
            }
        } finally {
            refresh()
        }
        DownloadBatchResult(success, failed, skipped)
    }
}

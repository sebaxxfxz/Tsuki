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

    init {
        scope.launch { _offlineTracks.value = getDownloadedTracks() }
    }

    fun refresh() {
        scope.launch { _offlineTracks.value = getDownloadedTracks() }
    }

    private val youtubeExtractor = YouTubeExtractor()
    private val httpClient = OkHttpClient()

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
        try {
            val videoId = track.videoId ?: track.id
            val detailed = youtubeExtractor.getStreamUrlsDetailed(videoId)


            val downloadUrl = detailed.audioUrl ?: detailed.aacAudioUrl ?: detailed.videoUrl
                ?: return@withContext null
            val ext = if (
                downloadUrl.contains("audio%2Fwebm") || downloadUrl.contains("audio/webm") ||
                downloadUrl.contains("codec%3Dopus") || downloadUrl.contains("codec=opus")
            ) "webm" else "m4a"
            val targetFile = File(downloadDir, "$videoId.$ext")
            val partFile = File(downloadDir, "$videoId.$ext.part")

            val request = Request.Builder()
                .url(downloadUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext null
                }
                val body = response.body ?: return@withContext null
                val contentLength = body.contentLength()

                body.byteStream().use { input ->
                    FileOutputStream(partFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var totalRead = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (contentLength > 0) {
                                val percent = ((totalRead * 100) / contentLength).toInt()
                                onProgress(percent)
                            }
                        }
                        output.flush()
                    }
                }
            }

            if (!partFile.renameTo(targetFile)) {
                Log.e(TAG, "Could not finalize audio download for $videoId")
                partFile.delete()
                return@withContext null
            }

            writeMeta(track, videoId)
            refresh()

            return@withContext track.copy(
                isLocal = true,
                streamUrl = android.net.Uri.fromFile(targetFile).toString(),
                mediaType = MediaType.LOCAL_AUDIO,
                isVideoItem = false
            )
        } catch (e: Exception) {
            val fallbackId = track.videoId ?: track.id
            Log.e(TAG, "downloadTrack failed for $fallbackId", e)
            File(downloadDir, "$fallbackId.m4a.part").delete()
            File(downloadDir, "$fallbackId.webm.part").delete()
            return@withContext null
        }
    }

    suspend fun downloadVideo(
        track: MediaTrack,
        onProgress: (Int) -> Unit = {}
    ): MediaTrack? = withContext(Dispatchers.IO) {
        try {
            val videoId = track.videoId ?: track.id
            val streams = youtubeExtractor.getStreamUrlsDetailed(videoId)
            val targetFile = File(downloadDir, "$videoId.mp4")

            val progressiveUrl = streams?.progressiveVideoUrl
            val videoUrl = streams?.videoUrl
            val audioUrl = streams?.aacAudioUrl ?: streams?.audioUrl

            if (progressiveUrl != null) {
                val downloaded = downloadToFile(progressiveUrl, targetFile, onProgress)
                if (downloaded) {
                    writeMeta(track, videoId)
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

            val fallbackUrl = progressiveUrl ?: videoUrl ?: audioUrl ?: return@withContext null
            val fallbackSuccess = downloadToFile(fallbackUrl, targetFile, onProgress)
            if (fallbackSuccess) {
                writeMeta(track, videoId)
                refresh()
                return@withContext track.copy(
                    isLocal = true,
                    streamUrl = android.net.Uri.fromFile(targetFile).toString(),
                    mediaType = MediaType.STREAM_VIDEO,
                    isVideoItem = true
                )
            }
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "downloadVideo failed for ${track.videoId ?: track.id}", e)
            return@withContext null
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
                    FileOutputStream(partFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var totalRead = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (contentLength > 0) {
                                val percent = ((totalRead * 100) / contentLength).toInt().coerceIn(0, 100)
                                onProgress(percent)
                            }
                        }
                        output.flush()
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

            if (outputFile.exists()) outputFile.delete()
            muxer = try {
                MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
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

            while (!videoDone || !audioDone) {
                val videoTime = if (!videoDone) videoExtractor.sampleTime else Long.MAX_VALUE
                val audioTime = if (!audioDone) audioExtractor.sampleTime else Long.MAX_VALUE

                if (!videoDone && (audioDone || videoTime <= audioTime)) {
                    bufferInfo.offset = 0
                    bufferInfo.size = videoExtractor.readSampleData(buffer, 0)
                    if (bufferInfo.size < 0) {
                        videoDone = true
                    } else {
                        bufferInfo.presentationTimeUs = videoTime
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
                        bufferInfo.presentationTimeUs = audioTime
                        bufferInfo.flags = audioExtractor.sampleFlags
                        muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)
                        audioExtractor.advance()
                    }
                }
            }

            muxer.stop()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "muxAudioVideo failed", e)
            if (outputFile.exists()) outputFile.delete()
            return false
        } finally {
            try { muxer?.release() } catch (_: Exception) {}
            try { videoExtractor?.release() } catch (_: Exception) {}
            try { audioExtractor?.release() } catch (_: Exception) {}
        }
    }

    fun isDownloaded(videoId: String): Boolean {
        return File(downloadDir, "$videoId.m4a").exists() || File(downloadDir, "$videoId.mp4").exists() || File(downloadDir, "$videoId.webm").exists() ||
            File(legacyDownloadDir, "$videoId.m4a").exists() || File(legacyDownloadDir, "$videoId.mp4").exists() || File(legacyDownloadDir, "$videoId.webm").exists()
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
        }
    }
}

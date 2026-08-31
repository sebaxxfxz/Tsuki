package com.example.tsuki.shazam

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RecognitionOutcome(
    val title: String?,
    val artist: String?,
    val album: String?,
    val coverArtUrl: String?,
    val shazamUrl: String?
) {
    val isMatch: Boolean get() = !title.isNullOrBlank()
}

object MusicRecognizer {

    @SuppressLint("MissingPermission")
    suspend fun recognizeFromMic(
        maxDurationMs: Long = 8000L,
        onProgress: (Int) -> Unit = {}
    ): Result<RecognitionOutcome> = withContext(Dispatchers.IO) {
        try {
            val sampleRate = 16000
            val minBuf = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuf <= 0) {
                return@withContext Result.failure(IllegalStateException("Formato de micrófono no compatible"))
            }
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuf, sampleRate * 2)
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                runCatching { record.release() }
                return@withContext Result.failure(IllegalStateException("Micrófono no disponible"))
            }

            record.startRecording()
            val generator = ShazamSignatureGenerator()
            val buffer = ShortArray(sampleRate / 10)
            val startAt = System.currentTimeMillis()

            try {
                while (System.currentTimeMillis() - startAt < maxDurationMs) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read > 0) generator.feedPcm16Mono(buffer.copyOf(read))
                    val progress = ((System.currentTimeMillis() - startAt) * 100 / maxDurationMs).toInt()
                    onProgress(progress.coerceIn(5, 95))
                }
            } finally {
                runCatching { record.stop() }
                runCatching { record.release() }
            }

            val signature = generator.nextSignatureOrNull()
            if (signature == null) {
                return@withContext Result.failure(IllegalStateException("Audio insuficiente para reconocer"))
            }

            onProgress(97)
            val recognized: com.example.tsuki.shazam.models.RecognitionResult =
                Shazam.recognize(
                    signature = signature.uri,
                    sampleDurationMs = signature.sampleDurationMs
                ).fold(
                    onSuccess = { it },
                    onFailure = { return@withContext Result.failure(it) }
                )

            if (recognized.trackId.isBlank() || recognized.title.isBlank()) {
                Result.success(RecognitionOutcome(null, null, null, null, null))
            } else {
                Result.success(
                    RecognitionOutcome(
                        title = recognized.title,
                        artist = recognized.artist,
                        album = recognized.album,
                        coverArtUrl = recognized.coverArtUrl ?: recognized.coverArtHqUrl,
                        shazamUrl = recognized.shazamUrl
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

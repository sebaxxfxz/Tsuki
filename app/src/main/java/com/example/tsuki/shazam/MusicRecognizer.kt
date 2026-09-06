package com.example.tsuki.shazam

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

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
        attempts: Int = 2,
        onProgress: (Int) -> Unit = {},
        onLevel: (Float) -> Unit = {},
        onAttempt: (Int) -> Unit = {}
    ): Result<RecognitionOutcome> = withContext(Dispatchers.IO) {
        try {
            val tries = attempts.coerceAtLeast(1)
            var last: Result<RecognitionOutcome> =
                Result.success(RecognitionOutcome(null, null, null, null, null))
            repeat(tries) { attempt ->
                onAttempt(attempt + 1)
                val outcome = captureAndRecognize(maxDurationMs, attempt, tries, onProgress, onLevel)
                val err = outcome.exceptionOrNull()
                if (err is CancellationException) throw err
                if (outcome.isFailure) return@withContext outcome
                last = outcome
                if (outcome.getOrNull()?.isMatch == true) return@withContext outcome
            }
            last
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun captureAndRecognize(
        maxDurationMs: Long,
        attempt: Int,
        tries: Int,
        onProgress: (Int) -> Unit,
        onLevel: (Float) -> Unit
    ): Result<RecognitionOutcome> = withContext(Dispatchers.IO) {
        try {
            val sampleRate = 16000
            val minBuf = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuf <= 0) {
                return@withContext Result.failure(IllegalStateException("FORMAT"))
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
                return@withContext Result.failure(IllegalStateException("MIC"))
            }

            record.startRecording()
            val generator = ShazamSignatureGenerator()
            val buffer = ShortArray(sampleRate / 10)
            val startAt = System.currentTimeMillis()
            var level = 0f
            val span = 90 / tries

            try {
                while (isActive && System.currentTimeMillis() - startAt < maxDurationMs) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read < 0) {
                        return@withContext Result.failure(IllegalStateException("READ:$read"))
                    }
                    if (read > 0) {
                        generator.feedPcm16Mono(buffer.copyOf(read))
                        var sum = 0.0
                        for (i in 0 until read) {
                            val v = buffer[i] / 32768.0
                            sum += v * v
                        }
                        val rms = sqrt(sum / read).toFloat()
                        level = (level * 0.72f + (rms * 4f).coerceIn(0f, 1f) * 0.28f).coerceIn(0f, 1f)
                        onLevel(level)
                    }
                    val elapsed = System.currentTimeMillis() - startAt
                    onProgress((attempt * span + (elapsed * span / maxDurationMs).toInt()).coerceIn(5, 95))
                }
            } finally {
                runCatching { record.stop() }
                runCatching { record.release() }
            }
            coroutineContext.ensureActive()

            val signature = generator.nextSignatureOrNull()
            if (signature == null) {
                return@withContext Result.failure(IllegalStateException("SILENCE"))
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

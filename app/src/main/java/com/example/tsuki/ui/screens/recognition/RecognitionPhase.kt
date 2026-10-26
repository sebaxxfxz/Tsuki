package com.example.tsuki.ui.screens.recognition

import android.content.Context
import com.example.tsuki.R

internal enum class RecognitionPhase {
    IDLE, LISTENING, PROCESSING, RESULT, NOMATCH, ERROR
}

internal enum class LevelBand {
    LOW, OK, HIGH
}

internal const val LevelLowThreshold = 0.14f
internal const val LevelHighThreshold = 0.52f
internal const val LowAudioHoldMs = 1500L

internal fun levelBandOf(level: Float): LevelBand = when {
    level < LevelLowThreshold -> LevelBand.LOW
    level < LevelHighThreshold -> LevelBand.OK
    else -> LevelBand.HIGH
}

internal fun friendlyError(context: Context, e: Throwable): String {
    val msg = e.message.orEmpty()
    return when {
        msg == "FORMAT" || msg == "MIC" -> context.getString(R.string.rec_mic_missing)
        msg.startsWith("READ") -> context.getString(R.string.rec_mic_read)
        msg == "SILENCE" -> context.getString(R.string.rec_quiet)
        e is java.io.IOException || msg.contains("Unable to resolve host", true) || msg.contains("Failed to connect", true) -> context.getString(R.string.rec_offline)
        msg.contains("timeout", true) || msg.contains("timed out", true) -> context.getString(R.string.rec_timeout)
        else -> context.getString(R.string.rec_failed)
    }
}

internal fun relativeTime(context: Context, ts: Long): String {
    val m = (System.currentTimeMillis() - ts) / 60000
    return when {
        m < 1 -> context.getString(R.string.rec_time_now)
        m < 60 -> context.getString(R.string.rec_time_mins, m)
        m < 1440 -> context.getString(R.string.rec_time_hours, m / 60)
        m < 2880 -> context.getString(R.string.rec_time_yesterday)
        else -> java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault()).format(java.util.Date(ts))
    }
}

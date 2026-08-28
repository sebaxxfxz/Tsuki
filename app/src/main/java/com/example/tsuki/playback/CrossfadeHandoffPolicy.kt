package com.example.tsuki.playback

import kotlin.math.cos
import kotlin.math.sin

data class EqualPowerGains(
    val outgoing: Float,
    val incoming: Float
)

const val PREPARE_AHEAD_MS = 8_000L
const val END_GUARD_MS = 300L
const val FRAME_MS = 50L
const val MAX_ALLOWED_DRIFT_MS = 75L

fun needsCorrectiveCrossfadeSeek(
    primaryPositionMs: Long,
    secondaryPositionMs: Long,
    maximumDriftMs: Long = MAX_ALLOWED_DRIFT_MS
): Boolean {
    require(maximumDriftMs >= 0)
    val delta = primaryPositionMs - secondaryPositionMs
    val drift = if (delta >= 0) delta else -delta
    return drift > maximumDriftMs
}

fun hasPlaybackPositionAdvanced(
    positionAfterSeekMs: Long,
    currentPositionMs: Long
): Boolean = currentPositionMs > positionAfterSeekMs

fun equalPowerGains(progress: Float): EqualPowerGains {
    val p = when {
        progress < 0f -> 0f
        progress > 1f -> 1f
        else -> progress
    }
    val angle = p * 1.5707963267948966
    return EqualPowerGains(
        outgoing = cos(angle).toFloat(),
        incoming = sin(angle).toFloat()
    )
}

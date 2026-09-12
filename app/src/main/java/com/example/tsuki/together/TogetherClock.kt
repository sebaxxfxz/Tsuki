package com.example.tsuki.together

import androidx.compose.runtime.Immutable
import kotlin.math.absoluteValue

@Immutable
data class TogetherClockSnapshot(
    val estimatedOffsetMs: Long = 0L,
    val estimatedRttMs: Long = 0L
)

class TogetherClock {
    private var offset: Double = 0.0
    private var rtt: Double = 0.0

    fun onPong(sentAtElapsedMs: Long, receivedAtElapsedMs: Long, serverElapsedMs: Long): TogetherClockSnapshot {
        val measuredRtt = (receivedAtElapsedMs - sentAtElapsedMs).coerceAtLeast(0L)
        val midpoint = sentAtElapsedMs + measuredRtt / 2
        val rawOffset = (serverElapsedMs - midpoint).toDouble()
        val rttWeight = 0.15
        val offsetWeight = if (rawOffset.absoluteValue > 1500) 0.6 else 0.2
        rtt = if (rtt == 0.0) measuredRtt.toDouble() else rtt + (measuredRtt - rtt) * rttWeight
        offset = if (offset == 0.0) rawOffset else offset + (rawOffset - offset) * offsetWeight
        return snapshot()
    }

    fun snapshot(): TogetherClockSnapshot = TogetherClockSnapshot(
        estimatedOffsetMs = offset.toLong(),
        estimatedRttMs = rtt.toLong()
    )
}

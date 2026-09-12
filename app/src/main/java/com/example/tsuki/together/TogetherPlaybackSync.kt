package com.example.tsuki.together

import kotlin.math.abs

internal object TogetherPlaybackSync {
    const val BroadcastIntervalMs = 500L
    private const val OnlineDeliveryEstimateMs = 350L
    private const val MaxAgeMs = 5_000L
    private const val DriftLanMs = 700L
    private const val DriftOnlineMs = 1_200L
    private const val DriftPausedMs = 150L
    private const val EchoWindowMs = 700L

    fun isStaleRoomState(sentAtElapsedRealtimeMs: Long, lastAppliedSentAtElapsedRealtimeMs: Long, force: Boolean): Boolean {
        if (force) return false
        if (sentAtElapsedRealtimeMs <= 0L || lastAppliedSentAtElapsedRealtimeMs <= 0L) return false
        return sentAtElapsedRealtimeMs <= lastAppliedSentAtElapsedRealtimeMs
    }

    fun echoSuppressionUntil(nowElapsedRealtimeMs: Long): Long = nowElapsedRealtimeMs + EchoWindowMs

    fun targetPositionMs(state: TogetherRoomState, isOnlineSession: Boolean, clockSnapshot: TogetherClockSnapshot?, nowElapsedRealtimeMs: Long): Long {
        val base = state.positionMs.coerceAtLeast(0L)
        if (!state.isPlaying) return base
        val extra = if (isOnlineSession) {
            OnlineDeliveryEstimateMs
        } else {
            val corrected = state.sentAtElapsedRealtimeMs + (clockSnapshot?.estimatedOffsetMs ?: 0L)
            (nowElapsedRealtimeMs - corrected).coerceIn(0L, MaxAgeMs)
        }
        return (base + extra).coerceAtLeast(0L)
    }

    fun needsQueueRebuild(desiredHash: String, desiredIds: List<String>, localHash: String, localIds: List<String>): Boolean {
        if (desiredIds.isEmpty()) return false
        return if (desiredHash.isNotBlank()) desiredHash != localHash else desiredIds != localIds
    }

    fun shouldSeekForDrift(currentPositionMs: Long, targetPositionMs: Long, isPlaying: Boolean, isOnlineSession: Boolean): Boolean {
        val limit = when {
            !isPlaying -> DriftPausedMs
            isOnlineSession -> DriftOnlineMs
            else -> DriftLanMs
        }
        return abs(currentPositionMs - targetPositionMs) > limit
    }
}

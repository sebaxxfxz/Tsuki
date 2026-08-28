package com.example.tsuki.ui.player.lyrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import com.example.tsuki.domain.model.LyricsEntry
import com.example.tsuki.domain.model.WordTimestamp
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import kotlin.math.roundToLong

private const val MIN_SYLLABLE_DURATION_MS = 1
private const val LINE_SYNC_MAX_GAP_MS = 3000L
private const val LINE_SYNC_FALLBACK_DURATION_MS = 4000L

const val SMOOTH_POSITION_MAX_FORWARD_DRIFT_MS = 80L
const val SMOOTH_POSITION_MAX_BACKWARD_DRIFT_MS = 900L
const val SMOOTH_POSITION_DRIFT_CORRECTION = 0.55f

fun List<LyricsEntry>.hasWordSyncedLine(): Boolean =
    any { it.hasWordSync && it.time >= 0 }

fun List<LyricsEntry>.toSyncedLyrics(): SyncedLyrics {
    val lines = mutableListOf<ISyncedLine>()

    forEachIndexed { index, entry ->
        if (entry.time < 0L) return@forEachIndexed
        if (entry.text.isBlank() && entry.words.isNullOrEmpty()) return@forEachIndexed

        val words = entry.words
        if (!words.isNullOrEmpty()) {
            val mainWords = words.filter { !it.isBackground }
            val backgroundWords = words.filter { it.isBackground }
            val alignment =
                when (entry.agent?.lowercase()) {
                    "v2" -> KaraokeAlignment.End
                    else -> KaraokeAlignment.Start
                }

            val mainSyllables = mainWords.ifEmpty { words }.toKaraokeSyllables()
            if (mainSyllables.isEmpty()) return@forEachIndexed

            val lineStart = mainSyllables.first().start
            val lineEnd = mainSyllables.last().end
            if (lineEnd <= lineStart) return@forEachIndexed

            lines.add(
                KaraokeLine.MainKaraokeLine(
                    syllables = mainSyllables,
                    translation = null,
                    alignment = alignment,
                    start = lineStart,
                    end = lineEnd,
                    phonetic = null,
                    accompanimentLines = backgroundWords.toAccompanimentLines(alignment),
                ),
            )
        } else {
            val nextEntry = getOrNull(index + 1)
            val lineEnd =
                if (nextEntry != null && nextEntry.time > entry.time) {
                    val gap = nextEntry.time - entry.time
                    if (gap > LINE_SYNC_MAX_GAP_MS) {
                        minOf(nextEntry.time - 1L, entry.time + LINE_SYNC_FALLBACK_DURATION_MS)
                            .coerceAtLeast(entry.time + 1L)
                    } else {
                        (nextEntry.time - 1L).coerceAtLeast(entry.time + 1L)
                    }
                } else {
                    entry.time + LINE_SYNC_FALLBACK_DURATION_MS
                }

            lines.add(
                SyncedLine(
                    content = entry.text,
                    translation = null,
                    start = entry.time.toInt(),
                    end = lineEnd.toInt(),
                ),
            )
        }
    }

    return SyncedLyrics(lines = lines)
}

private fun List<WordTimestamp>.toKaraokeSyllables(): List<KaraokeSyllable> =
    mapIndexed { index, word ->
        val start = word.startTime.toMilliseconds()
        val nextStart = getOrNull(index + 1)?.startTime?.toMilliseconds()
        val rawEnd = word.endTime.toMilliseconds()
        val end = nextStart?.let { minOf(rawEnd, it) } ?: rawEnd
        KaraokeSyllable(
            content = word.text,
            start = start,
            end = end.coerceAtLeast(start + MIN_SYLLABLE_DURATION_MS),
            phonetic = null,
        )
    }

private fun List<WordTimestamp>.toAccompanimentLines(alignment: KaraokeAlignment): List<KaraokeLine.AccompanimentKaraokeLine> {
    if (isEmpty()) return emptyList()
    val syllables = toKaraokeSyllables()
    if (syllables.isEmpty()) return emptyList()
    val start = syllables.first().start
    val end = syllables.last().end
    if (end <= start) return emptyList()
    return listOf(
        KaraokeLine.AccompanimentKaraokeLine(
            syllables = syllables,
            translation = null,
            alignment = alignment,
            start = start,
            end = end,
        ),
    )
}

private fun Double.toMilliseconds(): Int =
    (this * 1000.0).let { value ->
        if (value.isNaN() || value < 0.0) 0 else value.toLong().toInt()
    }

@Composable
fun rememberSmoothPositionMs(
    basePositionMsProvider: () -> Long,
    isPlaying: Boolean,
    playbackSpeed: Float,
): Long = rememberSmoothPositionState(basePositionMsProvider, isPlaying, playbackSpeed).value

@Composable
fun rememberSmoothPositionState(
    basePositionMsProvider: () -> Long,
    isPlaying: Boolean,
    playbackSpeed: Float,
): androidx.compose.runtime.State<Long> {
    val positionState = remember { androidx.compose.runtime.mutableStateOf(0L) }
    val latestProvider = androidx.compose.runtime.rememberUpdatedState(basePositionMsProvider)

    LaunchedEffect(isPlaying, playbackSpeed) {
        var anchorPositionMs = latestProvider.value().coerceAtLeast(0L)
        var anchorFrameNanos = 0L
        positionState.value = anchorPositionMs

        while (true) {
            val rawPositionMs = latestProvider.value().coerceAtLeast(0L)
            if (!isPlaying) {
                anchorPositionMs = rawPositionMs
                anchorFrameNanos = 0L
                positionState.value = rawPositionMs
                kotlinx.coroutines.delay(100)
                continue
            }
            withFrameNanos { frameTimeNanos ->
                if (anchorFrameNanos == 0L) {
                    anchorFrameNanos = frameTimeNanos
                    anchorPositionMs = rawPositionMs
                }
                val elapsedMs = ((frameTimeNanos - anchorFrameNanos) / 1_000_000f) * playbackSpeed
                var projectedPositionMs = anchorPositionMs + elapsedMs.roundToLong()
                val driftMs = rawPositionMs - projectedPositionMs
                projectedPositionMs =
                    when {
                        driftMs > SMOOTH_POSITION_MAX_FORWARD_DRIFT_MS ||
                            driftMs < -SMOOTH_POSITION_MAX_BACKWARD_DRIFT_MS -> {
                            anchorPositionMs = rawPositionMs
                            anchorFrameNanos = frameTimeNanos
                            rawPositionMs
                        }

                        driftMs != 0L ->
                            projectedPositionMs +
                                (driftMs * SMOOTH_POSITION_DRIFT_CORRECTION).roundToLong()

                        else -> projectedPositionMs
                    }
                positionState.value = maxOf(0L, projectedPositionMs)
            }
        }
    }

    return positionState
}

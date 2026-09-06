package com.example.tsuki.ui.player.lyrics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tsuki.domain.model.LyricsEntry
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLyricsView

@Composable
fun KaraokeWordByWordView(
    lyrics: List<LyricsEntry>,
    basePositionMs: () -> Long,
    isPlaying: Boolean,
    playbackSpeed: Float,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    textSizeSp: Int = 30,
    lineBlur: Boolean = true,
    syncOffsetMs: Int = 0
) {
    val syncedLyrics = remember(lyrics) { lyrics.toSyncedLyrics() }

    if (syncedLyrics.lines.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No hay letras disponibles",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        return
    }

    val smoothPositionState = rememberSmoothPositionState(
        basePositionMsProvider = basePositionMs,
        isPlaying = isPlaying,
        playbackSpeed = playbackSpeed,
    )

    val scale = textSizeSp / 30f
    val normalTextStyle = MaterialTheme.typography.headlineMedium.copy(
        fontWeight = FontWeight.ExtraBold,
        fontSize = textSizeSp.sp,
        lineHeight = (textSizeSp + 8).sp,
    )
    val accompanimentTextStyle = MaterialTheme.typography.titleLarge.copy(
        fontWeight = FontWeight.Bold,
        fontSize = (textSizeSp * 0.8f).sp,
        lineHeight = (textSizeSp * 0.8f + 6).sp,
    )
    val phoneticTextStyle = MaterialTheme.typography.bodyMedium.copy(
        fontWeight = FontWeight.Normal,
        fontSize = (textSizeSp * 0.47f).sp,
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val focusOffset = maxHeight * 0.35f

        KaraokeLyricsView(
            listState = remember(syncedLyrics) { androidx.compose.foundation.lazy.LazyListState() },
            lyrics = syncedLyrics,
            currentPosition = {
                (smoothPositionState.value + syncOffsetMs)
                    .coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
            },
            onLineClicked = { line ->
                if (line.start >= 0) onSeekTo(line.start.toLong())
            },
            onLinePressed = { },
            textColor = Color.White,
            normalLineTextStyle = normalTextStyle,
            accompanimentLineTextStyle = accompanimentTextStyle,
            phoneticTextStyle = phoneticTextStyle,
            blendMode = BlendMode.SrcOver,
            useBlurEffect = lineBlur,
            offset = focusOffset,
            keepAliveZone = 72.dp,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

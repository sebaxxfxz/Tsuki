package com.example.tsuki.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.tsuki.domain.model.LyricsEntry
import com.example.tsuki.ui.player.lyrics.KaraokeLyricRow
import com.example.tsuki.ui.player.lyrics.KaraokeWordByWordView
import com.example.tsuki.ui.player.lyrics.hasWordSyncedLine
import kotlinx.coroutines.delay
import kotlin.math.abs

private const val MANUAL_SCROLL_TIMEOUT_MS = 3000L
private const val MANUAL_SCROLL_DEBOUNCE_MS = 50L

@Composable
fun SyncedLyricsView(
    lyrics: List<LyricsEntry>,
    currentPositionMs: Long,
    accentColor: Color,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    playbackSpeed: Float = 1f,
    livePositionProvider: (() -> Long)? = null
) {
    if (lyrics.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No hay letras disponibles", color = Color.Gray)
        }
        return
    }

    if (lyrics.hasWordSyncedLine()) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val lyricsPrefs = remember { com.example.tsuki.data.local.PlayerPreferences(context) }
        val lyricsTextSize by lyricsPrefs.lyricsTextSize.collectAsStateWithLifecycle(initialValue = 30)
        val lyricsLineBlur by lyricsPrefs.lyricsLineBlur.collectAsStateWithLifecycle(initialValue = true)
        val lyricsSyncOffset by lyricsPrefs.lyricsSyncOffsetMs.collectAsStateWithLifecycle(initialValue = 0)
        KaraokeWordByWordView(
            lyrics = lyrics,
            basePositionMs = livePositionProvider ?: { currentPositionMs },
            isPlaying = isPlaying,
            playbackSpeed = playbackSpeed,
            onSeekTo = onSeekTo,
            modifier = modifier,
            textSizeSp = lyricsTextSize,
            lineBlur = lyricsLineBlur,
            syncOffsetMs = lyricsSyncOffset
        )
        return
    }

    val positionState = androidx.compose.runtime.rememberUpdatedState(currentPositionMs)
    val context = androidx.compose.ui.platform.LocalContext.current
    val lyricsPrefs = remember { com.example.tsuki.data.local.PlayerPreferences(context) }
    val lyricsTextSize by lyricsPrefs.lyricsTextSize.collectAsStateWithLifecycle(initialValue = 30)
    val lyricsSyncOffset by lyricsPrefs.lyricsSyncOffsetMs.collectAsStateWithLifecycle(initialValue = 0)
    val activeIndex by remember(lyrics, lyricsSyncOffset) {
        androidx.compose.runtime.derivedStateOf {
            val pos = positionState.value + lyricsSyncOffset
            if (lyrics.isEmpty() || pos < lyrics.first().time) {
                return@derivedStateOf -1
            }
            var low = 0
            var high = lyrics.size - 1
            var result = -1
            while (low <= high) {
                val mid = (low + high) ushr 1
                if (lyrics[mid].time <= pos) {
                    result = mid
                    low = mid + 1
                } else {
                    high = mid - 1
                }
            }
            result
        }
    }
    
    var isManualScrolling by remember { mutableStateOf(false) }
    var lastScrollTime by remember { mutableLongStateOf(0L) }
    
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastScrollTime > MANUAL_SCROLL_DEBOUNCE_MS) {
                        isManualScrolling = true
                        lastScrollTime = currentTime
                    }
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(isManualScrolling, lastScrollTime) {
        if (isManualScrolling) {
            delay(MANUAL_SCROLL_TIMEOUT_MS)
            isManualScrolling = false
        }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(activeIndex, isManualScrolling) {
        if (!isManualScrolling && activeIndex >= 0) {
            val jump = abs(listState.firstVisibleItemIndex - activeIndex)
            if (jump > 15) {
                listState.scrollToItem(activeIndex)
            }
            
            val viewportHeight = listState.layoutInfo.viewportSize.height
            val targetOffset = if (viewportHeight > 0) (viewportHeight * 0.1f).toInt().coerceAtLeast(0) else 0
            listState.animateScrollToItem(activeIndex, targetOffset)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
        contentPadding = PaddingValues(top = 90.dp, bottom = 160.dp, start = 20.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(
            count = lyrics.size,
            key = { index -> "${lyrics[index].time}_$index" },
            contentType = { "lyric" }
        ) { index ->
            val entry = lyrics[index]
            val distance = if (activeIndex >= 0) abs(index - activeIndex) else 1
            
            KaraokeLyricRow(
                entry = entry,
                isActive = activeIndex >= 0 && index == activeIndex,
                distance = distance,
                currentPositionMs = currentPositionMs + lyricsSyncOffset,
                isManualScrolling = isManualScrolling,
                accentColor = accentColor,
                onSeekTo = onSeekTo,
                textSizeSp = lyricsTextSize,
                modifier = Modifier
            )
        }
    }
}

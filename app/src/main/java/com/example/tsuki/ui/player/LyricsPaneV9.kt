package com.example.tsuki.ui.player

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.tsuki.domain.model.LyricsEntry
import com.example.tsuki.ui.components.M3WavySlider

@Composable
fun LyricsPaneV9(
    lyrics: List<LyricsEntry>,
    currentPosition: Long,
    duration: Long,
    isPlaying: Boolean,
    accentColor: Color,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    playbackSpeed: Float = 1f,
    livePositionProvider: (() -> Long)? = null,
    modifier: Modifier = Modifier
) {
    val progress = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
    var dragValue by remember { androidx.compose.runtime.mutableStateOf<Float?>(null) }
    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            SyncedLyricsView(
                lyrics = lyrics,
                currentPositionMs = currentPosition,
                accentColor = accentColor,
                onSeekTo = onSeek,
                isPlaying = isPlaying,
                playbackSpeed = playbackSpeed,
                livePositionProvider = livePositionProvider,
                modifier = Modifier.fillMaxSize()
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            M3WavySlider(
                value = dragValue ?: progress,
                onValueChange = { frac -> dragValue = frac },
                onValueChangeFinished = {
                    dragValue?.let { frac ->
                        onSeek((frac * duration).toLong())
                    }
                    dragValue = null
                },
                isPlaying = isPlaying,
                activeTrackColor = accentColor,
                thumbColor = accentColor,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                trackHeight = 3.dp,
                waveAmplitude = 2.dp,
                showThumb = false,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious, modifier = Modifier.size(44.dp)) {
                    Icon(imageVector = Icons.Rounded.SkipPrevious, contentDescription = "Anterior", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(56.dp)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .size(28.dp)
                            .padding(start = if (!isPlaying) 2.dp else 0.dp)
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(44.dp)) {
                    Icon(imageVector = Icons.Rounded.SkipNext, contentDescription = "Siguiente", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            VolumeSliderV9(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun VolumeSliderV9(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var volume by remember { mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat().coerceAtLeast(1f) }
    var isDragging by remember { mutableStateOf(false) }
    var sliderPos by remember { mutableFloatStateOf(volume / maxVolume) }

    DisposableEffect(audioManager) {
        val observer = object : ContentObserver(Handler(android.os.Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                volume = currentVol
                if (!isDragging) {
                    sliderPos = currentVol / maxVolume
                }
            }
        }
        context.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(imageVector = Icons.Rounded.VolumeDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Slider(
            value = sliderPos,
            onValueChange = {
                isDragging = true
                sliderPos = it
            },
            onValueChangeFinished = {
                isDragging = false
                val vol = (sliderPos * maxVolume).toInt().coerceIn(0, maxVolume.toInt())
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0)
                volume = vol.toFloat()
            },
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                activeTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                thumbColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Icon(imageVector = Icons.Rounded.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

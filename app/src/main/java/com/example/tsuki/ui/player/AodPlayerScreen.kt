package com.example.tsuki.ui.player

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.domain.model.LyricsEntry
import kotlinx.coroutines.delay
import kotlin.math.abs

private const val AUTO_DIM_TIMEOUT_MS = 5000L
private const val AUTO_DIM_ALPHA = 0.25f
private const val DIM_BRIGHTNESS = 0.15f
private const val SHAKE_THRESHOLD = 32f
private const val SLIDE_MAX_PX = 360f

private fun formatAodTime(ms: Long): String {
    val validMs = ms.coerceAtLeast(0L)
    val totalSeconds = validMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
}

@Composable
fun AodPlayerScreen(
    track: MediaTrack,
    lyrics: List<LyricsEntry>,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    accentColor: Color,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var isLocked by remember { mutableStateOf(false) }
    var lastInteractionAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isDimmed by remember { mutableStateOf(false) }
    var batteryLevel by remember { mutableIntStateOf(-1) }
    var clockStyle by remember { mutableIntStateOf(0) }

    val activity = remember(context) {
        var currentContext = context
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is Activity) break
            currentContext = currentContext.baseContext
        }
        currentContext as? Activity
    }

    fun resetInteraction() {
        lastInteractionAt = System.currentTimeMillis()
        isDimmed = false
    }

    DisposableEffect(Unit) {
        val window = activity?.window
        window?.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            activity?.setShowWhenLocked(true)
            activity?.setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window?.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
        onDispose {
            window?.clearFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
            )
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                activity?.setShowWhenLocked(false)
                activity?.setTurnScreenOn(false)
            } else {
                @Suppress("DEPRECATION")
                window?.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            }
            window?.attributes = window?.attributes?.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }
    }

    LaunchedEffect(isDimmed) {
        val window = activity?.window ?: return@LaunchedEffect
        window.attributes = window.attributes.apply {
            screenBrightness = if (isDimmed) DIM_BRIGHTNESS else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
    }

    LaunchedEffect(lastInteractionAt, isLocked) {
        if (!isDimmed) {
            val timeout = if (isLocked) 3000L else AUTO_DIM_TIMEOUT_MS
            delay(timeout)
            isDimmed = true
        }
    }

    DisposableEffect(isLocked) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) return@DisposableEffect onDispose { }
        var lastX = 0f
        var lastY = 0f
        var lastZ = 0f
        var first = true
        var lastShakeAt = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (first) {
                    lastX = event.values[0]; lastY = event.values[1]; lastZ = event.values[2]
                    first = false
                    return
                }
                val delta = abs(event.values[0] - lastX) + abs(event.values[1] - lastY) + abs(event.values[2] - lastZ)
                lastX = event.values[0]; lastY = event.values[1]; lastZ = event.values[2]
                val now = System.currentTimeMillis()
                if (delta > SHAKE_THRESHOLD && now - lastShakeAt > 1000L) {
                    lastShakeAt = now
                    isLocked = false
                    resetInteraction()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (isLocked) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    DisposableEffect(Unit) {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val initialSticky = context.registerReceiver(null, filter)
        batteryLevel = initialSticky?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                batteryLevel = intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            }
        }
        androidx.core.content.ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_EXPORTED
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    BackHandler(enabled = true) {
        if (isLocked) resetInteraction() else onExit()
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (isDimmed) AUTO_DIM_ALPHA else 1f,
        animationSpec = tween(500),
        label = "aodContentFade"
    )

    val unlockSlideProgress = remember { mutableStateOf(0f) }
    val audioManager = remember {
        context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
    }
    var volumeOverlayText by remember { mutableStateOf<String?>(null) }
    var seekOverlaySeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(volumeOverlayText) {
        if (volumeOverlayText != null) {
            kotlinx.coroutines.delay(1200L)
            volumeOverlayText = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isPlaying, isLocked) {
                detectTapGestures(
                    onTap = { resetInteraction() },
                    onDoubleTap = {
                        resetInteraction()
                        if (!isLocked) onPlayPause()
                    }
                )
            }
            .pointerInput(isLocked) {
                var volumeAccum = 0f
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (isLocked) return@awaitEachGesture
                    var isVerticalDrag = false
                    volumeAccum = 0f
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!isVerticalDrag) {
                            if (kotlin.math.abs(change.positionChange().y) > kotlin.math.abs(change.positionChange().x) * 1.4f &&
                                kotlin.math.abs(change.positionChange().y) > 8f
                            ) isVerticalDrag = true else if (change.isConsumed) break
                        }
                        if (isVerticalDrag) {
                            change.consume()
                            volumeAccum += change.positionChange().y
                            val maxVol = audioManager?.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) ?: 15
                            val step = size.height / (maxVol * 2.2f)
                            if (kotlin.math.abs(volumeAccum) >= step) {
                                val steps = (volumeAccum / step).toInt()
                                volumeAccum -= steps * step
                                val current = audioManager?.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) ?: 0
                                val newVol = (current - steps).coerceIn(0, maxVol)
                                audioManager?.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVol, 0)
                                volumeOverlayText = "${(newVol.toFloat() / maxVol * 100).toInt()}%"
                                resetInteraction()
                            }
                        }
                        if (event.changes.all { !it.pressed }) break
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = contentAlpha }
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 40.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AodClockWidget(
                styleIndex = clockStyle,
                batteryLevel = batteryLevel,
                accentColor = accentColor,
                onClick = { clockStyle = (clockStyle + 1) % 4; resetInteraction() }
            )

            Spacer(modifier = Modifier.height(24.dp))

            AsyncImage(
                model = track.artworkUrl,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .size(220.dp)
                    .background(Color(0xFF141418), RoundedCornerShape(28.dp))
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = track.title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val lyricLine = rememberAodLyricLine(lyrics, positionMs)
            if (!lyricLine.isNullOrBlank()) {
                Text(
                    text = lyricLine,
                    color = accentColor.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            AodSliderSection(
                positionMs = positionMs,
                durationMs = durationMs,
                accentColor = accentColor,
                onSeek = onSeek
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { resetInteraction(); onPrevious() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Anterior", tint = Color.White, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = { resetInteraction(); onPlayPause() }, modifier = Modifier.size(72.dp)) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        tint = accentColor,
                        modifier = Modifier.size(52.dp)
                    )
                }
                IconButton(onClick = { resetInteraction(); onNext() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Siguiente", tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }

            AnimatedVisibility(
                visible = !isLocked,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300)),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                AodSlideToLockButton(
                    accentColor = accentColor,
                    onLock = { isLocked = true }
                )
            }
        }

        IconButton(
            onClick = { resetInteraction(); onExit() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "Salir", tint = Color.White.copy(alpha = 0.7f))
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = !volumeOverlayText.isNullOrBlank(),
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            androidx.compose.material3.Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        volumeOverlayText ?: "",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isLocked,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            AodTouchLockOverlay(
                accentColor = accentColor,
                progressState = unlockSlideProgress,
                onUnlock = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isLocked = false
                    resetInteraction()
                }
            )
        }
    }
}

@Composable
private fun AodClockWidget(styleIndex: Int, batteryLevel: Int, accentColor: Color, onClick: () -> Unit) {
    val context = LocalContext.current
    var timeText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val calendar = java.util.Calendar.getInstance()
            val hourFormat = if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
            timeText = android.text.format.DateFormat.format(hourFormat, calendar).toString()
            dateText = android.text.format.DateFormat.format("EEE, MMM d", calendar).toString()
            delay(1000L)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) }) {
        when (styleIndex) {
            0 -> Text(timeText, color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            1 -> Text(timeText, color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Medium)
            2 -> Text(timeText, color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.ExtraLight)
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val parts = timeText.split(":")
                Text(parts.getOrElse(0) { "" }, color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Bold, lineHeight = 60.sp)
                Text(parts.getOrElse(1) { "" }, color = Color.White.copy(alpha = 0.80f), fontSize = 64.sp, fontWeight = FontWeight.Bold, lineHeight = 60.sp)
            }
        }
        Text(dateText, color = Color.White.copy(alpha = 0.65f), fontSize = 14.sp)
        if (batteryLevel >= 0) {
            Text("Batería $batteryLevel%", color = accentColor.copy(alpha = 0.85f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun rememberAodLyricLine(lyrics: List<LyricsEntry>, positionMs: Long): String? {
    return remember(lyrics, positionMs) {
        if (lyrics.isEmpty()) return@remember null
        var low = 0
        var high = lyrics.size - 1
        var result = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (lyrics[mid].time <= positionMs) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        lyrics.getOrNull(result)?.text?.trim()?.takeIf { it.isNotBlank() }
    }
}

@Composable
private fun AodSliderSection(positionMs: Long, durationMs: Long, accentColor: Color, onSeek: (Long) -> Unit) {
    var localValue by remember(durationMs) { mutableFloatStateOf(-1f) }
    val shown = if (localValue >= 0f) localValue.toLong() else positionMs.coerceIn(0L, durationMs.coerceAtLeast(1L))

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = shown.toFloat(),
            onValueChange = { localValue = it },
            onValueChangeFinished = {
                onSeek(localValue.toLong())
                localValue = -1f
            },
            valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.30f)
            ),
            enabled = durationMs > 0
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatAodTime(shown), color = Color.White.copy(alpha = 0.60f), fontSize = 12.sp)
            Text("-" + formatAodTime((durationMs - shown).coerceAtLeast(0)), color = Color.White.copy(alpha = 0.60f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun AodSlideToLockButton(accentColor: Color, onLock: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val knobTravel = 200.dp
    var progress by remember { mutableFloatStateOf(0f) }
    Box(
        modifier = Modifier
            .width(260.dp)
            .height(50.dp)
            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(25.dp))
            .pointerInput(Unit) {
                var accumulated = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        accumulated = (accumulated + amount).coerceIn(0f, size.width.toFloat())
                        progress = (accumulated / (size.width * 0.65f)).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        if (progress >= 0.85f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLock()
                        }
                        accumulated = 0f
                        progress = 0f
                    },
                    onDragCancel = {
                        accumulated = 0f
                        progress = 0f
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "Desliza para bloquear",
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.Center)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 3.dp)
                .graphicsLayer {
                    translationX = progress * knobTravel.toPx()
                    alpha = 1f - (progress * 0.4f)
                }
                .size(44.dp)
                .background(accentColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color.Black, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun AodTouchLockOverlay(
    accentColor: Color,
    progressState: androidx.compose.runtime.MutableState<Float>,
    onUnlock: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val knobTravel = 200.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .pointerInput(Unit) {
                detectTapGestures { }
            }
            .pointerInput(Unit) {
                var accumulated = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        accumulated = (accumulated + amount).coerceIn(0f, size.width * 0.6f)
                        progressState.value = accumulated / (size.width * 0.30f)
                    },
                    onDragEnd = {
                        if (progressState.value >= 1f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onUnlock()
                        }
                        accumulated = 0f
                        progressState.value = 0f
                    },
                    onDragCancel = {
                        accumulated = 0f
                        progressState.value = 0f
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 48.dp)
                .width(260.dp)
                .height(56.dp)
                .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "Desliza para desbloquear",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.Center)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp)
                    .graphicsLayer {
                        translationX = progressState.value.coerceIn(0f, 1f) * knobTravel.toPx()
                    }
                    .size(48.dp)
                    .background(accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.LockOpen, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
            }
        }
    }
}

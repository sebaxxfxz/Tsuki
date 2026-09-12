package com.example.tsuki.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.example.tsuki.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.tsuki.ui.components.M3WavySlider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class V9PlaybackButtonType { NONE, PREVIOUS, PLAY_PAUSE, NEXT }

@Composable
fun PlayerTopActionsV9(
    title: String,
    artist: String,
    accentColor: Color,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onAddToPlaylist: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (title.length > 28) Modifier.basicMarquee() else Modifier)
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (artist.length > 22) Modifier.basicMarquee() else Modifier)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        val haptic = LocalHapticFeedback.current
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onAddToPlaylist != null) {
                IconButton(
                    onClick = onAddToPlaylist,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = stringResource(R.string.player_add_to_playlist),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            V9FavoriteButton(
                isFavorite = isFavorite,
                onToggle = onToggleFavorite,
                activeColor = MaterialTheme.colorScheme.primary,
                inactiveColor = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun V9FavoriteButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val haptic = LocalHapticFeedback.current
    val heartScale = remember { Animatable(1f) }
    val ringScale = remember { Animatable(0.6f) }
    val ringAlpha = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val baseScale by animateFloatAsState(
        targetValue = if (isPressed) 0.82f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 600f),
        label = "FavBaseScale"
    )

    fun triggerBurst() {
        coroutineScope.launch {
            heartScale.snapTo(0.68f)
            heartScale.animateTo(
                targetValue = 1.35f,
                animationSpec = spring(dampingRatio = 0.42f, stiffness = 520f)
            )
            heartScale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 420f)
            )
        }
        coroutineScope.launch {
            ringScale.snapTo(0.6f)
            ringAlpha.snapTo(0.85f)
            ringScale.animateTo(
                targetValue = 1.5f,
                animationSpec = tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            )
        }
        coroutineScope.launch {
            ringAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 300, easing = androidx.compose.animation.core.LinearEasing)
            )
        }
    }

    fun triggerDeflate() {
        coroutineScope.launch {
            heartScale.snapTo(0.85f)
            heartScale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = 450f)
            )
        }
    }

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (!isFavorite) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        triggerBurst()
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        triggerDeflate()
                    }
                    onToggle()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (ringAlpha.value > 0.01f) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer {
                        scaleX = ringScale.value
                        scaleY = ringScale.value
                        alpha = ringAlpha.value
                    }
                    .border(
                        width = 2.dp,
                        color = activeColor,
                        shape = CircleShape
                    )
            )
        }

        Icon(
            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = if (isFavorite) stringResource(R.string.player_remove_fav) else stringResource(R.string.player_add_fav),
            tint = if (isFavorite) activeColor else inactiveColor.copy(alpha = 0.85f),
            modifier = Modifier
                .size(28.dp)
                .graphicsLayer {
                    scaleX = baseScale * heartScale.value
                    scaleY = baseScale * heartScale.value
                }
        )
    }
}

@Composable
fun V9AnimatedPlaybackControls(
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 78.dp,
    baseWeight: Float = 1f,
    expansionWeight: Float = 1.15f,
    compressionWeight: Float = 0.68f,
    colorOtherButtons: Color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
    colorPlayPause: Color = MaterialTheme.colorScheme.primary,
    tintPlayPauseIcon: Color = MaterialTheme.colorScheme.onPrimary,
    tintOtherIcons: Color = MaterialTheme.colorScheme.onSurface,
    playPauseCornerPlaying: Dp = 56.dp,
    playPauseCornerPaused: Dp = 24.dp
) {
    var lastClicked by remember { mutableStateOf<V9PlaybackButtonType?>(null) }
    var clickTrigger by remember { mutableIntStateOf(0) }
    val latestIsPlaying by rememberUpdatedState(newValue = isPlaying)
    val latestLastClicked by rememberUpdatedState(newValue = lastClicked)
    val isPlayPauseLocked = lastClicked == V9PlaybackButtonType.NEXT || lastClicked == V9PlaybackButtonType.PREVIOUS
    var playPauseVisualState by remember { mutableStateOf(isPlaying) }
    var pendingPlayPauseState by remember { mutableStateOf<Boolean?>(null) }
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(lastClicked, clickTrigger) {
        if (lastClicked != null) {
            val delayTime = when (lastClicked) {
                V9PlaybackButtonType.NEXT, V9PlaybackButtonType.PREVIOUS -> 550L
                else -> 220L
            }
            delay(delayTime)
            lastClicked = null
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            pendingPlayPauseState = true
            return@LaunchedEffect
        }
        val shouldDelay = latestLastClicked != V9PlaybackButtonType.PLAY_PAUSE
        if (shouldDelay) {
            delay(220L)
        }
        if (!latestIsPlaying) {
            pendingPlayPauseState = false
        }
    }

    LaunchedEffect(isPlayPauseLocked, pendingPlayPauseState) {
        if (!isPlayPauseLocked) {
            pendingPlayPauseState?.let {
                playPauseVisualState = it
                pendingPlayPauseState = null
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            fun weightFor(button: V9PlaybackButtonType): Float = when (lastClicked) {
                button -> expansionWeight
                null -> baseWeight
                else -> compressionWeight
            }

            val springSpec = spring<Float>(dampingRatio = 0.7f, stiffness = 380f)
            val dpSpringSpec = spring<Dp>(dampingRatio = 0.75f, stiffness = 350f)

            val prevWeight by animateFloatAsState(
                targetValue = weightFor(V9PlaybackButtonType.PREVIOUS),
                animationSpec = springSpec,
                label = "prevWeight"
            )
            Box(
                modifier = Modifier
                    .weight(prevWeight)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(colorOtherButtons)
                    .clickable(role = androidx.compose.ui.semantics.Role.Button) {
                        lastClicked = V9PlaybackButtonType.PREVIOUS
                        clickTrigger++
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPrevious()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = stringResource(R.string.player_previous),
                    tint = tintOtherIcons,
                    modifier = Modifier.size(32.dp)
                )
            }

            val playWeight by animateFloatAsState(
                targetValue = weightFor(V9PlaybackButtonType.PLAY_PAUSE),
                animationSpec = springSpec,
                label = "playWeight"
            )
            val playCorner by animateDpAsState(
                targetValue = if (playPauseVisualState) playPauseCornerPlaying else playPauseCornerPaused,
                animationSpec = dpSpringSpec,
                label = "playCorner"
            )
            val playPauseInteractionSource = remember { MutableInteractionSource() }
            val isPlayPausePressed by playPauseInteractionSource.collectIsPressedAsState()
            val playPauseButtonScale by animateFloatAsState(
                targetValue = if (isPlayPausePressed) 0.90f else 1.0f,
                animationSpec = spring(dampingRatio = 0.62f, stiffness = 600f),
                label = "playPauseButtonScale"
            )

            val playPauseRotation by animateFloatAsState(
                targetValue = if (playPauseVisualState) 90f else 0f,
                animationSpec = spring(dampingRatio = 0.58f, stiffness = 420f),
                label = "playPauseRotation"
            )

            Box(
                modifier = Modifier
                    .weight(playWeight)
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleX = playPauseButtonScale
                        scaleY = playPauseButtonScale
                        clip = true
                        shape = RoundedCornerShape(playCorner)
                    }
                    .background(colorPlayPause)
                    .clickable(
                        interactionSource = playPauseInteractionSource,
                        indication = null,
                        role = androidx.compose.ui.semantics.Role.Button
                    ) {
                        lastClicked = V9PlaybackButtonType.PLAY_PAUSE
                        clickTrigger++
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPlayPause()
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = playPauseVisualState,
                    transitionSpec = {
                        (scaleIn(
                            animationSpec = spring(dampingRatio = 0.55f, stiffness = 480f),
                            initialScale = 0.65f
                        ) + fadeIn(tween(140)))
                            .togetherWith(
                                scaleOut(
                                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 650f),
                                    targetScale = 0.65f
                                ) + fadeOut(tween(100))
                            )
                    },
                    label = "v9PlayPauseMorph"
                ) { playing ->
                    Icon(
                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playing) stringResource(R.string.player_pause) else stringResource(R.string.common_play),
                        tint = tintPlayPauseIcon,
                        modifier = Modifier
                            .size(38.dp)
                            .graphicsLayer {
                                rotationZ = if (playing) (playPauseRotation - 90f) else playPauseRotation
                            }
                    )
                }
            }

            val nextWeight by animateFloatAsState(
                targetValue = weightFor(V9PlaybackButtonType.NEXT),
                animationSpec = springSpec,
                label = "nextWeight"
            )
            Box(
                modifier = Modifier
                    .weight(nextWeight)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(colorOtherButtons)
                    .clickable(role = androidx.compose.ui.semantics.Role.Button) {
                        lastClicked = V9PlaybackButtonType.NEXT
                        clickTrigger++
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNext()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = stringResource(R.string.common_next),
                    tint = tintOtherIcons,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun V9SecondaryControlsRow(
    shuffleEnabled: Boolean,
    repeatMode: Int,
    accentColor: Color,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onEqualizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onShuffle()
            }
        ) {
            Icon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = stringResource(R.string.common_shuffle),
                tint = if (shuffleEnabled) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                modifier = Modifier.size(24.dp)
            )
        }

        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onEqualizer()
            }
        ) {
            Icon(
                imageVector = Icons.Rounded.Equalizer,
                contentDescription = stringResource(R.string.player_eq),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                modifier = Modifier.size(24.dp)
            )
        }

        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onRepeat()
            }
        ) {
            Icon(
                imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                contentDescription = stringResource(R.string.player_repeat),
                tint = if (repeatMode != Player.REPEAT_MODE_OFF) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun PlayerSliderV9(
    currentPosition: Long,
    duration: Long,
    isPlaying: Boolean,
    accentColor: Color,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasValidDuration = duration > 0L
    val max = if (hasValidDuration) duration.toFloat() else 1f
    val progress = if (hasValidDuration) (currentPosition.toFloat() / max).coerceIn(0f, 1f) else 0f
    var dragValue by remember { mutableStateOf<Float?>(null) }
    var optimisticProgress by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(currentPosition) {
        if (optimisticProgress != null) {
            val optPos = (optimisticProgress!! * max).toLong()
            if (kotlin.math.abs(currentPosition - optPos) < 600L) {
                optimisticProgress = null
            }
        }
    }

    LaunchedEffect(optimisticProgress) {
        if (optimisticProgress != null) {
            kotlinx.coroutines.delay(800)
            optimisticProgress = null
        }
    }

    val displayPosition = if (dragValue != null) (dragValue!! * max).toLong() else if (optimisticProgress != null) (optimisticProgress!! * max).toLong() else currentPosition
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { com.example.tsuki.data.local.PlayerPreferences(context) }
    val sliderStyle by prefs.progressBarStyle.collectAsStateWithLifecycle(initialValue = com.example.tsuki.data.local.ProgressBarStyle.STANDARD)
    val trackHeight = when (sliderStyle) {
        com.example.tsuki.data.local.ProgressBarStyle.THICK -> 8.dp
        com.example.tsuki.data.local.ProgressBarStyle.MINIMAL -> 2.dp
        else -> 4.dp
    }

    Column(modifier = modifier.fillMaxWidth()) {
        M3WavySlider(
            value = if (hasValidDuration) (dragValue ?: optimisticProgress ?: progress) else 0f,
            onValueChange = { frac -> if (hasValidDuration) dragValue = frac },
            onValueChangeFinished = {
                if (hasValidDuration) {
                    dragValue?.let { frac ->
                        optimisticProgress = frac
                        onSeek((frac * max).toLong())
                    }
                }
                dragValue = null
            },
            isPlaying = isPlaying,
            activeTrackColor = accentColor,
            thumbColor = accentColor,
            inactiveTrackColor = accentColor.copy(alpha = 0.22f),
            trackHeight = trackHeight,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTimeV9(displayPosition),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
            )
            Text(
                text = if (duration > 0L) formatTimeV9(duration) else "--:--",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
            )
        }
    }
}

private fun formatTimeV9(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) {
        String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
    }
}

package com.example.tsuki.ui.player

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tsuki.R
import com.example.tsuki.data.local.PlayerPreferences
import com.example.tsuki.playback.PlayerController
import com.example.tsuki.util.ConnectivityObserver
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun V9SoundSheetV9(
    playerController: PlayerController,
    qualityLabel: String?,
    sleepActive: Boolean,
    sleepRemainingMs: Long,
    playbackSpeed: Float,
    crossfadeEnabled: Boolean,
    crossfadeDurationSeconds: Float,
    onOpenEqualizer: () -> Unit,
    onDismiss: () -> Unit,
    onTogetherClick: (() -> Unit)? = null,
    onAodClick: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val prefs = remember { PlayerPreferences(context) }
    val ui by playerController.uiState.collectAsStateWithLifecycle()
    val sleepTimerInfo by playerController.sleepTimerState.collectAsStateWithLifecycle()
    val audioSetting by playerController.audioQualitySetting.collectAsStateWithLifecycle(
        initialValue = PlayerPreferences.AUDIO_QUALITY_AUTO
    )
    val saverActive by playerController.dataSaverActiveFlow.collectAsStateWithLifecycle()
    val eqPresetKey by prefs.eqPreset.collectAsStateWithLifecycle(initialValue = "custom")
    val eqEnabled by prefs.eqEnabled.collectAsStateWithLifecycle(initialValue = false)
    val connectivity = remember { ConnectivityObserver.getInstance(context) }
    val isOnline by connectivity.networkStatus.collectAsStateWithLifecycle(
        initialValue = connectivity.isCurrentlyOnline()
    )
    val currentTrack = ui.currentTrack

    var speedExpanded by rememberSaveable { mutableStateOf(false) }
    var qualityExpanded by rememberSaveable { mutableStateOf(false) }

    val pitch = ui.playbackPitchSemitones
    val skipSilence = ui.skipSilenceEnabled
    val normLevel = ui.loudnessNormalizerLevel
    val normLabels = listOf(
        stringResource(R.string.player_norm_off),
        stringResource(R.string.player_norm_gentle),
        stringResource(R.string.player_norm_balanced),
        stringResource(R.string.player_norm_energetic)
    )
    val speedLabel = if (abs(playbackSpeed - 1f) < 0.01f) {
        stringResource(R.string.player_speed_normal)
    } else {
        "${formatSpeed(playbackSpeed)}x"
    }
    val pitchLabel = if (pitch == 0) {
        stringResource(R.string.player_speed_normal)
    } else {
        stringResource(R.string.player_pitch_semitones, pitch)
    }
    val activeParts = mutableListOf<String>()
    if (abs(playbackSpeed - 1f) >= 0.01f) activeParts.add("${formatSpeed(playbackSpeed)}x")
    if (pitch != 0) activeParts.add(stringResource(R.string.player_pitch_semitones, pitch))
    if (normLevel > 0) activeParts.add(normLabels.getOrElse(normLevel) { normLabels.first() })
    if (crossfadeEnabled) activeParts.add(stringResource(R.string.player_crossfade))
    if (skipSilence) activeParts.add(stringResource(R.string.player_skip_silence))
    val summary = if (activeParts.isEmpty()) {
        stringResource(R.string.sound_summary_none)
    } else {
        activeParts.joinToString(" · ")
    }
    val resetEffects = {
        playerController.setPlaybackSpeed(1f)
        playerController.setPlaybackPitch(0)
        playerController.setLoudnessNormalizerLevel(0)
        playerController.setSkipSilenceEnabled(false)
    }

    val effectiveLabel = when (playerController.currentEffectiveQuality()) {
        PlayerPreferences.AUDIO_QUALITY_HIGH -> stringResource(R.string.set_quality_high)
        PlayerPreferences.AUDIO_QUALITY_MEDIUM -> stringResource(R.string.set_quality_medium)
        PlayerPreferences.AUDIO_QUALITY_LOW -> stringResource(R.string.set_quality_low)
        else -> stringResource(R.string.set_quality_auto)
    }
    val qualitySupport = listOfNotNull(
        effectiveLabel,
        qualityLabel,
        if (saverActive) stringResource(R.string.player_quality_saver_on) else null
    ).joinToString(" • ")

    val qualityOptions = listOf(
        stringResource(R.string.set_quality_auto) to PlayerPreferences.AUDIO_QUALITY_AUTO,
        stringResource(R.string.set_quality_high) to PlayerPreferences.AUDIO_QUALITY_HIGH,
        stringResource(R.string.set_quality_medium) to PlayerPreferences.AUDIO_QUALITY_MEDIUM,
        stringResource(R.string.set_quality_low) to PlayerPreferences.AUDIO_QUALITY_LOW
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.player_sound),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (activeParts.isNotEmpty()) {
                    TextButton(onClick = resetEffects) {
                        Text(stringResource(R.string.sound_reset))
                    }
                }
                FilledTonalIconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.common_close)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    SheetSectionLabel(stringResource(R.string.sound_section_audio))

                    SoundSheetRow(
                        icon = Icons.Rounded.Equalizer,
                        title = stringResource(R.string.player_eq),
                        support = when {
                            !eqEnabled -> stringResource(R.string.sound_eq_off)
                            eqPresetKey == "custom" -> stringResource(R.string.sound_eq_custom)
                            else -> eqPresetLabel(eqPresetKey)
                        },
                        supportActive = eqEnabled && eqPresetKey != "flat",
                        onClick = {
                            onDismiss()
                            onOpenEqualizer()
                        },
                        trailing = { Chevron() }
                    )

                    SoundSheetRow(
                        icon = Icons.Rounded.VolumeUp,
                        title = stringResource(R.string.player_norm),
                        support = if (normLevel > 0) {
                            normLabels.getOrElse(normLevel) { normLabels.first() }
                        } else {
                            stringResource(R.string.player_norm_on)
                        },
                        supportActive = normLevel > 0
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        normLabels.forEachIndexed { index, label ->
                            FilterChip(
                                selected = normLevel == index,
                                onClick = { playerController.setLoudnessNormalizerLevel(index) },
                                label = { Text(label) }
                            )
                        }
                    }

                    SoundSheetRow(
                        icon = Icons.Rounded.HighQuality,
                        title = stringResource(R.string.player_quality),
                        support = qualitySupport,
                        supportActive = false,
                        onClick = { qualityExpanded = !qualityExpanded },
                        trailing = { ExpandChevron(qualityExpanded) }
                    )
                    AnimatedVisibility(
                        visible = qualityExpanded,
                        enter = expandVertically(
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 520f)
                        ) + fadeIn(),
                        exit = shrinkVertically(
                            animationSpec = spring(dampingRatio = 0.9f, stiffness = 620f)
                        ) + fadeOut()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                qualityOptions.forEach { option ->
                                    FilterChip(
                                        selected = audioSetting == option.second,
                                        onClick = { playerController.setAudioQualitySetting(option.second) },
                                        label = { Text(option.first) }
                                    )
                                }
                            }
                            val streams = ui.availableAudioStreams
                            if (streams.isNotEmpty()) {
                                val codecs = streams.mapNotNull { it.codec?.takeIf { codec -> codec.isNotBlank() } }.distinct()
                                val bitrates = streams.map { it.bitrate }.distinct().sortedDescending()
                                Text(
                                    text = stringResource(R.string.sound_stream),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 6.dp)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp)
                                        .padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    codecs.forEach { codec ->
                                        FilterChip(
                                            selected = ui.selectedAudioCodec == codec,
                                            onClick = {
                                                streams.filter { it.codec == codec }.let { list ->
                                                    list.firstOrNull { it.bitrate == ui.selectedAudioBitrate }
                                                        ?: list.maxByOrNull { it.bitrate }
                                                }?.let { playerController.selectAudioStream(it) }
                                            },
                                            label = { Text(codec) }
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp)
                                        .padding(bottom = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    bitrates.forEach { bitrate ->
                                        val kbps = if (bitrate >= 1000) bitrate / 1000 else bitrate
                                        FilterChip(
                                            selected = ui.selectedAudioBitrate == bitrate,
                                            onClick = {
                                                streams.filter { it.bitrate == bitrate }.let { list ->
                                                    list.firstOrNull { it.codec == ui.selectedAudioCodec }
                                                        ?: list.first()
                                                }.let { playerController.selectAudioStream(it) }
                                            },
                                            label = { Text("$kbps kbps") }
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.size(8.dp))
                            }
                        }
                    }

                    SectionDivider()
                    SheetSectionLabel(stringResource(R.string.sound_section_playback))

                    SoundSheetRow(
                        icon = Icons.Rounded.Speed,
                        title = stringResource(R.string.sound_speed_pitch),
                        support = "$speedLabel • $pitchLabel",
                        supportActive = abs(playbackSpeed - 1f) >= 0.01f || pitch != 0,
                        onClick = { speedExpanded = !speedExpanded },
                        trailing = { ExpandChevron(speedExpanded) }
                    )
                    AnimatedVisibility(
                        visible = speedExpanded,
                        enter = expandVertically(
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 520f)
                        ) + fadeIn(),
                        exit = shrinkVertically(
                            animationSpec = spring(dampingRatio = 0.9f, stiffness = 620f)
                        ) + fadeOut()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = abs(playbackSpeed - 1f) < 0.01f && pitch == 0,
                                    onClick = { playerController.setAudioPreset(1f, 0) },
                                    label = { Text(stringResource(R.string.player_preset_original)) }
                                )
                                FilterChip(
                                    selected = abs(playbackSpeed - 1.25f) < 0.01f && pitch == 2,
                                    onClick = { playerController.setAudioPreset(1.25f, 2) },
                                    label = { Text(stringResource(R.string.player_preset_nightcore)) }
                                )
                                FilterChip(
                                    selected = abs(playbackSpeed - 0.85f) < 0.01f && pitch == -2,
                                    onClick = { playerController.setAudioPreset(0.85f, -2) },
                                    label = { Text(stringResource(R.string.player_preset_slowed)) }
                                )
                            }
                            var speedDraft by remember(playbackSpeed) {
                                mutableFloatStateOf(playbackSpeed.coerceIn(0.5f, 2f))
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.player_speed),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Slider(
                                    value = speedDraft,
                                    onValueChange = { speedDraft = it },
                                    onValueChangeFinished = { playerController.setPlaybackSpeed(speedDraft) },
                                    valueRange = 0.5f..2f,
                                    steps = 5,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${formatSpeed(speedDraft)}x",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(44.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.player_pitch),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedIconButton(
                                    onClick = { playerController.setPlaybackPitch((pitch - 1).coerceAtLeast(-6)) },
                                    enabled = pitch > -6,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Remove,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = pitchLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (pitch != 0) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .width(92.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .padding(vertical = 6.dp)
                                )
                                OutlinedIconButton(
                                    onClick = { playerController.setPlaybackPitch((pitch + 1).coerceAtMost(6)) },
                                    enabled = pitch < 6,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    SoundSheetRow(
                        icon = Icons.Rounded.BlurOn,
                        title = stringResource(R.string.player_crossfade),
                        support = if (crossfadeEnabled) {
                            "${crossfadeDurationSeconds.roundToInt()}s • ${stringResource(R.string.player_crossfade_sub)}"
                        } else {
                            stringResource(R.string.player_crossfade_sub)
                        },
                        supportActive = crossfadeEnabled,
                        trailing = {
                            Switch(
                                checked = crossfadeEnabled,
                                onCheckedChange = { playerController.setCrossfadeEnabled(it) }
                            )
                        }
                    )
                    AnimatedVisibility(
                        visible = crossfadeEnabled,
                        enter = expandVertically(
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 520f)
                        ) + fadeIn(),
                        exit = shrinkVertically(
                            animationSpec = spring(dampingRatio = 0.9f, stiffness = 620f)
                        ) + fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Slider(
                                value = crossfadeDurationSeconds,
                                onValueChange = { playerController.setCrossfadeDuration(it) },
                                valueRange = 1f..12f,
                                steps = 10,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${crossfadeDurationSeconds.roundToInt()}s",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(36.dp)
                            )
                        }
                    }

                    SoundSheetRow(
                        icon = Icons.Rounded.FastForward,
                        title = stringResource(R.string.player_skip_silence),
                        support = stringResource(R.string.player_skip_silence_sub),
                        supportActive = skipSilence,
                        trailing = {
                            Switch(
                                checked = skipSilence,
                                onCheckedChange = { playerController.setSkipSilenceEnabled(it) }
                            )
                        }
                    )

                    SectionDivider()
                    SheetSectionLabel(stringResource(R.string.sound_section_timer))

                    SoundSheetRow(
                        icon = Icons.Rounded.Bedtime,
                        title = stringResource(R.string.player_sleep),
                        support = when {
                            sleepActive && sleepRemainingMs < 0 -> stringResource(R.string.player_sleep_end)
                            sleepActive -> stringResource(
                                R.string.player_sleep_in,
                                (sleepRemainingMs / 60000).toInt(),
                                ((sleepRemainingMs % 60000) / 1000).toInt()
                            )
                            else -> stringResource(R.string.player_sleep_off)
                        },
                        supportActive = sleepActive,
                        trailing = if (sleepActive) ({
                            TextButton(onClick = { playerController.cancelSleepTimer() }) {
                                Text(stringResource(R.string.player_stop))
                            }
                        }) else null
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = sleepTimerInfo.stopAtSongEnd,
                            onClick = {
                                if (sleepTimerInfo.stopAtSongEnd) {
                                    playerController.cancelSleepTimer()
                                } else {
                                    playerController.setSleepTimerEndOfSong()
                                }
                            },
                            label = { Text(stringResource(R.string.player_stop_at_end)) },
                            leadingIcon = if (sleepTimerInfo.stopAtSongEnd) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                null
                            }
                        )
                        listOf(5, 15, 30, 45, 60).forEach { minutes ->
                            FilterChip(
                                selected = sleepTimerInfo.selectedMinutes == minutes,
                                onClick = {
                                    if (sleepTimerInfo.selectedMinutes == minutes) {
                                        playerController.cancelSleepTimer()
                                    } else {
                                        playerController.setSleepTimer(minutes)
                                    }
                                },
                                label = { Text(stringResource(R.string.player_minutes, minutes)) }
                            )
                        }
                    }

                    SectionDivider()
                    SheetSectionLabel(stringResource(R.string.sound_section_shortcuts))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                            .padding(top = 4.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                onDismiss()
                                onAodClick()
                            },
                            label = { Text(stringResource(R.string.player_aod)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.DarkMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                        currentTrack?.let { track ->
                            AssistChip(
                                onClick = {
                                    onDismiss()
                                    if (!isOnline) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.common_no_connection),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        playerController.startRadio(track)
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.pld_starting_radio_artist, track.artist),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                label = { Text(stringResource(R.string.player_radio)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Radio,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                        if (onTogetherClick != null) {
                            AssistChip(
                                onClick = {
                                    onDismiss()
                                    onTogetherClick()
                                },
                                label = { Text(stringResource(R.string.set_tool_together)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Group,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 18.dp, end = 16.dp, top = 18.dp, bottom = 4.dp)
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun Chevron() {
    Icon(
        imageVector = Icons.Rounded.ExpandMore,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.rotate(-90f)
    )
}

@Composable
private fun ExpandChevron(expanded: Boolean) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 480f),
        label = "soundChevron"
    )
    Icon(
        imageVector = Icons.Rounded.ExpandMore,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.rotate(rotation)
    )
}

@Composable
private fun eqPresetLabel(key: String): String = when (key) {
    "flat" -> stringResource(R.string.eq_preset_flat)
    "rock" -> stringResource(R.string.eq_preset_rock)
    "pop" -> stringResource(R.string.eq_preset_pop)
    "jazz" -> stringResource(R.string.eq_preset_jazz)
    "classical" -> stringResource(R.string.eq_preset_classical)
    "bass" -> stringResource(R.string.eq_preset_bass)
    "vocal" -> stringResource(R.string.eq_preset_vocal)
    "electronic" -> stringResource(R.string.eq_preset_electronic)
    else -> stringResource(R.string.sound_eq_custom)
}

private fun formatSpeed(speed: Float): String {
    val rounded = (speed * 100).roundToInt() / 100f
    return rounded.toString().trimEnd('0').trimEnd('.')
}

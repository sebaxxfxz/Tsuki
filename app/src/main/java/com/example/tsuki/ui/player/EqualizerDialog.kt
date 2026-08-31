package com.example.tsuki.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.tsuki.data.local.PlayerPreferences
import com.example.tsuki.playback.AudioEqualizerHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

private val EQ_PRESETS: Map<String, List<Int>> = mapOf(
    "Plano" to listOf(0, 0, 0, 0, 0),
    "Rock" to listOf(500, 300, -200, 300, 600),
    "Pop" to listOf(-150, 300, 450, 300, -150),
    "Jazz" to listOf(400, 250, -150, 200, 350),
    "Clásica" to listOf(450, 350, -100, 350, 400),
    "Bass" to listOf(700, 500, 0, -100, -200),
    "Vocal" to listOf(-250, -100, 400, 450, 100),
    "Electrónica" to listOf(550, 300, 0, 300, 500)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerDialog(
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { PlayerPreferences(context) }
    val scope = rememberCoroutineScope()

    var eqEnabled by remember { mutableStateOf(false) }
    var bassBoost by remember { mutableIntStateOf(0) }
    var virtualizer by remember { mutableIntStateOf(0) }
    var outputGain by remember { mutableIntStateOf(0) }
    var selectedPreset by remember { mutableStateOf("custom") }
    val bandLevels = remember { mutableStateListOf<Int>() }
    var capabilities by remember { mutableStateOf(AudioEqualizerHelper.capabilities) }

    LaunchedEffect(Unit) {
        capabilities = AudioEqualizerHelper.capabilities
        val count = if (capabilities.bandCount > 0) capabilities.bandCount else 5
        if (bandLevels.isEmpty()) {
            val saved = prefs.eqBandLevels.first()
            repeat(count) { index -> bandLevels.add(saved.getOrNull(index) ?: 0) }
        }
        eqEnabled = prefs.eqEnabled.first()
        bassBoost = prefs.eqBassBoost.first()
        virtualizer = prefs.eqVirtualizer.first()
        outputGain = prefs.eqOutputGainMb.first()
        selectedPreset = prefs.eqPreset.first()
    }

    fun persistBands() {
        scope.launch { prefs.setEqBandLevels(bandLevels.toList()) }
    }

    fun applyPreset(name: String) {
        val levels = EQ_PRESETS[name] ?: return
        val count = if (bandLevels.isEmpty()) 5 else bandLevels.size
        for (i in 0 until count) {
            val value = if (count == levels.size) {
                levels[i]
            } else {
                val fraction = i.toFloat() / (count - 1).coerceAtLeast(1)
                val presetIdxFloat = fraction * (levels.size - 1)
                val lowerIdx = presetIdxFloat.toInt().coerceIn(0, levels.size - 1)
                val upperIdx = (lowerIdx + 1).coerceAtMost(levels.size - 1)
                val t = presetIdxFloat - lowerIdx
                (levels[lowerIdx] + t * (levels[upperIdx] - levels[lowerIdx])).toInt()
            }
            bandLevels[i] = value
            AudioEqualizerHelper.setBandLevel(i, value)
        }
        persistBands()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ecualizador",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (capabilities.bandCount > 0) "${capabilities.bandCount} bandas · sesión activa" else "Esperando sesión de audio…",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = eqEnabled, onCheckedChange = { checked ->
                        eqEnabled = checked
                        AudioEqualizerHelper.setEnabled(checked)
                        scope.launch { prefs.setEqEnabled(checked) }
                    })
                }

                if (capabilities.bandCount > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EQ_PRESETS.keys.forEach { presetName ->
                            FilterChip(
                                selected = selectedPreset == presetName,
                                onClick = {
                                    selectedPreset = presetName
                                    applyPreset(presetName)
                                    scope.launch { prefs.setEqPreset(presetName) }
                                },
                                label = { Text(presetName, style = MaterialTheme.typography.labelSmall) },
                                enabled = eqEnabled
                            )
                        }
                    }

                    val minLevel = capabilities.minLevelMb.coerceAtMost(-500)
                    val maxLevel = capabilities.maxLevelMb.coerceAtLeast(500)

                    Row(
                        modifier = Modifier.fillMaxWidth().height(190.dp).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        bandLevels.forEachIndexed { index, level ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.width(52.dp).fillMaxHeight()
                            ) {
                                Text(
                                    text = formatDb(level / 100f),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                                VerticalEqSlider(
                                    value = level.toFloat(),
                                    onValueChange = { value ->
                                        bandLevels[index] = value.toInt()
                                        AudioEqualizerHelper.setBandLevel(index, value.toInt())
                                        if (selectedPreset != "custom") selectedPreset = "custom"
                                    },
                                    onValueChangeFinished = { persistBands() },
                                    valueRange = minLevel.toFloat()..maxLevel.toFloat(),
                                    enabled = eqEnabled,
                                    modifier = Modifier.weight(1f).padding(vertical = 4.dp)
                                )
                                Text(
                                    text = formatFreq(capabilities.centerFreqMiliHz.getOrNull(index) ?: 0),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                } else {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = RoundedCornerShape(16.dp)) {
                        Text(
                            text = "Reproduce una canción para activar el ecualizador del dispositivo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(18.dp)
                        )
                    }
                }

                EqEffectSlider(
                    title = "Refuerzo de graves",
                    value = bassBoost,
                    onValueChange = {
                        bassBoost = it
                        AudioEqualizerHelper.setBassBoostStrength(it)
                    },
                    onFinished = { scope.launch { prefs.setEqBassBoost(bassBoost) } },
                    enabled = eqEnabled
                )

                EqEffectSlider(
                    title = "Virtualizador surround",
                    value = virtualizer,
                    onValueChange = {
                        virtualizer = it
                        AudioEqualizerHelper.setVirtualizerStrength(it)
                    },
                    onFinished = { scope.launch { prefs.setEqVirtualizer(virtualizer) } },
                    enabled = eqEnabled
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Ganancia de salida",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatDb(outputGain / 100f),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = outputGain.toFloat(),
                    onValueChange = {
                        outputGain = it.toInt()
                        AudioEqualizerHelper.setOutputGainMb(it.toInt())
                    },
                    onValueChangeFinished = { scope.launch { prefs.setEqOutputGainMb(outputGain) } },
                    valueRange = -1500f..1500f,
                    enabled = eqEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        for (i in bandLevels.indices) {
                            bandLevels[i] = 0
                            AudioEqualizerHelper.setBandLevel(i, 0)
                        }
                        bassBoost = 0; virtualizer = 0; outputGain = 0
                        AudioEqualizerHelper.setBassBoostStrength(0)
                        AudioEqualizerHelper.setVirtualizerStrength(0)
                        AudioEqualizerHelper.setOutputGainMb(0)
                        selectedPreset = "custom"
                        scope.launch {
                            prefs.setEqBandLevels(bandLevels.toList())
                            prefs.setEqPreset("custom")
                            prefs.setEqBassBoost(0); prefs.setEqVirtualizer(0); prefs.setEqOutputGainMb(0)
                        }
                    }) { Text("Restablecer") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onDismiss) { Text("Cerrar") }
                }
            }
        }
    }
}

@Composable
private fun EqEffectSlider(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    onFinished: () -> Unit,
    enabled: Boolean
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${value / 10}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            onValueChangeFinished = onFinished,
            valueRange = 0f..1000f,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

private fun formatFreq(miliHz: Int): String {
    val hz = miliHz / 1000
    return when {
        hz >= 1000 -> String.format(Locale.US, "%.1fk", hz / 1000f)
        else -> "${hz}Hz"
    }
}

private fun formatDb(db: Float): String =
    String.format(Locale.US, "%+.1fdB", db)

@Composable
private fun VerticalEqSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackBgColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val thumbColor = if (enabled) primaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val activeTrackColor = if (enabled) primaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(44.dp)
            .then(
                if (enabled) {
                    Modifier.pointerInput(valueRange) {
                        detectVerticalDragGestures(
                            onDragEnd = onValueChangeFinished,
                            onDragCancel = onValueChangeFinished
                        ) { change, _ ->
                            change.consume()
                            val h = size.height.toFloat()
                            if (h > 0) {
                                val fraction = (1f - (change.position.y / h)).coerceIn(0f, 1f)
                                val newValue = valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
                                onValueChange(newValue)
                            }
                        }
                    }.pointerInput(valueRange) {
                        detectTapGestures { offset ->
                            val h = size.height.toFloat()
                            if (h > 0) {
                                val fraction = (1f - (offset.y / h)).coerceIn(0f, 1f)
                                val newValue = valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
                                onValueChange(newValue)
                                onValueChangeFinished()
                            }
                        }
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        val totalRange = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
        val fraction = ((value - valueRange.start) / totalRange).coerceIn(0f, 1f)

        Canvas(modifier = Modifier.fillMaxSize().padding(vertical = 12.dp)) {
            val w = size.width
            val h = size.height
            val trackWidth = 6.dp.toPx()
            val thumbRadius = 9.dp.toPx()
            val trackX = w / 2f

            drawLine(
                color = trackBgColor,
                start = Offset(trackX, 0f),
                end = Offset(trackX, h),
                strokeWidth = trackWidth,
                cap = StrokeCap.Round
            )

            val zeroFraction = ((0f - valueRange.start) / totalRange).coerceIn(0f, 1f)
            val zeroY = h * (1f - zeroFraction)
            val thumbY = h * (1f - fraction)

            drawLine(
                color = activeTrackColor,
                start = Offset(trackX, zeroY),
                end = Offset(trackX, thumbY),
                strokeWidth = trackWidth,
                cap = StrokeCap.Round
            )

            drawCircle(
                color = thumbColor,
                radius = thumbRadius,
                center = Offset(trackX, thumbY)
            )
        }
    }
}

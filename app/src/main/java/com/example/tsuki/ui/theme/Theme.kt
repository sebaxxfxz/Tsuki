package com.example.tsuki.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
)

val LocalThumbCornerDp = androidx.compose.runtime.staticCompositionLocalOf { 12f }

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
)

fun ColorScheme.pureBlack(apply: Boolean = true): ColorScheme =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black,
        surfaceVariant = Color(0xFF121212)
    ) else this

@Composable
fun TSukiTheme(

    darkTheme: Boolean = true,
    pureBlack: Boolean = false,
    dynamicColor: Boolean = true,
    artworkColors: Pair<Color, Color>? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val baseColorScheme = when {
        artworkColors != null -> buildArtworkColorScheme(artworkColors.first, artworkColors.second, darkTheme)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val finalColorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
        if (darkTheme && pureBlack) {
            baseColorScheme.pureBlack(true)
        } else {
            baseColorScheme
        }
    }

    val animatedScheme = animateColorScheme(finalColorScheme)

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.material3.LocalContentColor provides animatedScheme.onBackground
    ) {
        MaterialTheme(
            colorScheme = animatedScheme,
            typography = Typography,
            shapes = ExpressiveShapes,
            content = content
        )
    }
}

@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme {
    @Composable
    fun anim(c: Color) = androidx.compose.animation.animateColorAsState(
        targetValue = c,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 600),
        label = "themeColor"
    ).value
    return target.copy(
        primary = anim(target.primary),
        onPrimary = anim(target.onPrimary),
        primaryContainer = anim(target.primaryContainer),
        onPrimaryContainer = anim(target.onPrimaryContainer),
        secondary = anim(target.secondary),
        onSecondary = anim(target.onSecondary),
        secondaryContainer = anim(target.secondaryContainer),
        onSecondaryContainer = anim(target.onSecondaryContainer),
        tertiary = anim(target.tertiary),
        background = anim(target.background),
        onBackground = anim(target.onBackground),
        surface = anim(target.surface),
        onSurface = anim(target.onSurface),
        surfaceVariant = anim(target.surfaceVariant),
        onSurfaceVariant = anim(target.onSurfaceVariant),
        outline = anim(target.outline)
    )
}

fun Bitmap.extractDominantColor(defaultColor: Color = CrimsonThemeColor): Color {
    return PlayerColorManager.extractHarmonizedColors(this, defaultDominant = defaultColor).dominant
}

private fun Color.toHsv(): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    return hsv
}

private fun tonalColor(hue: Float, saturation: Float, value: Float): Color =
    Color(
        android.graphics.Color.HSVToColor(
            floatArrayOf(((hue % 360f) + 360f) % 360f, saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
        )
    )

fun buildArtworkColorScheme(dominant: Color, accent: Color, darkTheme: Boolean): ColorScheme {
    val dominantHsv = dominant.toHsv()
    val accentHsv = accent.toHsv()
    val neutralHue = dominantHsv[0]
    val rawAccentHue = accentHsv[0]
    val hueDelta = kotlin.math.abs(rawAccentHue - neutralHue).let { if (it > 180f) 360f - it else it }
    val accentHue = if (hueDelta < 18f) neutralHue else rawAccentHue
    val accentChroma = accentHsv[1].coerceIn(0.38f, 0.68f)
    val tertiaryHue = (accentHue + 58f) % 360f

    fun neutral(value: Float, chroma: Float = 0.042f) = tonalColor(neutralHue, chroma, value)
    fun neutralVariant(value: Float) = tonalColor(neutralHue, 0.068f, value)
    fun primaryTone(value: Float) = tonalColor(accentHue, accentChroma, value)
    fun secondaryTone(value: Float) = tonalColor(accentHue, (accentChroma * 0.5f).coerceIn(0.16f, 1f), value)
    fun tertiaryTone(value: Float) = tonalColor(tertiaryHue, (accentChroma * 0.8f).coerceIn(0.22f, 1f), value)
    fun onAccentTone(value: Float, chroma: Float) = tonalColor(accentHue, chroma, value)

    return if (darkTheme) {
        darkColorScheme(
            primary = primaryTone(0.80f),
            onPrimary = onAccentTone(0.20f, accentChroma * 0.45f),
            primaryContainer = primaryTone(0.30f),
            onPrimaryContainer = onAccentTone(0.92f, accentChroma * 0.35f),
            secondary = secondaryTone(0.80f),
            onSecondary = onAccentTone(0.20f, 0.25f),
            secondaryContainer = secondaryTone(0.30f),
            onSecondaryContainer = onAccentTone(0.90f, 0.30f),
            tertiary = tertiaryTone(0.80f),
            onTertiary = tonalColor(tertiaryHue, 0.30f, 0.20f),
            tertiaryContainer = tertiaryTone(0.30f),
            onTertiaryContainer = tonalColor(tertiaryHue, 0.30f, 0.90f),
            background = neutral(0.10f),
            onBackground = neutral(0.90f, 0.030f),
            surface = neutral(0.10f),
            onSurface = neutral(0.90f, 0.030f),
            surfaceVariant = neutralVariant(0.27f),
            onSurfaceVariant = neutral(0.78f, 0.060f),
            outline = neutral(0.58f, 0.060f),
            surfaceContainerLowest = neutral(0.05f),
            surfaceContainerLow = neutral(0.13f),
            surfaceContainer = neutral(0.16f),
            surfaceContainerHigh = neutral(0.20f),
            surfaceContainerHighest = neutral(0.24f)
        )
    } else {
        lightColorScheme(
            primary = primaryTone(0.42f),
            onPrimary = Color.White,
            primaryContainer = primaryTone(0.90f),
            onPrimaryContainer = onAccentTone(0.14f, accentChroma * 0.75f),
            secondary = secondaryTone(0.42f),
            onSecondary = Color.White,
            secondaryContainer = secondaryTone(0.90f),
            onSecondaryContainer = onAccentTone(0.14f, 0.30f),
            tertiary = tertiaryTone(0.42f),
            onTertiary = Color.White,
            tertiaryContainer = tertiaryTone(0.90f),
            onTertiaryContainer = tonalColor(tertiaryHue, 0.30f, 0.14f),
            background = neutral(0.985f),
            onBackground = neutral(0.12f, 0.060f),
            surface = neutral(0.985f),
            onSurface = neutral(0.12f, 0.060f),
            surfaceVariant = neutralVariant(0.90f),
            onSurfaceVariant = neutral(0.30f, 0.060f),
            outline = neutral(0.50f, 0.060f),
            surfaceContainerLowest = neutral(1f),
            surfaceContainerLow = neutral(0.96f),
            surfaceContainer = neutral(0.94f),
            surfaceContainerHigh = neutral(0.92f),
            surfaceContainerHighest = neutral(0.90f)
        )
    }
}
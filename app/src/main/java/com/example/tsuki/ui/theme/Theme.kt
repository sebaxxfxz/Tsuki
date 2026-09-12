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
    val palette = Palette.from(this).maximumColorCount(16).generate()
    val dominantSwatch = palette.dominantSwatch ?: palette.vibrantSwatch ?: palette.mutedSwatch
    return dominantSwatch?.rgb?.let { Color(it) } ?: defaultColor
}

private fun Color.blendHue(other: Color, amount: Float): Color {
    val a = FloatArray(3)
    val b = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), a)
    android.graphics.Color.colorToHSV(other.toArgb(), b)
    var dh = b[0] - a[0]
    if (dh > 180f) dh -= 360f
    if (dh < -180f) dh += 360f
    val h = (a[0] + dh * amount + 360f) % 360f
    return Color(android.graphics.Color.HSVToColor(floatArrayOf(h, a[1], a[2])))
}

private fun Color.withLightness(lightness: Float, saturationScale: Float = 1f): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    hsv[1] = (hsv[1] * saturationScale).coerceIn(0f, 1f)
    hsv[2] = lightness
    return Color(android.graphics.Color.HSVToColor(hsv))
}

fun buildArtworkColorScheme(dominant: Color, accent: Color, darkTheme: Boolean): ColorScheme {
    val base = dominant.withLightness(if (darkTheme) 0.16f else 0.96f, 1.45f)
    val surfaceTone = dominant.withLightness(if (darkTheme) 0.22f else 0.91f, 1.35f)
    val containerTone = dominant.withLightness(if (darkTheme) 0.32f else 0.80f, 1.25f)
    val primary = if (darkTheme) accent.withLightness(0.85f, 1.05f) else accent.withLightness(0.42f)
    val onPrimary = if (darkTheme) Color(0xFF10101A) else Color.White
    val onSurface = if (darkTheme) Color(0xFFE6E4EF) else Color(0xFF1A1922)
    val onSurfaceVariant = if (darkTheme) base.blendHue(Color.White, 0.5f).withLightness(0.72f) else base.blendHue(Color.Black, 0.4f).withLightness(0.34f)
    return if (darkTheme) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = containerTone,
            onPrimaryContainer = Color(0xFFF2F0FA),
            secondary = dominant.withLightness(0.65f),
            onSecondary = Color(0xFF15141C),
            secondaryContainer = dominant.withLightness(0.30f, 1.2f),
            onSecondaryContainer = Color(0xFFF2F0FA),
            tertiary = accent.blendHue(dominant, 0.35f).withLightness(0.70f),
            onTertiary = Color(0xFF171420),
            tertiaryContainer = dominant.withLightness(0.34f, 1.2f),
            onTertiaryContainer = Color(0xFFF2F0FA),
            background = base,
            onBackground = onSurface,
            surface = base,
            onSurface = onSurface,
            surfaceVariant = surfaceTone,
            onSurfaceVariant = onSurfaceVariant,
            outline = onSurfaceVariant,
            surfaceContainerLowest = dominant.withLightness(0.10f, 1.1f),
            surfaceContainerLow = dominant.withLightness(0.19f, 1.15f),
            surfaceContainer = dominant.withLightness(0.23f, 1.2f),
            surfaceContainerHigh = dominant.withLightness(0.29f, 1.25f),
            surfaceContainerHighest = dominant.withLightness(0.35f, 1.3f)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = containerTone,
            onPrimaryContainer = Color(0xFF201F28),
            secondary = dominant.withLightness(0.40f),
            onSecondary = Color.White,
            secondaryContainer = dominant.withLightness(0.82f, 1.2f),
            onSecondaryContainer = Color(0xFF17141F),
            tertiary = accent.blendHue(dominant, 0.35f).withLightness(0.45f),
            onTertiary = Color.White,
            tertiaryContainer = dominant.withLightness(0.78f, 1.2f),
            onTertiaryContainer = Color(0xFF17141F),
            background = base,
            onBackground = onSurface,
            surface = base,
            onSurface = onSurface,
            surfaceVariant = surfaceTone,
            onSurfaceVariant = onSurfaceVariant,
            outline = onSurfaceVariant,
            surfaceContainerLowest = dominant.withLightness(0.97f, 1.1f),
            surfaceContainerLow = dominant.withLightness(0.93f, 1.15f),
            surfaceContainer = dominant.withLightness(0.90f, 1.2f),
            surfaceContainerHigh = dominant.withLightness(0.86f, 1.25f),
            surfaceContainerHighest = dominant.withLightness(0.82f, 1.3f)
        )
    }
}
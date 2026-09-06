package com.example.tsuki.ui.widget.glance

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.glance.GlanceTheme
import androidx.glance.unit.ColorProvider

object TSukiGlancePalette {
    val Background: ColorProvider = ColorProvider(Color(0xFF09090B))
    val Surface: ColorProvider = ColorProvider(Color(0xFF111218))
    val SurfaceVariant: ColorProvider = ColorProvider(Color(0xFF181924))
    val Primary: ColorProvider = ColorProvider(Color(0xFFB48CFF))
    val Secondary: ColorProvider = ColorProvider(Color(0xFFD8C2FF))
    val Outline: ColorProvider = ColorProvider(Color(0xFF262736))
    val TextMain: ColorProvider = ColorProvider(Color(0xFFFFFFFF))
    val TextSub: ColorProvider = ColorProvider(Color(0xFF94A3B8))
    val Accent: ColorProvider = ColorProvider(Color(0xFF8E6BFF))
    val HeartRed: ColorProvider = ColorProvider(Color(0xFFFF5277))

    private val LightBackground: ColorProvider = ColorProvider(Color(0xFFFFFFFF))
    private val LightSurface: ColorProvider = ColorProvider(Color(0xFFF4F2F8))
    private val LightSurfaceVariant: ColorProvider = ColorProvider(Color(0xFFE9E6F0))
    private val LightPrimary: ColorProvider = ColorProvider(Color(0xFF6B4BC8))
    private val LightOutline: ColorProvider = ColorProvider(Color(0xFFDCD8E6))
    private val LightTextSub: ColorProvider = ColorProvider(Color(0xFF5A5F6B))

    val LocalGlanceIsLight = staticCompositionLocalOf { false }

    @Composable
    private fun isLight(): Boolean = LocalGlanceIsLight.current

    @Composable
    fun background(): ColorProvider = if (isLight()) LightBackground else Background

    @Composable
    fun textSub(): ColorProvider = if (isLight()) LightTextSub else TextSub

    @Composable
    fun getSurfaceColor(dominant: Long?): ColorProvider {
        if (dominant == null || dominant == 0L) return if (isLight()) LightSurface else Surface
        val argb = dominant.toInt()
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        if (isLight()) {
            hsv[1] = (hsv[1] * 0.20f).coerceIn(0.03f, 0.16f)
            hsv[2] = 0.93f
        } else {
            hsv[1] = (hsv[1] * 0.35f).coerceIn(0.12f, 0.40f)
            hsv[2] = 0.10f
        }
        return ColorProvider(Color(android.graphics.Color.HSVToColor(hsv)))
    }

    @Composable
    fun getSurfaceVariantColor(dominant: Long?): ColorProvider {
        if (dominant == null || dominant == 0L) return if (isLight()) LightSurfaceVariant else SurfaceVariant
        val argb = dominant.toInt()
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        if (isLight()) {
            hsv[1] = (hsv[1] * 0.30f).coerceIn(0.06f, 0.24f)
            hsv[2] = 0.87f
        } else {
            hsv[1] = (hsv[1] * 0.45f).coerceIn(0.18f, 0.50f)
            hsv[2] = 0.15f
        }
        return ColorProvider(Color(android.graphics.Color.HSVToColor(hsv)))
    }

    @Composable
    fun getPrimaryColor(accent: Long?): ColorProvider {
        if (accent == null || accent == 0L) return if (isLight()) LightPrimary else Primary
        val argb = accent.toInt()
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        if (isLight()) {
            hsv[1] = (hsv[1] * 1.1f).coerceIn(0.45f, 0.85f)
            hsv[2] = 0.72f
        } else {
            hsv[1] = (hsv[1] * 1.1f).coerceIn(0.40f, 0.90f)
            hsv[2] = 0.95f
        }
        return ColorProvider(Color(android.graphics.Color.HSVToColor(hsv)))
    }

    @Composable
    fun getOutlineColor(dominant: Long?): ColorProvider {
        if (dominant == null || dominant == 0L) return if (isLight()) LightOutline else Outline
        val argb = dominant.toInt()
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        if (isLight()) {
            hsv[1] = (hsv[1] * 0.30f).coerceIn(0.06f, 0.24f)
            hsv[2] = 0.82f
        } else {
            hsv[1] = (hsv[1] * 0.50f).coerceIn(0.20f, 0.60f)
            hsv[2] = 0.22f
        }
        return ColorProvider(Color(android.graphics.Color.HSVToColor(hsv)))
    }
}

@Composable
fun TSukiGlanceTheme(content: @Composable () -> Unit) {
    val isLight = LocalContext.current.resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK != Configuration.UI_MODE_NIGHT_YES
    CompositionLocalProvider(TSukiGlancePalette.LocalGlanceIsLight provides isLight) {
        GlanceTheme {
            content()
        }
    }
}

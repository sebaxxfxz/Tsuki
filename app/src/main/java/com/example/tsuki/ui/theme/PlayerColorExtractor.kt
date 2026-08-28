package com.example.tsuki.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.collection.LruCache
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val playerPaletteCache = LruCache<String, Pair<Color, Color>>(60)

suspend fun extractPlayerColors(
    context: Context,
    artworkUrl: String?,
    defaultDominant: Color = Color(0xFF1E1B24),
    defaultAccent: Color = Color(0xFFE2E0F4)
): Pair<Color, Color> {
    if (artworkUrl == null) return Pair(defaultDominant, defaultAccent)
    playerPaletteCache.get(artworkUrl)?.let { return it }
    return withContext(Dispatchers.IO) {
        try {
            val loader = SingletonImageLoader.get(context)
            val request = ImageRequest.Builder(context)
                .data(artworkUrl)
                .size(128, 128)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                val bitmap: Bitmap? = when (val img = result.image) {
                    is BitmapImage -> img.bitmap
                    is coil3.DrawableImage -> (img.drawable as? BitmapDrawable)?.bitmap ?: runCatching { img.drawable.toBitmap() }.getOrNull()
                    else -> null
                }
                if (bitmap != null) {
                    val palette = Palette.from(bitmap).maximumColorCount(24).generate()
                    fun swatchToColor(s: Palette.Swatch?) = s?.let { Color(it.rgb) }
                    val candidates = linkedSetOf<Color>()
                    listOf(
                        palette.vibrantSwatch,
                        palette.dominantSwatch,
                        palette.lightVibrantSwatch,
                        palette.darkVibrantSwatch,
                        palette.mutedSwatch,
                        palette.lightMutedSwatch,
                        palette.darkMutedSwatch
                    ).forEach { swatchToColor(it)?.let { candidates.add(it) } }
                    palette.swatches.sortedByDescending { it.population }.forEach { swatchToColor(it)?.let { candidates.add(it) } }
                    val list = candidates.toList()
                    fun isDistinct(a: Color, b: Color): Boolean {
                        val hsvA = FloatArray(3)
                        val hsvB = FloatArray(3)
                        android.graphics.Color.colorToHSV(a.toArgb(), hsvA)
                        android.graphics.Color.colorToHSV(b.toArgb(), hsvB)
                        var dh = kotlin.math.abs(hsvA[0] - hsvB[0])
                        if (dh > 180f) dh = 360f - dh
                        val ds = kotlin.math.abs(hsvA[1] - hsvB[1])
                        val dv = kotlin.math.abs(hsvA[2] - hsvB[2])
                        return dh > 18f || ds > 0.18f || dv > 0.18f
                    }
                    val domColor = list.firstOrNull() ?: defaultDominant
                    var accColor = list.firstOrNull { it != domColor && isDistinct(it, domColor) } ?: list.getOrNull(1) ?: defaultAccent
                    if (!isDistinct(domColor, accColor)) {
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(accColor.toArgb(), hsv)
                        hsv[0] = (hsv[0] + 35f) % 360f
                        hsv[1] = hsv[1].coerceIn(0.55f, 0.95f)
                        accColor = Color(android.graphics.Color.HSVToColor(hsv))
                    }
                    val pair = Pair(domColor, accColor)
                    playerPaletteCache.put(artworkUrl, pair)
                    return@withContext pair
                }
            }
        } catch (_: Exception) {}
        Pair(defaultDominant, defaultAccent)
    }
}

fun adjustColorForTheme(color: Color, isDark: Boolean): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    if (isDark) {
        hsv[2] = (hsv[2] * 0.85f).coerceIn(0f, 1f)
    } else {
        hsv[1] = (hsv[1] * 0.9f).coerceIn(0f, 1f)
    }
    return Color(android.graphics.Color.HSVToColor(hsv))
}

fun Color.relativeLuminance(): Float {
    fun channel(c: Float): Float =
        if (c <= 0.03928f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    return 0.2126f * channel(red) + 0.7152f * channel(green) + 0.0722f * channel(blue)
}

fun contrastRatio(a: Color, b: Color): Float {
    val la = a.relativeLuminance()
    val lb = b.relativeLuminance()
    val lighter = maxOf(la, lb)
    val darker = minOf(la, lb)
    return (lighter + 0.05f) / (darker + 0.05f)
}

fun ensureContrastAgainst(
    foreground: Color,
    background: Color,
    minRatio: Float = 3.0f,
    saturationFloor: Float = 0.35f
): Color {
    if (contrastRatio(foreground, background) >= minRatio) return foreground
    val bgLum = background.relativeLuminance()
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(foreground.toArgb(), hsv)
    hsv[1] = hsv[1].coerceAtLeast(saturationFloor)
    var adjusted = foreground
    repeat(14) {
        if (bgLum < 0.35f) {
            hsv[2] = (hsv[2] + 0.07f).coerceIn(0f, 1f)
        } else {
            hsv[2] = (hsv[2] - 0.07f).coerceIn(0f, 0.55f)
        }
        adjusted = Color(android.graphics.Color.HSVToColor(hsv))
        if (contrastRatio(adjusted, background) >= minRatio) return adjusted
    }
    return if (bgLum < 0.35f) Color.White.copy(alpha = foreground.alpha) else Color(0xFF17141F)
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt().coerceIn(0, 255),
    (red * 255).toInt().coerceIn(0, 255),
    (green * 255).toInt().coerceIn(0, 255),
    (blue * 255).toInt().coerceIn(0, 255)
)

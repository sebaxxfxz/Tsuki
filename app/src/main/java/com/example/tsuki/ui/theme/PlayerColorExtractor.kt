package com.example.tsuki.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.collection.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

data class PlayerColors(
    val dominant: Color,
    val accent: Color
)

val playerPaletteCache = LruCache<String, Pair<Color, Color>>(120)

private const val PALETTE_SAMPLE_SIZE = 96
private const val PALETTE_MAX_COLORS = 32
private const val PALETTE_CROP_FRACTION = 0.08f
private const val MIN_SWATCH_POPULATION = 0.02f
private const val MIN_ACCENT_POPULATION = 0.01f

private fun Float.srgbToLinear(): Float =
    if (this <= 0.04045f) this / 12.92f else ((this + 0.055f) / 1.055f).pow(2.4f)

private fun Float.linearToSrgb(): Float =
    if (this <= 0.0031308f) this * 12.92f else 1.055f * this.pow(1f / 2.4f) - 0.055f

private fun Color.toHsv(): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    return hsv
}

private fun Palette.Swatch.toHsv(): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(rgb, hsv)
    return hsv
}

private fun Bitmap.prepareForPalette(): Bitmap {
    val cropped = runCatching {
        val insetX = (width * PALETTE_CROP_FRACTION).roundToInt()
        val insetY = (height * PALETTE_CROP_FRACTION).roundToInt()
        val targetWidth = (width - insetX * 2).coerceAtLeast(1)
        val targetHeight = (height - insetY * 2).coerceAtLeast(1)
        Bitmap.createBitmap(this, insetX, insetY, targetWidth, targetHeight)
    }.getOrDefault(this)

    val largestSide = max(cropped.width, cropped.height)
    if (largestSide <= PALETTE_SAMPLE_SIZE) return cropped

    val scale = PALETTE_SAMPLE_SIZE.toFloat() / largestSide
    return runCatching {
        Bitmap.createScaledBitmap(
            cropped,
            (cropped.width * scale).roundToInt().coerceAtLeast(1),
            (cropped.height * scale).roundToInt().coerceAtLeast(1),
            true
        )
    }.getOrDefault(cropped)
}

private fun swatchScore(
    swatch: Palette.Swatch,
    totalPopulation: Int,
    chromaWeight: Float
): Float {
    val population = swatch.population.toFloat() / totalPopulation.coerceAtLeast(1)
    if (population < MIN_SWATCH_POPULATION) return 0f
    val hsv = swatch.toHsv()
    val midTone = (1f - abs(hsv[2] - 0.52f) * 1.25f).coerceIn(0.05f, 1f)
    val chroma = hsv[1].coerceIn(0f, 1f)
    return population * (1f - chromaWeight + chromaWeight * chroma) * midTone
}

private fun mixLinear(entries: List<Pair<Color, Float>>): Color {
    var red = 0.0
    var green = 0.0
    var blue = 0.0
    var totalWeight = 0f
    entries.forEach { (color, weight) ->
        val safeWeight = weight.coerceAtLeast(0.0001f)
        red += color.red.srgbToLinear() * safeWeight
        green += color.green.srgbToLinear() * safeWeight
        blue += color.blue.srgbToLinear() * safeWeight
        totalWeight += safeWeight
    }
    if (totalWeight <= 0f) return entries.firstOrNull()?.first ?: Color.Black
    return Color(
        red = (red / totalWeight).toFloat().linearToSrgb(),
        green = (green / totalWeight).toFloat().linearToSrgb(),
        blue = (blue / totalWeight).toFloat().linearToSrgb()
    )
}

private fun tameDominant(color: Color, fallback: Color): Color {
    val hsv = color.toHsv()
    if (hsv[2] < 0.06f || (hsv[2] > 0.95f && hsv[1] < 0.10f)) return fallback
    hsv[1] = hsv[1].coerceAtMost(0.62f)
    hsv[2] = hsv[2].coerceIn(0.20f, 0.88f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

private fun rotateHue(color: Color): Color {
    val hsv = color.toHsv()
    hsv[0] = (hsv[0] + 35f) % 360f
    hsv[1] = hsv[1].coerceIn(0.55f, 0.90f)
    hsv[2] = hsv[2].coerceIn(0.60f, 0.95f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

object PlayerColorManager {
    val DefaultDominant = Color(0xFF1E1B24)
    val DefaultAccent = Color(0xFFE2E0F4)
    val Default = PlayerColors(DefaultDominant, DefaultAccent)

    private val cache = LruCache<String, PlayerColors>(120)
    private val _currentColors = MutableStateFlow(Default)
    val currentColors: StateFlow<PlayerColors> = _currentColors.asStateFlow()

    fun extractHarmonizedColors(
        bitmap: Bitmap,
        defaultDominant: Color = DefaultDominant,
        defaultAccent: Color = DefaultAccent
    ): PlayerColors {
        val safeBitmap = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                bitmap
            }
        }.getOrNull() ?: return PlayerColors(defaultDominant, defaultAccent)

        val targetBitmap = safeBitmap.prepareForPalette()

        val palette = runCatching {
            Palette.from(targetBitmap).maximumColorCount(PALETTE_MAX_COLORS).generate()
        }.getOrNull() ?: return PlayerColors(defaultDominant, defaultAccent)

        val totalPopulation = palette.swatches.sumOf { it.population }.coerceAtLeast(1)

        val backgroundCandidates = palette.swatches.mapNotNull { swatch ->
            val score = swatchScore(swatch, totalPopulation, chromaWeight = 0.30f)
            if (score <= 0f) null else Color(swatch.rgb) to score
        }.sortedByDescending { it.second }.take(3)

        val rawDominant = if (backgroundCandidates.isEmpty()) {
            palette.dominantSwatch?.let { Color(it.rgb) } ?: defaultDominant
        } else {
            mixLinear(backgroundCandidates)
        }
        val domColor = tameDominant(rawDominant, defaultDominant)

        fun isDistinct(a: Color, b: Color): Boolean {
            val hsvA = a.toHsv()
            val hsvB = b.toHsv()
            var hueDelta = abs(hsvA[0] - hsvB[0])
            if (hueDelta > 180f) hueDelta = 360f - hueDelta
            val saturationDelta = abs(hsvA[1] - hsvB[1])
            val valueDelta = abs(hsvA[2] - hsvB[2])
            return hueDelta > 20f || saturationDelta > 0.22f || valueDelta > 0.22f
        }

        val accentCandidates = palette.swatches.mapNotNull { swatch ->
            val hsv = swatch.toHsv()
            val population = swatch.population.toFloat() / totalPopulation
            if (hsv[1] < 0.28f || hsv[2] < 0.20f || population < MIN_ACCENT_POPULATION) {
                null
            } else {
                val score = swatchScore(swatch, totalPopulation, chromaWeight = 0.70f)
                if (score <= 0f) null else Color(swatch.rgb) to score
            }
        }.sortedByDescending { it.second }.map { it.first }

        var accColor = accentCandidates.firstOrNull { isDistinct(it, domColor) }
            ?: accentCandidates.firstOrNull()
            ?: if (domColor.toHsv()[1] < 0.10f) defaultAccent else rotateHue(domColor)

        if (!isDistinct(domColor, accColor)) {
            accColor = rotateHue(domColor)
        }

        accColor = ensureContrastAgainst(accColor, domColor, minRatio = 3.2f)
        return PlayerColors(domColor, accColor)
    }

    suspend fun getOrExtract(
        context: Context,
        artworkUrl: String?,
        defaultDominant: Color = DefaultDominant,
        defaultAccent: Color = DefaultAccent,
        publish: Boolean = true
    ): PlayerColors {
        if (artworkUrl.isNullOrBlank()) return PlayerColors(defaultDominant, defaultAccent)
        cache.get(artworkUrl)?.let {
            if (publish) _currentColors.value = it
            return it
        }

        delay(90)

        return withContext(Dispatchers.Default) {
            currentCoroutineContext().ensureActive()
            val loader = SingletonImageLoader.get(context)
            val cachedValue = loader.memoryCache?.get(MemoryCache.Key(artworkUrl))
            val memoryBmp = cachedValue?.let { v ->
                when (val img = v.image) {
                    is BitmapImage -> img.bitmap
                    is coil3.DrawableImage -> (img.drawable as? BitmapDrawable)?.bitmap ?: runCatching { img.drawable.toBitmap() }.getOrNull()
                    else -> null
                }
            }
            if (memoryBmp != null) {
                val colors = extractHarmonizedColors(memoryBmp, defaultDominant, defaultAccent)
                cache.put(artworkUrl, colors)
                playerPaletteCache.put(artworkUrl, Pair(colors.dominant, colors.accent))
                if (publish) _currentColors.value = colors
                return@withContext colors
            }

            try {
                val request = ImageRequest.Builder(context)
                    .data(artworkUrl)
                    .size(PALETTE_SAMPLE_SIZE, PALETTE_SAMPLE_SIZE)
                    .allowHardware(false)
                    .build()
                val result = withContext(Dispatchers.IO) { loader.execute(request) }
                currentCoroutineContext().ensureActive()
                if (result is SuccessResult) {
                    val bitmap: Bitmap? = when (val img = result.image) {
                        is BitmapImage -> img.bitmap
                        is coil3.DrawableImage -> (img.drawable as? BitmapDrawable)?.bitmap ?: runCatching { img.drawable.toBitmap() }.getOrNull()
                        else -> null
                    }
                    if (bitmap != null) {
                        val colors = extractHarmonizedColors(bitmap, defaultDominant, defaultAccent)
                        cache.put(artworkUrl, colors)
                        playerPaletteCache.put(artworkUrl, Pair(colors.dominant, colors.accent))
                        if (publish) _currentColors.value = colors
                        return@withContext colors
                    }
                }
            } catch (_: Exception) {}
            val fallback = PlayerColors(defaultDominant, defaultAccent)
            if (publish) _currentColors.value = fallback
            fallback
        }
    }
}

suspend fun extractPlayerColors(
    context: Context,
    artworkUrl: String?,
    defaultDominant: Color = PlayerColorManager.DefaultDominant,
    defaultAccent: Color = PlayerColorManager.DefaultAccent
): Pair<Color, Color> {
    val colors = PlayerColorManager.getOrExtract(context, artworkUrl, defaultDominant, defaultAccent)
    return Pair(colors.dominant, colors.accent)
}

fun adjustColorForTheme(color: Color, isDark: Boolean): Color {
    val hsv = color.toHsv()
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
    val hsv = foreground.toHsv()
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

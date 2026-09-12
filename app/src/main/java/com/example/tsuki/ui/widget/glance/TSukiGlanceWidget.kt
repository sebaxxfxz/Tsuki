package com.example.tsuki.ui.widget.glance

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.tsuki.R

class TSukiGlanceWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    companion object {
        private val Compact = DpSize(120.dp, 120.dp)
        private val Square = DpSize(180.dp, 180.dp)
        private val Wide = DpSize(260.dp, 100.dp)
        private val Tall = DpSize(260.dp, 160.dp)
        private val Large = DpSize(260.dp, 260.dp)
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(Compact, Square, Wide, Tall, Large)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideGlanceInternal(context, id) { title, artist, bitmap, isPlaying, shuffle, repeatMode, isFav, hasTrack, domColor, accColor, posMs, durMs ->
            val size = LocalSize.current
            val w = size.width
            val h = size.height
            when {
                w <= 130.dp && h <= 130.dp -> QuickResumeLayout(context, bitmap, isPlaying, hasTrack, domColor, accColor)
                w <= 200.dp && h > 130.dp -> SquarePlayerLayout(context, title, artist, bitmap, isPlaying, shuffle, isFav, hasTrack, domColor, accColor, posMs, durMs)
                h < 85.dp -> MiniPlayerLayout(context, title, artist, bitmap, isPlaying, isFav, hasTrack, domColor, accColor, posMs, durMs)
                h < 185.dp -> NowPlayingLayout(context, title, artist, bitmap, isPlaying, shuffle, repeatMode, isFav, hasTrack, domColor, accColor, posMs, durMs)
                else -> LibraryHeroLayout(context, title, artist, bitmap, isPlaying, shuffle, repeatMode, isFav, hasTrack, domColor, accColor, posMs, durMs)
            }
        }
    }
}

class TSukiMiniGlanceWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideGlanceInternal(context, id) { title, artist, bitmap, isPlaying, _, _, isFav, hasTrack, domColor, accColor, posMs, durMs ->
            MiniPlayerLayout(context, title, artist, bitmap, isPlaying, isFav, hasTrack, domColor, accColor, posMs, durMs)
        }
    }
}

class TSukiSquareGlanceWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideGlanceInternal(context, id) { title, artist, bitmap, isPlaying, shuffle, _, isFav, hasTrack, domColor, accColor, posMs, durMs ->
            SquarePlayerLayout(context, title, artist, bitmap, isPlaying, shuffle, isFav, hasTrack, domColor, accColor, posMs, durMs)
        }
    }
}

class TSukiQuickResumeGlanceWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideGlanceInternal(context, id) { _, _, bitmap, isPlaying, _, _, _, hasTrack, domColor, accColor, _, _ ->
            QuickResumeLayout(context, bitmap, isPlaying, hasTrack, domColor, accColor)
        }
    }
}

class TSukiHeroGlanceWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideGlanceInternal(context, id) { title, artist, bitmap, isPlaying, shuffle, repeatMode, isFav, hasTrack, domColor, accColor, posMs, durMs ->
            LibraryHeroLayout(context, title, artist, bitmap, isPlaying, shuffle, repeatMode, isFav, hasTrack, domColor, accColor, posMs, durMs)
        }
    }
}

private suspend fun GlanceAppWidget.provideGlanceInternal(
    context: Context,
    id: GlanceId,
    content: @Composable (
        title: String,
        artist: String,
        bitmap: Bitmap?,
        isPlaying: Boolean,
        shuffle: Boolean,
        repeatMode: Int,
        isFavorite: Boolean,
        hasTrack: Boolean,
        dominantColor: Long,
        accentColor: Long,
        positionMs: Long,
        durationMs: Long
    ) -> Unit
) {
    provideContent {
        val prefs = currentState<Preferences>()
        val title = prefs[TSukiGlanceKeys.Title].orEmpty()
        val artist = prefs[TSukiGlanceKeys.Artist].orEmpty()
        val mediaId = prefs[TSukiGlanceKeys.MediaId].orEmpty()
        val isPlaying = prefs[TSukiGlanceKeys.IsPlaying] == true
        val shuffle = prefs[TSukiGlanceKeys.Shuffle] == true
        val repeatMode = prefs[TSukiGlanceKeys.RepeatMode] ?: 0
        val isFavorite = prefs[TSukiGlanceKeys.IsFavorite] == true
        val hasTrack = (prefs[TSukiGlanceKeys.HasTrack] == true || title.isNotBlank()) && (mediaId.isNotBlank() || title.isNotBlank())
        val dominantColor = prefs[TSukiGlanceKeys.DominantColor] ?: 0L
        val accentColor = prefs[TSukiGlanceKeys.AccentColor] ?: 0L
        val positionMs = prefs[TSukiGlanceKeys.PositionMs] ?: 0L
        val durationMs = prefs[TSukiGlanceKeys.DurationMs] ?: 0L
        val bitmap = if (hasTrack && mediaId.isNotBlank()) {
            TSukiGlanceSync.getCachedArtworkBitmap(context.applicationContext, mediaId)
        } else null

        GlanceTheme {
            content(
                title, artist, bitmap, isPlaying, shuffle, repeatMode,
                isFavorite, hasTrack, dominantColor, accentColor, positionMs, durationMs
            )
        }
    }
}

@Composable
private fun WidgetFrame(
    dominantColor: Long,
    paddingDp: androidx.compose.ui.unit.Dp = 12.dp,
    content: @GlanceComposable @Composable () -> Unit
) {
    val surfaceColor = TSukiGlancePalette.getSurfaceColor(dominantColor)
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(surfaceColor)
            .cornerRadius(24.dp)
            .padding(paddingDp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

fun formatWidgetTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "$min:${sec.toString().padStart(2, '0')}"
}

@Composable
fun QuickResumeLayout(
    context: Context,
    bitmap: Bitmap?,
    isPlaying: Boolean,
    hasTrack: Boolean,
    dominantColor: Long,
    accentColor: Long
) {
    val primaryColor = TSukiGlancePalette.getPrimaryColor(accentColor)
    WidgetFrame(dominantColor = dominantColor, paddingDp = 6.dp) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .clickable(actionStartActivity(openAppIntent(context))),
            contentAlignment = Alignment.Center
        ) {
            if (hasTrack && bitmap != null) {
                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = context.getString(R.string.common_cover),
                    modifier = GlanceModifier.fillMaxSize().cornerRadius(18.dp)
                )
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.TopStart
                ) {
                    Box(
                        modifier = GlanceModifier
                            .padding(4.dp)
                            .size(18.dp)
                            .background(TSukiGlancePalette.background())
                            .cornerRadius(9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_tsuki_kanji),
                            contentDescription = "TSuki",
                            modifier = GlanceModifier.size(11.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = GlanceModifier
                        .size(52.dp)
                        .background(TSukiGlancePalette.getSurfaceVariantColor(dominantColor))
                        .cornerRadius(26.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_tsuki_kanji),
                        contentDescription = "TSuki",
                        modifier = GlanceModifier.size(30.dp)
                    )
                }
            }

            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(32.dp)
                        .background(primaryColor)
                        .cornerRadius(16.dp)
                        .clickable(actionRunCallback<TogglePlayAction>()),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(if (isPlaying) R.drawable.widget_ic_pause else R.drawable.widget_ic_play),
                        contentDescription = if (isPlaying) context.getString(R.string.player_pause) else context.getString(R.string.common_play),
                        modifier = GlanceModifier.size(16.dp),
                        colorFilter = ColorFilter.tint(TSukiGlancePalette.background())
                    )
                }
            }
        }
    }
}

@Composable
fun SquarePlayerLayout(
    context: Context,
    title: String,
    artist: String,
    bitmap: Bitmap?,
    isPlaying: Boolean,
    shuffle: Boolean,
    isFavorite: Boolean,
    hasTrack: Boolean,
    dominantColor: Long,
    accentColor: Long,
    positionMs: Long = 0L,
    durationMs: Long = 0L
) {
    val primaryColor = TSukiGlancePalette.getPrimaryColor(accentColor)
    val progressFraction = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    WidgetFrame(dominantColor = dominantColor, paddingDp = 10.dp) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .cornerRadius(16.dp)
                    .clickable(actionStartActivity(openAppIntent(context))),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        provider = ImageProvider(bitmap),
                        contentDescription = context.getString(R.string.common_cover),
                        modifier = GlanceModifier.fillMaxSize().cornerRadius(16.dp)
                    )
                } else {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(TSukiGlancePalette.getSurfaceVariantColor(dominantColor))
                            .cornerRadius(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_tsuki_kanji),
                            contentDescription = "TSuki",
                            modifier = GlanceModifier.size(38.dp)
                        )
                    }
                }

                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.TopStart
                ) {
                    Box(
                        modifier = GlanceModifier
                            .padding(6.dp)
                            .background(TSukiGlancePalette.background())
                            .cornerRadius(8.dp)
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_tsuki_kanji),
                                contentDescription = null,
                                modifier = GlanceModifier.size(10.dp),
                                colorFilter = ColorFilter.tint(primaryColor)
                            )
                            Spacer(modifier = GlanceModifier.width(3.dp))
                            Text(
                                text = "TSuki",
                                style = TextStyle(
                                    color = primaryColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Box(
                        modifier = GlanceModifier
                            .padding(4.dp)
                            .size(30.dp)
                            .background(TSukiGlancePalette.background())
                            .cornerRadius(15.dp)
                            .clickable(actionRunCallback<ToggleFavoriteAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(if (isFavorite) R.drawable.widget_ic_favorite else R.drawable.widget_ic_favorite_border),
                            contentDescription = context.getString(R.string.common_favorite),
                            modifier = GlanceModifier.size(16.dp),
                            colorFilter = ColorFilter.tint(
                                if (isFavorite) TSukiGlancePalette.HeartRed else TSukiGlancePalette.textSub()
                            )
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            Column(
                modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity(openAppIntent(context)))
            ) {
                Text(
                    text = if (hasTrack) title.ifBlank { "TSuki" } else "TSuki Music",
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = if (hasTrack) artist.ifBlank { context.getString(R.string.wd_tap_open) } else context.getString(R.string.wd_tap_play),
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }

            if (hasTrack) {
                Spacer(modifier = GlanceModifier.height(3.dp))
                LinearProgressIndicator(
                    progress = progressFraction,
                    modifier = GlanceModifier.fillMaxWidth().height(3.dp).cornerRadius(1.5.dp),
                    color = primaryColor,
                    backgroundColor = TSukiGlancePalette.getOutlineColor(dominantColor)
                )
                if (durationMs > 0L) {
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatWidgetTime(positionMs),
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 9.sp
                            )
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = formatWidgetTime(durationMs),
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 9.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier.size(34.dp).cornerRadius(17.dp).clickable(actionRunCallback<ToggleShuffleAction>()),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.widget_ic_shuffle),
                        contentDescription = context.getString(R.string.common_shuffle),
                        modifier = GlanceModifier.size(17.dp),
                        colorFilter = ColorFilter.tint(
                            if (shuffle) primaryColor else GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                }
                Spacer(modifier = GlanceModifier.width(2.dp))
                Box(
                    modifier = GlanceModifier.size(34.dp).cornerRadius(17.dp).clickable(actionRunCallback<PlayPrevAction>()),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.widget_ic_prev),
                        contentDescription = context.getString(R.string.player_previous),
                        modifier = GlanceModifier.size(19.dp),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface)
                    )
                }
                Spacer(modifier = GlanceModifier.width(2.dp))
                Box(
                    modifier = GlanceModifier.size(42.dp).background(primaryColor).cornerRadius(21.dp).clickable(actionRunCallback<TogglePlayAction>()),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(if (isPlaying) R.drawable.widget_ic_pause else R.drawable.widget_ic_play),
                        contentDescription = if (isPlaying) context.getString(R.string.player_pause) else context.getString(R.string.common_play),
                        modifier = GlanceModifier.size(20.dp),
                        colorFilter = ColorFilter.tint(TSukiGlancePalette.background())
                    )
                }
                Spacer(modifier = GlanceModifier.width(2.dp))
                Box(
                    modifier = GlanceModifier.size(34.dp).cornerRadius(17.dp).clickable(actionRunCallback<PlayNextAction>()),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.widget_ic_next),
                        contentDescription = context.getString(R.string.common_next),
                        modifier = GlanceModifier.size(19.dp),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface)
                    )
                }
            }
        }
    }
}

@Composable
fun MiniPlayerLayout(
    context: Context,
    title: String,
    artist: String,
    bitmap: Bitmap?,
    isPlaying: Boolean,
    isFavorite: Boolean,
    hasTrack: Boolean,
    dominantColor: Long,
    accentColor: Long,
    positionMs: Long = 0L,
    durationMs: Long = 0L
) {
    val primaryColor = TSukiGlancePalette.getPrimaryColor(accentColor)
    val progressFraction = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    WidgetFrame(dominantColor = dominantColor, paddingDp = 10.dp) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .size(50.dp)
                    .background(TSukiGlancePalette.getSurfaceVariantColor(dominantColor))
                    .cornerRadius(14.dp)
                    .clickable(actionStartActivity(openAppIntent(context))),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        provider = ImageProvider(bitmap),
                        contentDescription = context.getString(R.string.common_cover),
                        modifier = GlanceModifier.fillMaxSize().cornerRadius(14.dp)
                    )
                } else {
                    Image(
                        provider = ImageProvider(R.drawable.ic_tsuki_kanji),
                        contentDescription = "TSuki",
                        modifier = GlanceModifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.width(10.dp))

            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .clickable(actionStartActivity(openAppIntent(context))),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (hasTrack) title.ifBlank { "TSuki" } else "TSuki Music",
                        maxLines = 1,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    Image(
                        provider = ImageProvider(R.drawable.ic_tsuki_kanji),
                        contentDescription = "TSuki",
                        modifier = GlanceModifier.size(12.dp),
                        colorFilter = ColorFilter.tint(primaryColor)
                    )
                }
                Text(
                    text = if (hasTrack) artist.ifBlank { context.getString(R.string.wd_tap_open) } else context.getString(R.string.wd_tap_play),
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
                if (hasTrack) {
                    Spacer(modifier = GlanceModifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = progressFraction,
                        modifier = GlanceModifier.fillMaxWidth().height(3.dp).cornerRadius(1.5.dp),
                        color = primaryColor,
                        backgroundColor = TSukiGlancePalette.getOutlineColor(dominantColor)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.width(6.dp))

            Box(
                modifier = GlanceModifier.size(32.dp).cornerRadius(16.dp).clickable(actionRunCallback<ToggleFavoriteAction>()),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(if (isFavorite) R.drawable.widget_ic_favorite else R.drawable.widget_ic_favorite_border),
                    contentDescription = context.getString(R.string.common_favorite),
                    modifier = GlanceModifier.size(17.dp),
                    colorFilter = ColorFilter.tint(
                        if (isFavorite) TSukiGlancePalette.HeartRed else GlanceTheme.colors.onSurfaceVariant
                    )
                )
            }

            Box(
                modifier = GlanceModifier.size(34.dp).cornerRadius(17.dp).clickable(actionRunCallback<PlayPrevAction>()),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.widget_ic_prev),
                    contentDescription = context.getString(R.string.player_previous),
                    modifier = GlanceModifier.size(19.dp),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface)
                )
            }

            Box(
                modifier = GlanceModifier.size(40.dp).background(primaryColor).cornerRadius(20.dp).clickable(actionRunCallback<TogglePlayAction>()),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(if (isPlaying) R.drawable.widget_ic_pause else R.drawable.widget_ic_play),
                    contentDescription = if (isPlaying) context.getString(R.string.player_pause) else context.getString(R.string.common_play),
                    modifier = GlanceModifier.size(20.dp),
                    colorFilter = ColorFilter.tint(TSukiGlancePalette.background())
                )
            }

            Box(
                modifier = GlanceModifier.size(34.dp).cornerRadius(17.dp).clickable(actionRunCallback<PlayNextAction>()),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.widget_ic_next),
                    contentDescription = context.getString(R.string.common_next),
                    modifier = GlanceModifier.size(19.dp),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface)
                )
            }
        }
    }
}

@Composable
fun NowPlayingLayout(
    context: Context,
    title: String,
    artist: String,
    artwork: Bitmap?,
    isPlaying: Boolean,
    shuffle: Boolean,
    repeatMode: Int,
    isFavorite: Boolean,
    hasTrack: Boolean,
    dominantColor: Long,
    accentColor: Long,
    positionMs: Long,
    durationMs: Long
) {
    val size = LocalSize.current
    val artSize = (size.height - 24.dp).coerceIn(76.dp, 130.dp)
    val primaryColor = TSukiGlancePalette.getPrimaryColor(accentColor)
    val progressFraction = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    WidgetFrame(dominantColor = dominantColor, paddingDp = 12.dp) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .size(artSize)
                    .background(TSukiGlancePalette.getSurfaceVariantColor(dominantColor))
                    .cornerRadius(18.dp)
                    .clickable(actionStartActivity(openAppIntent(context))),
                contentAlignment = Alignment.Center
            ) {
                if (artwork != null) {
                    Image(
                        provider = ImageProvider(artwork),
                        contentDescription = context.getString(R.string.common_cover),
                        modifier = GlanceModifier.fillMaxSize().cornerRadius(18.dp)
                    )
                } else {
                    Image(
                        provider = ImageProvider(R.drawable.ic_tsuki_kanji),
                        contentDescription = "TSuki",
                        modifier = GlanceModifier.size(36.dp)
                    )
                }

                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.TopStart
                ) {
                    Box(
                        modifier = GlanceModifier
                            .padding(6.dp)
                            .size(18.dp)
                            .background(TSukiGlancePalette.background())
                            .cornerRadius(9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_tsuki_kanji),
                            contentDescription = "TSuki",
                            modifier = GlanceModifier.size(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.width(12.dp))

            Column(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = GlanceModifier.defaultWeight())

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .clickable(actionStartActivity(openAppIntent(context)))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_tsuki_kanji),
                                contentDescription = null,
                                modifier = GlanceModifier.size(11.dp),
                                colorFilter = ColorFilter.tint(primaryColor)
                            )
                            Spacer(modifier = GlanceModifier.width(3.dp))
                            Text(
                                text = if (isPlaying) context.getString(R.string.wd_playing) else context.getString(R.string.wd_paused),
                                style = TextStyle(
                                    color = primaryColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Text(
                            text = if (hasTrack) title.ifBlank { "TSuki" } else "TSuki Music",
                            maxLines = 1,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = if (hasTrack) artist.ifBlank { context.getString(R.string.wd_tap_open) } else context.getString(R.string.wd_tap_play),
                            maxLines = 1,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Box(
                        modifier = GlanceModifier
                            .size(36.dp)
                            .cornerRadius(18.dp)
                            .clickable(actionRunCallback<ToggleFavoriteAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(if (isFavorite) R.drawable.widget_ic_favorite else R.drawable.widget_ic_favorite_border),
                            contentDescription = context.getString(R.string.common_favorite),
                            modifier = GlanceModifier.size(19.dp),
                            colorFilter = ColorFilter.tint(
                                if (isFavorite) TSukiGlancePalette.HeartRed else GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(5.dp))

                LinearProgressIndicator(
                    progress = progressFraction,
                    modifier = GlanceModifier.fillMaxWidth().height(3.dp).cornerRadius(1.5.dp),
                    color = primaryColor,
                    backgroundColor = TSukiGlancePalette.getOutlineColor(dominantColor)
                )

                Spacer(modifier = GlanceModifier.height(2.dp))

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatWidgetTime(positionMs),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 9.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = if (durationMs > 0L) formatWidgetTime(durationMs) else "",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 9.sp
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(6.dp))

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier.size(34.dp).cornerRadius(17.dp).clickable(actionRunCallback<ToggleShuffleAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.widget_ic_shuffle),
                            contentDescription = context.getString(R.string.common_shuffle),
                            modifier = GlanceModifier.size(18.dp),
                            colorFilter = ColorFilter.tint(
                                if (shuffle) primaryColor else GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                    }
                    Spacer(modifier = GlanceModifier.width(2.dp))
                    Box(
                        modifier = GlanceModifier.size(36.dp).cornerRadius(18.dp).clickable(actionRunCallback<PlayPrevAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.widget_ic_prev),
                            contentDescription = context.getString(R.string.player_previous),
                            modifier = GlanceModifier.size(20.dp),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface)
                        )
                    }
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Box(
                        modifier = GlanceModifier.size(46.dp).background(primaryColor).cornerRadius(23.dp).clickable(actionRunCallback<TogglePlayAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(if (isPlaying) R.drawable.widget_ic_pause else R.drawable.widget_ic_play),
                            contentDescription = if (isPlaying) context.getString(R.string.player_pause) else context.getString(R.string.common_play),
                            modifier = GlanceModifier.size(22.dp),
                            colorFilter = ColorFilter.tint(TSukiGlancePalette.background())
                        )
                    }
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Box(
                        modifier = GlanceModifier.size(36.dp).cornerRadius(18.dp).clickable(actionRunCallback<PlayNextAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.widget_ic_next),
                            contentDescription = context.getString(R.string.common_next),
                            modifier = GlanceModifier.size(20.dp),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface)
                        )
                    }
                    Spacer(modifier = GlanceModifier.width(2.dp))
                    Box(
                        modifier = GlanceModifier.size(34.dp).cornerRadius(17.dp).clickable(actionRunCallback<ToggleRepeatAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.widget_ic_repeat),
                            contentDescription = context.getString(R.string.player_repeat),
                            modifier = GlanceModifier.size(18.dp),
                            colorFilter = ColorFilter.tint(
                                if (repeatMode != 0) primaryColor else GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.defaultWeight())
            }
        }
    }
}

@Composable
fun LibraryHeroLayout(
    context: Context,
    title: String,
    artist: String,
    artwork: Bitmap?,
    isPlaying: Boolean,
    shuffle: Boolean,
    repeatMode: Int,
    isFavorite: Boolean,
    hasTrack: Boolean,
    dominantColor: Long,
    accentColor: Long,
    positionMs: Long = 0L,
    durationMs: Long = 0L
) {
    val size = LocalSize.current
    val artSize = (size.height - 84.dp).coerceIn(86.dp, 130.dp)
    val primaryColor = TSukiGlancePalette.getPrimaryColor(accentColor)
    val chipBg = TSukiGlancePalette.getSurfaceVariantColor(dominantColor)
    val progressFraction = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    WidgetFrame(dominantColor = dominantColor, paddingDp = 14.dp) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(artSize)
                        .background(TSukiGlancePalette.getSurfaceVariantColor(dominantColor))
                        .cornerRadius(18.dp)
                        .clickable(actionStartActivity(openAppIntent(context))),
                    contentAlignment = Alignment.Center
                ) {
                    if (artwork != null) {
                        Image(
                            provider = ImageProvider(artwork),
                            contentDescription = context.getString(R.string.common_cover),
                            modifier = GlanceModifier.fillMaxSize().cornerRadius(18.dp)
                        )
                    } else {
                        Image(
                            provider = ImageProvider(R.drawable.ic_tsuki_kanji),
                            contentDescription = "TSuki",
                            modifier = GlanceModifier.size(40.dp)
                        )
                    }

                    Box(
                        modifier = GlanceModifier.fillMaxSize(),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .padding(6.dp)
                                .size(20.dp)
                                .background(TSukiGlancePalette.background())
                                .cornerRadius(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_tsuki_kanji),
                                contentDescription = "TSuki",
                                modifier = GlanceModifier.size(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = GlanceModifier.width(12.dp))

                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_tsuki_kanji),
                            contentDescription = null,
                            modifier = GlanceModifier.size(12.dp),
                            colorFilter = ColorFilter.tint(primaryColor)
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Text(
                            text = if (isPlaying) context.getString(R.string.wd_playing) else context.getString(R.string.wd_dashboard),
                            style = TextStyle(
                                color = primaryColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Text(
                        text = if (hasTrack) title.ifBlank { "TSuki" } else "TSuki Music",
                        maxLines = 2,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = if (hasTrack) artist.ifBlank { context.getString(R.string.wd_tap_open) } else context.getString(R.string.wd_hifi),
                        maxLines = 1,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = GlanceModifier.size(42.dp).background(primaryColor).cornerRadius(21.dp).clickable(actionRunCallback<TogglePlayAction>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(if (isPlaying) R.drawable.widget_ic_pause else R.drawable.widget_ic_play),
                                contentDescription = if (isPlaying) context.getString(R.string.player_pause) else context.getString(R.string.common_play),
                                modifier = GlanceModifier.size(20.dp),
                                colorFilter = ColorFilter.tint(TSukiGlancePalette.background())
                            )
                        }
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Box(
                            modifier = GlanceModifier.size(34.dp).cornerRadius(17.dp).clickable(actionRunCallback<PlayPrevAction>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.widget_ic_prev),
                                contentDescription = context.getString(R.string.player_previous),
                                modifier = GlanceModifier.size(19.dp),
                                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface)
                            )
                        }
                        Box(
                            modifier = GlanceModifier.size(34.dp).cornerRadius(17.dp).clickable(actionRunCallback<PlayNextAction>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.widget_ic_next),
                                contentDescription = context.getString(R.string.common_next),
                                modifier = GlanceModifier.size(19.dp),
                                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface)
                            )
                        }
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Box(
                            modifier = GlanceModifier.size(34.dp).cornerRadius(17.dp).clickable(actionRunCallback<ToggleFavoriteAction>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(if (isFavorite) R.drawable.widget_ic_favorite else R.drawable.widget_ic_favorite_border),
                                contentDescription = context.getString(R.string.common_favorite),
                                modifier = GlanceModifier.size(18.dp),
                                colorFilter = ColorFilter.tint(
                                    if (isFavorite) TSukiGlancePalette.HeartRed else GlanceTheme.colors.onSurfaceVariant
                                )
                            )
                        }
                        Box(
                            modifier = GlanceModifier.size(34.dp).cornerRadius(17.dp).clickable(actionRunCallback<ToggleShuffleAction>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.widget_ic_shuffle),
                                contentDescription = context.getString(R.string.common_shuffle),
                                modifier = GlanceModifier.size(17.dp),
                                colorFilter = ColorFilter.tint(
                                    if (shuffle) primaryColor else GlanceTheme.colors.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            if (hasTrack) {
                Spacer(modifier = GlanceModifier.height(6.dp))
                LinearProgressIndicator(
                    progress = progressFraction,
                    modifier = GlanceModifier.fillMaxWidth().height(3.dp).cornerRadius(1.5.dp),
                    color = primaryColor,
                    backgroundColor = TSukiGlancePalette.getOutlineColor(dominantColor)
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatWidgetTime(positionMs),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 9.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = if (durationMs > 0L) formatWidgetTime(durationMs) else "",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 9.sp
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .height(34.dp)
                        .background(chipBg)
                        .cornerRadius(12.dp)
                        .clickable(actionStartActivity(openAppDestinationIntent(context, "LIKES"))),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            provider = ImageProvider(R.drawable.widget_ic_favorite),
                            contentDescription = context.getString(R.string.common_likes),
                            modifier = GlanceModifier.size(14.dp),
                            colorFilter = ColorFilter.tint(TSukiGlancePalette.HeartRed)
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Text(
                            text = context.getString(R.string.common_likes),
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.width(6.dp))

                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .height(34.dp)
                        .background(chipBg)
                        .cornerRadius(12.dp)
                        .clickable(actionStartActivity(openAppDestinationIntent(context, "LIBRARY"))),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            provider = ImageProvider(R.drawable.widget_ic_library),
                            contentDescription = context.getString(R.string.lib_title),
                            modifier = GlanceModifier.size(14.dp),
                            colorFilter = ColorFilter.tint(primaryColor)
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Text(
                            text = context.getString(R.string.lib_title),
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.width(6.dp))

                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .height(34.dp)
                        .background(chipBg)
                        .cornerRadius(12.dp)
                        .clickable(actionStartActivity(openAppDestinationIntent(context, "SEARCH"))),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            provider = ImageProvider(R.drawable.widget_ic_search),
                            contentDescription = context.getString(R.string.common_search),
                            modifier = GlanceModifier.size(14.dp),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant)
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Text(
                            text = context.getString(R.string.common_search),
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}

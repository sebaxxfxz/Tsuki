package com.example.tsuki.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.tsuki.R
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.network.TSukiPlaylist
import com.example.tsuki.ui.theme.CardHeroShape
import com.example.tsuki.ui.theme.LocalThumbCornerDp

private val ytimgLowResPattern = Regex("/(default|mqdefault)\\.jpg")
private val gcuSizePattern = Regex("=w(\\d+)-h(\\d+)")

fun upgradeImageUrl(url: String?): String? {
    if (url.isNullOrBlank()) return url
    var result = url
    if (result.contains("i.ytimg.com/vi/")) {
        result = result.replace(ytimgLowResPattern, "/hqdefault.jpg")
    }
    if (result.contains("googleusercontent.com") || result.contains("ggpht.com")) {
        result = result.replace(gcuSizePattern) { match ->
            val w = match.groupValues[1].toIntOrNull() ?: 1080
            val h = match.groupValues[2].toIntOrNull() ?: 1080
            if (w < 1080 || h < 1080) "=w${maxOf(w, 1080)}-h${maxOf(h, 1080)}" else match.value
        }
    }
    return result
}

fun optimizeListThumbnail(url: String?): String? {
    if (url.isNullOrBlank()) return url
    var result = url
    if (result.contains("i.ytimg.com/vi/")) {
        result = result.replace(Regex("/(hqdefault|sddefault|maxresdefault)\\.jpg"), "/mqdefault.jpg")
    }
    if (result.contains("googleusercontent.com") || result.contains("ggpht.com")) {
        result = result.replace(gcuSizePattern, "=w320-h320")
    }
    return result
}

@Composable
fun rememberHiResImageModel(url: String?): Any? {
    val context = LocalContext.current
    return remember(url) {
        if (url.isNullOrBlank()) null
        else ImageRequest.Builder(context)
            .data(upgradeImageUrl(url))
            .memoryCacheKey(upgradeImageUrl(url) ?: url)
            .diskCacheKey(upgradeImageUrl(url) ?: url)
            .crossfade(true)
            .build()
    }
}

fun playlistCoverFile(context: android.content.Context, playlistId: String): java.io.File {
    val safe = playlistId.replace(Regex("[^A-Za-z0-9_-]"), "_")
    return java.io.File(java.io.File(context.filesDir, "playlist_covers"), "$safe.jpg")
}

@Composable
fun rememberPlaylistCoverModel(playlistId: String, thumbnailUrl: String?): Any? {
    val context = LocalContext.current
    val custom = remember(playlistId) {
        playlistCoverFile(context, playlistId).takeIf { it.exists() }
    }
    val remote = rememberHiResImageModel(thumbnailUrl)
    return custom ?: remote
}

@Composable
fun rememberListImageModel(url: String?): Any? {
    val context = LocalContext.current
    return remember(url) {
        if (url.isNullOrBlank()) null
        else {
            val optimized = optimizeListThumbnail(url)
            ImageRequest.Builder(context)
                .data(optimized)
                .memoryCacheKey(optimized ?: url)
                .diskCacheKey(url)
                .crossfade(120)
                .build()
        }
    }
}

@Composable
fun MusicHomeSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "music_skeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.38f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "pulse"
    )
    Column(modifier = modifier.padding(bottom = 24.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) {
                Box(
                    Modifier
                        .size(width = 92.dp, height = 32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = pulse))
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier
                .padding(horizontal = 16.dp)
                .size(width = 160.dp, height = 18.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = pulse))
        )
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .padding(horizontal = 16.dp)
                .size(width = 220.dp, height = 28.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = pulse * 0.85f))
        )
        Spacer(Modifier.height(16.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(3) { _ ->
                Box(
                    Modifier
                        .size(width = 260.dp, height = 340.dp)
                        .clip(CardHeroShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = pulse))
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(4) { _ ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .size(148.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = pulse * 0.9f))
                    )
                    Box(
                        Modifier
                            .size(width = 110.dp, height = 12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = pulse * 0.7f))
                    )
                }
            }
        }
    }
}

@Composable
fun MusicSectionHeader(
    label: String,
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.9.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (count != null && count > 0) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.height(22.dp)
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
        if (onClick != null) {
            FilledTonalIconButton(
                onClick = onClick,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AccountPlaylistsHeader(
    avatarUrl: String?,
    accountName: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    accountName?.take(1)?.uppercase() ?: "U",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.music_sub_yt_playlists),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.9.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                accountName ?: stringResource(R.string.home_account_connected),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                stringResource(R.string.home_manage_account),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onClick != null) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun MusicHeroCard(
    track: MediaTrack,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CardHeroShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        modifier = modifier
            .size(width = 260.dp, height = 340.dp)
            .m3PressBounce(targetScale = 0.97f, onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = rememberHiResImageModel(track.artworkUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.30f to Color.Transparent,
                                0.58f to Color.Black.copy(alpha = 0.22f),
                                1f to Color.Black.copy(alpha = 0.88f)
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.42f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    stringResource(R.string.common_play).uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 0.6.sp),
                    maxLines = 1
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .padding(end = 52.dp)
            ) {
                Text(
                    track.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, lineHeight = 20.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    track.artist,
                    color = Color.White.copy(alpha = 0.86f),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!track.viewCountText.isNullOrBlank()) {
                    Spacer(Modifier.size(2.dp))
                    Text(
                        track.viewCountText ?: "",
                        color = Color.White.copy(alpha = 0.66f),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp)
                    .size(44.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistCard(
    playlist: TSukiPlaylist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(162.dp)
            .m3PressBounce(targetScale = 0.96f, onClick = onClick)
    ) {
        val thumbCorner = LocalThumbCornerDp.current
        Surface(
            shape = RoundedCornerShape(thumbCorner.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 1.dp,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
        ) {
            AsyncImage(
                model = rememberPlaylistCoverModel(playlist.id, playlist.thumbnailUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.size(10.dp))
        Text(
            playlist.title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            playlist.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

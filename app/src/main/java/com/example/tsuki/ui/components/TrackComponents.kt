package com.example.tsuki.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.tsuki.domain.model.MediaTrack

@Composable
fun TrackListItem(
    track: MediaTrack,
    onClick: () -> Unit,
    onMoreClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .m3PressBounce(targetScale = 0.94f, onClick = onClick)
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val isDownloaded = remember(track.id) {
            com.example.tsuki.playback.DownloadEngine.getInstance(context).isDownloaded(track.videoId ?: track.id)
        }
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val thumbCorner = com.example.tsuki.ui.theme.LocalThumbCornerDp.current
            Surface(
                shape = RoundedCornerShape(thumbCorner.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shadowElevation = 1.dp,
                modifier = Modifier.size(56.dp)
            ) {
                AsyncImage(
                    model = rememberListImageModel(track.artworkUrl),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isDownloaded) {
                        Icon(
                            imageVector = Icons.Rounded.DownloadDone,
                            contentDescription = "Descargado",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!track.viewCountText.isNullOrBlank() || track.durationText != null) {
                    Text(
                        text = listOfNotNull(track.viewCountText, track.durationText).joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (onMoreClick != null) {
                IconButton(onClick = onMoreClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Opciones",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TrackCard(
    track: MediaTrack,
    onClick: () -> Unit,
    onPlayRadio: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .width(140.dp)
                .m3PressBounce(targetScale = 0.95f, onClick = onClick)
                .padding(8.dp)
        ) {
            val thumbCorner = com.example.tsuki.ui.theme.LocalThumbCornerDp.current
            Box {
                AsyncImage(
                    model = rememberListImageModel(track.artworkUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(thumbCorner.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                if (onPlayRadio != null) {
                    Surface(
                        onClick = onPlayRadio,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Rounded.Radio,
                                contentDescription = "Iniciar radio",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (showMenu && onPlayRadio != null) {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Iniciar radio") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Radio, contentDescription = null)
                    },
                    onClick = {
                        showMenu = false
                        onPlayRadio.invoke()
                    }
                )
            }
        }
    }
}

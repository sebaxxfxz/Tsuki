package com.example.tsuki.ui.screens

import androidx.compose.foundation.background
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.tsuki.data.local.HomePreferences
import com.example.tsuki.data.local.TSukiSubscriptionRepository
import com.example.tsuki.data.subscriptions.TSukiSubscriptionFeedRepository
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.playback.PlayerController
import com.example.tsuki.ui.components.VideoCardEnhanced
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.itemsIndexed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    playerController: PlayerController?,
    onExpandPlayer: () -> Unit = {}
) {
    val context = LocalContext.current
    val subRepo = remember { TSukiSubscriptionRepository.getInstance(context) }
    val feedRepo = remember { TSukiSubscriptionFeedRepository(context) }
    val homePrefs = remember { HomePreferences(context) }
    val scope = rememberCoroutineScope()
    val subs by subRepo.getAllSubscriptions().collectAsStateWithLifecycle(initialValue = emptyList())
    val favChannels by homePrefs.favoriteChannels.collectAsStateWithLifecycle(initialValue = emptySet())
    var videos by remember { mutableStateOf<List<MediaTrack>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("Todos") }
    var selectedChannelId by remember { mutableStateOf<String?>(null) }
    var loadJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val displaySubs = remember(subs, favChannels) {
        if (subs.isNotEmpty()) subs
        else favChannels.mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.isEmpty() || parts[0].isBlank()) null
            else com.example.tsuki.data.local.TSukiChannelSubscription(
                channelId = parts[0],
                channelName = parts.getOrNull(1) ?: parts[0],
                channelThumbnail = parts.getOrNull(2) ?: ""
            )
        }
    }

    fun load() {
        loadJob?.cancel()
        isLoading = true
        loadJob = scope.launch {
            val res = withContext(Dispatchers.IO) { try { feedRepo.getRecentVideos() } catch (_: Exception) { emptyList() } }
            val fallback = if (res.isEmpty() && favChannels.isNotEmpty()) {
                try {
                    val ids = favChannels.take(12).map { it.substringBefore("|") }
                    withContext(Dispatchers.IO) {
                        val deferred = ids.map { id ->
                            async {
                                try { com.example.tsuki.network.ChannelRssClient().fetchChannelVideos(id).take(4) } catch (_: Exception) { emptyList() }
                            }
                        }
                        deferred.awaitAll().flatten().take(20)
                    }
                } catch (_: Exception) { emptyList() }
            } else res
            videos = fallback
            isLoading = false
            isRefreshing = false
        }
    }

    val backupRepo = remember { com.example.tsuki.data.local.TSukiBackupRepository(context) }
    var exportMsg by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { scope.launch { val r = backupRepo.exportSubscriptionsAsNewPipe(it); android.widget.Toast.makeText(context, if (r.isSuccess) "Exportado" else "Error", android.widget.Toast.LENGTH_SHORT).show() } }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { scope.launch { val r = backupRepo.importNewPipe(it); exportMsg = if (r.isSuccess) "Importados ${r.getOrNull()}" else "Error"; android.widget.Toast.makeText(context, exportMsg ?: "", android.widget.Toast.LENGTH_SHORT).show(); if (r.isSuccess) load() } }
    }

    LaunchedEffect(displaySubs.size) {
        if (displaySubs.isNotEmpty()) load()
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Suscripciones", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { importLauncher.launch("application/json") }) { Icon(Icons.Rounded.Download, contentDescription = null) }
                    IconButton(onClick = { exportLauncher.launch("tsuki_subs_${System.currentTimeMillis()}.json") }) { Icon(Icons.Rounded.Upload, contentDescription = null) }
                    Text("${displaySubs.size} canales", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (displaySubs.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displaySubs, key = { it.channelId }) { sub ->
                    val isSelected = selectedChannelId == sub.channelId
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(64.dp)
                            .clickable {
                                selectedChannelId = if (selectedChannelId == sub.channelId) null else sub.channelId
                            }
                    ) {
                        AsyncImage(
                            model = sub.channelThumbnail,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .then(
                                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    else Modifier
                                )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            sub.channelName,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Todos", "Videos", "Shorts").forEach { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { filter = f },
                        label = { Text(f) }
                    )
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                load()
            },
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                displaySubs.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.Subscriptions, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text("Aún no sigues canales", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Busca creadores en el onboarding o desde la búsqueda y síguelos para ver sus videos aquí", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                scope.launch {
                                    homePrefs.setOnboardingDone(false)
                                }
                            }
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Explorar canales", color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                videos.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Sin videos recientes de tus suscripciones", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    val filtered = remember(videos, filter, selectedChannelId) {
                        val base = if (selectedChannelId == null) videos
                        else {
                            val ch = displaySubs.firstOrNull { it.channelId == selectedChannelId }
                            videos.filter { it.channelId == selectedChannelId || (ch != null && it.artist.equals(ch.channelName, ignoreCase = true)) }
                        }
                        when (filter) {
                            "Shorts" -> base.filter { it.isShort || it.durationSeconds in 1..65 }
                            "Videos" -> base.filter { !it.isShort && it.durationSeconds > 65 }
                            else -> base
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = filtered,
                            key = { index, track -> "${track.id}_$index" }
                        ) { index, track ->
                            VideoCardEnhanced(
                                video = track,
                                onClick = {
                                    playerController?.playQueue(filtered, index, playAsVideo = true)
                                    onExpandPlayer()
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
private fun ManageListItem() {
}

package com.example.tsuki.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.tsuki.R
import com.example.tsuki.data.local.HomePreferences
import com.example.tsuki.data.local.TSukiChannelSubscription
import com.example.tsuki.data.local.TSukiSubscriptionRepository
import com.example.tsuki.data.subscriptions.SubNotifyScheduler
import com.example.tsuki.data.subscriptions.TSukiSubscriptionFeedRepository
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.playback.PlayerController
import com.example.tsuki.ui.components.PermissionRationaleSheet
import com.example.tsuki.ui.components.VideoCardEnhanced
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.itemsIndexed

private enum class SubSort(val labelRes: Int) {
    RECENT(R.string.subs_sort_recent),
    VIEWS(R.string.subs_sort_views),
    CHANNEL(R.string.subs_sort_channel)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SubscriptionsScreen(
    playerController: PlayerController?,
    onExpandPlayer: () -> Unit = {},
    onExploreClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val subRepo = remember { TSukiSubscriptionRepository.getInstance(context) }
    val feedRepo = remember { TSukiSubscriptionFeedRepository(context) }
    val homePrefs = remember { HomePreferences(context) }
    val scope = rememberCoroutineScope()
    val subs by subRepo.getAllSubscriptions().collectAsStateWithLifecycle(initialValue = emptyList())
    val favChannels by homePrefs.favoriteChannels.collectAsStateWithLifecycle(initialValue = emptySet())
    val blockedChannels by homePrefs.blockedChannels.collectAsStateWithLifecycle(initialValue = emptySet())
    val notifyEnabled by homePrefs.subNotifyEnabled.collectAsStateWithLifecycle(initialValue = false)
    var videos by remember { mutableStateOf<List<MediaTrack>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("Todos") }
    var showOnlyNew by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf(SubSort.RECENT) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedChannelId by remember { mutableStateOf<String?>(null) }
    var sheetChannel by remember { mutableStateOf<TSukiChannelSubscription?>(null) }
    var sessionNewIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var lastMarkedKey by remember { mutableStateOf<Set<String>?>(null) }
    var loadJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val displaySubs = remember(subs, favChannels, blockedChannels) {
        val list = if (subs.isNotEmpty()) subs
        else favChannels.mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.isEmpty() || parts[0].isBlank()) null
            else TSukiChannelSubscription(
                channelId = parts[0],
                channelName = parts.getOrNull(1) ?: parts[0],
                channelThumbnail = parts.getOrNull(2) ?: ""
            )
        }
        val distinct = list.distinctBy { it.channelId }
        val (active, muted) = distinct.partition { it.channelId !in blockedChannels }
        active + muted
    }

    fun matchesChannel(track: MediaTrack, sub: TSukiChannelSubscription): Boolean =
        track.channelId == sub.channelId || track.artist.equals(sub.channelName, ignoreCase = true)

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

    LaunchedEffect(videos) {
        if (videos.isNotEmpty()) {
            val key = videos.map { it.id }.toSet()
            if (key != lastMarkedKey) {
                lastMarkedKey = key
                withContext(Dispatchers.IO) {
                    val seen = try { homePrefs.seenSubVideos.first() } catch (_: Exception) { emptySet() }
                    sessionNewIds = key - seen
                    homePrefs.markSeenSubVideos(key)
                }
            }
        }
    }

    val notifyPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch(Dispatchers.IO) {
                homePrefs.setSubNotifyEnabled(true)
                SubNotifyScheduler.setEnabled(context, true)
            }
            Toast.makeText(context, context.getString(R.string.subs_notify_on), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, context.getString(R.string.subs_notify_sys), Toast.LENGTH_LONG).show()
        }
    }

    var showNotifyRationale by remember { mutableStateOf(false) }

    fun toggleNotify() {
        if (notifyEnabled) {
            scope.launch(Dispatchers.IO) {
                homePrefs.setSubNotifyEnabled(false)
                SubNotifyScheduler.setEnabled(context, false)
            }
            Toast.makeText(context, context.getString(R.string.subs_notify_off), Toast.LENGTH_SHORT).show()
        } else {
            val perm = android.Manifest.permission.POST_NOTIFICATIONS
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, perm) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                scope.launch(Dispatchers.IO) {
                    homePrefs.setSubNotifyEnabled(true)
                    SubNotifyScheduler.setEnabled(context, true)
                }
                Toast.makeText(context, context.getString(R.string.subs_notify_on), Toast.LENGTH_SHORT).show()
            } else {
                showNotifyRationale = true
            }
        }
    }

    val backupRepo = remember { com.example.tsuki.data.local.TSukiBackupRepository(context) }
    var exportMsg by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { scope.launch { val r = backupRepo.exportSubscriptionsAsNewPipe(it); android.widget.Toast.makeText(context, if (r.isSuccess) context.getString(R.string.set_backup_ok) else context.getString(R.string.common_error), android.widget.Toast.LENGTH_SHORT).show() } }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { scope.launch { val r = backupRepo.importNewPipe(it); exportMsg = if (r.isSuccess) context.getString(R.string.subs_imported, r.getOrNull()) else context.getString(R.string.common_error); android.widget.Toast.makeText(context, exportMsg ?: "", android.widget.Toast.LENGTH_SHORT).show(); if (r.isSuccess) load() } }
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
                Text(stringResource(R.string.subs_title), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { toggleNotify() }) {
                        Icon(
                            if (notifyEnabled) Icons.Rounded.Notifications else Icons.Rounded.NotificationsOff,
                            contentDescription = stringResource(R.string.subs_notify_title),
                            tint = if (notifyEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { importLauncher.launch("application/json") }) { Icon(Icons.Rounded.Download, contentDescription = null) }
                    IconButton(onClick = { exportLauncher.launch("tsuki_subs_${System.currentTimeMillis()}.json") }) { Icon(Icons.Rounded.Upload, contentDescription = null) }
                    Text(pluralStringResource(R.plurals.subs_channels, displaySubs.size, displaySubs.size), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (displaySubs.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(displaySubs, key = { index, sub -> "${sub.channelId}_$index" }) { _, sub ->
                    val isSelected = selectedChannelId == sub.channelId
                    val isMuted = sub.channelId in blockedChannels
                    val isFav = favChannels.any { it.substringBefore("|") == sub.channelId }
                    val hasNew = videos.any { it.id in sessionNewIds && matchesChannel(it, sub) }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(64.dp)
                            .alpha(if (isMuted) 0.45f else 1f)
                            .combinedClickable(
                                onClick = {
                                    selectedChannelId = if (selectedChannelId == sub.channelId) null else sub.channelId
                                },
                                onLongClick = { sheetChannel = sub }
                            )
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
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
                            if (isFav) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            Icons.Rounded.Star,
                                            contentDescription = stringResource(R.string.common_favorite),
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (hasNew) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 3.dp)
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        } else {
                            Spacer(Modifier.height(10.dp))
                        }
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Todos", "Videos", "Shorts").forEach { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { filter = f },
                        label = { Text(f) }
                    )
                }
                if (sessionNewIds.isNotEmpty()) {
                    FilterChip(
                        selected = showOnlyNew,
                        onClick = { showOnlyNew = !showOnlyNew },
                        label = { Text(stringResource(R.string.subs_new, sessionNewIds.size)) }
                    )
                }
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Rounded.Sort, contentDescription = stringResource(R.string.common_sort))
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        shape = RoundedCornerShape(24.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 3.dp
                    ) {
                        SubSort.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(stringResource(mode.labelRes)) },
                                leadingIcon = {
                                    if (sortMode == mode) Icon(Icons.Rounded.Check, contentDescription = null)
                                },
                                onClick = {
                                    sortMode = mode
                                    showSortMenu = false
                                }
                            )
                        }
                    }
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
                        Text(stringResource(R.string.subs_empty), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(stringResource(R.string.subs_empty_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                onExploreClick?.invoke() ?: run {
                                    android.widget.Toast.makeText(context, context.getString(R.string.subs_empty_hint2), android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.subs_explore), color = MaterialTheme.colorScheme.onPrimary)
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
                        Text(stringResource(R.string.subs_no_recent), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    val filtered = remember(videos, filter, selectedChannelId, showOnlyNew, sortMode, displaySubs) {
                        val byChannel = if (selectedChannelId == null) videos
                        else {
                            val ch = displaySubs.firstOrNull { it.channelId == selectedChannelId }
                            videos.filter { it.channelId == selectedChannelId || (ch != null && it.artist.equals(ch.channelName, ignoreCase = true)) }
                        }
                        val onlyNew = if (showOnlyNew) byChannel.filter { it.id in sessionNewIds } else byChannel
                        val byType = when (filter) {
                            "Shorts" -> onlyNew.filter { it.isShort || it.durationSeconds in 1..65 }
                            "Videos" -> onlyNew.filter { !it.isShort && it.durationSeconds > 65 }
                            else -> onlyNew
                        }
                        when (sortMode) {
                            SubSort.VIEWS -> byType.sortedByDescending { it.viewCount }
                            else -> byType
                        }
                    }
                    if (filtered.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.subs_no_filter), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else if (sortMode == SubSort.CHANNEL && selectedChannelId == null) {
                        val groups = remember(filtered, displaySubs) {
                            val order = displaySubs.map { it.channelId }
                            filtered.groupBy { track ->
                                displaySubs.firstOrNull {
                                    it.channelId == track.channelId || track.artist.equals(it.channelName, ignoreCase = true)
                                }?.channelId ?: track.channelId.orEmpty()
                            }.toList().sortedBy { (id, _) ->
                                order.indexOf(id).let { if (it == -1) Int.MAX_VALUE else it }
                            }
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            groups.forEach { (channelId, groupVideos) ->
                                val sub = displaySubs.firstOrNull { it.channelId == channelId }
                                stickyHeader(key = "ch_$channelId") {
                                    Surface(
                                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            AsyncImage(
                                                model = sub?.channelThumbnail,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                                            )
                                            Text(
                                                sub?.channelName ?: groupVideos.firstOrNull()?.artist.orEmpty(),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                "${groupVideos.size}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                itemsIndexed(
                                    items = groupVideos,
                                    key = { index, track -> "g_${channelId}_${track.id}_$index" }
                                ) { _, track ->
                                    val playIndex = filtered.indexOf(track)
                                    VideoCardEnhanced(
                                        video = track,
                                        onClick = {
                                            playerController?.playQueue(filtered, playIndex.coerceAtLeast(0), playAsVideo = true)
                                            onExpandPlayer()
                                        }
                                    )
                                }
                            }
                        }
                    } else {
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

        val channel = sheetChannel
        if (channel != null) {
            val isFav = favChannels.any { it.substringBefore("|") == channel.channelId }
            val isMuted = channel.channelId in blockedChannels
            val inFeed = videos.count { matchesChannel(it, channel) }
            ModalBottomSheet(
                onDismissRequest = { sheetChannel = null },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        AsyncImage(
                            model = channel.channelThumbnail,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                channel.channelName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (inFeed == 1) "1 video en el feed" else "$inFeed videos en el feed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    SubSheetRow(
                        icon = if (isFav) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        label = if (isFav) stringResource(R.string.subs_unfav) else stringResource(R.string.subs_fav),
                        onClick = {
                            sheetChannel = null
                            scope.launch(Dispatchers.IO) {
                                if (isFav) {
                                    val entry = try { favChannels.first { it.substringBefore("|") == channel.channelId } } catch (_: Exception) { null }
                                    if (entry != null) homePrefs.removeFavoriteChannel(entry)
                                } else {
                                    homePrefs.addFavoriteChannel("${channel.channelId}|${channel.channelName}|${channel.channelThumbnail}")
                                }
                            }
                            Toast.makeText(context, if (isFav) context.getString(R.string.subs_unfav_done) else context.getString(R.string.subs_fav_done), Toast.LENGTH_SHORT).show()
                        }
                    )
                    SubSheetRow(
                        icon = if (isMuted) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                        label = if (isMuted) stringResource(R.string.subs_unmute) else stringResource(R.string.subs_mute),
                        onClick = {
                            sheetChannel = null
                            scope.launch(Dispatchers.IO) {
                                if (isMuted) homePrefs.unblockChannel(channel.channelId)
                                else homePrefs.blockChannel(channel.channelId)
                            }
                            Toast.makeText(context, if (isMuted) context.getString(R.string.subs_unmuted) else context.getString(R.string.subs_muted), Toast.LENGTH_SHORT).show()
                            load()
                        }
                    )
                    SubSheetRow(
                        icon = Icons.Rounded.PersonRemove,
                        label = stringResource(R.string.subs_unfollow),
                        onClick = {
                            sheetChannel = null
                            if (selectedChannelId == channel.channelId) selectedChannelId = null
                            scope.launch(Dispatchers.IO) { subRepo.unsubscribe(channel.channelId) }
                            Toast.makeText(context, context.getString(R.string.subs_unfollowed, channel.channelName), Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        if (showNotifyRationale) {
            PermissionRationaleSheet(
                title = stringResource(R.string.subs_notify_rationale_title),
                body = stringResource(R.string.subs_notify_rationale_body),
                icon = Icons.Rounded.Notifications,
                onConfirm = {
                    showNotifyRationale = false
                    notifyPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                },
                onDismiss = { showNotifyRationale = false }
            )
        }
    }
}

@Composable
private fun SubSheetRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ManageListItem() {
}

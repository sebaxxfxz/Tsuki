package com.example.tsuki.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.tsuki.R
import com.example.tsuki.auth.YouTubeAuthManager
import com.example.tsuki.data.local.HomeLayoutMode
import com.example.tsuki.data.local.HomePreferences
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.network.YouTubeExtractor
import com.example.tsuki.ui.components.ResumeWatchingCard
import com.example.tsuki.ui.components.ShimmerVideoCardFullWidth
import com.example.tsuki.ui.components.ShimmerVideoCardHorizontal
import com.example.tsuki.ui.components.VideoCardCompact
import com.example.tsuki.ui.components.VideoCardEnhanced
import com.example.tsuki.ui.components.VideoCardGrid
import com.example.tsuki.ui.components.VideoCardShort
import com.example.tsuki.ui.viewmodels.TSukiHomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val REVEAL_BATCH = 6

@Composable
private fun HomeSectionHeader(
    title: String,
    subtitle: String? = null,
    count: Int? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (subtitle != null) {
                Text(
                    text = subtitle.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 0.9.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (count != null && count > 0) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun OfflineBanner() {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(Icons.Rounded.WifiOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(16.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = "Modo sin conexión",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Mostrando contenido disponible sin datos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun RevealMoreButton(revealed: Int, total: Int, onReveal: () -> Unit, label: String = "Mostrar más") {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
        FilledTonalButton(onClick = onReveal, shape = RoundedCornerShape(20.dp)) {
            Text(text = "$label  •  $revealed / $total", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TSukiHomeViewModel,
    playerController: com.example.tsuki.playback.PlayerController?,
    onExpandPlayer: () -> Unit = {},
    onPersonalizationClick: () -> Unit = {},
    onShortClick: (List<MediaTrack>, Int) -> Unit = { _, _ -> },
    onLoginClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val extractor = remember { YouTubeExtractor() }
    val focusManager = LocalFocusManager.current

    val homePreferences = remember { HomePreferences(context) }
    val layoutMode by homePreferences.homeLayoutMode.collectAsState(initial = HomeLayoutMode.IMMERSIVE)

    val authManager = remember { YouTubeAuthManager(context) }
    val isLoggedIn by authManager.isLoggedIn.collectAsState(initial = false)
    val accountInfo by authManager.accountInfo.collectAsState(initial = null)
    var showAccountDialog by remember { mutableStateOf(false) }

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<MediaTrack>>(emptyList()) }
    var searchChannels by remember { mutableStateOf<List<YouTubeExtractor.ChannelResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var openChannelResult by remember {
        mutableStateOf<YouTubeExtractor.ChannelResult?>(null)
    }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            delay(120)
            try { searchFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    LaunchedEffect(uiState.sections) {
        val firstTracks = uiState.sections.flatMap { it.tracks }.take(2)
        if (firstTracks.isNotEmpty()) playerController?.prefetchTracks(firstTracks)
    }

    LaunchedEffect(searchResults) {
        if (searchResults.isNotEmpty()) playerController?.prefetchTracks(searchResults.take(2))
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                AnimatedContent(
                    targetState = isSearchActive,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "HomeTopBarSwitch"
                ) { searching ->
                    if (searching) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = {
                                isSearchActive = false
                                searchQuery = ""
                                searchResults = emptyList()
                                searchChannels = emptyList()
                                focusManager.clearFocus()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cerrar búsqueda")
                            }
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { q ->
                                    searchQuery = q
                                    searchJob?.cancel()
                                    if (q.isBlank()) {
                                        searchResults = emptyList()
                                        searchChannels = emptyList()
                                        isSearching = false
                                    } else {
                                        searchJob = scope.launch {
                                            delay(340)
                                            isSearching = true
                                            val videosDeferred = async { withContext(Dispatchers.IO) { extractor.searchVideos(q) } }
                                            val channelsDeferred = async { withContext(Dispatchers.IO) { extractor.searchChannels(q) } }
                                            val videos = videosDeferred.await()
                                            val channels = channelsDeferred.await()
                                            if (searchQuery == q) {
                                                searchResults = videos
                                                searchChannels = channels
                                            }
                                            isSearching = false
                                        }
                                    }
                                },
                                placeholder = { Text("Buscar videos y creadores", style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                trailingIcon = {
                                    when {
                                        isSearching -> Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                                            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        }
                                        searchQuery.isNotEmpty() -> IconButton(onClick = { searchQuery = ""; searchResults = emptyList(); searchChannels = emptyList() }) {
                                            Icon(Icons.Rounded.Close, contentDescription = "Limpiar")
                                        }
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    focusManager.clearFocus()
                                    if (searchQuery.isNotBlank()) {
                                        val q = searchQuery
                                        searchJob?.cancel()
                                        searchJob = scope.launch {
                                            isSearching = true
                                            val videosDeferred = async { withContext(Dispatchers.IO) { extractor.searchVideos(q) } }
                                            val channelsDeferred = async { withContext(Dispatchers.IO) { extractor.searchChannels(q) } }
                                            val videos = videosDeferred.await()
                                            val channels = channelsDeferred.await()
                                            if (searchQuery == q) {
                                                searchResults = videos
                                                searchChannels = channels
                                            }
                                            isSearching = false
                                        }
                                    }
                                }),
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                modifier = Modifier.weight(1f).focusRequester(searchFocusRequester)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_tsuki_kanji),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = "TSUKI",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 1.6.sp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { isSearchActive = true }) {
                                    Icon(Icons.Rounded.Search, contentDescription = "Buscar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val nextMode = when (layoutMode) {
                                        HomeLayoutMode.IMMERSIVE -> HomeLayoutMode.GRID
                                        HomeLayoutMode.GRID -> HomeLayoutMode.COMPACT
                                        HomeLayoutMode.COMPACT -> HomeLayoutMode.IMMERSIVE
                                    }
                                    scope.launch { homePreferences.setHomeLayoutMode(nextMode) }
                                }) {
                                    Icon(
                                        imageVector = when (layoutMode) {
                                            HomeLayoutMode.IMMERSIVE -> Icons.Rounded.ViewAgenda
                                            HomeLayoutMode.GRID -> Icons.Rounded.GridView
                                            HomeLayoutMode.COMPACT -> Icons.AutoMirrored.Rounded.ViewList
                                        },
                                        contentDescription = "Cambiar vista",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (isLoggedIn && accountInfo != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .clickable { showAccountDialog = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!accountInfo?.avatarUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = accountInfo?.avatarUrl,
                                                contentDescription = "Cuenta",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Text(
                                                text = accountInfo?.name?.take(1)?.uppercase() ?: "U",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                } else {
                                    IconButton(onClick = onLoginClick) {
                                        Icon(Icons.Rounded.AccountCircle, contentDescription = "Conectar cuenta", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.isOfflineMode && !isSearchActive,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            OfflineBanner()
        }

        if (isSearchActive) {
            when {
                isSearching && searchResults.isEmpty() -> {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 12.dp, bottom = 140.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(3) { ShimmerVideoCardFullWidth() }
                    }
                }
                searchResults.isEmpty() && searchChannels.isEmpty() && searchQuery.isNotBlank() && !isSearching -> {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.size(64.dp)) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
                                }
                            }
                            Text("Sin resultados para \"$searchQuery\"", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                            Text("Prueba con otras palabras clave", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                searchResults.isNotEmpty() || searchChannels.isNotEmpty() -> {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 8.dp, bottom = 140.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (searchChannels.isNotEmpty()) {
                            item(key = "search_channels_header") {
                                HomeSectionHeader(title = "Canales", subtitle = "BÚSQUEDA", count = searchChannels.size)
                            }
                        }
                        items(searchChannels, key = { it.channelId }) { channel ->
                            ChannelSearchRow(channel = channel, onClick = { openChannelResult = channel })
                        }
                        if (searchResults.isNotEmpty()) {
                            item(key = "search_header") {
                                HomeSectionHeader(title = "Videos", subtitle = "RESULTADOS", count = searchResults.size)
                            }
                        }
                        items(searchResults, key = { it.id }) { track ->
                            VideoCardEnhanced(
                                video = track,
                                onClick = {
                                    viewModel.onTrackClicked(track)
                                    if (playerController != null) {
                                        val idx = searchResults.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                                        playerController.playQueue(searchResults, idx, playAsVideo = true)
                                        onExpandPlayer()
                                    }
                                },
                                onAddToQueue = { item -> playerController?.playQueue(listOf(item), 0, playAsVideo = item.isVideoItem) }
                            )
                        }
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(72.dp)) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp))
                                }
                            }
                            Text("Explora TSuki", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                            Text("Busca videos, artistas y canales", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                            Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(16.dp)) {
                                Text("Tip: pega un link de YouTube para reproducirlo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                            }
                        }
                    }
                }
            }
        } else if (uiState.isLoading && uiState.sections.isEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 12.dp, bottom = 140.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(2) { ShimmerVideoCardFullWidth() }
                items(3) { ShimmerVideoCardHorizontal() }
            }
        } else if (uiState.errorMessage != null && uiState.sections.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.size(56.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Rounded.WifiOff, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(26.dp))
                            }
                        }
                        Text("No se pudo cargar el feed", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        Text(uiState.errorMessage!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FilledTonalButton(onClick = { viewModel.loadFeed() }, shape = RoundedCornerShape(20.dp)) { Text("Reintentar") }
                    }
                }
            }
        } else if (uiState.sections.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(56.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Rounded.SmartDisplay, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
                            }
                        }
                        Text("Preparando tu feed", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Estamos personalizando tus recomendaciones", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FilledTonalButton(onClick = { viewModel.loadFeed() }, shape = RoundedCornerShape(20.dp)) { Text("Actualizar Feed") }
                    }
                }
            }
        } else {
            val displayedSections = remember(uiState.sections) {
                uiState.sections.mapNotNull { section ->
                    val videoTracks = section.tracks.filter { it.isVideoItem }
                    if (videoTracks.isEmpty()) null else section.copy(tracks = videoTracks)
                }
            }
            val resumeItems = remember(uiState.resumeWatching) {
                uiState.resumeWatching.filter { it.isVideoItem }.map { entry ->
                    entry to MediaTrack(
                        id = entry.videoId,
                        title = entry.title,
                        artist = entry.artist,
                        artworkUrl = entry.artworkUrl,
                        isVideoItem = true
                    )
                }
            }
            val gridChunksByTitle = remember(displayedSections) {
                displayedSections.associate { section -> section.title to section.tracks.chunked(2) }
            }
            val revealCounts = remember { androidx.compose.runtime.mutableStateMapOf<String, Int>() }
            fun revealedCount(sectionTitle: String, total: Int): Int = minOf(revealCounts[sectionTitle] ?: REVEAL_BATCH, total)
            fun revealMore(sectionTitle: String, current: Int, total: Int) { revealCounts[sectionTitle] = minOf(current + REVEAL_BATCH, total) }

            PullToRefreshBox(isRefreshing = uiState.isRefreshing, onRefresh = { viewModel.refresh() }, modifier = Modifier.fillMaxSize()) {
                val feedListState = androidx.compose.foundation.lazy.rememberLazyListState()
                val isFeedScrolling by remember { androidx.compose.runtime.derivedStateOf { feedListState.isScrollInProgress } }
                val shouldLoadMore by remember { androidx.compose.runtime.derivedStateOf {
                    if (uiState.forYouContinuation == null) false else {
                        val lastVisible = feedListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        val total = feedListState.layoutInfo.totalItemsCount
                        total > 0 && lastVisible >= total - 3
                    }
                } }
                LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) viewModel.loadMoreForYou() }
                LazyColumn(
                    state = feedListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    if (resumeItems.isNotEmpty()) {
                        item(key = "resume_watching_shelf") {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                HomeSectionHeader(title = "Seguir viendo", subtitle = "CONTINUAR", count = resumeItems.size)
                                Spacer(Modifier.height(6.dp))
                                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(resumeItems, key = { it.first.videoId }, contentType = { "resume_card" }) { (entry, track) ->
                                        ResumeWatchingCard(
                                            track = track,
                                            watchPositionMs = entry.watchDurationMs,
                                            totalDurationMs = null,
                                            onResumeClick = {
                                                viewModel.onTrackClicked(track)
                                                playerController?.playQueue(listOf(track), 0, playAsVideo = true)
                                                playerController?.seekTo(entry.watchDurationMs)
                                                onExpandPlayer()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    displayedSections.forEach { section ->
                        val isShortsShelf = section.title.lowercase().contains("shorts")
                        val isMainVerticalShelf = section.title.lowercase().let { t ->
                            t.contains("para ti") || t.contains("descubrimiento") || t.contains("descargas") || t.contains("tendencias") || t.contains("suscripciones") || t.contains("canales que sigues")
                        }
                        if (isShortsShelf) {
                            item(key = "shorts_${section.title}") {
                                ShortsShelf(tracks = section.tracks, isScrolling = isFeedScrolling, onTrackClick = { track ->
                                    viewModel.onTrackClicked(track)
                                    val idx = section.tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                                    onShortClick(section.tracks, idx)
                                })
                            }
                        } else if (isMainVerticalShelf) {
                            item(key = "header_${section.title}") {
                                val subtitle = when {
                                    section.title.lowercase().contains("para ti") -> "RECOMENDADO PARA TI"
                                    section.title.lowercase().contains("tendencias") -> "POPULAR AHORA"
                                    section.title.lowercase().contains("suscripciones") -> "DE TUS CANALES"
                                    section.title.lowercase().contains("descubrimiento") -> "DESCUBRE ALGO NUEVO"
                                    section.title.lowercase().contains("descargas") -> "DISPONIBLE SIN CONEXIÓN"
                                    else -> null
                                }
                                HomeSectionHeader(title = section.title, subtitle = subtitle, count = section.tracks.size)
                            }
                            when (layoutMode) {
                                HomeLayoutMode.IMMERSIVE -> {
                                    val revealed = revealedCount(section.title, section.tracks.size)
                                    val visibleTracks = section.tracks.take(revealed)
                                    items(visibleTracks, key = { "${section.title}_${it.id}" }, contentType = { "video_card" }) { track ->
                                        VideoCardEnhanced(
                                            video = track,
                                            onClick = {
                                                viewModel.onTrackClicked(track)
                                                val idx = section.tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                                                playerController?.playQueue(section.tracks, idx, playAsVideo = true)
                                                onExpandPlayer()
                                            },
                                            onAddToQueue = { t -> playerController?.playQueue(listOf(t), 0, playAsVideo = t.isVideoItem) }
                                        )
                                    }
                                    if (visibleTracks.size < section.tracks.size) {
                                        item(key = "more_${section.title}", contentType = { "load_more" }) {
                                            RevealMoreButton(revealed = revealed, total = section.tracks.size, onReveal = { revealMore(section.title, revealed, section.tracks.size) })
                                        }
                                    }
                                }
                                HomeLayoutMode.COMPACT -> {
                                    val revealed = revealedCount(section.title, section.tracks.size)
                                    val visibleTracks = section.tracks.take(revealed)
                                    items(visibleTracks, key = { "${section.title}_${it.id}" }, contentType = { "video_compact" }) { track ->
                                        VideoCardCompact(
                                            video = track,
                                            onClick = {
                                                viewModel.onTrackClicked(track)
                                                val idx = section.tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                                                playerController?.playQueue(section.tracks, idx, playAsVideo = true)
                                                onExpandPlayer()
                                            },
                                            onAddToQueue = { t -> playerController?.playQueue(listOf(t), 0, playAsVideo = t.isVideoItem) }
                                        )
                                    }
                                    if (visibleTracks.size < section.tracks.size) {
                                        item(key = "more_${section.title}", contentType = { "load_more" }) {
                                            RevealMoreButton(revealed = revealed, total = section.tracks.size, onReveal = { revealMore(section.title, revealed, section.tracks.size) })
                                        }
                                    }
                                }
                                HomeLayoutMode.GRID -> {
                                    val revealed = revealedCount(section.title, section.tracks.size)
                                    val chunked = gridChunksByTitle[section.title].orEmpty().take((revealed + 1) / 2)
                                    items(chunked, key = { chunk -> "${section.title}_grid_${chunk.firstOrNull()?.id}_${chunk.getOrNull(1)?.id}" }, contentType = { "video_grid" }) { pair ->
                                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            pair.forEach { track ->
                                                Box(modifier = Modifier.weight(1f)) {
                                                    VideoCardGrid(video = track, onClick = {
                                                        viewModel.onTrackClicked(track)
                                                        val idx = section.tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                                                        playerController?.playQueue(section.tracks, idx, playAsVideo = true)
                                                        onExpandPlayer()
                                                    })
                                                }
                                            }
                                            if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                    if (revealed < section.tracks.size) {
                                        item(key = "more_${section.title}", contentType = { "load_more" }) {
                                            RevealMoreButton(revealed = revealed, total = section.tracks.size, onReveal = { revealMore(section.title, revealed, section.tracks.size) })
                                        }
                                    }
                                }
                            }
                        } else {
                            item(key = "hrow_${section.title}") {
                                HorizontalMediaRow(title = section.title, tracks = section.tracks, isScrolling = isFeedScrolling, onTrackClick = { track ->
                                    viewModel.onTrackClicked(track)
                                    val idx = section.tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                                    playerController?.playQueue(section.tracks, idx, playAsVideo = true)
                                    onExpandPlayer()
                                })
                            }
                        }
                    }
                    if (uiState.isLoadingMore && uiState.forYouContinuation != null) {
                        item(key = "loading_more") {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                            }
                        }
                    }
                }
            }
        }
        if (showAccountDialog && accountInfo != null) {
            AlertDialog(
                onDismissRequest = { showAccountDialog = false },
                title = { Text(accountInfo?.name ?: "Cuenta de Google") },
                text = {
                    Column {
                        accountInfo?.email?.takeIf { it.isNotBlank() }?.let {
                            Text("Email: $it", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                        }
                        accountInfo?.channelHandle?.takeIf { it.isNotBlank() }?.let {
                            Text("Canal: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showAccountDialog = false
                        scope.launch { authManager.logout(); viewModel.loadFeed() }
                    }) { Text("Cerrar sesión", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showAccountDialog = false }) { Text("Cerrar") }
                }
            )
        }
    }

    openChannelResult?.let { channel ->
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            ChannelScreen(
                channelId = channel.channelId,
                channelName = channel.name,
                subscriberText = channel.subscriberText,
                avatarUrl = channel.avatarUrl,
                playerController = playerController,
                onPlayVideo = { video, queue ->
                    if (playerController != null) {
                        viewModel.onTrackClicked(video)
                        playerController.playQueue(queue, queue.indexOf(video).coerceAtLeast(0), playAsVideo = true)
                        onExpandPlayer()
                    }
                },
                onBack = { openChannelResult = null }
            )
        }
    }
    }
}

@Composable
private fun ChannelSearchRow(channel: YouTubeExtractor.ChannelResult, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(56.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (!channel.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.avatarUrl,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = channel.name.trim().firstOrNull()?.uppercase() ?: "C",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val metaInfo = listOfNotNull(channel.subscriberText, channel.videoCountText).joinToString(" • ")
            if (metaInfo.isNotBlank()) {
                Text(
                    text = metaInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!channel.description.isNullOrBlank()) {
                Text(
                    text = channel.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ShortsShelf(tracks: List<MediaTrack>, isScrolling: Boolean = false, onTrackClick: (MediaTrack) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HomeSectionHeader(title = "Shorts", subtitle = "VÍDEOS CORTOS", count = tracks.size)
        Spacer(Modifier.height(6.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(tracks, key = { it.id }, contentType = { "short_card" }) { track -> VideoCardShort(track = track, isScrolling = isScrolling, onClick = { onTrackClick(track) }) }
        }
    }
}

@Composable
private fun HorizontalMediaRow(title: String, tracks: List<MediaTrack>, isScrolling: Boolean = false, onTrackClick: (MediaTrack) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HomeSectionHeader(title = title, subtitle = "COLECCIÓN", count = tracks.size)
        Spacer(Modifier.height(6.dp))
        val rowState = androidx.compose.foundation.lazy.rememberLazyListState()
        val isRowScrolling by remember { androidx.compose.runtime.derivedStateOf { rowState.isScrollInProgress } }
        val deferImages = isScrolling || isRowScrolling
        LazyRow(state = rowState, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(tracks, key = { it.id }, contentType = { "media_square" }) { track -> MediaCardSquare(track = track, isScrolling = deferImages, onClick = { onTrackClick(track) }) }
        }
    }
}

@Composable
fun MediaCardSquare(
    track: MediaTrack,
    isScrolling: Boolean = false,
    onClick: () -> Unit
) {
    val isVideo = track.isVideoItem
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "MediaCardBounce"
    )
    Column(
        modifier = Modifier
            .width(if (isVideo) 220.dp else 156.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interactionSource, indication = androidx.compose.material3.ripple(), onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (isVideo) 16f / 9f else 1f)
                .graphicsLayer { shape = RoundedCornerShape(16.dp); clip = true }
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            AsyncImage(model = com.example.tsuki.ui.components.rememberListImageModel(track.artworkUrl), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            if (track.durationText != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.78f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(text = track.durationText!!, color = Color.White, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), fontSize = 11.sp)
                }
            } else {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(text = track.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, lineHeight = 18.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(3.dp))
        Text(
            text = buildString {
                append(track.artist)
                if (!track.viewCountText.isNullOrBlank()) { append(" • "); append(track.viewCountText) }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

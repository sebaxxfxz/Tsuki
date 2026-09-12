package com.example.tsuki.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.tsuki.R
import com.example.tsuki.data.local.HomePreferences
import com.example.tsuki.data.recommendation.TSukiNeuroEngine
import com.example.tsuki.data.recommendation.TSukiTopicCatalog
import com.example.tsuki.util.AppLocale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import androidx.compose.material.icons.Icons as MIcons

private data class ChannelSearchResult(
    val channelId      : String,
    val name           : String,
    val thumbnailUrl   : String,
    val subscriberCount: Long = -1L
)

private suspend fun searchChannels(query: String): List<ChannelSearchResult> =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        fun mapItems(items: List<Any>): List<ChannelSearchResult> =
            items.filterIsInstance<ChannelInfoItem>().mapNotNull { item ->
                val url = item.url ?: return@mapNotNull null
                val channelId = when {
                    url.contains("/channel/") -> url.substringAfter("/channel/").substringBefore("/").substringBefore("?")
                    url.contains("/@") -> url.substringAfter("/@").substringBefore("/").substringBefore("?")
                    url.contains("/c/") -> url.substringAfter("/c/").substringBefore("/").substringBefore("?")
                    url.contains("/user/") -> url.substringAfter("/user/").substringBefore("/").substringBefore("?")
                    else -> url.substringAfterLast("/").substringBefore("?")
                }
                if (channelId.isEmpty() || item.name.isNullOrEmpty()) return@mapNotNull null
                ChannelSearchResult(
                    channelId       = channelId,
                    name            = item.name ?: "",
                    thumbnailUrl    = item.thumbnails.sortedByDescending { it.height }.firstOrNull()?.url ?: "",
                    subscriberCount = item.subscriberCount
                )
            }.take(15)
        try {
            val ext = ServiceList.YouTube.getSearchExtractor(query.trim(), listOf("channels"), null)
            ext.fetchPage()
            val result = mapItems(ext.initialPage.items)
            if (result.isNotEmpty()) return@withContext result
        } catch (_: Exception) {}
        try {
            val ext2 = ServiceList.YouTube.getSearchExtractor(query.trim())
            ext2.fetchPage()
            mapItems(ext2.initialPage.items)
        } catch (_: Exception) { emptyList() }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { HomePreferences(context) }

    var step by remember { mutableStateOf(0) }
    var selectedTopics by remember { mutableStateOf<Set<String>>(emptySet()) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<ChannelSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var subscribed by remember { mutableStateOf<Set<String>>(emptySet()) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var appLocale by remember { mutableStateOf(AppLocale.readStored(context)) }
    var showLanguageMenu by remember { mutableStateOf(false) }

    val isEnglish = AppLocale.resolveTag(appLocale) == AppLocale.ENGLISH

    val stepTitles = listOf(
        stringResource(R.string.onboard_step_interests),
        stringResource(R.string.onboard_step_channels)
    )
    val totalSteps = 2

    fun finish() {
        scope.launch {
            val resolved = AppLocale.resolveTag(appLocale)
            val nextLang = if (resolved == AppLocale.ENGLISH) "en" else "es"
            val nextCountry = if (resolved == AppLocale.ENGLISH) "US" else "ES"
            prefs.setContentLanguage(nextLang)
            prefs.setContentCountry(nextCountry)
            com.example.tsuki.network.TSukiContentLocale.languageTag = nextLang
            com.example.tsuki.network.TSukiContentLocale.countryCode = nextCountry
            try { java.io.File(context.cacheDir, "tsuki_home_feed_cache.json").delete() } catch (_: Exception) {}

            TSukiNeuroEngine.completeOnboarding(selectedTopics)
            prefs.setSelectedTopics(selectedTopics)
            prefs.setOnboardingDone(true)
            onComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stepTitles.getOrElse(step) { "" },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            stringResource(R.string.onboard_step_of, step + 1, totalSteps),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Box {
                        Surface(
                            onClick = { showLanguageMenu = true },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = MIcons.Rounded.Language,
                                    contentDescription = stringResource(R.string.lang_app_title),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isEnglish) "EN" else "ES",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false }
                        ) {
                            val options = listOf(
                                "" to stringResource(R.string.lang_app_system),
                                AppLocale.SPANISH to stringResource(R.string.lang_app_spanish),
                                AppLocale.ENGLISH to stringResource(R.string.lang_app_english)
                            )
                            options.forEach { (tag, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = label,
                                            fontWeight = if (appLocale == tag) FontWeight.Bold else FontWeight.Normal,
                                            color = if (appLocale == tag) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        showLanguageMenu = false
                                        if (tag != appLocale) {
                                            appLocale = tag
                                            scope.launch {
                                                prefs.setAppLocale(tag)
                                                (context as? android.app.Activity)?.let { AppLocale.applyAndRecreate(it, tag) }
                                            }
                                        }
                                    },
                                    leadingIcon = if (appLocale == tag) {
                                        { Icon(MIcons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 0) {
                    OutlinedButton(onClick = { step-- }) { Text(stringResource(R.string.common_back)) }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                val canAdvance = if (step == 0) selectedTopics.size >= 3 else true
                val nextLabel = if (step == totalSteps - 1) stringResource(R.string.common_finish) else stringResource(R.string.common_next)
                FilledTonalButton(
                    onClick = { if (step == totalSteps - 1) finish() else step++ },
                    enabled = canAdvance
                ) { Text(nextLabel) }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        androidx.compose.material3.LinearProgressIndicator(
            progress = { (step + 1).toFloat() / totalSteps.toFloat() },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
        )

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState > initialState
                val enter = if (forward)
                    slideInHorizontally { it / 4 } + fadeIn()
                else
                    slideInHorizontally { -it / 4 } + fadeIn()
                val exit = if (forward)
                    slideOutHorizontally { -it / 4 } + fadeOut()
                else
                    slideOutHorizontally { it / 4 } + fadeOut()
                enter togetherWith exit
            },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) { current ->
            when (current) {
                0 -> InterestsStep(
                    isEnglish = isEnglish,
                    selectedTopics = selectedTopics,
                    onToggle = { topic ->
                        val canonical = TSukiTopicCatalog.getCanonicalTopic(topic)
                        selectedTopics = if (selectedTopics.contains(topic) || selectedTopics.contains(canonical)) {
                            selectedTopics.filterNot { it.equals(topic, true) || it.equals(canonical, true) }.toSet()
                        } else {
                            selectedTopics + canonical
                        }
                    }
                )
                1 -> ChannelsStep(
                    searchQuery = searchQuery,
                    searchResults = searchResults,
                    isSearching = isSearching,
                    subscribed = subscribed,
                    onQueryChange = { q ->
                        searchQuery = q
                        searchJob?.cancel()
                        if (q.isBlank()) {
                            searchResults = emptyList(); isSearching = false; return@ChannelsStep
                        }
                        searchJob = scope.launch {
                            delay(400)
                            isSearching = true
                            searchResults = searchChannels(q)
                            isSearching = false
                        }
                    },
                    onToggle = { result ->
                        scope.launch {
                            if (subscribed.contains(result.channelId)) {
                                subscribed = subscribed - result.channelId
                            } else {
                                prefs.addFavoriteChannel("${result.channelId}|${result.name}|${result.thumbnailUrl}")
                                try {
                                    com.example.tsuki.data.local.TSukiSubscriptionRepository.getInstance(context).subscribe(
                                        com.example.tsuki.data.local.TSukiChannelSubscription(
                                            channelId = result.channelId,
                                            channelName = result.name,
                                            channelThumbnail = result.thumbnailUrl
                                        )
                                    )
                                } catch (_: Exception) {}
                                subscribed = subscribed + result.channelId
                            }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InterestsStep(isEnglish: Boolean, selectedTopics: Set<String>, onToggle: (String) -> Unit) {
    val categories    = remember(isEnglish) { TSukiTopicCatalog.getCategories(isEnglish) }
    val remaining     = (3 - selectedTopics.size).coerceAtLeast(0)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.onboard_interests_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (remaining > 0)
                        stringResource(R.string.onboard_pick_more, remaining)
                    else
                        stringResource(R.string.onboard_ready),
                    style = MaterialTheme.typography.bodyMedium,
                    color  = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        itemsIndexed(categories) { _, category ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(category.icon, fontSize = 18.sp)
                    Text(
                        category.name.uppercase(),
                        style  = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color  = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val count = category.topics.count { t ->
                        selectedTopics.contains(t) || selectedTopics.contains(TSukiTopicCatalog.getCanonicalTopic(t))
                    }
                    if (count > 0) {
                        Text(
                            count.toString(),
                            style  = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color  = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    category.topics.forEach { topic ->
                        val canonical = TSukiTopicCatalog.getCanonicalTopic(topic)
                        val selected = selectedTopics.contains(topic) || selectedTopics.contains(canonical)
                        FilterChip(
                            selected  = selected,
                            onClick   = { onToggle(topic) },
                            label     = { Text(topic, style = MaterialTheme.typography.labelLarge, maxLines = 1) },
                            leadingIcon = if (selected) {
                                { Icon(MIcons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor  = MaterialTheme.colorScheme.primary,
                                selectedLabelColor      = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}


@Composable
private fun ChannelsStep(
    searchQuery   : String,
    searchResults : List<ChannelSearchResult>,
    isSearching   : Boolean,
    subscribed    : Set<String>,
    onQueryChange : (String) -> Unit,
    onToggle      : (ChannelSearchResult) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager   = LocalFocusManager.current
    LaunchedEffect(Unit) {
        delay(200)
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp)
    ) {
        Text(
            stringResource(R.string.onboard_channels_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(R.string.onboard_channels_sub),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value       = searchQuery,
            onValueChange = onQueryChange,
            modifier    = Modifier.fillMaxWidth().focusRequester(focusRequester),
            placeholder = { Text(stringResource(R.string.onboard_channels_hint)) },
            leadingIcon = { Icon(MIcons.Outlined.Search, contentDescription = null) },
            trailingIcon = if (isSearching) {
                { CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp) }
            } else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            shape = RoundedCornerShape(28.dp)
        )
        Spacer(Modifier.height(10.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            if (searchQuery.isBlank()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                MIcons.Outlined.Search,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.onboard_search_hint_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            } else if (searchResults.isEmpty() && !isSearching) {
                item {
                    Text(
                        stringResource(R.string.home_no_results, searchQuery),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            }
            items(searchResults.size, key = { searchResults[it].channelId }) { idx ->
                val result       = searchResults[idx]
                val isSubscribed = subscribed.contains(result.channelId)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model       = result.thumbnailUrl,
                        contentDescription = null,
                        modifier    = Modifier.size(44.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(result.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (result.subscriberCount > 0) {
                            Text("${result.subscriberCount} subs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f))
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    if (isSubscribed) {
                        FilledTonalButton(
                            onClick = { onToggle(result) },
                            modifier = Modifier.height(38.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Icon(MIcons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.onboard_following))
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onToggle(result) },
                            modifier = Modifier.height(38.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Icon(MIcons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.onboard_follow))
                        }
                    }
                }
            }
            if (subscribed.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.onboard_added, subscribed.size),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

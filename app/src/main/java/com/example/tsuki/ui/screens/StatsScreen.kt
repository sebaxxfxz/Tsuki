package com.example.tsuki.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.tsuki.data.local.WatchHistoryManager
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.launch
import com.example.tsuki.ui.components.M3MotionTokens
import com.example.tsuki.ui.components.m3StaggeredEntrance
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class StatsUiData(
    val totals: WatchHistoryManager.ListeningTotals = WatchHistoryManager.ListeningTotals(0, 0L, 0, 0),
    val topSongs: List<WatchHistoryManager.TopEntry> = emptyList(),
    val topArtists: List<WatchHistoryManager.TopEntry> = emptyList(),
    val hourBuckets: List<Long> = List(24) { 0L },
    val weekdayBuckets: List<Long> = List(7) { 0L }
)

private const val DAY_MS = 24L * 3600L * 1000L


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val historyManager = remember { WatchHistoryManager.getInstance(context) }
    var periodIndex by remember { mutableIntStateOf(1) }
    var stats by remember { mutableStateOf(StatsUiData()) }
    var isLoading by remember { mutableStateOf(true) }
    var daily by remember { mutableStateOf<List<WatchHistoryManager.DailyListen>>(emptyList()) }
    var weekly by remember { mutableStateOf<List<WatchHistoryManager.WeeklyWrapped>>(emptyList()) }
    var monthlyWrapped by remember { mutableStateOf<WatchHistoryManager.WeeklyWrapped?>(null) }
    var showMonthlyWrapped by remember { mutableStateOf(false) }
    var showShareCard by remember { mutableStateOf(false) }
    var statsCardBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var statsDialogWindow by remember { mutableStateOf<android.view.Window?>(null) }
    val statsScope = androidx.compose.runtime.rememberCoroutineScope()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    fun shareStatsCard() {
        val bounds = statsCardBounds
        val window = statsDialogWindow
        if (window == null || bounds == null || android.os.Build.VERSION.SDK_INT < 26) {
            android.widget.Toast.makeText(context, "No se pudo generar la imagen", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val width = bounds.width.toInt().coerceAtLeast(1)
        val height = bounds.height.toInt().coerceAtLeast(1)
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val copyRect = android.graphics.Rect(bounds.left.toInt(), bounds.top.toInt(), bounds.right.toInt(), bounds.bottom.toInt())
        android.view.PixelCopy.request(
            window,
            copyRect,
            bitmap,
            { result ->
                if (result == android.view.PixelCopy.SUCCESS) {
                    showShareCard = false
                    statsScope.launch {
                        val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            runCatching {
                                val dir = java.io.File(context.cacheDir, "shared").apply { mkdirs() }
                                val file = java.io.File(dir, "tsuki_stats.png")
                                file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(android.content.Intent.createChooser(sendIntent, "Compartir"))
                                true
                            }.getOrDefault(false)
                        }
                        if (!ok) android.widget.Toast.makeText(context, "No se pudo generar la imagen", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else {
                    android.widget.Toast.makeText(context, "No se pudo generar la imagen", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            android.os.Handler(android.os.Looper.getMainLooper())
        )
    }

    val periods = listOf("7 días" to 7L, "30 días" to 30L, "90 días" to 90L, "Todo" to 0L)

    LaunchedEffect(periodIndex) {
        isLoading = true
        val days = periods[periodIndex].second
        val fromTs = if (days <= 0L) 0L else System.currentTimeMillis() - days * DAY_MS
        stats = StatsUiData(
            totals = historyManager.getListeningTotals(fromTs),
            topSongs = historyManager.getTopSongs(10, fromTs),
            topArtists = historyManager.getTopArtists(5, fromTs),
            hourBuckets = historyManager.getHourDistribution(fromTs),
            weekdayBuckets = historyManager.getWeekdayDistribution(fromTs)
        )
        isLoading = false
    }

    LaunchedEffect(Unit) {
        daily = historyManager.getDailyListenTime(365)
        weekly = historyManager.getWeeklyWrapped(weeksCount = 12, topLimit = 5)
        monthlyWrapped = historyManager.getMonthlyWrapped(5)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text("Estadísticas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { androidx.compose.material3.SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item(key = "period_chips") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).m3StaggeredEntrance(0),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    periods.forEachIndexed { index, (label, _) ->
                        FilterChip(
                            selected = periodIndex == index,
                            onClick = { periodIndex = index },
                            label = { Text(label) }
                        )
                    }
                }
            }

            item(key = "monthly_wrapped") {
                monthlyWrapped?.let { month ->
                    if (month.plays > 0) {
                        val monthFormatter = remember { SimpleDateFormat("MMMM 'de' yyyy", Locale("es")) }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .m3StaggeredEntrance(0)
                                .clickable { showMonthlyWrapped = true }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(26.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Wrapped del mes",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        "Tu resumen de ${monthFormatter.format(Date(month.weekStartMs))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            item(key = "share_summary") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    androidx.compose.material3.FilledTonalButton(
                        onClick = { showShareCard = true },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                        enabled = stats.totals.totalPlays > 0
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compartir resumen")
                    }
                }
            }

            item(key = "hero") {
                StatsHero(
                    totalMs = stats.totals.totalTimeListenedMs,
                    plays = stats.totals.totalPlays,
                    uniqueSongs = stats.totals.uniqueSongs,
                    uniqueArtists = stats.totals.uniqueArtists
                )
            }

            if (!isLoading && stats.totals.totalPlays == 0 && daily.isEmpty()) {
                item(key = "empty") {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Aún no hay datos de escucha.\nReproduce música para generar estadísticas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                return@LazyColumn
            }

            item(key = "heatmap") {
                Column(modifier = Modifier.fillMaxWidth().m3StaggeredEntrance(1), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Actividad del último año",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    ListenHeatmap(daily = daily)
                }
            }

            if (weekly.any { it.plays > 0 }) {
                item(key = "weekly_wrapped") {
                    Column(modifier = Modifier.fillMaxWidth().m3StaggeredEntrance(2), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Wrapped semanal",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        WeeklyWrappedRow(weekly = weekly)
                    }
                }
            }

            item(key = "patterns_hour") {
                Column(modifier = Modifier.fillMaxWidth().m3StaggeredEntrance(3)) {
                    StatsBarChart(
                        title = "Patrón por hora del día",
                        buckets = stats.hourBuckets,
                        labels = (0 until 24 step 3).map { String.format(Locale.US, "%02d", it) }
                    )
                }
            }

            item(key = "patterns_weekday") {
                StatsBarChart(
                    title = "Patrón por día de la semana",
                    buckets = stats.weekdayBuckets,
                    labels = listOf("D", "L", "M", "X", "J", "V", "S")
                )
            }

            if (stats.topSongs.isNotEmpty()) {
                item(key = "top_songs_header") {
                    Text(
                        "Top canciones",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp).m3StaggeredEntrance(4)
                    )
                }
                itemsIndexed(stats.topSongs, key = { index, entry -> "song_${entry.title}_${entry.subtitle}_$index" }) { index, entry ->
                    TopEntryRow(entry = entry, rank = index + 1, modifier = Modifier.m3StaggeredEntrance(5 + index))
                }
            }

            if (stats.topArtists.isNotEmpty()) {
                item(key = "top_artists_header") {
                    Text(
                        "Top artistas",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                itemsIndexed(stats.topArtists, key = { index, entry -> "artist_${entry.title}_$index" }) { index, entry ->
                    TopEntryRow(entry = entry, rank = index + 1, modifier = Modifier.m3StaggeredEntrance(index))
                }
            }
        }
    }

    if (showMonthlyWrapped) {
        monthlyWrapped?.let { month ->
            val monthFormatter = remember { SimpleDateFormat("MMMM 'de' yyyy", Locale("es")) }
            com.example.tsuki.ui.components.WeeklyWrappedOverlay(
                week = month,
                isCurrentWeek = true,
                onDismiss = { showMonthlyWrapped = false },
                titleText = "TU MES",
                periodLabel = monthFormatter.format(Date(month.weekStartMs)).replaceFirstChar { it.uppercase() },
                badgeText = "Este mes"
            )
        }
    }

    if (showShareCard) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showShareCard = false }) {
            statsDialogWindow = (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                com.example.tsuki.ui.player.StatsShareCard(
                    periodLabel = periods[periodIndex].first,
                    totalTimeMs = stats.totals.totalTimeListenedMs,
                    plays = stats.totals.totalPlays,
                    uniqueSongs = stats.totals.uniqueSongs,
                    uniqueArtists = stats.totals.uniqueArtists,
                    topSongs = stats.topSongs,
                    modifier = Modifier
                        .width(300.dp)
                        .onGloballyPositioned { statsCardBounds = it.boundsInWindow() }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { showShareCard = false },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                    ) { Text("Cancelar") }
                    androidx.compose.material3.Button(
                        onClick = { shareStatsCard() },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                    ) { Text("Compartir imagen") }
                }
            }
        }
    }
}

@Composable
private fun StatsHero(totalMs: Long, plays: Int, uniqueSongs: Int, uniqueArtists: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val entrance by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = M3MotionTokens.ExpressiveHeroSpring,
        label = "StatsHeroEntrance"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .graphicsLayer {
                alpha = entrance
                translationY = (1f - entrance) * 40f
            }
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "TIEMPO TOTAL",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 3.sp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                formatTotalTime(totalMs),
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatPill("$plays", "reproducciones", Modifier.weight(1.6f))
                StatPill("$uniqueSongs", "canciones", Modifier.weight(1f))
                StatPill("$uniqueArtists", "artistas", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatPill(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ListenHeatmap(daily: List<WatchHistoryManager.DailyListen>) {
    if (daily.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Text(
                "Sin actividad registrada todavía",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp)
            )
        }
        return
    }

    val cellSize = 13.dp
    val gap = 3.dp
    val weekCount = 53
    val byDay = remember(daily) { daily.associateBy { it.dayStartMs } }
    val lastWeekStart = remember { startOfWeekMs(System.currentTimeMillis()) }
    val firstWeekStart = remember { lastWeekStart - (weekCount - 1) * 7L * DAY_MS }
    val todayStart = remember { startOfDayMs(System.currentTimeMillis()) }
    val maxMs = remember(daily) { daily.maxOfOrNull { it.timeListenedMs } ?: 0L }
    val levelColors = listOf(
        MaterialTheme.colorScheme.surfaceContainerHighest,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
        MaterialTheme.colorScheme.primary.copy(alpha = 0.80f),
        MaterialTheme.colorScheme.primary
    )
    val monthFormatter = remember { SimpleDateFormat("MMM", Locale("es")) }
    val dayFormatter = remember { SimpleDateFormat("EEE d MMM", Locale("es")) }
    var selectedDay by remember { mutableStateOf<Long?>(null) }
    val scrollState = rememberScrollState()

    LaunchedEffect(daily) {
        snapshotFlow { scrollState.maxValue }.filter { it > 0 }.first()
        scrollState.scrollTo(scrollState.maxValue)
    }

    fun levelFor(ms: Long): Int {
        if (ms <= 0L || maxMs <= 0L) return 0
        val q = maxMs / 4f
        return when {
            ms <= q -> 1
            ms <= q * 2 -> 2
            ms <= q * 3 -> 3
            else -> 4
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                Box(modifier = Modifier.width(14.dp).height(16.dp))
                listOf(0, 3).forEach { row ->
                    Box(modifier = Modifier.width(14.dp).height(cellSize), contentAlignment = Alignment.Center) {
                        Text(
                            if (row == 0) "L" else "J",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.horizontalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    var previousMonth = -1
                    repeat(weekCount) { wi ->
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = firstWeekStart + wi * 7L * DAY_MS
                        val month = cal.get(Calendar.MONTH)
                        val showLabel = month != previousMonth
                        previousMonth = month
                        Box(modifier = Modifier.width(cellSize).height(16.dp)) {
                            if (showLabel) {
                                Text(
                                    monthFormatter.format(cal.time).take(1).uppercase(Locale("es")),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                (0 until 7).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        repeat(weekCount) { wi ->
                            val dayStart = firstWeekStart + wi * 7L * DAY_MS + row * DAY_MS
                            if (dayStart > todayStart) {
                                Box(modifier = Modifier.size(cellSize))
                            } else {
                                val listen = byDay[dayStart]
                                val isSelected = selectedDay == dayStart
                                val cellColor by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface else levelColors[levelFor(listen?.timeListenedMs ?: 0L)],
                                    animationSpec = M3MotionTokens.effectsDefault(),
                                    label = "HeatCellColor"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(cellSize)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(cellColor)
                                        .clickable(
                                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                            indication = null
                                        ) { selectedDay = dayStart }
                                )
                            }
                        }
                    }
                }
            }
        }

        val selectedListen = selectedDay?.let { byDay[it] }
        val selectedLabel = selectedDay?.let { day ->
            selectedListen?.let { "${formatTotalTime(it.timeListenedMs)} · ${dayFormatter.format(Date(day)).lowercase(Locale("es"))}" }
                ?: "Sin escucha ese día"
        }
        Text(
            selectedLabel ?: "Toca un día para ver el detalle",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (selectedListen != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text("Menos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            levelColors.forEach { color ->
                Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
            }
            Text("Más", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WeeklyWrappedRow(weekly: List<WatchHistoryManager.WeeklyWrapped>) {
    val currentWeekStart = remember { startOfWeekMs(System.currentTimeMillis()) }
    val startFormatter = remember { SimpleDateFormat("d", Locale("es")) }
    val endFormatter = remember { SimpleDateFormat("d MMM", Locale("es")) }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        state = rememberLazyListState()
    ) {
        itemsIndexed(weekly, key = { _, w -> "week_${w.weekStartMs}" }) { _, week ->
            val isCurrent = week.weekStartMs == currentWeekStart
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = if (isCurrent) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier.width(280.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Semana ${startFormatter.format(Date(week.weekStartMs))}–${endFormatter.format(Date(week.weekStartMs + 6L * DAY_MS))}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (isCurrent) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                Text(
                                    "Esta semana",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            formatTotalTime(week.totalTimeMs),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "${week.plays} reproducciones",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (week.topSongs.isNotEmpty()) {
                        Text(
                            "Top canciones",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            week.topSongs.take(3).forEachIndexed { index, song ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = Modifier.size(40.dp)) {
                                        AsyncImage(
                                            model = song.artworkUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                        Text(song.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(song.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text(
                                        "${index + 1} · ${song.plays}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    if (week.topArtists.isNotEmpty()) {
                        Text(
                            "Top artistas",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            week.topArtists.take(3).forEachIndexed { index, artist ->
                                Text(
                                    "${index + 1}. ${artist.title} · ${artist.plays} plays",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsBarChart(title: String, buckets: List<Long>, labels: List<String>) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        val groupCount = labels.size
        if (groupCount == 0) return
        val groupSize = if (buckets.isEmpty()) 1 else ((buckets.size + groupCount - 1) / groupCount).coerceAtLeast(1)
        val grouped: List<Long> = (0 until groupCount).map { g ->
            buckets.drop(g * groupSize).take(groupSize).sum()
        }
        val maxMs = grouped.maxOrNull()?.takeIf { it > 0 } ?: 1L
        Row(modifier = Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            grouped.forEachIndexed { index, ms ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                        if (ms == maxMs) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((ms.toFloat() / maxMs * 90f).dp.coerceAtLeast(6.dp))
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary
                                            )
                                        )
                                    )
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((if (maxMs > 0) (ms.toFloat() / maxMs * 90f).dp.coerceAtLeast(6.dp) else 6.dp))
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f))
                            )
                        }
                    }
                    Text(labels[index], style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun TopEntryRow(entry: WatchHistoryManager.TopEntry, rank: Int, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            "$rank",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.width(44.dp),
            maxLines = 1
        )
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.size(48.dp)) {
            AsyncImage(
                model = entry.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(entry.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(entry.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${entry.plays}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
            Text(formatShortTime(entry.timeListenedMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun startOfDayMs(timestamp: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = timestamp
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun startOfWeekMs(timestamp: Long): Long {
    val cal = Calendar.getInstance()
    cal.firstDayOfWeek = Calendar.MONDAY
    cal.timeInMillis = startOfDayMs(timestamp)
    val dow = cal.get(Calendar.DAY_OF_WEEK)
    val diff = if (dow == Calendar.SUNDAY) 6 else dow - Calendar.MONDAY
    cal.add(Calendar.DAY_OF_YEAR, -diff)
    return cal.timeInMillis
}

private fun formatTotalTime(ms: Long): String {
    val totalMin = ms / 60000
    return when {
        totalMin >= 1440 -> String.format(Locale.getDefault(), "%dd %dh", totalMin / 1440, (totalMin % 1440) / 60)
        totalMin >= 60 -> String.format(Locale.getDefault(), "%dh %dm", totalMin / 60, totalMin % 60)
        else -> "${totalMin}m"
    }
}

private fun formatShortTime(ms: Long): String {
    val totalMin = ms / 60000
    return when {
        totalMin >= 60 -> String.format(Locale.getDefault(), "%dh", totalMin / 60)
        totalMin > 0 -> "${totalMin}m"
        else -> ""
    }
}

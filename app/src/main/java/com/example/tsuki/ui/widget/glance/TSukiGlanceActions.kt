package com.example.tsuki.ui.widget.glance

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.example.tsuki.MainActivity
import com.example.tsuki.data.local.FavoritesManager
import com.example.tsuki.data.local.WatchHistoryManager
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.playback.PlayerController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TogglePlayAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val appContext = context.applicationContext
        runCatching {
            val prefs = getAppWidgetState(appContext, PreferencesGlanceStateDefinition, glanceId)
            val current = prefs[TSukiGlanceKeys.IsPlaying] == true
            val nextState = !current
            updateAllGlanceWidgets(appContext) {
                it[TSukiGlanceKeys.IsPlaying] = nextState
            }
        }
        withContext(Dispatchers.Main) {
            runCatching {
                val controller = PlayerController.getInstance(appContext)
                val state = controller.uiState.value
                if (state.currentTrack == null && state.queue.isEmpty()) {
                    val recent = WatchHistoryManager.getInstance(appContext).getRecentTracks(1).firstOrNull()
                    if (recent != null) {
                        controller.playQueue(listOf(recent), 0)
                        return@runCatching
                    }
                }
                controller.togglePlayPause()
            }
        }
    }
}

class PlayNextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val appContext = context.applicationContext
        withContext(Dispatchers.Main) {
            runCatching {
                val controller = PlayerController.getInstance(appContext)
                val state = controller.uiState.value
                if (state.queue.isEmpty()) {
                    val recent = WatchHistoryManager.getInstance(appContext).getRecentTracks(10)
                    if (recent.isNotEmpty()) {
                        controller.playQueue(recent, 0)
                        return@runCatching
                    }
                }
                controller.playNext()
            }
        }
    }
}

class PlayPrevAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val appContext = context.applicationContext
        withContext(Dispatchers.Main) {
            runCatching {
                val controller = PlayerController.getInstance(appContext)
                val state = controller.uiState.value
                if (state.queue.isEmpty()) {
                    val recent = WatchHistoryManager.getInstance(appContext).getRecentTracks(10)
                    if (recent.isNotEmpty()) {
                        controller.playQueue(recent, 0)
                        return@runCatching
                    }
                }
                controller.playPrevious()
            }
        }
    }
}

class ToggleShuffleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val appContext = context.applicationContext
        runCatching {
            val prefs = getAppWidgetState(appContext, PreferencesGlanceStateDefinition, glanceId)
            val current = prefs[TSukiGlanceKeys.Shuffle] == true
            val nextState = !current
            updateAllGlanceWidgets(appContext) {
                it[TSukiGlanceKeys.Shuffle] = nextState
            }
        }
        withContext(Dispatchers.Main) {
            runCatching {
                PlayerController.getInstance(appContext).toggleShuffle()
            }
        }
    }
}

class ToggleRepeatAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val appContext = context.applicationContext
        runCatching {
            val prefs = getAppWidgetState(appContext, PreferencesGlanceStateDefinition, glanceId)
            val current = prefs[TSukiGlanceKeys.RepeatMode] ?: 0
            val nextMode = (current + 1) % 3
            updateAllGlanceWidgets(appContext) {
                it[TSukiGlanceKeys.RepeatMode] = nextMode
            }
        }
        withContext(Dispatchers.Main) {
            runCatching {
                PlayerController.getInstance(appContext).toggleRepeat()
            }
        }
    }
}

class ToggleFavoriteAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val appContext = context.applicationContext
        runCatching {
            val prefs: Preferences = getAppWidgetState(appContext, PreferencesGlanceStateDefinition, glanceId)
            val mediaId: String = prefs[TSukiGlanceKeys.MediaId].orEmpty()
            val title: String = prefs[TSukiGlanceKeys.Title].orEmpty()
            val artist: String = prefs[TSukiGlanceKeys.Artist].orEmpty()
            val artwork: String = prefs[TSukiGlanceKeys.ArtworkUrl].orEmpty()
            val current = prefs[TSukiGlanceKeys.IsFavorite] == true
            val nextFav = !current
            updateAllGlanceWidgets(appContext) {
                it[TSukiGlanceKeys.IsFavorite] = nextFav
            }
            if (mediaId.isNotBlank()) {
                val track = MediaTrack(
                    id = mediaId,
                    title = title.ifBlank { mediaId },
                    artist = artist,
                    artworkUrl = artwork.ifBlank { null },
                    videoId = mediaId.takeIf { it.length == 11 }
                )
                withContext(Dispatchers.IO) {
                    FavoritesManager.getInstance(appContext).toggleFavorite(track)
                }
                TSukiGlanceSync.pushCurrentState(appContext)
            }
        }
    }
}

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val appContext = context.applicationContext
        runCatching {
            TSukiGlanceSync.pushCurrentState(appContext)
        }
    }
}

suspend fun updateAllGlanceWidgets(
    context: Context,
    transform: (MutablePreferences) -> Unit
) {
    val manager = GlanceAppWidgetManager(context)
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val pairs = listOf(
        Pair(TSukiGlanceWidget::class.java, TSukiGlanceReceiver::class.java),
        Pair(TSukiMiniGlanceWidget::class.java, TSukiMiniGlanceReceiver::class.java),
        Pair(TSukiSquareGlanceWidget::class.java, TSukiSquareGlanceReceiver::class.java),
        Pair(TSukiQuickResumeGlanceWidget::class.java, TSukiQuickResumeGlanceReceiver::class.java),
        Pair(TSukiHeroGlanceWidget::class.java, TSukiHeroGlanceReceiver::class.java)
    )
    for ((widgetClass, receiverClass) in pairs) {
        val directIds = runCatching { manager.getGlanceIds(widgetClass) }.getOrDefault(emptyList())
        val nativeIds = runCatching {
            appWidgetManager.getAppWidgetIds(ComponentName(context, receiverClass))
        }.getOrDefault(intArrayOf())
        val mappedIds = nativeIds.toList().mapNotNull { id -> runCatching { manager.getGlanceIdBy(id) }.getOrNull() }
        val allIds = (directIds + mappedIds).distinct()
        for (id in allIds) {
            runCatching {
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { current ->
                    current.toMutablePreferences().apply(transform).toPreferences()
                }
                when (widgetClass) {
                    TSukiGlanceWidget::class.java -> TSukiGlanceWidget().update(context, id)
                    TSukiMiniGlanceWidget::class.java -> TSukiMiniGlanceWidget().update(context, id)
                    TSukiSquareGlanceWidget::class.java -> TSukiSquareGlanceWidget().update(context, id)
                    TSukiQuickResumeGlanceWidget::class.java -> TSukiQuickResumeGlanceWidget().update(context, id)
                    TSukiHeroGlanceWidget::class.java -> TSukiHeroGlanceWidget().update(context, id)
                }
            }
        }
    }
}

fun openAppIntent(context: Context): Intent {
    return Intent(context, MainActivity::class.java).apply {
        action = "com.example.tsuki.action.OPEN_PLAYER"
        data = android.net.Uri.parse("tsuki://player")
        putExtra("open_player", true)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
}

fun openAppDestinationIntent(context: Context, destination: String): Intent {
    return Intent(context, MainActivity::class.java).apply {
        action = "com.example.tsuki.action.OPEN_$destination"
        data = android.net.Uri.parse("tsuki://destination/$destination")
        putExtra("destination", destination)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
}

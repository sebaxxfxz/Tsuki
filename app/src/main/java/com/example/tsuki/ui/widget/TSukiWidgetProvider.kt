package com.example.tsuki.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import com.example.tsuki.MainActivity
import com.example.tsuki.R
import com.example.tsuki.playback.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TSukiWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appContext = context.applicationContext
        when (intent.action) {
            ACTION_PLAY_PAUSE -> {
                CoroutineScope(Dispatchers.Main).launch {
                    runCatching {
                        PlayerController.getInstance(appContext).togglePlayPause()
                    }
                }
            }
            ACTION_NEXT -> {
                CoroutineScope(Dispatchers.Main).launch {
                    runCatching {
                        PlayerController.getInstance(appContext).playNext()
                    }
                }
            }
            ACTION_PREV -> {
                CoroutineScope(Dispatchers.Main).launch {
                    runCatching {
                        PlayerController.getInstance(appContext).playPrevious()
                    }
                }
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId, "TSuki", "Toca para abrir", false, null)
        }
        CoroutineScope(Dispatchers.IO).launch {
            com.example.tsuki.ui.widget.glance.TSukiGlanceSync.pushCurrentState(context.applicationContext)
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.example.tsuki.widget.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.tsuki.widget.ACTION_NEXT"
        const val ACTION_PREV = "com.example.tsuki.widget.ACTION_PREV"

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            title: String,
            artist: String,
            isPlaying: Boolean,
            bitmap: Bitmap?
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_tsuki_player)

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_player", true)
            }
            val openPendingIntent = PendingIntent.getActivity(
                context,
                100,
                openAppIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_container, openPendingIntent)

            val playPauseIntent = Intent(context, TSukiWidgetProvider::class.java).apply {
                action = ACTION_PLAY_PAUSE
            }
            val playPausePendingIntent = PendingIntent.getBroadcast(
                context,
                101,
                playPauseIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_btn_play, playPausePendingIntent)

            val prevIntent = Intent(context, TSukiWidgetProvider::class.java).apply {
                action = ACTION_PREV
            }
            val prevPendingIntent = PendingIntent.getBroadcast(
                context,
                102,
                prevIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_btn_prev, prevPendingIntent)

            val nextIntent = Intent(context, TSukiWidgetProvider::class.java).apply {
                action = ACTION_NEXT
            }
            val nextPendingIntent = PendingIntent.getBroadcast(
                context,
                103,
                nextIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_btn_next, nextPendingIntent)

            views.setTextViewText(R.id.widget_title, title.ifBlank { "TSuki" })
            views.setTextViewText(R.id.widget_artist, artist.ifBlank { "Toca para abrir" })
            views.setImageViewResource(
                R.id.widget_btn_play,
                if (isPlaying) R.drawable.widget_ic_pause else R.drawable.widget_ic_play
            )

            if (bitmap != null) {
                views.setImageViewBitmap(R.id.widget_cover, bitmap)
            } else {
                views.setImageViewResource(R.id.widget_cover, R.drawable.ic_tsuki_kanji)
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }

        fun updateAllWidgets(
            context: Context,
            title: String,
            artist: String,
            isPlaying: Boolean,
            bitmap: Bitmap? = null
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TSukiWidgetProvider::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (widgetIds.isEmpty()) return

            for (widgetId in widgetIds) {
                updateWidget(context, appWidgetManager, widgetId, title, artist, isPlaying, bitmap)
            }
        }
    }
}

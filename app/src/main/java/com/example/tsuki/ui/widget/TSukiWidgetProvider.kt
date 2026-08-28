package com.example.tsuki.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.tsuki.MainActivity
import com.example.tsuki.R

class TSukiWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_tsuki_player)

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                1,
                openAppIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }

        fun updateAllWidgets(context: Context, title: String, artist: String, isPlaying: Boolean) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TSukiWidgetProvider::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (widgetId in widgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_tsuki_player)
                views.setTextViewText(R.id.widget_title, title)
                views.setTextViewText(R.id.widget_artist, artist)

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}

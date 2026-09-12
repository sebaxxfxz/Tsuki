package com.example.tsuki.ui.widget.glance

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class BaseTSukiGlanceReceiver(override val glanceAppWidget: GlanceAppWidget) : GlanceAppWidgetReceiver() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        CoroutineScope(Dispatchers.IO).launch {
            TSukiGlanceSync.pushCurrentState(context.applicationContext)
        }
    }
}

class TSukiGlanceReceiver : BaseTSukiGlanceReceiver(TSukiGlanceWidget())
class TSukiMiniGlanceReceiver : BaseTSukiGlanceReceiver(TSukiMiniGlanceWidget())
class TSukiSquareGlanceReceiver : BaseTSukiGlanceReceiver(TSukiSquareGlanceWidget())
class TSukiQuickResumeGlanceReceiver : BaseTSukiGlanceReceiver(TSukiQuickResumeGlanceWidget())
class TSukiHeroGlanceReceiver : BaseTSukiGlanceReceiver(TSukiHeroGlanceWidget())

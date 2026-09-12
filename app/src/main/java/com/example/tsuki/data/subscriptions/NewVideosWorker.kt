package com.example.tsuki.data.subscriptions

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.tsuki.MainActivity
import com.example.tsuki.R
import com.example.tsuki.data.local.HomePreferences
import com.example.tsuki.data.local.TSukiSubscriptionRepository
import com.example.tsuki.network.ChannelRssClient
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class NewVideosWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        try {
            if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return Result.success()
            }
            val prefs = HomePreferences(applicationContext)
            if (!prefs.subNotifyEnabled.first()) return Result.success()
            val ids = TSukiSubscriptionRepository.getInstance(applicationContext).getAllIds()
            if (ids.isEmpty()) return Result.success()
            val rss = ChannelRssClient()
            val entries = ids.take(30).mapNotNull { id ->
                runCatching { rss.fetchChannelVideos(id).take(2) }.getOrNull()
            }.flatten().distinctBy { it.id }
            if (entries.isEmpty()) return Result.success()
            val notified = prefs.notifiedSubVideos.first()
            val fresh = entries.filter { it.id !in notified }
            prefs.addNotifiedSubVideos(entries.map { it.id })
            if (fresh.isEmpty()) return Result.success()
            showNotification(fresh.map { "${it.title} • ${it.artist}" }.take(5), fresh.size)
            return Result.success()
        } catch (_: Exception) {
            return Result.retry()
        }
    }

    private fun showNotification(lines: List<String>, total: Int) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Videos nuevos", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val openIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            applicationContext, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val style = NotificationCompat.InboxStyle().setSummaryText(
            if (total == 1) "1 video nuevo" else "$total videos nuevos"
        )
        lines.forEach { style.addLine(it) }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.media3_notification_small_icon)
            .setContentTitle(if (total == 1) "Video nuevo de tus suscripciones" else "$total videos nuevos")
            .setContentText(lines.firstOrNull().orEmpty())
            .setStyle(style)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "tsuki_new_videos"
        const val NOTIFICATION_ID = 1001
    }
}

object SubNotifyScheduler {
    private const val WORK_TAG = "tsuki_new_videos"

    fun setEnabled(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context.applicationContext)
        if (enabled) {
            val req = PeriodicWorkRequestBuilder<NewVideosWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            wm.enqueueUniquePeriodicWork(WORK_TAG, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, req)
        } else {
            wm.cancelUniqueWork(WORK_TAG)
        }
    }
}

package com.example.tsuki.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

object UpdateNotificationHelper {
    private const val CID = "tsuki_updates"
    private const val NID = 2101

    fun showUpdateNotification(context: Context, info: UpdateInfo) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CID, "Actualizaciones de TSuki", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Nuevas versiones de TSuki"
            }
            nm.createNotificationChannel(ch)
        }
        val url = info.apkUrl ?: "https://github.com/sebaxxfxz/TSuki/releases/latest"
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        val pend = PendingIntent.getActivity(context, NID, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(context, CID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("TSuki v${info.latestVersion} disponible")
            .setContentText("Toca para descargar la actualización")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pend)
            .setAutoCancel(true)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            try { NotificationManagerCompat.from(context).notify(NID, builder.build()) } catch (_: Exception) {}
        }
    }
}

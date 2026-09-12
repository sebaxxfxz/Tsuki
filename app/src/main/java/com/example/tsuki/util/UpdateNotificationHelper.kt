package com.example.tsuki.util

import android.Manifest
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.tsuki.R

object UpdateNotificationHelper {
    private const val CID = "tsuki_updates"
    private const val NID = 2101
    private const val BRAND_COLOR = 0xFF8E6BFF.toInt()

    fun showUpdateNotification(context: Context, info: UpdateInfo) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CID, "Actualizaciones de TSuki", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Nuevas versiones de TSuki"
                setShowBadge(true)
                enableVibration(false)
                setSound(null, null)
            }
            nm.createNotificationChannel(ch)
        }
        val releaseUrl = "https://github.com/sebaxxfxz/TSuki/releases/latest"
        val apkUrl = info.apkUrl ?: releaseUrl

        val downloadIntent = Intent(context, UpdateDownloadReceiver::class.java).apply {
            action = UpdateDownloadReceiver.ACTION_DOWNLOAD_UPDATE
            putExtra(UpdateDownloadReceiver.EXTRA_APK_URL, apkUrl)
            putExtra(UpdateDownloadReceiver.EXTRA_VERSION, info.latestVersion)
        }
        val downloadPend = PendingIntent.getBroadcast(
            context, NID + 1, downloadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val changesIntent = Intent(Intent.ACTION_VIEW, releaseUrl.toUri())
        val changesPend = PendingIntent.getActivity(
            context, NID + 2, changesIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openIntent = Intent(Intent.ACTION_VIEW, apkUrl.toUri())
        val openPend = PendingIntent.getActivity(
            context, NID, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val subtext = buildString {
            append("v${info.latestVersion}")
            if (info.apkSizeMb.isNotBlank()) append(" • ${info.apkSizeMb}")
            if (info.releaseDate.isNotBlank()) append(" • ${info.releaseDate}")
        }
        val changelog = summarizeChangelog(info.changelog)
        val builder = NotificationCompat.Builder(context, CID)
            .setSmallIcon(R.drawable.media3_notification_small_icon)
            .setContentTitle("TSuki v${info.latestVersion} disponible")
            .setContentText(if (changelog.isBlank()) "Nueva versión lista para descargar" else changelog)
            .setStyle(NotificationCompat.BigTextStyle().bigText(changelog.ifBlank { "Nueva versión lista para descargar" }))
            .setSubText(subtext)
            .setColor(BRAND_COLOR)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(openPend)
            .addAction(0, "Descargar", downloadPend)
            .addAction(0, "Ver cambios", changesPend)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            try { NotificationManagerCompat.from(context).notify(NID, builder.build()) } catch (_: Exception) {}
        }
    }

    internal fun summarizeChangelog(body: String): String {
        if (body.isBlank()) return ""
        val cleaned = body
            .replace(Regex("!\\[[^\\]]*\\]\\([^)]*\\)"), "")
            .replace(Regex("\\[([^\\]]+)\\]\\([^)]*\\)"), "$1")
            .replace(Regex("^#{1,6}\\s*", RegexOption.MULTILINE), "")
            .replace(Regex("^[\\*\\-+]\\s*", RegexOption.MULTILINE), "• ")
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
            .replace(Regex("`([^`]*)`"), "$1")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
        return if (cleaned.length > 480) cleaned.take(477) + "…" else cleaned
    }
}

class UpdateDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DOWNLOAD_UPDATE -> startDownload(context, intent)
            DownloadManager.ACTION_DOWNLOAD_COMPLETE -> handleDownloadComplete(context, intent)
        }
    }

    private fun startDownload(context: Context, intent: Intent) {
        val url = intent.getStringExtra(EXTRA_APK_URL) ?: return
        val version = intent.getStringExtra(EXTRA_VERSION) ?: ""
        val request = DownloadManager.Request(url.toUri()).apply {
            setTitle("TSuki v$version")
            setDescription("Descargando actualización…")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "tsuki-v$version.apk")
            setMimeType("application/vnd.android.package-archive")
        }
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        activeDownloadId = try { dm.enqueue(request) } catch (_: Exception) { -1L }
        if (activeDownloadId > 0) {
            ensureDownloadReceiverRegistered(context)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(2101)
        }
    }

    private fun handleDownloadComplete(context: Context, intent: Intent) {
        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (id != activeDownloadId) return
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri: Uri = try { dm.getUriForDownloadedFile(id) ?: return } catch (_: Exception) { return }
        val install = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(install) }
    }

    private fun ensureDownloadReceiverRegistered(context: Context) {
        if (downloadCompleteRegistered) return
        downloadCompleteRegistered = true
        val app = context.applicationContext
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            app.registerReceiver(this, filter, Context.RECEIVER_EXPORTED)
        } else {
            app.registerReceiver(this, filter)
        }
    }

    companion object {
        const val ACTION_DOWNLOAD_UPDATE = "com.example.tsuki.action.DOWNLOAD_UPDATE"
        const val EXTRA_APK_URL = "extra_apk_url"
        const val EXTRA_VERSION = "extra_version"

        private var downloadCompleteRegistered = false
        private var activeDownloadId = -1L
    }
}

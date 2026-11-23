package com.example.services

import android.Manifest
import android.annotation.SuppressLint
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
import com.example.MainActivity
import com.example.R

class RitamNotificationManager(private val context: Context) {

    init {
        createChannels()
    }

    private fun createChannels() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val downloadsChannel = NotificationChannel(
            CHANNEL_DOWNLOADS,
            "Downloads & Saved Pages",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts when downloads or offline saved webpages finish."
        }

        val alertsChannel = NotificationChannel(
            CHANNEL_ALERTS,
            "Shields, Ad-Blocker & Reader Alerts",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notifications from Ritam Ad-Blocker, Read Aloud playback, and browser alerts."
        }

        manager.createNotificationChannel(downloadsChannel)
        manager.createNotificationChannel(alertsChannel)
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    @SuppressLint("MissingPermission")
    fun notifyPageSaved(pageTitle: String, filename: String): Boolean {
        if (!hasNotificationPermission()) return false
        val notification = NotificationCompat.Builder(context, CHANNEL_DOWNLOADS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Webpage Saved Offline")
            .setContentText("${pageTitle.ifBlank { filename }} • $filename")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(launchAppIntent(201))
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SAVED_PAGE, notification)
        return true
    }

    @SuppressLint("MissingPermission")
    fun notifyDownloadCompleted(filename: String): Boolean {
        if (!hasNotificationPermission()) return false
        val notification = NotificationCompat.Builder(context, CHANNEL_DOWNLOADS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Download Complete")
            .setContentText(filename)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(launchAppIntent(202))
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_DOWNLOAD, notification)
        return true
    }

    @SuppressLint("MissingPermission")
    fun notifyShieldAlert(title: String, body: String): Boolean {
        if (!hasNotificationPermission()) return false
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(launchAppIntent(203))
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SHIELD_ALERT, notification)
        return true
    }

    @SuppressLint("MissingPermission")
    fun notifyReadAloudActive(title: String, progressLabel: String) {
        if (!hasNotificationPermission()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Reading Aloud: $title")
            .setContentText(progressLabel)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(launchAppIntent(204))
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_READ_ALOUD, notification)
    }

    fun cancelReadAloudNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_READ_ALOUD)
    }

    private fun launchAppIntent(requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_DOWNLOADS = "ritam_downloads"
        const val CHANNEL_ALERTS = "ritam_privacy_alerts"

        private const val NOTIFICATION_ID_SAVED_PAGE = 3001
        private const val NOTIFICATION_ID_DOWNLOAD = 3002
        private const val NOTIFICATION_ID_SHIELD_ALERT = 3003
        private const val NOTIFICATION_ID_READ_ALOUD = 3004
    }
}

package com.example.services

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R

object PwaShortcutHelper {
    const val EXTRA_PWA_URL = "ritam_pwa_url"
    const val EXTRA_PWA_STANDALONE = "ritam_pwa_standalone"

    fun pinWebAppShortcut(
        context: Context,
        url: String,
        title: String,
        standaloneWindow: Boolean = true
    ): Boolean {
        if (url.isBlank() || !ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            return false
        }
        val cleanLabel = title.trim().ifBlank { url }.take(28)
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = android.net.Uri.parse(url)
            putExtra(EXTRA_PWA_URL, url)
            putExtra(EXTRA_PWA_STANDALONE, standaloneWindow)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val shortcut = ShortcutInfoCompat.Builder(context, "ritam_pwa_${url.hashCode()}")
            .setShortLabel(cleanLabel)
            .setLongLabel(title.take(40))
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(launchIntent)
            .build()
        return runCatching {
            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        }.getOrDefault(false)
    }
}

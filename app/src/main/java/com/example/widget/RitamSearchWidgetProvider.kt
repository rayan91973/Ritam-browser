package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class RitamSearchWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            updateAppWidget(context, appWidgetManager, widgetId)
        }
    }

    companion object {
        const val EXTRA_WIDGET_ACTION = "ritam_widget_action"
        const val ACTION_SEARCH = "SEARCH"
        const val ACTION_VOICE = "VOICE"
        const val ACTION_PRIVATE = "PRIVATE"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.ritam_search_widget)
            views.setOnClickPendingIntent(
                R.id.widget_search_pill,
                buildActionIntent(context, ACTION_SEARCH, 101)
            )
            views.setOnClickPendingIntent(
                R.id.widget_voice_btn,
                buildActionIntent(context, ACTION_VOICE, 102)
            )
            views.setOnClickPendingIntent(
                R.id.widget_private_btn,
                buildActionIntent(context, ACTION_PRIVATE, 103)
            )
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun buildActionIntent(
            context: Context,
            action: String,
            requestCode: Int
        ): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_WIDGET_ACTION, action)
            }
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun requestPinWidget(context: Context): Boolean {
            val manager = AppWidgetManager.getInstance(context)
            if (!manager.isRequestPinAppWidgetSupported) return false
            val provider = ComponentName(context, RitamSearchWidgetProvider::class.java)
            return manager.requestPinAppWidget(provider, null, null)
        }
    }
}

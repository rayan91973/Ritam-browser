package com.example.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build

class MediaSessionHelper(
    private val context: Context,
    private val notificationManager: RitamNotificationManager
) {
    private var mediaSession: MediaSession? = null
    private var receiverRegistered = false
    private var onToggleAction: (() -> Unit)? = null
    private var onStopAction: (() -> Unit)? = null

    private val actionReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_MEDIA_TOGGLE -> onToggleAction?.invoke()
                ACTION_MEDIA_STOP -> onStopAction?.invoke()
            }
        }
    }

    fun setHandlers(onToggle: () -> Unit, onStop: () -> Unit) {
        onToggleAction = onToggle
        onStopAction = onStop
        ensureReceiverRegistered()
    }

    private fun ensureReceiverRegistered() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(ACTION_MEDIA_TOGGLE)
            addAction(ACTION_MEDIA_STOP)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(actionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(actionReceiver, filter)
            }
            receiverRegistered = true
        }
    }

    fun updatePlaybackState(
        title: String,
        domain: String,
        isPlaying: Boolean,
        hasActiveMedia: Boolean,
        notificationsEnabled: Boolean
    ) {
        if (!hasActiveMedia || title.isBlank()) {
            release()
            return
        }
        ensureReceiverRegistered()
        val session = mediaSession ?: MediaSession(context, "RitamMediaSession").also { s ->
            s.setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    onToggleAction?.invoke()
                }

                override fun onPause() {
                    onToggleAction?.invoke()
                }

                override fun onStop() {
                    onStopAction?.invoke()
                }
            })
            s.isActive = true
            mediaSession = s
        }

        session.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, domain.ifBlank { "Ritam Browser" })
                .build()
        )
        val stateCode = if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_STOP)
                .setState(stateCode, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                .build()
        )

        if (isPlaying) {
            RitamMediaPlaybackService.startOrUpdate(
                context = context,
                title = title,
                domain = domain.ifBlank { "Ritam Browser" }
            )
            if (notificationsEnabled) {
                notificationManager.notifyMediaPlayback(
                    title = title,
                    domain = domain.ifBlank { "Ritam Browser" },
                    isPlaying = true
                )
            }
        } else {
            RitamMediaPlaybackService.stop(context)
            notificationManager.cancelMediaPlaybackNotification()
        }
    }

    fun release() {
        RitamMediaPlaybackService.stop(context)
        notificationManager.cancelMediaPlaybackNotification()
        runCatching {
            mediaSession?.isActive = false
            mediaSession?.release()
        }
        mediaSession = null
    }

    fun shutdown() {
        release()
        if (receiverRegistered) {
            runCatching { context.unregisterReceiver(actionReceiver) }
            receiverRegistered = false
        }
    }

    companion object {
        const val ACTION_MEDIA_TOGGLE = "com.example.ritam.ACTION_MEDIA_TOGGLE"
        const val ACTION_MEDIA_STOP = "com.example.ritam.ACTION_MEDIA_STOP"
    }
}

package com.example.services

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class ReadAloudState(
    val isActive: Boolean = false,
    val isPlaying: Boolean = false,
    val title: String = "",
    val paragraphs: List<String> = emptyList(),
    val currentIndex: Int = 0,
    val speechRate: Float = 1.0f,
    val statusNote: String? = null
) {
    val totalCount: Int get() = paragraphs.size
    val progressFraction: Float
        get() = if (paragraphs.isEmpty()) 0f else ((currentIndex + 1).toFloat() / paragraphs.size.toFloat()).coerceIn(0f, 1f)
}

class ReadAloudController(
    private val context: Context,
    private val notificationManager: RitamNotificationManager
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var ttsInitialized = false
    private var pendingStartAfterInit = false
    private var notificationsEnabled = true

    private val _state = MutableStateFlow(ReadAloudState())
    val state: StateFlow<ReadAloudState> = _state.asStateFlow()

    fun setNotificationsEnabled(enabled: Boolean) {
        notificationsEnabled = enabled
        if (!enabled) notificationManager.cancelReadAloudNotification()
    }

    fun startReading(title: String, paragraphs: List<String>) {
        val clean = paragraphs.map { it.trim() }.filter { it.isNotEmpty() }
        if (clean.isEmpty()) {
            _state.value = ReadAloudState(
                isActive = true,
                isPlaying = false,
                title = title,
                paragraphs = listOf("No readable text content found on this page."),
                currentIndex = 0,
                speechRate = _state.value.speechRate,
                statusNote = "Nothing to read"
            )
            return
        }

        _state.value = _state.value.copy(
            isActive = true,
            isPlaying = true,
            title = title.ifBlank { "Webpage" },
            paragraphs = clean,
            currentIndex = 0,
            statusNote = null
        )

        ensureTts {
            speakCurrentParagraph()
        }
    }

    fun togglePlayPause() {
        val current = _state.value
        if (!current.isActive) return
        if (current.isPlaying) {
            tts?.stop()
            _state.value = current.copy(isPlaying = false)
            notificationManager.cancelReadAloudNotification()
        } else {
            _state.value = current.copy(isPlaying = true)
            ensureTts {
                speakCurrentParagraph()
            }
        }
    }

    fun nextParagraph() {
        val current = _state.value
        if (!current.isActive || current.paragraphs.isEmpty()) return
        val next = (current.currentIndex + 1).coerceAtMost(current.paragraphs.lastIndex)
        _state.value = current.copy(currentIndex = next, isPlaying = true)
        ensureTts {
            speakCurrentParagraph()
        }
    }

    fun prevParagraph() {
        val current = _state.value
        if (!current.isActive || current.paragraphs.isEmpty()) return
        val prev = (current.currentIndex - 1).coerceAtLeast(0)
        _state.value = current.copy(currentIndex = prev, isPlaying = true)
        ensureTts {
            speakCurrentParagraph()
        }
    }

    fun cycleSpeed() {
        val current = _state.value
        val nextSpeed = when {
            current.speechRate < 0.9f -> 1.0f
            current.speechRate < 1.15f -> 1.25f
            current.speechRate < 1.4f -> 1.5f
            else -> 0.8f
        }
        _state.value = current.copy(speechRate = nextSpeed)
        tts?.setSpeechRate(nextSpeed)
        if (current.isPlaying) {
            speakCurrentParagraph()
        }
    }

    fun stop() {
        pendingStartAfterInit = false
        tts?.stop()
        notificationManager.cancelReadAloudNotification()
        _state.value = ReadAloudState(speechRate = _state.value.speechRate)
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        ttsInitialized = false
    }

    private fun ensureTts(onReady: () -> Unit) {
        if (tts != null && ttsInitialized) {
            onReady()
            return
        }
        pendingStartAfterInit = true
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext) { status ->
                mainHandler.post {
                    if (status == TextToSpeech.SUCCESS) {
                        tts?.language = Locale.getDefault()
                        tts?.setSpeechRate(_state.value.speechRate)
                        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) = Unit
                            override fun onError(utteranceId: String?) {
                                mainHandler.post {
                                    _state.value = _state.value.copy(isPlaying = false, statusNote = "TTS unavailable")
                                }
                            }
                            override fun onDone(utteranceId: String?) {
                                mainHandler.post {
                                    val cur = _state.value
                                    if (!cur.isActive || !cur.isPlaying) return@post
                                    if (cur.currentIndex + 1 < cur.paragraphs.size) {
                                        _state.value = cur.copy(currentIndex = cur.currentIndex + 1)
                                        speakCurrentParagraph()
                                    } else {
                                        _state.value = cur.copy(isPlaying = false, currentIndex = 0)
                                        notificationManager.cancelReadAloudNotification()
                                    }
                                }
                            }
                        })
                        ttsInitialized = true
                        if (pendingStartAfterInit && _state.value.isActive && _state.value.isPlaying) {
                            pendingStartAfterInit = false
                            onReady()
                        }
                    } else {
                        ttsInitialized = false
                        _state.value = _state.value.copy(
                            isPlaying = false,
                            statusNote = "Text-to-Speech engine not ready on device"
                        )
                    }
                }
            }
        }
    }

    private fun speakCurrentParagraph() {
        val current = _state.value
        val text = current.paragraphs.getOrNull(current.currentIndex) ?: return
        tts?.setSpeechRate(current.speechRate)
        val utteranceId = "ritam_tts_${current.currentIndex}_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (notificationsEnabled) {
            notificationManager.notifyReadAloudActive(
                title = current.title,
                progressLabel = "Paragraph ${current.currentIndex + 1} of ${current.paragraphs.size}"
            )
        }
    }
}

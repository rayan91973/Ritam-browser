package com.example.services

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class TranslateLanguage(
    val code: String,
    val label: String,
    val nativeLabel: String
)

object TranslationHelper {
    val supportedLanguages = listOf(
        TranslateLanguage("en", "English", "English"),
        TranslateLanguage("es", "Spanish", "Español"),
        TranslateLanguage("fr", "French", "Français"),
        TranslateLanguage("de", "German", "Deutsch"),
        TranslateLanguage("hi", "Hindi", "हिन्दी"),
        TranslateLanguage("ja", "Japanese", "日本語"),
        TranslateLanguage("pt", "Portuguese", "Português"),
        TranslateLanguage("ar", "Arabic", "العربية"),
        TranslateLanguage("zh-CN", "Chinese", "中文")
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    fun labelForCode(code: String): String =
        supportedLanguages.firstOrNull { it.code.equals(code, ignoreCase = true) }?.label ?: code.uppercase()

    fun buildGoogleTranslateWebUrl(targetUrl: String, targetLangCode: String): String {
        val cleanTarget = targetUrl.trim()
        val lang = Uri.encode(targetLangCode)
        val encodedUrl = Uri.encode(cleanTarget)
        return "https://translate.google.com/translate?sl=auto&tl=$lang&u=$encodedUrl"
    }

    suspend fun translateText(
        text: String,
        targetLangCode: String,
        sourceLangCode: String = "en"
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = text.trim().take(420)
        if (trimmed.isBlank()) return@withContext Result.success("")
        val src = if (sourceLangCode.equals(targetLangCode, ignoreCase = true)) "auto" else sourceLangCode
        val tl = if (targetLangCode == "zh-CN") "zh" else targetLangCode
        runCatching {
            val q = URLEncoder.encode(trimmed, "UTF-8")
            val pair = URLEncoder.encode("$src|$tl", "UTF-8")
            val url = "https://api.mymemory.translated.net/get?q=$q&langpair=$pair"
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val body = resp.body?.string().orEmpty()
                val json = JSONObject(body)
                val translated = json.optJSONObject("responseData")?.optString("translatedText", "") ?: ""
                if (translated.isBlank()) error("Empty translation result")
                translated
            }
        }
    }
}

package com.example.services

import android.content.Context
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.example.core.RitamHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class StructuredDocType(val label: String) {
    JSON("JSON"),
    MARKDOWN("Markdown"),
    XML("XML / RSS"),
    PLAIN_CODE("Plain Text / Code")
}

data class StructuredViewerState(
    val title: String,
    val url: String,
    val docType: StructuredDocType,
    val formattedText: String,
    val rawText: String,
    val isLoading: Boolean = false,
    val error: String? = null
)

object StructuredViewerHelper {
    private val client = RitamHttp.builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun isStructuredUrl(url: String): Boolean {
        val clean = url.substringBefore("?").substringBefore("#").lowercase()
        return clean.endsWith(".json") ||
            clean.endsWith(".md") ||
            clean.endsWith(".markdown") ||
            clean.endsWith(".txt") ||
            clean.endsWith(".xml") ||
            clean.endsWith(".yaml") ||
            clean.endsWith(".yml")
    }

    fun detectDocType(url: String, content: String): StructuredDocType {
        val clean = url.substringBefore("?").substringBefore("#").lowercase()
        val trimmed = content.trim()
        return when {
            clean.endsWith(".json") || trimmed.startsWith("{") || trimmed.startsWith("[") -> StructuredDocType.JSON
            clean.endsWith(".md") || clean.endsWith(".markdown") -> StructuredDocType.MARKDOWN
            clean.endsWith(".xml") || trimmed.startsWith("<") -> StructuredDocType.XML
            else -> StructuredDocType.PLAIN_CODE
        }
    }

    fun formatPrettyJson(raw: String): String? {
        val trimmed = raw.trim()
        return runCatching {
            when {
                trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
                trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
                else -> null
            }
        }.getOrNull()
    }

    suspend fun fetchStructuredDocument(
        url: String,
        context: Context? = null
    ): Result<StructuredViewerState> = withContext(Dispatchers.IO) {
        runCatching {
            val cacheKey = "struct_$url"
            val memHit = RitamCacheManager.getMemoryText(cacheKey)
            val raw = if (memHit != null) {
                memHit
            } else {
                val activeClient = if (context != null) RitamCacheManager.sharedHttpClient(context) else client
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json, text/markdown, text/plain, application/xml, */*")
                    .build()
                activeClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("HTTP ${response.code}")
                    }
                    (response.body?.string()?.take(250_000) ?: "").also {
                        if (it.isNotBlank()) RitamCacheManager.putMemoryText(cacheKey, it)
                    }
                }
            }
            val docType = detectDocType(url, raw)
            val formatted = if (docType == StructuredDocType.JSON) {
                formatPrettyJson(raw) ?: raw
            } else {
                raw
            }
            StructuredViewerState(
                title = url.substringAfterLast("/").ifBlank { url },
                url = url,
                docType = docType,
                formattedText = formatted,
                rawText = raw
            )
        }
    }

    /**
     * Converts ordinary prose into a Focus / Fixation ("Bionic Reading") AnnotatedString
     * where the opening prefix of each word is bolded to anchor rapid eye fixation.
     */
    fun buildFixationAnnotatedString(text: String): AnnotatedString {
        return buildAnnotatedString {
            val tokenRegex = Regex("""(\S+|\s+)""")
            tokenRegex.findAll(text).forEach { match ->
                val token = match.value
                if (token.isBlank()) {
                    append(token)
                } else {
                    val letterCount = token.count { it.isLetterOrDigit() }
                    val splitIndex = when {
                        letterCount <= 1 -> token.length
                        letterCount <= 3 -> 1
                        letterCount <= 6 -> 2
                        letterCount <= 9 -> 3
                        else -> (token.length * 0.42f).toInt().coerceAtLeast(3)
                    }.coerceAtMost(token.length)
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(token.substring(0, splitIndex))
                    }
                    if (splitIndex < token.length) {
                        append(token.substring(splitIndex))
                    }
                }
            }
        }
    }
}

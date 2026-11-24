package com.example.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object WatchlistAndSyncHelper {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun computePageContentHash(url: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "RitamBrowser/1.0 Watchlist")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("HTTP ${response.code}")
                }
                val raw = response.body?.string() ?: ""
                val visibleText = raw
                    .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
                    .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
                    .replace(Regex("<[^>]+>"), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                sha256Hex(visibleText)
            }
        }
    }

    suspend fun syncFilterListRuleCount(sourceUrl: String, currentCount: Int): Result<Int> =
        withContext(Dispatchers.IO) {
            if (!sourceUrl.startsWith("http")) {
                return@withContext Result.success(currentCount)
            }
            runCatching {
                val request = Request.Builder()
                    .url(sourceUrl)
                    .header("User-Agent", "RitamBrowser/1.0 FilterSync")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("Sync HTTP ${response.code}")
                    }
                    val stream = response.body?.charStream() ?: return@use currentCount
                    var count = 0
                    stream.buffered().useLines { lines ->
                        lines.take(80_000).forEach { line ->
                            val t = line.trim()
                            if (t.isNotEmpty() && !t.startsWith("!") && !t.startsWith("[") && !t.startsWith("#")) {
                                count++
                            }
                        }
                    }
                    if (count > 0) count else currentCount
                }
            }
        }

    fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

package com.example.privacy.adblock

import android.content.Context
import com.example.core.RitamHttp
import com.example.core.RitamLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Loads filter lists from bundled assets and keeps a downloadable copy in app storage.
 *
 * The bundled assets mean blocking works on first launch and offline; the copies refreshed from
 * upstream take precedence once downloaded, similar to Brave's list updates.
 */
class AdBlockFilterStore(private val context: Context) {

    data class Source(
        val id: String,
        val assetName: String,
        val url: String,
        val category: AdCategory,
        val displayName: String
    )

    private val client = RitamHttp.builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val cacheDir: File
        get() = File(context.filesDir, "filterlists").apply { if (!exists()) mkdirs() }

    private fun cacheFile(source: Source) = File(cacheDir, "${source.id}.txt")
    private fun stampFile() = File(cacheDir, "last_update.txt")

    fun lastUpdateMillis(): Long =
        runCatching { stampFile().readText().trim().toLong() }.getOrDefault(0L)

    fun isStale(maxAgeMillis: Long): Boolean =
        System.currentTimeMillis() - lastUpdateMillis() > maxAgeMillis

    suspend fun loadEngine(enabledIds: Set<String>): AdBlockEngine = withContext(Dispatchers.IO) {
        val lists = ArrayList<ParsedFilterList>(SOURCES.size)
        for (source in SOURCES) {
            if (!enabledIds.contains(source.id)) continue
            val text = readList(source)
            if (text == null) {
                RitamLog.w(TAG, "filter list '${source.id}' unavailable; continuing with the remaining lists")
                continue
            }
            val parsed = runCatching { AdBlockListParser.parse(text, source.category) }
                .onFailure { RitamLog.e(TAG, "failed to parse filter list '${source.id}'", it) }
                .getOrNull()
            if (parsed == null) continue
            lists.add(parsed)
        }
        if (lists.isEmpty()) {
            RitamLog.e(TAG, "no filter lists could be loaded; ad blocking is using the built-in fallbacks only")
        }
        AdBlockEngine.build(lists)
    }

    /** Downloads every enabled list. Returns how many lists were replaced. */
    suspend fun refresh(enabledIds: Set<String>): Int = withContext(Dispatchers.IO) {
        var updated = 0
        for (source in SOURCES) {
            if (!enabledIds.contains(source.id)) continue
            val body = runCatching { download(source.url) }
                .onFailure { RitamLog.w(TAG, "download failed for '${source.id}'", it) }
                .getOrNull()
            if (body == null) continue
            if (!looksLikeFilterList(body)) {
                RitamLog.w(TAG, "ignoring '${source.id}' update: response is not a filter list (${body.length} chars)")
                continue
            }
            val target = cacheFile(source)
            val temp = File(cacheDir, "${source.id}.tmp")
            val written = runCatching {
                temp.writeText(body)
                if (target.exists() && !target.delete()) return@runCatching false
                temp.renameTo(target)
            }.onFailure { RitamLog.w(TAG, "could not store updated list '${source.id}'", it) }
                .getOrDefault(false)
            if (written) updated++
        }
        if (updated > 0) {
            runCatching { stampFile().writeText(System.currentTimeMillis().toString()) }
                .onFailure { RitamLog.w(TAG, "could not persist filter list timestamp", it) }
        }
        updated
    }

    fun ruleCountFor(sourceId: String): Int {
        val source = SOURCES.firstOrNull { it.id == sourceId } ?: return 0
        val text = readList(source) ?: return 0
        return text.count { it == '\n' }
    }

    private fun readList(source: Source): String? {
        val cached = cacheFile(source)
        if (cached.exists() && cached.length() > MIN_LIST_BYTES) {
            val text = runCatching { cached.readText() }
                .onFailure { RitamLog.w(TAG, "cached copy of '${source.id}' is unreadable; falling back to the bundled list", it) }
                .getOrNull()
            if (text != null) return text
        }
        return runCatching {
            context.assets.open(source.assetName).bufferedReader().use { it.readText() }
        }.onFailure { RitamLog.e(TAG, "bundled asset '${source.assetName}' could not be opened", it) }
            .getOrNull()
    }

    private fun download(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "RitamBrowser/1.0 (Android)")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body ?: return null
            return body.string()
        }
    }

    private fun looksLikeFilterList(body: String): Boolean =
        body.length > MIN_LIST_BYTES &&
            (body.contains("##") || body.contains("||")) &&
            !body.trimStart().startsWith("<")

    companion object {
        private const val TAG = "AdBlockStore"
        private const val MIN_LIST_BYTES = 20_000L
        const val REFRESH_INTERVAL_MS = 24L * 60L * 60L * 1000L

        val SOURCES = listOf(
            Source(
                id = "easylist",
                assetName = "filters/easylist.txt",
                url = "https://easylist.to/easylist/easylist.txt",
                category = AdCategory.AD,
                displayName = "EasyList"
            ),
            Source(
                id = "easyprivacy",
                assetName = "filters/easyprivacy.txt",
                url = "https://easylist.to/easylist/easyprivacy.txt",
                category = AdCategory.TRACKER,
                displayName = "EasyPrivacy"
            ),
            Source(
                id = "peter_lowe",
                assetName = "filters/peter_lowe.txt",
                url = "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=adblockplus&showintro=0&mimetype=plaintext",
                category = AdCategory.AD,
                displayName = "Peter Lowe's List"
            )
        )

        val ALL_IDS: Set<String> = SOURCES.map { it.id }.toSet()

        fun sourceIdForListName(name: String): String? = when {
            name.contains("EasyPrivacy", ignoreCase = true) -> "easyprivacy"
            name.contains("EasyList", ignoreCase = true) -> "easylist"
            name.contains("Peter Lowe", ignoreCase = true) -> "peter_lowe"
            else -> null
        }
    }
}

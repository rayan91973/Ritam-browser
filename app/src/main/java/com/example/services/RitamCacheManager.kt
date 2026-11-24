package com.example.services

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

data class AppCacheStats(
    val httpCacheBytes: Long = 0L,
    val previewCacheBytes: Long = 0L,
    val webViewCacheBytes: Long = 0L,
    val memoryEntries: Int = 0,
    val cachedFileCount: Int = 0,
    val maxHttpCacheBytes: Long = 32L * 1024L * 1024L
) {
    val totalBytes: Long get() = httpCacheBytes + previewCacheBytes + webViewCacheBytes
}

object RitamCacheManager {
    private const val HTTP_CACHE_DIR = "ritam_http_cache"
    private const val PREVIEW_CACHE_DIR = "universal_viewer"
    private const val PDF_CACHE_DIR = "pdfs"
    private const val MAX_HTTP_CACHE_BYTES = 32L * 1024L * 1024L

    @Volatile
    private var cachedHttpClient: OkHttpClient? = null

    @Volatile
    private var okHttpCache: Cache? = null

    private val memoryTextCache = object : LruCache<String, String>(48) {}

    fun sharedHttpClient(context: Context): OkHttpClient {
        cachedHttpClient?.let { return it }
        return synchronized(this) {
            cachedHttpClient ?: buildHttpClient(context.applicationContext).also {
                cachedHttpClient = it
            }
        }
    }

    private fun buildHttpClient(appContext: Context): OkHttpClient {
        val httpDir = File(appContext.cacheDir, HTTP_CACHE_DIR).apply { mkdirs() }
        val cache = Cache(httpDir, MAX_HTTP_CACHE_BYTES).also { okHttpCache = it }
        return OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .followRedirects(true)
            .addInterceptor { chain ->
                val req = chain.request()
                val nextReq = if (!isOnline(appContext)) {
                    req.newBuilder()
                        .cacheControl(
                            CacheControl.Builder()
                                .onlyIfCached()
                                .maxStale(7, TimeUnit.DAYS)
                                .build()
                        )
                        .build()
                } else {
                    req
                }
                chain.proceed(nextReq)
            }
            .build()
    }

    fun getMemoryText(key: String): String? = memoryTextCache.get(key)

    fun putMemoryText(key: String, value: String) {
        if (key.isNotBlank() && value.length <= 180_000) {
            memoryTextCache.put(key, value)
        }
    }

    fun viewerCacheDir(context: Context): File =
        File(context.cacheDir, PREVIEW_CACHE_DIR).apply { mkdirs() }

    fun pdfCacheDir(context: Context): File =
        File(context.cacheDir, PDF_CACHE_DIR).apply { mkdirs() }

    fun isOnline(context: Context): Boolean {
        return runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return@runCatching true
            val network = cm.activeNetwork ?: return@runCatching false
            val caps = cm.getNetworkCapabilities(network) ?: return@runCatching false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }.getOrDefault(true)
    }

    suspend fun computeCacheStats(context: Context): AppCacheStats = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val httpDir = File(appContext.cacheDir, HTTP_CACHE_DIR)
        val previewDir = File(appContext.cacheDir, PREVIEW_CACHE_DIR)
        val pdfDir = File(appContext.cacheDir, PDF_CACHE_DIR)
        val webViewCacheDir = File(appContext.cacheDir, "WebView")
        val appWebViewDir = File(appContext.dataDir, "app_webview/Default/Cache")

        val httpStats = measureDir(httpDir)
        val previewStats = measureDir(previewDir) + measureDir(pdfDir)
        val webViewStats = measureDir(webViewCacheDir) + measureDir(appWebViewDir)

        // Include top-level image captures cached in cacheDir
        var strayTempBytes = 0L
        var strayCount = 0
        appContext.cacheDir.listFiles()?.forEach { child ->
            if (child.isFile && child.name.startsWith("ritam_")) {
                strayTempBytes += child.length()
                strayCount++
            }
        }

        AppCacheStats(
            httpCacheBytes = httpStats.first,
            previewCacheBytes = previewStats.first + strayTempBytes,
            webViewCacheBytes = webViewStats.first,
            memoryEntries = memoryTextCache.size(),
            cachedFileCount = httpStats.second + previewStats.second + webViewStats.second + strayCount,
            maxHttpCacheBytes = MAX_HTTP_CACHE_BYTES
        )
    }

    suspend fun clearAppCache(context: Context) = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        memoryTextCache.evictAll()
        runCatching { okHttpCache?.evictAll() }
        listOf(
            File(appContext.cacheDir, HTTP_CACHE_DIR),
            File(appContext.cacheDir, PREVIEW_CACHE_DIR),
            File(appContext.cacheDir, PDF_CACHE_DIR)
        ).forEach { dir ->
            if (dir.exists()) {
                dir.listFiles()?.forEach { it.deleteRecursively() }
            }
        }
        appContext.cacheDir.listFiles()?.forEach { child ->
            if (child.isFile && child.name.startsWith("ritam_")) {
                child.delete()
            }
        }
    }

    private fun measureDir(dir: File): Pair<Long, Int> {
        if (!dir.exists()) return 0L to 0
        var totalBytes = 0L
        var count = 0
        val stack = ArrayDeque<File>()
        stack.addLast(dir)
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            if (current.isDirectory) {
                current.listFiles()?.forEach { stack.addLast(it) }
            } else if (current.isFile) {
                totalBytes += current.length()
                count++
            }
        }
        return totalBytes to count
    }

    private operator fun Pair<Long, Int>.plus(other: Pair<Long, Int>): Pair<Long, Int> =
        (first + other.first) to (second + other.second)
}

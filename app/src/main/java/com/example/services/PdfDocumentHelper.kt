package com.example.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

data class PdfViewerState(
    val title: String,
    val sourceUrl: String = "",
    val localFile: File? = null,
    val pageCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

object PdfDocumentHelper {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun downloadRemotePdfToCache(
        context: Context,
        url: String,
        filenameHint: String
    ): File? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
            val safeName = filenameHint
                .ifBlank { "document.pdf" }
                .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                .let { if (it.endsWith(".pdf", true)) it else "$it.pdf" }
            val outFile = File(dir, safeName)
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching null
                val body = resp.body ?: return@runCatching null
                outFile.outputStream().use { out ->
                    body.byteStream().copyTo(out)
                }
            }
            if (outFile.exists() && outFile.length() > 0L) outFile else null
        }.getOrNull()
    }

    suspend fun getPageCount(file: File): Int = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext 0
        runCatching {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount
                }
            }
        }.getOrDefault(0)
    }

    suspend fun renderPage(file: File, pageIndex: Int, widthPx: Int = 1080): Bitmap? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null
        runCatching {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (pageIndex !in 0 until renderer.pageCount) return@withContext null
                    renderer.openPage(pageIndex).use { page ->
                        val targetW = widthPx.coerceIn(320, 2200)
                        val ratio = targetW.toFloat() / page.width.coerceAtLeast(1)
                        val targetH = (page.height * ratio).toInt().coerceIn(200, 3600)
                        val bmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp
                    }
                }
            }
        }.getOrNull()
    }
}

package com.example.services

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.webkit.URLUtil
import androidx.core.content.FileProvider
import com.example.data.local.DownloadCategory
import com.example.data.local.DownloadEntity
import com.example.data.local.DownloadStatus
import com.example.data.repository.BrowserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class StorageStats(
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long
) {
    val usedPercent: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

class BrowserDownloadManager(
    private val context: Context,
    private val repository: BrowserRepository,
    private val notificationManager: RitamNotificationManager
) {
    private val systemDownloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var downloadNotificationsEnabled: Boolean = true

    init {
        startPollingActiveDownloads()
    }

    fun setDownloadNotificationsEnabled(enabled: Boolean) {
        downloadNotificationsEnabled = enabled
    }

    fun recordSavedPage(
        url: String,
        filename: String,
        filePath: String,
        mimeType: String,
        sizeBytes: Long,
        notify: Boolean
    ) {
        scope.launch {
            repository.insertDownload(
                DownloadEntity(
                    systemDownloadId = -1L,
                    url = url,
                    filename = filename,
                    mimeType = mimeType,
                    filePath = filePath,
                    totalBytes = sizeBytes,
                    downloadedBytes = sizeBytes,
                    status = DownloadStatus.COMPLETED,
                    category = DownloadCategory.DOCUMENTS
                )
            )
        }
        if (notify && downloadNotificationsEnabled) {
            notificationManager.notifyPageSaved(filename.substringBeforeLast('.'), filename)
        }
    }

    fun getDeviceStorageStats(): StorageStats {
        return runCatching {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val total = stat.totalBytes
            val free = stat.availableBytes
            StorageStats(
                totalBytes = total,
                freeBytes = free,
                usedBytes = (total - free).coerceAtLeast(0L)
            )
        }.getOrElse {
            StorageStats(totalBytes = 128L * 1024 * 1024 * 1024, freeBytes = 108L * 1024 * 1024 * 1024, usedBytes = 20L * 1024 * 1024 * 1024)
        }
    }

    fun enqueueDownload(
        url: String,
        userAgent: String = "",
        contentDisposition: String = "",
        mimeType: String = "application/octet-stream",
        contentLength: Long = 0L
    ): Long {
        val filename = URLUtil.guessFileName(url, contentDisposition, mimeType)
        val category = DownloadCategory.classify(mimeType, filename)

        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(filename)
            setDescription("Ritam Browser Download")
            setMimeType(mimeType)
            if (userAgent.isNotBlank()) {
                addRequestHeader("User-Agent", userAgent)
            }
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Ritam/$filename")
        }

        val sysId = runCatching { systemDownloadManager.enqueue(request) }.getOrDefault(-1L)
        val targetPath = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Ritam/$filename"
        ).absolutePath

        scope.launch {
            repository.insertDownload(
                DownloadEntity(
                    systemDownloadId = sysId,
                    url = url,
                    filename = filename,
                    mimeType = mimeType,
                    filePath = targetPath,
                    totalBytes = contentLength.coerceAtLeast(0L),
                    downloadedBytes = 0L,
                    status = if (sysId > 0) DownloadStatus.RUNNING else DownloadStatus.FAILED,
                    category = category
                )
            )
        }
        return sysId
    }

    fun cancelDownload(download: DownloadEntity) {
        if (download.systemDownloadId > 0) {
            runCatching { systemDownloadManager.remove(download.systemDownloadId) }
        }
        scope.launch {
            repository.updateDownload(download.copy(status = DownloadStatus.CANCELLED))
        }
    }

    fun openDownloadedFile(download: DownloadEntity): Boolean {
        val file = File(download.filePath)
        val uri: Uri = if (file.exists()) {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } else if (download.systemDownloadId > 0) {
            systemDownloadManager.getUriForDownloadedFile(download.systemDownloadId) ?: return false
        } else {
            return false
        }

        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, download.mimeType.ifBlank { "*/*" })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun shareDownloadedFile(download: DownloadEntity): Boolean {
        val file = File(download.filePath)
        val uri: Uri? = if (file.exists()) {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } else if (download.systemDownloadId > 0) {
            systemDownloadManager.getUriForDownloadedFile(download.systemDownloadId)
        } else null

        val intent = if (uri != null) {
            Intent(Intent.ACTION_SEND).apply {
                type = download.mimeType.ifBlank { "*/*" }
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "${download.filename}: ${download.url}")
            }
        }
        return runCatching {
            context.startActivity(
                Intent.createChooser(intent, "Share ${download.filename}").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            true
        }.getOrDefault(false)
    }

    private fun startPollingActiveDownloads() {
        scope.launch {
            while (isActive) {
                val active = repository.getActiveDownloads()
                active.forEach { entity ->
                    if (entity.systemDownloadId > 0) {
                        syncSystemDownload(entity)
                    }
                }
                delay(if (active.isEmpty()) 2500L else 800L)
            }
        }
    }

    private suspend fun syncSystemDownload(entity: DownloadEntity) {
        val query = DownloadManager.Query().setFilterById(entity.systemDownloadId)
        var cursor: Cursor? = null
        try {
            cursor = systemDownloadManager.query(query)
            if (cursor != null && cursor.moveToFirst()) {
                val statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val bytesCol = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val totalCol = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                val sysStatus = if (statusCol >= 0) cursor.getInt(statusCol) else DownloadManager.STATUS_RUNNING
                val downloaded = if (bytesCol >= 0) cursor.getLong(bytesCol) else entity.downloadedBytes
                val total = if (totalCol >= 0) cursor.getLong(totalCol).coerceAtLeast(entity.totalBytes) else entity.totalBytes

                val mappedStatus = when (sysStatus) {
                    DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.COMPLETED
                    DownloadManager.STATUS_FAILED -> DownloadStatus.FAILED
                    DownloadManager.STATUS_PAUSED -> DownloadStatus.PAUSED
                    else -> DownloadStatus.RUNNING
                }

                if (mappedStatus != entity.status || downloaded != entity.downloadedBytes || total != entity.totalBytes) {
                    repository.updateDownload(
                        entity.copy(
                            status = mappedStatus,
                            downloadedBytes = downloaded,
                            totalBytes = total
                        )
                    )
                    if (mappedStatus == DownloadStatus.COMPLETED &&
                        entity.status != DownloadStatus.COMPLETED &&
                        downloadNotificationsEnabled
                    ) {
                        notificationManager.notifyDownloadCompleted(entity.filename)
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            cursor?.close()
        }
    }
}

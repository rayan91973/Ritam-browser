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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.example.core.RitamHttp
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

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
    private val httpClient = RitamHttp.builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val _activeSpeeds = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val activeSpeeds: StateFlow<Map<Long, Long>> = _activeSpeeds.asStateFlow()

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

        if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
            val dir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir,
                "Ritam"
            ).apply { if (!exists()) mkdirs() }
            val targetFile = resolveUniqueFile(dir, filename)
            scope.launch {
                val id = repository.insertDownload(
                    DownloadEntity(
                        systemDownloadId = -2L, // -2 = Managed resumable OkHttp downloader
                        url = url,
                        filename = targetFile.name,
                        mimeType = mimeType,
                        filePath = targetFile.absolutePath,
                        totalBytes = contentLength.coerceAtLeast(0L),
                        downloadedBytes = 0L,
                        status = DownloadStatus.RUNNING,
                        category = category
                    )
                )
                startOrResumeOkHttpDownload(
                    entity = DownloadEntity(
                        id = id,
                        systemDownloadId = -2L,
                        url = url,
                        filename = targetFile.name,
                        mimeType = mimeType,
                        filePath = targetFile.absolutePath,
                        totalBytes = contentLength.coerceAtLeast(0L),
                        downloadedBytes = 0L,
                        status = DownloadStatus.RUNNING,
                        category = category
                    ),
                    userAgent = userAgent
                )
            }
            return -2L
        }

        // Fallback to Android system DownloadManager for non-HTTP URIs
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(filename)
            setDescription("Ritam Browser Download")
            setMimeType(mimeType)
            if (userAgent.isNotBlank()) addRequestHeader("User-Agent", userAgent)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "Ritam/$filename")
        }
        val sysId = runCatching { systemDownloadManager.enqueue(request) }.getOrDefault(-1L)
        val targetPath = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
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

    fun pauseDownload(download: DownloadEntity) {
        activeJobs.remove(download.id)?.cancel()
        _activeSpeeds.update { it - download.id }
        scope.launch {
            val file = File(download.filePath)
            val currentBytes = if (file.exists()) file.length() else download.downloadedBytes
            repository.updateDownload(
                download.copy(
                    status = DownloadStatus.PAUSED,
                    downloadedBytes = currentBytes
                )
            )
        }
    }

    fun resumeDownload(download: DownloadEntity) {
        if (download.systemDownloadId > 0) return
        scope.launch {
            val updated = download.copy(status = DownloadStatus.RUNNING)
            repository.updateDownload(updated)
            startOrResumeOkHttpDownload(updated, userAgent = "")
        }
    }

    private fun startOrResumeOkHttpDownload(entity: DownloadEntity, userAgent: String) {
        activeJobs.remove(entity.id)?.cancel()
        val job = scope.launch {
            val file = File(entity.filePath)
            file.parentFile?.mkdirs()
            val existingBytes = if (file.exists()) file.length() else 0L
            try {
                val reqBuilder = Request.Builder().url(entity.url)
                if (userAgent.isNotBlank()) {
                    reqBuilder.header("User-Agent", userAgent)
                }
                if (existingBytes > 0L) {
                    reqBuilder.header("Range", "bytes=$existingBytes-")
                }

                httpClient.newCall(reqBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful && response.code != 206) {
                        if (response.code == 416 && entity.totalBytes > 0 && existingBytes >= entity.totalBytes) {
                            repository.updateDownload(
                                entity.copy(
                                    status = DownloadStatus.COMPLETED,
                                    downloadedBytes = existingBytes,
                                    totalBytes = existingBytes
                                )
                            )
                            return@launch
                        }
                        repository.updateDownload(entity.copy(status = DownloadStatus.FAILED))
                        return@launch
                    }

                    val body = response.body ?: run {
                        repository.updateDownload(entity.copy(status = DownloadStatus.FAILED))
                        return@launch
                    }
                    val isResumed = response.code == 206
                    val startOffset = if (isResumed) existingBytes else 0L
                    val remainingLen = body.contentLength()
                    val totalSize = if (remainingLen > 0) startOffset + remainingLen else entity.totalBytes

                    RandomAccessFile(file, "rw").use { raf ->
                        raf.seek(startOffset)
                        val buffer = ByteArray(16 * 1024)
                        val input = body.byteStream()
                        var downloaded = startOffset
                        var lastUiTime = System.currentTimeMillis()
                        var bytesSinceTick = 0L

                        while (isActive) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            raf.write(buffer, 0, read)
                            downloaded += read
                            bytesSinceTick += read

                            val now = System.currentTimeMillis()
                            val elapsed = now - lastUiTime
                            if (elapsed >= 500L) {
                                val speedBps = (bytesSinceTick * 1000L) / elapsed.coerceAtLeast(1L)
                                _activeSpeeds.update { it + (entity.id to speedBps) }
                                repository.updateDownload(
                                    entity.copy(
                                        downloadedBytes = downloaded,
                                        totalBytes = totalSize.coerceAtLeast(downloaded),
                                        status = DownloadStatus.RUNNING
                                    )
                                )
                                lastUiTime = now
                                bytesSinceTick = 0L
                            }
                        }

                        if (!isActive) {
                            repository.updateDownload(
                                entity.copy(
                                    downloadedBytes = downloaded,
                                    totalBytes = totalSize.coerceAtLeast(downloaded),
                                    status = DownloadStatus.PAUSED
                                )
                            )
                            return@launch
                        }

                        _activeSpeeds.update { it - entity.id }
                        repository.updateDownload(
                            entity.copy(
                                downloadedBytes = downloaded,
                                totalBytes = totalSize.coerceAtLeast(downloaded),
                                status = DownloadStatus.COMPLETED
                            )
                        )
                        if (downloadNotificationsEnabled) {
                            notificationManager.notifyDownloadCompleted(entity.filename)
                        }
                    }
                }
            } catch (_: kotlinx.coroutines.CancellationException) {
                _activeSpeeds.update { it - entity.id }
            } catch (_: Exception) {
                _activeSpeeds.update { it - entity.id }
                repository.updateDownload(entity.copy(status = DownloadStatus.FAILED))
            } finally {
                activeJobs.remove(entity.id)
            }
        }
        activeJobs[entity.id] = job
    }

    fun cancelDownload(download: DownloadEntity) {
        activeJobs.remove(download.id)?.cancel()
        _activeSpeeds.update { it - download.id }
        if (download.systemDownloadId > 0) {
            runCatching { systemDownloadManager.remove(download.systemDownloadId) }
        }
        scope.launch {
            runCatching { File(download.filePath).delete() }
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

    private fun resolveUniqueFile(dir: File, filename: String): File {
        var candidate = File(dir, filename)
        if (!candidate.exists()) return candidate
        val base = filename.substringBeforeLast('.')
        val ext = filename.substringAfterLast('.', "")
        var idx = 1
        while (candidate.exists()) {
            val nextName = if (ext.isNotEmpty()) "${base}_$idx.$ext" else "${base}_$idx"
            candidate = File(dir, nextName)
            idx++
        }
        return candidate
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
                delay(if (active.isEmpty()) 2500L else 900L)
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

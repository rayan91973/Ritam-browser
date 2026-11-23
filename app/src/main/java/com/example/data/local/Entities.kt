package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "history",
    indices = [Index(value = ["visitedAt"]), Index(value = ["domain"])]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val domain: String,
    val visitedAt: Long = System.currentTimeMillis(),
    val isBookmarked: Boolean = false
)

@Entity(
    tableName = "bookmark_folders"
)
data class BookmarkFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#325346",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["url"], unique = true), Index(value = ["folderId"])]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val folderId: Long = 1L,
    val iconKey: String = "public",
    val isPinnedHome: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "downloads",
    indices = [Index(value = ["createdAt"]), Index(value = ["status"])]
)
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val systemDownloadId: Long = -1L,
    val url: String,
    val filename: String,
    val mimeType: String,
    val filePath: String = "",
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: String = DownloadStatus.RUNNING,
    val category: String = DownloadCategory.DOCUMENTS,
    val createdAt: Long = System.currentTimeMillis()
)

object DownloadStatus {
    const val RUNNING = "RUNNING"
    const val PAUSED = "PAUSED"
    const val COMPLETED = "COMPLETED"
    const val FAILED = "FAILED"
    const val CANCELLED = "CANCELLED"
}

object DownloadCategory {
    const val DOCUMENTS = "Documents"
    const val MEDIA = "Media & Audio"
    const val ARCHIVES = "Archives"
    const val IMAGES = "Images"
    const val OTHER = "Other"

    fun classify(mimeType: String, filename: String): String {
        val lowerName = filename.lowercase()
        val lowerMime = mimeType.lowercase()
        return when {
            lowerMime.startsWith("audio/") || lowerMime.startsWith("video/") ||
                lowerName.endsWith(".mp3") || lowerName.endsWith(".flac") ||
                lowerName.endsWith(".mp4") || lowerName.endsWith(".wav") -> MEDIA
            lowerMime.startsWith("image/") || lowerName.endsWith(".png") ||
                lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                lowerName.endsWith(".webp") || lowerName.endsWith(".svg") -> IMAGES
            lowerName.endsWith(".zip") || lowerName.endsWith(".tar") ||
                lowerName.endsWith(".gz") || lowerName.endsWith(".7z") ||
                lowerName.endsWith(".rar") || lowerMime.contains("zip") -> ARCHIVES
            lowerMime.contains("pdf") || lowerMime.startsWith("text/") ||
                lowerName.endsWith(".pdf") || lowerName.endsWith(".json") ||
                lowerName.endsWith(".md") || lowerName.endsWith(".txt") ||
                lowerName.endsWith(".html") -> DOCUMENTS
            else -> OTHER
        }
    }
}

@Entity(tableName = "website_settings")
data class WebsiteSettingsEntity(
    @PrimaryKey val domain: String,
    val shieldsEnabled: Boolean = true,
    val javascriptEnabled: Boolean = true,
    val desktopMode: Boolean = false,
    val cookiesAllowed: Boolean = true,
    val httpsOnly: Boolean = true,
    val zoomPercent: Int = 100,
    val forceDarkOverride: Int = -1, // -1 = Follow global, 0 = Off, 1 = On
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "reading_list",
    indices = [Index(value = ["url"], unique = true), Index(value = ["savedAt"])]
)
data class ReadingListItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val domain: String,
    val excerpt: String = "",
    val readTimeMinutes: Int = 3,
    val isRead: Boolean = false,
    val savedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "filter_lists")
data class FilterListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val source: String,
    val description: String,
    val enabled: Boolean = true,
    val ruleCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_filters")
data class CustomFilterRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rule: String,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "blocking_stats",
    indices = [Index(value = ["domain", "type", "dateKey"], unique = true)]
)
data class BlockingStatsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val type: String, // "AD", "TRACKER", "MALICIOUS", "HTTPS"
    val count: Long = 0L,
    val bytesSaved: Long = 0L,
    val dateKey: String
)

@Entity(
    tableName = "search_history",
    indices = [Index(value = ["searchedAt"])]
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val searchedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "vault_entries")
data class VaultEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val domain: String,
    val username: String,
    val secretValue: String,
    val category: String = "Login", // "Login", "ApiKey", "Passkey"
    val createdAt: Long = System.currentTimeMillis()
)

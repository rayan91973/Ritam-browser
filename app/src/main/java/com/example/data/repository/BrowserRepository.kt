package com.example.data.repository

import com.example.data.local.BlockingStatsDao
import com.example.data.local.BlockingStatsEntity
import com.example.data.local.BookmarkDao
import com.example.data.local.BookmarkEntity
import com.example.data.local.BookmarkFolderEntity
import com.example.data.local.CustomFilterRuleEntity
import com.example.data.local.DownloadDao
import com.example.data.local.DownloadEntity
import com.example.data.local.FilterDao
import com.example.data.local.FilterListEntity
import com.example.data.local.HistoryDao
import com.example.data.local.HistoryEntity
import com.example.data.local.ReadingListDao
import com.example.data.local.ReadingListItemEntity
import com.example.data.local.SearchHistoryDao
import com.example.data.local.SearchHistoryEntity
import com.example.data.local.VaultDao
import com.example.data.local.VaultEntryEntity
import com.example.data.local.WebsiteSettingsDao
import com.example.data.local.WebsiteSettingsEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BrowserRepository(
    private val historyDao: HistoryDao,
    private val bookmarkDao: BookmarkDao,
    private val downloadDao: DownloadDao,
    private val websiteSettingsDao: WebsiteSettingsDao,
    private val filterDao: FilterDao,
    private val blockingStatsDao: BlockingStatsDao,
    private val searchHistoryDao: SearchHistoryDao,
    private val vaultDao: VaultDao,
    private val readingListDao: ReadingListDao
) {
    val history: Flow<List<HistoryEntity>> = historyDao.observeHistory()
    val allBookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.observeAllBookmarks()
    val homeBookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.observeHomeBookmarks()
    val bookmarkFolders: Flow<List<BookmarkFolderEntity>> = bookmarkDao.observeFolders()
    val downloads: Flow<List<DownloadEntity>> = downloadDao.observeDownloads()
    val websiteSettings: Flow<List<WebsiteSettingsEntity>> = websiteSettingsDao.observeAll()
    val filterLists: Flow<List<FilterListEntity>> = filterDao.observeFilterLists()
    val customFilterRules: Flow<List<CustomFilterRuleEntity>> = filterDao.observeCustomRules()
    val blockingStats: Flow<List<BlockingStatsEntity>> = blockingStatsDao.observeAllStats()
    val adsBlockedTotal: Flow<Long> = blockingStatsDao.observeTotalByType("AD")
    val trackersBlockedTotal: Flow<Long> = blockingStatsDao.observeTotalByType("TRACKER")
    val totalBytesSaved: Flow<Long> = blockingStatsDao.observeTotalBytesSaved()
    val recentSearches: Flow<List<SearchHistoryEntity>> = searchHistoryDao.observeRecentSearches()
    val vaultEntries: Flow<List<VaultEntryEntity>> = vaultDao.observeEntries()
    val readingList: Flow<List<ReadingListItemEntity>> = readingListDao.observeAll()

    fun observeIsBookmarked(url: String): Flow<Boolean> = bookmarkDao.observeIsBookmarked(url)

    suspend fun recordHistory(url: String, title: String, domain: String) {
        if (url.isBlank() || url.startsWith("ritam://")) return
        historyDao.insert(
            HistoryEntity(
                url = url,
                title = title.ifBlank { domain.ifBlank { url } },
                domain = domain
            )
        )
    }

    suspend fun deleteHistoryItem(id: Long) = historyDao.deleteById(id)
    suspend fun deleteHistoryItems(ids: List<Long>) = historyDao.deleteByIds(ids)
    suspend fun clearHistory() = historyDao.clearAll()

    suspend fun recordSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isNotEmpty()) {
            searchHistoryDao.insert(SearchHistoryEntity(query = trimmed))
        }
    }

    suspend fun clearSearchHistory() = searchHistoryDao.clearAll()

    suspend fun addBookmark(url: String, title: String, folderId: Long = 1L, isPinnedHome: Boolean = false) {
        val icon = title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "public"
        bookmarkDao.insertBookmark(
            BookmarkEntity(
                url = url,
                title = title.ifBlank { url },
                folderId = folderId,
                iconKey = icon,
                isPinnedHome = isPinnedHome
            )
        )
    }

    suspend fun addBookmarkFolder(name: String) {
        if (name.isNotBlank()) {
            bookmarkDao.insertFolder(BookmarkFolderEntity(name = name.trim()))
        }
    }

    suspend fun removeBookmarkById(id: Long) = bookmarkDao.deleteBookmarkById(id)
    suspend fun removeBookmarkByUrl(url: String) = bookmarkDao.deleteBookmarkByUrl(url)

    suspend fun exportBookmarksAsNetscapeHtml(): String {
        val bookmarks = bookmarkDao.getAllOnce()
        val folders = bookmarkDao.getAllFoldersOnce().associateBy { it.id }
        val sb = StringBuilder()
        sb.append("<!DOCTYPE NETSCAPE-Bookmark-file-1>\n")
        sb.append("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">\n")
        sb.append("<TITLE>Ritam Browser Bookmarks</TITLE>\n")
        sb.append("<H1>Ritam Sanctuary Bookmarks</H1>\n<DL><p>\n")
        bookmarks.groupBy { it.folderId }.forEach { (folderId, items) ->
            val folderName = folders[folderId]?.name ?: "Bookmarks"
            sb.append("    <DT><H3>${escapeHtml(folderName)}</H3>\n    <DL><p>\n")
            items.forEach { item ->
                sb.append("        <DT><A HREF=\"${escapeHtml(item.url)}\" ADD_DATE=\"${item.createdAt / 1000}\">${escapeHtml(item.title)}</A>\n")
            }
            sb.append("    </DL><p>\n")
        }
        sb.append("</DL><p>\n")
        return sb.toString()
    }

    suspend fun importBookmarksFromHtml(html: String): Int {
        val regex = Regex("""<A\s+[^>]*HREF="([^"]+)"[^>]*>([^<]+)</A>""", RegexOption.IGNORE_CASE)
        var count = 0
        regex.findAll(html).forEach { match ->
            val url = match.groupValues[1].trim()
            val title = match.groupValues[2].trim()
            if (url.startsWith("http://") || url.startsWith("https://")) {
                addBookmark(url = url, title = title)
                count++
            }
        }
        return count
    }

    suspend fun exportHistoryAsMarkdown(): String {
        val items = historyDao.getAllOnce()
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val sb = StringBuilder("# Ritam Browser History Export\n\n")
        items.forEach { item ->
            sb.append("- **${item.title}** — [${item.domain}](${item.url}) *(${fmt.format(Date(item.visitedAt))})*\n")
        }
        return sb.toString()
    }

    suspend fun insertDownload(download: DownloadEntity): Long = downloadDao.insert(download)
    suspend fun updateDownload(download: DownloadEntity) = downloadDao.update(download)
    suspend fun getActiveDownloads(): List<DownloadEntity> = downloadDao.getActiveDownloads()
    suspend fun deleteDownload(id: Long) = downloadDao.deleteById(id)
    suspend fun clearCompletedDownloads() = downloadDao.clearFinished()

    suspend fun getWebsiteSettings(domain: String): WebsiteSettingsEntity? =
        websiteSettingsDao.getForDomain(domain)

    suspend fun upsertWebsiteSettings(settings: WebsiteSettingsEntity) =
        websiteSettingsDao.upsert(settings)

    suspend fun setFilterListEnabled(id: Long, enabled: Boolean) =
        filterDao.setFilterListEnabled(id, enabled)

    suspend fun addCustomFilterRule(rule: String) {
        val trimmed = rule.trim()
        if (trimmed.isNotEmpty()) {
            filterDao.insertCustomRule(CustomFilterRuleEntity(rule = trimmed))
        }
    }

    suspend fun setCustomRuleEnabled(id: Long, enabled: Boolean) =
        filterDao.setCustomRuleEnabled(id, enabled)

    suspend fun deleteCustomRule(id: Long) = filterDao.deleteCustomRule(id)
    suspend fun getEnabledCustomRulesOnce(): List<CustomFilterRuleEntity> =
        filterDao.getEnabledCustomRulesOnce()

    suspend fun getFilterListsOnce(): List<FilterListEntity> = filterDao.getFilterListsOnce()

    suspend fun recordBlockedStat(domain: String, type: String, bytesSaved: Long) {
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val existing = blockingStatsDao.findEntry(domain, type, dateKey)
        if (existing != null) {
            blockingStatsDao.upsert(
                existing.copy(
                    count = existing.count + 1,
                    bytesSaved = existing.bytesSaved + bytesSaved
                )
            )
        } else {
            blockingStatsDao.upsert(
                BlockingStatsEntity(
                    domain = domain,
                    type = type,
                    count = 1L,
                    bytesSaved = bytesSaved,
                    dateKey = dateKey
                )
            )
        }
    }

    suspend fun clearBlockingStats() = blockingStatsDao.clearAll()

    suspend fun addVaultEntry(label: String, domain: String, username: String, secretValue: String, category: String) {
        vaultDao.insert(
            VaultEntryEntity(
                label = label.trim(),
                domain = domain.trim(),
                username = username.trim(),
                secretValue = secretValue,
                category = category
            )
        )
    }

    suspend fun deleteVaultEntry(id: Long) = vaultDao.deleteById(id)

    suspend fun addToReadingList(
        url: String,
        title: String,
        domain: String,
        excerpt: String = "",
        readTimeMinutes: Int = 3
    ) {
        if (url.isBlank() || url.startsWith("ritam://")) return
        readingListDao.insert(
            ReadingListItemEntity(
                url = url,
                title = title.ifBlank { domain.ifBlank { url } },
                domain = domain,
                excerpt = excerpt.take(240),
                readTimeMinutes = readTimeMinutes.coerceIn(1, 60)
            )
        )
    }

    suspend fun setReadingListRead(id: Long, isRead: Boolean) = readingListDao.setRead(id, isRead)
    suspend fun deleteReadingListItem(id: Long) = readingListDao.deleteById(id)
    suspend fun clearReadReadingList() = readingListDao.clearRead()
    suspend fun clearAllReadingList() = readingListDao.clearAll()

    private fun escapeHtml(input: String): String =
        input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}

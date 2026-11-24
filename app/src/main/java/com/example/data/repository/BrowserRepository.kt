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
import com.example.data.local.HighlightDao
import com.example.data.local.HighlightEntity
import com.example.data.local.HistoryDao
import com.example.data.local.HistoryEntity
import com.example.data.local.QuickControlUsageDao
import com.example.data.local.QuickControlUsageEntity
import com.example.data.local.ReadingListDao
import com.example.data.local.ReadingListItemEntity
import com.example.data.local.RssArticleEntity
import com.example.data.local.RssDao
import com.example.data.local.RssFeedEntity
import com.example.data.local.SearchHistoryDao
import com.example.data.local.SearchHistoryEntity
import com.example.data.local.SiteUsageDao
import com.example.data.local.SiteUsageEntity
import com.example.data.local.UserscriptDao
import com.example.data.local.UserscriptEntity
import com.example.data.local.VaultDao
import com.example.data.local.VaultEntryEntity
import com.example.data.local.WatchlistDao
import com.example.data.local.WatchlistItemEntity
import com.example.data.local.WebsiteSettingsDao
import com.example.data.local.WebsiteSettingsEntity
import com.example.services.CryptoVaultHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.json.JSONArray
import org.json.JSONObject
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
    private val readingListDao: ReadingListDao,
    private val userscriptDao: UserscriptDao? = null,
    private val watchlistDao: WatchlistDao? = null,
    private val highlightDao: HighlightDao? = null,
    private val rssDao: RssDao? = null,
    private val siteUsageDao: SiteUsageDao? = null,
    private val quickControlUsageDao: QuickControlUsageDao? = null
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
    val userscripts: Flow<List<UserscriptEntity>> = userscriptDao?.observeAll() ?: flowOf(emptyList())
    val watchlistItems: Flow<List<WatchlistItemEntity>> = watchlistDao?.observeAll() ?: flowOf(emptyList())
    val highlights: Flow<List<HighlightEntity>> = highlightDao?.observeAll() ?: flowOf(emptyList())
    val rssFeeds: Flow<List<RssFeedEntity>> = rssDao?.observeFeeds() ?: flowOf(emptyList())
    val rssArticles: Flow<List<RssArticleEntity>> = rssDao?.observeArticles() ?: flowOf(emptyList())
    val topSites: Flow<List<SiteUsageEntity>> = siteUsageDao?.observeTopSites() ?: flowOf(emptyList())
    val quickControlUsage: Flow<List<QuickControlUsageEntity>> =
        quickControlUsageDao?.observeAll() ?: flowOf(emptyList())

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

    suspend fun recordSiteVisit(url: String, title: String, domain: String) {
        val dao = siteUsageDao ?: return
        if (url.isBlank() || url.startsWith("ritam://") || url.startsWith("about:") ||
            url.startsWith("file://") || url.startsWith("content://")
        ) return
        val normalizedDomain = domain.trim().lowercase().removePrefix("www.").removePrefix("m.")
        if (normalizedDomain.isBlank()) return
        val existing = dao.findByDomain(normalizedDomain)
        val now = System.currentTimeMillis()
        val resolvedTitle = title.trim().takeIf {
            it.isNotBlank() && !it.startsWith("http://") && !it.startsWith("https://")
        } ?: existing?.title?.takeIf { it.isNotBlank() } ?: normalizedDomain.substringBefore('.').replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        if (existing != null) {
            dao.upsert(
                existing.copy(
                    url = url,
                    title = resolvedTitle,
                    visitCount = existing.visitCount + 1L,
                    lastVisitedAt = now
                )
            )
        } else {
            dao.upsert(
                SiteUsageEntity(
                    domain = normalizedDomain,
                    url = url,
                    title = resolvedTitle,
                    visitCount = 1L,
                    lastVisitedAt = now
                )
            )
        }
    }

    suspend fun clearSiteUsage() {
        siteUsageDao?.clearAll()
    }

    suspend fun recordQuickControlUse(controlId: String) {
        val dao = quickControlUsageDao ?: return
        val id = controlId.trim()
        if (id.isEmpty()) return
        val existing = dao.findByControlId(id)
        val now = System.currentTimeMillis()
        if (existing != null) {
            dao.upsert(
                existing.copy(
                    usageCount = existing.usageCount + 1L,
                    lastUsedAt = now
                )
            )
        } else {
            dao.upsert(
                QuickControlUsageEntity(
                    controlId = id,
                    usageCount = 1L,
                    lastUsedAt = now
                )
            )
        }
    }

    suspend fun clearQuickControlUsage() {
        quickControlUsageDao?.clearAll()
    }

    suspend fun deleteHistoryItem(id: Long) = historyDao.deleteById(id)
    suspend fun deleteHistoryItems(ids: List<Long>) = historyDao.deleteByIds(ids)
    suspend fun clearHistory() {
        historyDao.clearAll()
        siteUsageDao?.clearAll()
    }

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

    suspend fun importBrowserData(rawContent: String): Int {
        val trimmed = rawContent.trim()
        if (trimmed.startsWith("<")) {
            return importBookmarksFromHtml(trimmed)
        } else if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return runCatching {
                if (trimmed.startsWith("[")) {
                    val arr = JSONArray(trimmed)
                    var count = 0
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val url = obj.optString("url").ifBlank { obj.optString("uri") }
                        val title = obj.optString("title").ifBlank { obj.optString("name") }
                        if (url.startsWith("http")) {
                            addBookmark(url = url, title = title.ifBlank { url })
                            count++
                        }
                    }
                    count
                } else {
                    val root = JSONObject(trimmed)
                    var count = 0
                    val roots = root.optJSONObject("roots")
                    if (roots != null) {
                        val keys = listOf("bookmark_bar", "other", "synced")
                        keys.forEach { key ->
                            val folder = roots.optJSONObject(key)
                            if (folder != null) {
                                count += parseChromeBookmarkNode(folder)
                            }
                        }
                    } else if (root.has("bookmarks")) {
                        return importConfigBackupJson(trimmed)
                    } else {
                        val url = root.optString("url")
                        val title = root.optString("title")
                        if (url.startsWith("http")) {
                            addBookmark(url = url, title = title.ifBlank { url })
                            count++
                        }
                    }
                    count
                }
            }.getOrDefault(0)
        }
        return 0
    }

    private suspend fun parseChromeBookmarkNode(node: JSONObject): Int {
        var count = 0
        val type = node.optString("type")
        if (type == "url") {
            val url = node.optString("url")
            val name = node.optString("name").ifBlank { node.optString("title") }
            if (url.startsWith("http")) {
                addBookmark(url = url, title = name.ifBlank { url })
                count++
            }
        }
        val children = node.optJSONArray("children")
        if (children != null) {
            for (i in 0 until children.length()) {
                val child = children.optJSONObject(i)
                if (child != null) {
                    count += parseChromeBookmarkNode(child)
                }
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

    suspend fun updateFilterListStats(id: Long, ruleCount: Int, updatedAt: Long = System.currentTimeMillis()) =
        filterDao.updateFilterListStats(id, ruleCount, updatedAt)

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
        val encrypted = CryptoVaultHelper.encryptLocalSecret(secretValue)
        vaultDao.insert(
            VaultEntryEntity(
                label = label.trim(),
                domain = domain.trim(),
                username = username.trim(),
                secretValue = encrypted,
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

    // --- Userscripts & Custom Per-Site CSS/JS ---
    suspend fun getEnabledUserscriptsOnce(): List<UserscriptEntity> =
        userscriptDao?.getEnabledOnce() ?: emptyList()

    suspend fun addUserscript(name: String, urlPattern: String, code: String, isCss: Boolean) {
        if (name.isBlank() || code.isBlank()) return
        userscriptDao?.insert(
            UserscriptEntity(
                name = name.trim(),
                urlPattern = urlPattern.trim().ifBlank { "*" },
                code = code,
                isCss = isCss,
                enabled = true
            )
        )
    }

    suspend fun setUserscriptEnabled(id: Long, enabled: Boolean) {
        userscriptDao?.setEnabled(id, enabled)
    }

    suspend fun deleteUserscript(id: Long) {
        userscriptDao?.deleteById(id)
    }

    // --- Page Change Watchlist ---
    suspend fun upsertWatchlistItem(item: WatchlistItemEntity) {
        watchlistDao?.upsert(item)
    }

    suspend fun getWatchlistOnce(): List<WatchlistItemEntity> =
        watchlistDao?.getAllOnce() ?: emptyList()

    suspend fun markWatchlistSeen(id: Long) {
        watchlistDao?.markSeen(id)
    }

    suspend fun deleteWatchlistItem(id: Long) {
        watchlistDao?.deleteById(id)
    }

    // --- Highlights & Private Notes ---
    suspend fun addHighlight(
        url: String,
        domain: String,
        title: String,
        quoteText: String,
        note: String = "",
        colorHex: String = "#91D5AE"
    ) {
        if (quoteText.isBlank()) return
        highlightDao?.insert(
            HighlightEntity(
                url = url,
                domain = domain,
                title = title.ifBlank { domain.ifBlank { url } },
                quoteText = quoteText.trim(),
                note = note.trim(),
                colorHex = colorHex
            )
        )
    }

    suspend fun deleteHighlight(id: Long) {
        highlightDao?.deleteById(id)
    }

    suspend fun exportHighlightsAsMarkdown(): String {
        val list = highlightDao?.getAllOnce() ?: emptyList()
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val sb = StringBuilder("# Ritam Webpage Highlights & Notes\n\n")
        list.forEach { h ->
            sb.append("## ${h.title}\n")
            sb.append("Source: [${h.domain}](${h.url}) • ${fmt.format(Date(h.createdAt))}\n\n")
            sb.append("> ${h.quoteText.replace("\n", "\n> ")}\n\n")
            if (h.note.isNotBlank()) {
                sb.append("**Note:** ${h.note}\n\n")
            }
            sb.append("---\n\n")
        }
        return sb.toString()
    }

    // --- RSS / Atom Feeds ---
    suspend fun saveRssFeedAndArticles(
        feedTitle: String,
        feedUrl: String,
        siteUrl: String,
        articles: List<RssArticleEntity>
    ): Long {
        val dao = rssDao ?: return -1L
        val existingId = dao.getFeedsOnce().firstOrNull { it.feedUrl == feedUrl }?.id ?: 0L
        val savedFeedId = dao.insertFeed(
            RssFeedEntity(
                id = existingId,
                title = feedTitle.ifBlank { feedUrl },
                feedUrl = feedUrl,
                siteUrl = siteUrl,
                lastSyncedAt = System.currentTimeMillis()
            )
        )
        val resolvedId = if (existingId > 0L) existingId else savedFeedId
        if (articles.isNotEmpty()) {
            dao.insertArticles(articles.map { it.copy(feedId = resolvedId, feedTitle = feedTitle) })
        }
        return resolvedId
    }

    suspend fun getRssFeedsOnce(): List<RssFeedEntity> = rssDao?.getFeedsOnce() ?: emptyList()

    suspend fun setRssArticleRead(id: Long, isRead: Boolean) {
        rssDao?.setArticleRead(id, isRead)
    }

    suspend fun deleteRssFeed(feedId: Long) {
        rssDao?.deleteArticlesByFeed(feedId)
        rssDao?.deleteFeed(feedId)
    }

    // --- Full Configuration Backup & Restore (JSON) ---
    suspend fun exportConfigBackupJson(): String {
        val root = JSONObject()
        root.put("format", "ritam_backup_v1")
        root.put("exportedAt", System.currentTimeMillis())

        val bmArray = JSONArray()
        bookmarkDao.getAllOnce().forEach { bm ->
            bmArray.put(JSONObject().apply {
                put("url", bm.url)
                put("title", bm.title)
                put("folderId", bm.folderId)
                put("isPinnedHome", bm.isPinnedHome)
            })
        }
        root.put("bookmarks", bmArray)

        val rulesArray = JSONArray()
        filterDao.getEnabledCustomRulesOnce().forEach { r ->
            rulesArray.put(JSONObject().apply {
                put("rule", r.rule)
                put("enabled", r.enabled)
            })
        }
        root.put("customRules", rulesArray)

        val scriptsArray = JSONArray()
        userscriptDao?.getAllOnce()?.forEach { s ->
            scriptsArray.put(JSONObject().apply {
                put("name", s.name)
                put("urlPattern", s.urlPattern)
                put("code", s.code)
                put("isCss", s.isCss)
                put("enabled", s.enabled)
            })
        }
        root.put("userscripts", scriptsArray)

        val highlightsArray = JSONArray()
        highlightDao?.getAllOnce()?.forEach { h ->
            highlightsArray.put(JSONObject().apply {
                put("url", h.url)
                put("domain", h.domain)
                put("title", h.title)
                put("quoteText", h.quoteText)
                put("note", h.note)
                put("colorHex", h.colorHex)
            })
        }
        root.put("highlights", highlightsArray)

        return root.toString(2)
    }

    suspend fun importConfigBackupJson(rawJson: String): Int {
        val root = JSONObject(rawJson)
        var restoredCount = 0

        val bmArray = root.optJSONArray("bookmarks")
        if (bmArray != null) {
            for (i in 0 until bmArray.length()) {
                val o = bmArray.optJSONObject(i) ?: continue
                val url = o.optString("url")
                if (url.startsWith("http")) {
                    addBookmark(
                        url = url,
                        title = o.optString("title", url),
                        folderId = o.optLong("folderId", 1L),
                        isPinnedHome = o.optBoolean("isPinnedHome", false)
                    )
                    restoredCount++
                }
            }
        }

        val rulesArray = root.optJSONArray("customRules")
        if (rulesArray != null) {
            for (i in 0 until rulesArray.length()) {
                val o = rulesArray.optJSONObject(i) ?: continue
                val rule = o.optString("rule")
                if (rule.isNotBlank()) {
                    addCustomFilterRule(rule)
                    restoredCount++
                }
            }
        }

        val scriptsArray = root.optJSONArray("userscripts")
        if (scriptsArray != null) {
            for (i in 0 until scriptsArray.length()) {
                val o = scriptsArray.optJSONObject(i) ?: continue
                val name = o.optString("name")
                val code = o.optString("code")
                if (name.isNotBlank() && code.isNotBlank()) {
                    addUserscript(
                        name = name,
                        urlPattern = o.optString("urlPattern", "*"),
                        code = code,
                        isCss = o.optBoolean("isCss", false)
                    )
                    restoredCount++
                }
            }
        }

        val highlightsArray = root.optJSONArray("highlights")
        if (highlightsArray != null) {
            for (i in 0 until highlightsArray.length()) {
                val o = highlightsArray.optJSONObject(i) ?: continue
                val quote = o.optString("quoteText")
                if (quote.isNotBlank()) {
                    addHighlight(
                        url = o.optString("url"),
                        domain = o.optString("domain"),
                        title = o.optString("title"),
                        quoteText = quote,
                        note = o.optString("note"),
                        colorHex = o.optString("colorHex", "#91D5AE")
                    )
                    restoredCount++
                }
            }
        }

        return restoredCount
    }

    suspend fun exportEncryptedVaultAndConfigJson(passphrase: String): String {
        val baseJson = exportConfigBackupJson()
        val root = JSONObject(baseJson)
        root.put("format", "ritam_vault_backup_v2")
        val vaultArray = JSONArray()
        vaultDao.getAllOnce().forEach { entry ->
            val plainSecret = CryptoVaultHelper.decryptLocalSecret(entry.secretValue)
            vaultArray.put(JSONObject().apply {
                put("label", entry.label)
                put("domain", entry.domain)
                put("username", entry.username)
                put("secretValue", plainSecret)
                put("category", entry.category)
            })
        }
        root.put("vaultEntries", vaultArray)
        return CryptoVaultHelper.encryptBackupWithPassphrase(root.toString(), passphrase)
    }

    suspend fun importEncryptedBackup(rawContent: String, passphrase: String?): Int {
        val plainJson = if (CryptoVaultHelper.isEncryptedBackupEnvelope(rawContent)) {
            val pass = passphrase?.takeIf { it.isNotEmpty() }
                ?: throw IllegalArgumentException("Passphrase required for encrypted backup")
            CryptoVaultHelper.decryptBackupWithPassphrase(rawContent, pass)
        } else {
            rawContent
        }
        var restoredCount = importConfigBackupJson(plainJson)
        val root = JSONObject(plainJson)
        val vaultArray = root.optJSONArray("vaultEntries")
        if (vaultArray != null) {
            for (i in 0 until vaultArray.length()) {
                val o = vaultArray.optJSONObject(i) ?: continue
                val label = o.optString("label")
                val secret = o.optString("secretValue")
                if (label.isNotBlank() && secret.isNotBlank()) {
                    addVaultEntry(
                        label = label,
                        domain = o.optString("domain"),
                        username = o.optString("username"),
                        secretValue = secret,
                        category = o.optString("category", "Login")
                    )
                    restoredCount++
                }
            }
        }
        return restoredCount
    }

    private fun escapeHtml(input: String): String =
        input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}

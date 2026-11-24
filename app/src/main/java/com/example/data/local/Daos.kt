package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY visitedAt DESC LIMIT 500")
    fun observeHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' OR domain LIKE '%' || :query || '%' ORDER BY visitedAt DESC LIMIT 200")
    fun searchHistory(query: String): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntity)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM history WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM history")
    suspend fun clearAll()

    @Query("SELECT * FROM history ORDER BY visitedAt DESC")
    suspend fun getAllOnce(): List<HistoryEntity>
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY isPinnedHome DESC, createdAt DESC")
    fun observeAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE isPinnedHome = 1 ORDER BY createdAt ASC LIMIT 8")
    fun observeHomeBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmark_folders ORDER BY id ASC")
    fun observeFolders(): Flow<List<BookmarkFolderEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url)")
    fun observeIsBookmarked(url: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: BookmarkFolderEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteBookmarkByUrl(url: String)

    @Query("SELECT * FROM bookmarks ORDER BY id ASC")
    suspend fun getAllOnce(): List<BookmarkEntity>

    @Query("SELECT * FROM bookmark_folders ORDER BY id ASC")
    suspend fun getAllFoldersOnce(): List<BookmarkFolderEntity>
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeDownloads(): Flow<List<DownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity): Long

    @Update
    suspend fun update(download: DownloadEntity)

    @Query("SELECT * FROM downloads WHERE status IN ('RUNNING', 'PAUSED')")
    suspend fun getActiveDownloads(): List<DownloadEntity>

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM downloads WHERE status IN ('COMPLETED', 'FAILED', 'CANCELLED')")
    suspend fun clearFinished()
}

@Dao
interface WebsiteSettingsDao {
    @Query("SELECT * FROM website_settings ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<WebsiteSettingsEntity>>

    @Query("SELECT * FROM website_settings WHERE domain = :domain LIMIT 1")
    suspend fun getForDomain(domain: String): WebsiteSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: WebsiteSettingsEntity)

    @Query("DELETE FROM website_settings WHERE domain = :domain")
    suspend fun deleteForDomain(domain: String)
}

@Dao
interface FilterDao {
    @Query("SELECT * FROM filter_lists ORDER BY id ASC")
    fun observeFilterLists(): Flow<List<FilterListEntity>>

    @Query("SELECT * FROM custom_filters ORDER BY createdAt DESC")
    fun observeCustomRules(): Flow<List<CustomFilterRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFilterList(item: FilterListEntity)

    @Query("UPDATE filter_lists SET enabled = :enabled WHERE id = :id")
    suspend fun setFilterListEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE filter_lists SET ruleCount = :ruleCount, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFilterListStats(id: Long, ruleCount: Int, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomRule(rule: CustomFilterRuleEntity)

    @Query("UPDATE custom_filters SET enabled = :enabled WHERE id = :id")
    suspend fun setCustomRuleEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM custom_filters WHERE id = :id")
    suspend fun deleteCustomRule(id: Long)

    @Query("SELECT * FROM custom_filters WHERE enabled = 1")
    suspend fun getEnabledCustomRulesOnce(): List<CustomFilterRuleEntity>

    @Query("SELECT * FROM filter_lists")
    suspend fun getFilterListsOnce(): List<FilterListEntity>
}

@Dao
interface BlockingStatsDao {
    @Query("SELECT * FROM blocking_stats")
    fun observeAllStats(): Flow<List<BlockingStatsEntity>>

    @Query("SELECT COALESCE(SUM(count), 0) FROM blocking_stats WHERE type = :type")
    fun observeTotalByType(type: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(bytesSaved), 0) FROM blocking_stats")
    fun observeTotalBytesSaved(): Flow<Long>

    @Query("SELECT * FROM blocking_stats WHERE domain = :domain AND type = :type AND dateKey = :dateKey LIMIT 1")
    suspend fun findEntry(domain: String, type: String, dateKey: String): BlockingStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stat: BlockingStatsEntity)

    @Query("DELETE FROM blocking_stats")
    suspend fun clearAll()
}

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT 20")
    fun observeRecentSearches(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(search: SearchHistoryEntity)

    @Query("DELETE FROM search_history")
    suspend fun clearAll()
}

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_entries ORDER BY createdAt DESC")
    fun observeEntries(): Flow<List<VaultEntryEntity>>

    @Query("SELECT * FROM vault_entries ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<VaultEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: VaultEntryEntity)

    @Query("DELETE FROM vault_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM vault_entries")
    suspend fun clearAll()
}

@Dao
interface ReadingListDao {
    @Query("SELECT * FROM reading_list ORDER BY isRead ASC, savedAt DESC")
    fun observeAll(): Flow<List<ReadingListItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ReadingListItemEntity): Long

    @Query("UPDATE reading_list SET isRead = :isRead WHERE id = :id")
    suspend fun setRead(id: Long, isRead: Boolean)

    @Query("DELETE FROM reading_list WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM reading_list WHERE isRead = 1")
    suspend fun clearRead()

    @Query("DELETE FROM reading_list")
    suspend fun clearAll()
}

@Dao
interface UserscriptDao {
    @Query("SELECT * FROM userscripts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<UserscriptEntity>>

    @Query("SELECT * FROM userscripts WHERE enabled = 1")
    suspend fun getEnabledOnce(): List<UserscriptEntity>

    @Query("SELECT * FROM userscripts")
    suspend fun getAllOnce(): List<UserscriptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(script: UserscriptEntity): Long

    @Query("UPDATE userscripts SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM userscripts WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist_items ORDER BY hasUnreadChange DESC, lastCheckedAt DESC")
    fun observeAll(): Flow<List<WatchlistItemEntity>>

    @Query("SELECT * FROM watchlist_items")
    suspend fun getAllOnce(): List<WatchlistItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: WatchlistItemEntity): Long

    @Query("UPDATE watchlist_items SET hasUnreadChange = 0 WHERE id = :id")
    suspend fun markSeen(id: Long)

    @Query("DELETE FROM watchlist_items WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface HighlightDao {
    @Query("SELECT * FROM highlights ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<HighlightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(highlight: HighlightEntity): Long

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM highlights")
    suspend fun clearAll()
}

@Dao
interface RssDao {
    @Query("SELECT * FROM rss_feeds ORDER BY lastSyncedAt DESC")
    fun observeFeeds(): Flow<List<RssFeedEntity>>

    @Query("SELECT * FROM rss_articles ORDER BY isRead ASC, fetchedAt DESC LIMIT 200")
    fun observeArticles(): Flow<List<RssArticleEntity>>

    @Query("SELECT * FROM rss_feeds")
    suspend fun getFeedsOnce(): List<RssFeedEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeed(feed: RssFeedEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticles(articles: List<RssArticleEntity>)

    @Query("UPDATE rss_articles SET isRead = :isRead WHERE id = :id")
    suspend fun setArticleRead(id: Long, isRead: Boolean)

    @Query("DELETE FROM rss_articles WHERE feedId = :feedId")
    suspend fun deleteArticlesByFeed(feedId: Long)

    @Query("DELETE FROM rss_feeds WHERE id = :feedId")
    suspend fun deleteFeed(feedId: Long)
}

@Dao
interface SiteUsageDao {
    @Query("SELECT * FROM site_usage ORDER BY visitCount DESC, lastVisitedAt DESC LIMIT 16")
    fun observeTopSites(): Flow<List<SiteUsageEntity>>

    @Query("SELECT * FROM site_usage WHERE domain = :domain LIMIT 1")
    suspend fun findByDomain(domain: String): SiteUsageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: SiteUsageEntity)

    @Query("DELETE FROM site_usage")
    suspend fun clearAll()
}

@Dao
interface QuickControlUsageDao {
    @Query("SELECT * FROM quick_control_usage ORDER BY usageCount DESC, lastUsedAt DESC")
    fun observeAll(): Flow<List<QuickControlUsageEntity>>

    @Query("SELECT * FROM quick_control_usage WHERE controlId = :controlId LIMIT 1")
    suspend fun findByControlId(controlId: String): QuickControlUsageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: QuickControlUsageEntity)

    @Query("DELETE FROM quick_control_usage")
    suspend fun clearAll()
}

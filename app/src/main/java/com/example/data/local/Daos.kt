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

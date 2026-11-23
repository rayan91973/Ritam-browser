package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import androidx.room.migration.Migration

@Database(
    entities = [
        HistoryEntity::class,
        BookmarkEntity::class,
        BookmarkFolderEntity::class,
        DownloadEntity::class,
        WebsiteSettingsEntity::class,
        FilterListEntity::class,
        CustomFilterRuleEntity::class,
        BlockingStatsEntity::class,
        SearchHistoryEntity::class,
        VaultEntryEntity::class,
        ReadingListItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class RitamDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun downloadDao(): DownloadDao
    abstract fun websiteSettingsDao(): WebsiteSettingsDao
    abstract fun filterDao(): FilterDao
    abstract fun blockingStatsDao(): BlockingStatsDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun vaultDao(): VaultDao
    abstract fun readingListDao(): ReadingListDao

    companion object {
        @Volatile
        private var INSTANCE: RitamDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE website_settings ADD COLUMN zoomPercent INTEGER NOT NULL DEFAULT 100")
                db.execSQL("ALTER TABLE website_settings ADD COLUMN forceDarkOverride INTEGER NOT NULL DEFAULT -1")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reading_list` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `url` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `domain` TEXT NOT NULL,
                        `excerpt` TEXT NOT NULL,
                        `readTimeMinutes` INTEGER NOT NULL,
                        `isRead` INTEGER NOT NULL,
                        `savedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_reading_list_url` ON `reading_list` (`url`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_list_savedAt` ON `reading_list` (`savedAt`)")
            }
        }

        fun getInstance(context: Context): RitamDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): RitamDatabase {
            return Room.databaseBuilder(
                appContext,
                RitamDatabase::class.java,
                "ritam_browser.db"
            )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            seedInitialData(getInstance(appContext))
                        }
                    }
                })
                .build()
        }

        private suspend fun seedInitialData(database: RitamDatabase) {
            val bookmarkDao = database.bookmarkDao()
            val filterDao = database.filterDao()

            // Default bookmark folders matching Library / Settings design
            val designId = bookmarkDao.insertFolder(BookmarkFolderEntity(name = "Design", colorHex = "#325346"))
            val researchId = bookmarkDao.insertFolder(BookmarkFolderEntity(name = "Research", colorHex = "#8B4513"))
            val engineeringId = bookmarkDao.insertFolder(BookmarkFolderEntity(name = "Engineering", colorHex = "#2C3E50"))
            bookmarkDao.insertFolder(BookmarkFolderEntity(name = "Reading List", colorHex = "#526069"))

            // 7 pinned Sanctuary Bookmarks + 1 Architecture Essay (8th slot is "+ Add" or 8 destinations)
            val sanctuaryBookmarks = listOf(
                BookmarkEntity(
                    url = "https://www.google.com",
                    title = "Google",
                    folderId = engineeringId,
                    iconKey = "G",
                    isPinnedHome = true
                ),
                BookmarkEntity(
                    url = "https://m.youtube.com",
                    title = "YouTube",
                    folderId = designId,
                    iconKey = "smart_display",
                    isPinnedHome = true
                ),
                BookmarkEntity(
                    url = "https://en.wikipedia.org/wiki/Architecture",
                    title = "Wikipedia",
                    folderId = researchId,
                    iconKey = "W",
                    isPinnedHome = true
                ),
                BookmarkEntity(
                    url = "https://github.com/trending",
                    title = "GitHub",
                    folderId = engineeringId,
                    iconKey = "code",
                    isPinnedHome = true
                ),
                BookmarkEntity(
                    url = "https://duckduckgo.com",
                    title = "DuckDuckGo",
                    folderId = researchId,
                    iconKey = "travel_explore",
                    isPinnedHome = true
                ),
                BookmarkEntity(
                    url = "https://www.reddit.com",
                    title = "Reddit",
                    folderId = designId,
                    iconKey = "forum",
                    isPinnedHome = true
                ),
                BookmarkEntity(
                    url = "https://news.ycombinator.com",
                    title = "HackerNews",
                    folderId = engineeringId,
                    iconKey = "Y",
                    isPinnedHome = true
                )
            )
            sanctuaryBookmarks.forEach { bookmarkDao.insertBookmark(it) }

            // Default Ritam Shields filter lists
            filterDao.insertFilterList(
                FilterListEntity(
                    name = "EasyList Core",
                    source = "https://easylist.to/easylist/easylist.txt",
                    description = "Primary advertising domain & URL rules",
                    enabled = true,
                    ruleCount = 412
                )
            )
            filterDao.insertFilterList(
                FilterListEntity(
                    name = "EasyPrivacy Trackers",
                    source = "https://easylist.to/easylist/easyprivacy.txt",
                    description = "Third-party analytics, pixels & fingerprinting scripts",
                    enabled = true,
                    ruleCount = 286
                )
            )
            filterDao.insertFilterList(
                FilterListEntity(
                    name = "Peter Lowe's Ad & Tracking List",
                    source = "https://pgl.yoyo.org/adservers/",
                    description = "Compact host-level ad and tracking server blocklist",
                    enabled = true,
                    ruleCount = 194
                )
            )
            filterDao.insertFilterList(
                FilterListEntity(
                    name = "Ritam Threat Intelligence",
                    source = "ritam://shields/threat-list",
                    description = "Known phishing & malicious domain protection",
                    enabled = true,
                    ruleCount = 64
                )
            )
        }
    }
}

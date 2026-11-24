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
        ReadingListItemEntity::class,
        UserscriptEntity::class,
        WatchlistItemEntity::class,
        HighlightEntity::class,
        RssFeedEntity::class,
        RssArticleEntity::class,
        SiteUsageEntity::class,
        QuickControlUsageEntity::class
    ],
    version = 5,
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
    abstract fun userscriptDao(): UserscriptDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun highlightDao(): HighlightDao
    abstract fun rssDao(): RssDao
    abstract fun siteUsageDao(): SiteUsageDao
    abstract fun quickControlUsageDao(): QuickControlUsageDao

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE website_settings ADD COLUMN isEphemeralContainer INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `userscripts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `urlPattern` TEXT NOT NULL,
                        `code` TEXT NOT NULL,
                        `isCss` INTEGER NOT NULL,
                        `enabled` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `watchlist_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `url` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `domain` TEXT NOT NULL,
                        `lastContentHash` TEXT NOT NULL,
                        `lastCheckedAt` INTEGER NOT NULL,
                        `hasUnreadChange` INTEGER NOT NULL,
                        `checkIntervalHours` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_watchlist_items_url` ON `watchlist_items` (`url`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `highlights` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `url` TEXT NOT NULL,
                        `domain` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `quoteText` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `colorHex` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_highlights_url` ON `highlights` (`url`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_highlights_createdAt` ON `highlights` (`createdAt`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `rss_feeds` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `feedUrl` TEXT NOT NULL,
                        `siteUrl` TEXT NOT NULL,
                        `lastSyncedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_rss_feeds_feedUrl` ON `rss_feeds` (`feedUrl`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `rss_articles` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `feedId` INTEGER NOT NULL,
                        `feedTitle` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `link` TEXT NOT NULL,
                        `summary` TEXT NOT NULL,
                        `publishedLabel` TEXT NOT NULL,
                        `isRead` INTEGER NOT NULL,
                        `fetchedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_rss_articles_feedId` ON `rss_articles` (`feedId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_rss_articles_link` ON `rss_articles` (`link`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE website_settings ADD COLUMN cameraPermission INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE website_settings ADD COLUMN micPermission INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE website_settings ADD COLUMN locationPermission INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `site_usage` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `domain` TEXT NOT NULL,
                        `url` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `visitCount` INTEGER NOT NULL,
                        `lastVisitedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_site_usage_domain` ON `site_usage` (`domain`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_site_usage_visitCount` ON `site_usage` (`visitCount`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `quick_control_usage` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `controlId` TEXT NOT NULL,
                        `usageCount` INTEGER NOT NULL,
                        `lastUsedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_quick_control_usage_controlId` ON `quick_control_usage` (`controlId`)")
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `site_usage` (`domain`, `url`, `title`, `visitCount`, `lastVisitedAt`)
                    SELECT `domain`, MAX(`url`), MAX(`title`), COUNT(*), MAX(`visitedAt`)
                    FROM `history`
                    WHERE `domain` != '' AND `url` NOT LIKE 'ritam://%'
                    GROUP BY `domain`
                    """.trimIndent()
                )
                db.execSQL("UPDATE `bookmarks` SET `iconKey` = 'YT' WHERE `iconKey` = 'smart_display'")
                db.execSQL("UPDATE `bookmarks` SET `iconKey` = 'GH' WHERE `iconKey` = 'code'")
                db.execSQL("UPDATE `bookmarks` SET `iconKey` = 'DD' WHERE `iconKey` = 'travel_explore'")
                db.execSQL("UPDATE `bookmarks` SET `iconKey` = 'RD' WHERE `iconKey` = 'forum'")
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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
                    iconKey = "YT",
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
                    iconKey = "GH",
                    isPinnedHome = true
                ),
                BookmarkEntity(
                    url = "https://duckduckgo.com",
                    title = "DuckDuckGo",
                    folderId = researchId,
                    iconKey = "DD",
                    isPinnedHome = true
                ),
                BookmarkEntity(
                    url = "https://www.reddit.com",
                    title = "Reddit",
                    folderId = designId,
                    iconKey = "RD",
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

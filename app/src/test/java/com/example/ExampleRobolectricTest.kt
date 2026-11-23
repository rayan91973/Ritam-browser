package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.RitamDatabase
import com.example.data.repository.BrowserRepository
import com.example.privacy.BlockCategory
import com.example.privacy.RitamShieldsEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: RitamDatabase
    private lateinit var repository: BrowserRepository
    private lateinit var shields: RitamShieldsEngine

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RitamDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = BrowserRepository(
            historyDao = db.historyDao(),
            bookmarkDao = db.bookmarkDao(),
            downloadDao = db.downloadDao(),
            websiteSettingsDao = db.websiteSettingsDao(),
            filterDao = db.filterDao(),
            blockingStatsDao = db.blockingStatsDao(),
            searchHistoryDao = db.searchHistoryDao(),
            vaultDao = db.vaultDao(),
            readingListDao = db.readingListDao()
        )
        shields = RitamShieldsEngine(repository)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun appNameMatchesRitam() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Ritam", appName)
    }

    @Test
    fun shieldsBlocksKnownAdsAndTrackersAndUpgradesHttps() {
        val adDecision = shields.shouldInterceptRequest(
            requestUrl = "https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js",
            pageDomain = "example.com",
            isPrivate = false
        )
        assertTrue(adDecision.isBlocked)
        assertEquals(BlockCategory.AD, adDecision.category)

        val trackerDecision = shields.shouldInterceptRequest(
            requestUrl = "https://www.google-analytics.com/analytics.js",
            pageDomain = "example.com",
            isPrivate = false
        )
        assertTrue(trackerDecision.isBlocked)
        assertEquals(BlockCategory.TRACKER, trackerDecision.category)

        val firstParty = shields.shouldInterceptRequest(
            requestUrl = "https://example.com/styles.css",
            pageDomain = "example.com",
            isPrivate = false
        )
        assertFalse(firstParty.isBlocked)

        assertTrue(shields.isMaliciousUrl("https://testsafebrowsing.appspot.com/s/phishing.html"))
        assertEquals("https://en.wikipedia.org/wiki/Architecture", shields.maybeUpgradeToHttps("http://en.wikipedia.org/wiki/Architecture", null))
    }

    @Test
    fun bookmarksExportAndImportRoundTrip() = runBlocking {
        repository.addBookmark("https://kotlinlang.org", "Kotlin", 1L, true)
        val html = repository.exportBookmarksAsNetscapeHtml()
        assertTrue(html.contains("https://kotlinlang.org"))
        assertTrue(html.contains("Kotlin"))

        val imported = repository.importBookmarksFromHtml(
            """<DT><A HREF="https://developer.android.com">Android Docs</A>"""
        )
        assertEquals(1, imported)
    }
}

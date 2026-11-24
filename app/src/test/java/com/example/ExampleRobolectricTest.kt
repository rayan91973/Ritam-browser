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
            readingListDao = db.readingListDao(),
            userscriptDao = db.userscriptDao(),
            watchlistDao = db.watchlistDao(),
            highlightDao = db.highlightDao(),
            rssDao = db.rssDao()
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

    @Test
    fun rssFeedParserHandlesRssAndAtomXml() {
        val rssXml = """
            <rss version="2.0">
              <channel>
                <title>Ritam Journal</title>
                <link>https://ritam.example</link>
                <item>
                  <title>Calm Typography</title>
                  <link>https://ritam.example/calm-typography</link>
                  <description><![CDATA[<p>Notes on serene reading.</p>]]></description>
                </item>
              </channel>
            </rss>
        """.trimIndent()
        val parsed = com.example.services.RssFeedHelper.parseXml(rssXml, "https://ritam.example/feed.xml")
        assertEquals("Ritam Journal", parsed.title)
        assertEquals(1, parsed.articles.size)
        assertEquals("Calm Typography", parsed.articles.first().title)
        assertTrue(parsed.articles.first().summary.contains("Notes on serene reading."))

        val atomXml = """
            <feed xmlns="http://www.w3.org/2005/Atom">
              <title>Engineering Feed</title>
              <link href="https://eng.example"/>
              <entry>
                <title>WebView Isolation</title>
                <link href="https://eng.example/isolation"/>
                <summary>Ephemeral origins.</summary>
              </entry>
            </feed>
        """.trimIndent()
        val atomParsed = com.example.services.RssFeedHelper.parseXml(atomXml, "https://eng.example/atom.xml")
        assertEquals("Engineering Feed", atomParsed.title)
        assertEquals(1, atomParsed.articles.size)
        assertEquals("https://eng.example/isolation", atomParsed.articles.first().link)
    }

    @Test
    fun structuredViewerFormatsJsonAndFixationStrings() {
        val pretty = com.example.services.StructuredViewerHelper.formatPrettyJson("""{"app":"Ritam","features":19}""")
        assertTrue(pretty != null && pretty.contains("\n") && pretty.contains("\"Ritam\""))
        assertEquals(
            com.example.services.StructuredDocType.JSON,
            com.example.services.StructuredViewerHelper.detectDocType("https://api.example/data.json", "{}")
        )
        assertEquals(
            com.example.services.StructuredDocType.MARKDOWN,
            com.example.services.StructuredViewerHelper.detectDocType("https://docs.example/README.md", "# Title")
        )
        val fixation = com.example.services.StructuredViewerHelper.buildFixationAnnotatedString("Contemplative reading")
        assertEquals("Contemplative reading", fixation.text)
        assertTrue(fixation.spanStyles.isNotEmpty())
    }

    @Test
    fun configBackupJsonExportsAndImportsCleanly() = runBlocking {
        repository.addCustomFilterRule("||ads.example.com^")
        repository.addUserscript("Dark Focus", "example.org", "body { opacity: 0.95; }", isCss = true)
        repository.addBookmark("https://arxiv.org", "arXiv", 1L, true)
        val json = repository.exportConfigBackupJson()
        assertTrue(json.contains("||ads.example.com^"))
        assertTrue(json.contains("Dark Focus"))

        val imported = repository.importConfigBackupJson(json)
        assertTrue(imported >= 3)
    }

    @Test
    fun homographGuardDetectsPunycodeUserinfoAndCleanUrls() {
        val clean = com.example.privacy.HomographAndPhishingGuard.inspectUrl("https://kotlinlang.org/docs/home.html", "tab1")
        assertEquals(null, clean)

        val userInfoPhish = com.example.privacy.HomographAndPhishingGuard.inspectUrl("https://paypal.com@evil-login.example/signin", "tab1")
        assertTrue(userInfoPhish != null)
        assertTrue(userInfoPhish!!.displayHost.contains("@"))

        val punycode = com.example.privacy.HomographAndPhishingGuard.inspectUrl("https://xn--pple-43d.com/account", "tab1")
        assertTrue(punycode != null)
        assertTrue(punycode!!.asciiHost.contains("xn--"))

        val leetTld = com.example.privacy.HomographAndPhishingGuard.inspectUrl("https://g00gle-verify.tk/auth", "tab1")
        assertTrue(leetTld != null)
    }

    @Test
    fun redirectUnwrapperStripsFacebookGoogleRedditAndNestedBounceLinks() {
        val fb = com.example.privacy.UrlCleaner.unwrapRedirectUrl(
            "https://l.facebook.com/l.php?u=https%3A%2F%2Fen.wikipedia.org%2Fwiki%2FKotlin"
        )
        assertTrue(fb.wasUnwrapped)
        assertEquals("https://en.wikipedia.org/wiki/Kotlin", fb.unwrappedUrl)
        assertEquals("l.facebook.com", fb.wrapperHost)

        val google = com.example.privacy.UrlCleaner.unwrapRedirectUrl(
            "https://www.google.com/url?q=https%3A%2F%2Fexample.org%2Farticle"
        )
        assertTrue(google.wasUnwrapped)
        assertEquals("https://example.org/article", google.unwrappedUrl)

        val nested = com.example.privacy.UrlCleaner.unwrapRedirectUrl(
            "https://out.reddit.com/t3_abc?url=https%3A%2F%2Fl.facebook.com%2Fl.php%3Fu%3Dhttps%253A%252F%252Frust-lang.org%252F"
        )
        assertTrue(nested.wasUnwrapped)
        assertEquals("https://rust-lang.org/", nested.unwrappedUrl)
    }

    @Test
    fun encryptedBackupRoundTripEncryptsAndRestoresVaultAndRules() = runBlocking {
        repository.addVaultEntry(
            label = "GitHub",
            domain = "github.com",
            username = "dev@ritam.app",
            secretValue = "S3cur3-T0ken-99!",
            category = "API Key"
        )
        repository.addCustomFilterRule("||beacon.example.org^")

        val encrypted = repository.exportEncryptedVaultAndConfigJson("ritam-pass-2026")
        assertTrue(com.example.services.CryptoVaultHelper.isEncryptedBackupEnvelope(encrypted))
        assertFalse(encrypted.contains("S3cur3-T0ken-99!"))

        var wrongPassFailed = false
        try {
            com.example.services.CryptoVaultHelper.decryptBackupWithPassphrase(encrypted, "wrong-pass")
        } catch (_: Exception) {
            wrongPassFailed = true
        }
        assertTrue(wrongPassFailed)

        val restoredCount = repository.importEncryptedBackup(encrypted, "ritam-pass-2026")
        assertTrue(restoredCount >= 2)
        val vaultAfter = db.vaultDao().getAllOnce()
        assertTrue(vaultAfter.any { it.label == "GitHub" })
    }

    @Test
    fun websiteSettingsPersistsGranularSensorPermissions() = runBlocking {
        repository.upsertWebsiteSettings(
            com.example.data.local.WebsiteSettingsEntity(
                domain = "meet.example.org",
                shieldsEnabled = true,
                javascriptEnabled = true,
                httpsOnly = true,
                cameraPermission = 1,
                micPermission = -1,
                locationPermission = 0
            )
        )
        val loaded = repository.getWebsiteSettings("meet.example.org")
        assertTrue(loaded != null)
        assertEquals(1, loaded!!.cameraPermission)
        assertEquals(-1, loaded.micPermission)
        assertEquals(0, loaded.locationPermission)
    }
}

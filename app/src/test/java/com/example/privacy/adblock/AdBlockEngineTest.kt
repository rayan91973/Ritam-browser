package com.example.privacy.adblock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdBlockEngineTest {

    private fun engine(text: String, category: AdCategory = AdCategory.AD): AdBlockEngine =
        AdBlockEngine.build(listOf(AdBlockListParser.parse(text, category)))

    @Test
    fun `blocks host anchored rule and subdomains`() {
        val engine = engine("||doubleclick.net^")
        assertNotNull(engine.findBlockingRule("https://doubleclick.net/pixel", "news.example", RequestType.IMAGE))
        assertNotNull(engine.findBlockingRule("https://ad.doubleclick.net/pixel", "news.example", RequestType.IMAGE))
        assertNull(engine.findBlockingRule("https://example.com/app.js", "example.com", RequestType.SCRIPT))
    }

    @Test
    fun `does not block lookalike host that only shares a prefix`() {
        val engine = engine("||ads.example.com^")
        assertNull(
            engine.findBlockingRule("https://ads.example.com.evil.net/x.js", "news.test", RequestType.SCRIPT)
        )
    }

    @Test
    fun `exception rule overrides block rule`() {
        val engine = engine(
            """
            ||ads.example.com^
            @@||ads.example.com/allowed.js
            """.trimIndent()
        )
        assertNull(
            engine.findBlockingRule("https://ads.example.com/allowed.js", "news.test", RequestType.SCRIPT)
        )
        assertNotNull(
            engine.findBlockingRule("https://ads.example.com/other.js", "news.test", RequestType.SCRIPT)
        )
    }

    @Test
    fun `third party option is honoured`() {
        val engine = engine("||cdn.example.com^${'$'}third-party")
        assertNotNull(engine.findBlockingRule("https://cdn.example.com/a.js", "other.test", RequestType.SCRIPT))
        assertNull(engine.findBlockingRule("https://cdn.example.com/a.js", "example.com", RequestType.SCRIPT))
    }

    @Test
    fun `domain option is honoured`() {
        val engine = engine("/ad-frame.js${'$'}domain=only.test|~skip.only.test")
        assertNotNull(engine.findBlockingRule("https://cdn.test/ad-frame.js", "only.test", RequestType.SCRIPT))
        assertNull(engine.findBlockingRule("https://cdn.test/ad-frame.js", "skip.only.test", RequestType.SCRIPT))
        assertNull(engine.findBlockingRule("https://cdn.test/ad-frame.js", "another.test", RequestType.SCRIPT))
    }

    @Test
    fun `resource type option is honoured`() {
        val engine = engine("||tracker.test^${'$'}image")
        assertNotNull(engine.findBlockingRule("https://tracker.test/p.gif", "site.test", RequestType.IMAGE))
        assertNull(engine.findBlockingRule("https://tracker.test/p.gif", "site.test", RequestType.SCRIPT))
    }

    @Test
    fun `popup rules never block ordinary resources`() {
        val engine = engine("||example.com^${'$'}popup")
        assertNull(engine.findBlockingRule("https://example.com/app.js", "example.com", RequestType.SCRIPT))
    }

    @Test
    fun `path pattern and wildcard are matched`() {
        val engine = engine("/ads/banner*.gif")
        assertNotNull(
            engine.findBlockingRule("https://cdn.test/ads/banner_300x250.gif", "cdn.test", RequestType.IMAGE)
        )
        assertNull(engine.findBlockingRule("https://cdn.test/img/picture.gif", "cdn.test", RequestType.IMAGE))
    }

    @Test
    fun `separator matches end of url`() {
        val engine = engine("||ads.test/campaign^")
        assertNotNull(engine.findBlockingRule("https://ads.test/campaign", "site.test", RequestType.OTHER))
        assertNotNull(engine.findBlockingRule("https://ads.test/campaign/x", "site.test", RequestType.OTHER))
    }

    @Test
    fun `page whitelist disables blocking for the site`() {
        val engine = engine(
            """
            ||ads.test^
            @@||friendly.test^${'$'}document
            """.trimIndent()
        )
        assertNull(engine.findBlockingRule("https://ads.test/x.js", "friendly.test", RequestType.SCRIPT))
        assertNotNull(engine.findBlockingRule("https://ads.test/x.js", "other.test", RequestType.SCRIPT))
    }

    @Test
    fun `cosmetic rules are scoped by domain and generic cap`() {
        val engine = engine(
            """
            ##.generic-ad
            example.com##.site-ad
            example.com,~news.example.com##.news-only-ad
            example.com##.ad-with-exception
            example.com#@#.ad-with-exception
            """.trimIndent()
        )
        val exampleCss = engine.cosmeticCss("example.com")
        assertTrue(exampleCss.contains(".generic-ad"))
        assertTrue(exampleCss.contains(".site-ad"))
        assertTrue(!exampleCss.contains(".ad-with-exception"))

        val subCss = engine.cosmeticCss("news.example.com")
        assertTrue(subCss.contains(".generic-ad"))
        assertTrue(!subCss.contains(".news-only-ad"))

        assertTrue(engine.cosmeticCss("unrelated.test").contains(".generic-ad"))
    }

    @Test
    fun `tracker list rules keep their category`() {
        val engine = engine("||analytics.test^", AdCategory.TRACKER)
        val match = engine.findBlockingRule("https://analytics.test/collect", "site.test", RequestType.PING)
        assertEquals(AdCategory.TRACKER, match?.category)
    }

    @Test
    fun `comments and empty lines are ignored`() {
        val engine = engine(
            """
            [Adblock Plus 2.0]
            ! Title: Test
            ! comment

            ||ads.test^
            """.trimIndent()
        )
        assertNotNull(engine.findBlockingRule("https://ads.test/x", "site.test", RequestType.OTHER))
    }

    @Test
    fun `regex rules are supported`() {
        val engine = engine("/adserver\\d+\\.test/")
        assertNotNull(
            engine.findBlockingRule("https://adserver42.test/pixel.js", "site.test", RequestType.SCRIPT)
        )
    }
}

package com.example.privacy.adblock

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Sanity checks against the filter lists actually bundled in the APK. These guard the bucketed
 * pattern index: a lookup bug would silently let real ads through, which unit fixtures cannot see.
 */
class RealListAdBlockTest {

    private fun parseAll(): AdBlockEngine {
        val dir = File("src/main/assets/filters")
        assertTrue("bundled filter lists missing at ${dir.absolutePath}", dir.isDirectory)
        val lists = listOf(
            "easylist.txt" to AdCategory.AD,
            "easyprivacy.txt" to AdCategory.TRACKER,
            "peter_lowe.txt" to AdCategory.AD
        ).map { (name, category) ->
            val file = File(dir, name)
            assertTrue("$name missing", file.isFile)
            AdBlockListParser.parse(file.readText(), category)
        }
        return AdBlockEngine.build(lists)
    }

    @Test
    fun `real lists block known ad and tracker hosts`() {
        val engine = parseAll()
        assertTrue("expected a large compiled rule set", engine.networkRuleCount > 50_000)

        val blocked = listOf(
            "https://securepubads.g.doubleclick.net/gpt/pubads_impl.js",
            "https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js",
            "https://static.criteo.net/js/ld/publishertag.js",
            "https://cdn.taboola.com/libtrc/loader.js",
            "https://www.google-analytics.com/analytics.js",
            "https://www.googletagmanager.com/gtag/js?id=G-TEST",
            "https://sb.scorecardresearch.com/beacon.js",
            "https://connect.facebook.net/en_US/fbevents.js"
        )
        for (url in blocked) {
            val page = "https://news.example.com"
            assertNotNull("should block $url", engine.findBlockingRule(url, page, RequestType.SCRIPT))
        }
    }

    @Test
    fun `real lists keep ordinary content working`() {
        val engine = parseAll()
        val allowed = listOf(
            "https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.js",
            "https://upload.wikimedia.org/wikipedia/commons/a/a9/Example.jpg",
            "https://fonts.gstatic.com/s/inter/v13/UcCO3FwrK3iLTeHuS_fvQtMwCp50KnMw2boKoduKmMEVuLyfAZ9hjp-Ek-_EeA.woff2",
            "https://api.github.com/repos/rayan91973/Ritam-browser",
            "https://news.example.com/assets/app.js"
        )
        for (url in allowed) {
            assertNull("should not block $url", engine.findBlockingRule(url, "news.example.com", RequestType.SCRIPT))
        }
    }

    @Test
    fun `real lists produce cosmetic rules for a news site`() {
        val engine = parseAll()
        assertTrue("expected cosmetic rules", engine.cosmeticRuleCount > 10_000)
        val css = engine.cosmeticCss("www.forbes.com")
        assertTrue("expected element hiding CSS for forbes.com", css.isNotBlank())
        assertTrue("generic selectors should be included", css.length > 200)
        assertTrue("CSS must hide elements", css.contains("display: none"))
    }
}

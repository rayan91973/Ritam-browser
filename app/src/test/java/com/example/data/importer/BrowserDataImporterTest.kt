package com.example.data.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BrowserDataImporterTest {

    @Test
    fun `netscape html keeps folders and unescapes titles`() {
        val html = """
            <!DOCTYPE NETSCAPE-Bookmark-file-1>
            <DL><p>
                <DT><H3 ADD_DATE="1700000000">Dev</H3>
                <DL><p>
                    <DT><A HREF="https://kotlinlang.org" ADD_DATE="1700000001">Kotlin &amp; Docs</A>
                    <DT><H3>Android</H3>
                    <DL><p>
                        <DT><A HREF="https://developer.android.com">Android&nbsp;Developers</A>
                    </DL><p>
                </DL><p>
                <DT><A HREF="https://example.com">Example</A>
            </DL><p>
        """.trimIndent()

        val result = BrowserDataImporter.parse(html)

        assertEquals("browser bookmarks (HTML)", result.sourceLabel)
        assertEquals(3, result.bookmarks.size)
        val kotlin = result.bookmarks.first { it.url == "https://kotlinlang.org" }
        assertEquals("Kotlin & Docs", kotlin.title)
        assertEquals("Dev", kotlin.folder)
        val android = result.bookmarks.first { it.url == "https://developer.android.com" }
        assertEquals("Dev/Android", android.folder)
        assertEquals("Android Developers", android.title)
    }

    @Test
    fun `html duplicates and non web links are skipped`() {
        val html = """
            <DL><p>
                <DT><A HREF="https://example.com">Example</A>
                <DT><A HREF="https://example.com/">Example again</A>
                <DT><A HREF="javascript:alert(1)">Bad</A>
                <DT><A HREF="place:sort=8">Places</A>
            </DL><p>
        """.trimIndent()

        val result = BrowserDataImporter.parse(html)

        assertEquals(1, result.bookmarks.size)
        assertEquals(3, result.skippedEntries)
    }

    @Test
    fun `chrome bookmarks json is flattened with folder names`() {
        val json = """
            {
              "roots": {
                "bookmark_bar": {
                  "type": "folder",
                  "name": "Bookmarks bar",
                  "children": [
                    { "type": "url", "name": "Kotlin", "url": "https://kotlinlang.org/" },
                    {
                      "type": "folder",
                      "name": "Reading",
                      "children": [
                        { "type": "url", "name": "Docs", "url": "https://docs.example.com/" }
                      ]
                    }
                  ]
                },
                "other": {
                  "type": "folder",
                  "name": "Other bookmarks",
                  "children": []
                }
              }
            }
        """.trimIndent()

        val result = BrowserDataImporter.parse(json)

        assertTrue(result.sourceLabel.contains("Chrome"))
        assertEquals(2, result.bookmarks.size)
        assertEquals("Bookmarks bar", result.bookmarks.first { it.url.contains("kotlinlang") }.folder)
        assertEquals("Bookmarks bar/Reading", result.bookmarks.first { it.url.contains("docs.") }.folder)
    }

    @Test
    fun `firefox backup uses place leaves`() {
        val json = """
            {
              "type": "text/x-moz-place-container",
              "title": "",
              "children": [
                {
                  "type": "text/x-moz-place-container",
                  "title": "menu",
                  "children": [
                    {
                      "type": "text/x-moz-place-container",
                      "title": "Bookmarks Toolbar",
                      "children": [
                        { "type": "text/x-moz-place", "title": "Mozilla", "uri": "https://mozilla.org/" }
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = BrowserDataImporter.parse(json)

        assertEquals("Firefox", result.sourceLabel)
        assertEquals(1, result.bookmarks.size)
        assertEquals("Bookmarks Toolbar", result.bookmarks.single().folder)
    }

    @Test
    fun `plain text urls are imported as bookmarks`() {
        val text = """
            Some notes
            https://one.example/page.
            https://two.example
            https://one.example/page
        """.trimIndent()

        val result = BrowserDataImporter.parse(text)

        assertEquals("URL list", result.sourceLabel)
        assertEquals(2, result.bookmarks.size)
        assertEquals("one.example", result.bookmarks.first().title)
        assertEquals("one.example", result.bookmarks.first().url.substringAfter("://").substringBefore('/'))
    }

    @Test
    fun `summary message reports folders and duplicates`() {
        val summary = BrowserImportSummary("Chrome", 4, 2, 1)
        assertEquals(
            "Imported 4 bookmarks from Chrome into 1 new folder • skipped 2 duplicate/invalid entries",
            summary.toMessage()
        )
    }
}

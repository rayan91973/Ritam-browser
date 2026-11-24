package com.example

import com.example.data.local.DownloadCategory
import com.example.privacy.DnsProvider
import com.example.services.QrCodeGenerator
import com.example.services.TranslationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun downloadCategoryClassification_worksForCommonFiles() {
        assertEquals(DownloadCategory.DOCUMENTS, DownloadCategory.classify("application/pdf", "Ritam-Whitepaper-v2.pdf"))
        assertEquals(DownloadCategory.MEDIA, DownloadCategory.classify("audio/flac", "ambient-forest-audio.flac"))
        assertEquals(DownloadCategory.ARCHIVES, DownloadCategory.classify("application/zip", "webgpu-compute-pipeline.zip"))
        assertEquals(DownloadCategory.IMAGES, DownloadCategory.classify("image/svg+xml", "figma-backup-tokens.svg"))
    }

    @Test
    fun qrCodeGenerator_buildsValidFinderAndSquareMatrix() {
        val matrix = QrCodeGenerator.generateMatrix("https://wikipedia.org/wiki/Ritam")
        assertTrue(matrix.isNotEmpty())
        assertEquals(matrix.size, matrix[0].size)
        assertTrue(matrix.size >= 21)
        // Top-left finder pattern center (2,2) must be dark module
        assertTrue(matrix[2][2])
    }

    @Test
    fun dohProviders_exposeValidHttpsEndpoints() {
        assertTrue(DnsProvider.CLOUDFLARE.dohJsonEndpoint.startsWith("https://"))
        assertTrue(DnsProvider.QUAD9.dohJsonEndpoint.startsWith("https://"))
        assertTrue(DnsProvider.ADGUARD.dohJsonEndpoint.startsWith("https://"))
        assertTrue(DnsProvider.GOOGLE.dohJsonEndpoint.startsWith("https://"))
        assertEquals("", DnsProvider.SYSTEM.dohJsonEndpoint)
    }

    @Test
    fun translationHelper_resolvesSupportedLanguageLabels() {
        assertEquals("English", TranslationHelper.labelForCode("en"))
        assertEquals("Japanese", TranslationHelper.labelForCode("ja"))
        assertEquals("Hindi", TranslationHelper.labelForCode("hi"))
        assertTrue(TranslationHelper.supportedLanguages.size >= 8)
    }

    @Test
    fun passwordGenerator_createsStrongPasswordsAndAuditsVault() {
        val pw = com.example.services.PasswordGenerator.generateStrongPassword(length = 20)
        assertEquals(20, pw.length)
        assertEquals(
            com.example.services.PasswordStrength.FORTRESS,
            com.example.services.PasswordGenerator.evaluateStrength(pw)
        )
        val phrase = com.example.services.PasswordGenerator.generatePassphrase(wordCount = 4)
        assertTrue(phrase.split("-").size >= 5)

        val entries = listOf(
            com.example.data.local.VaultEntryEntity(id = 1, label = "A", domain = "a.com", username = "u", secretValue = "1234", category = "Login"),
            com.example.data.local.VaultEntryEntity(id = 2, label = "B", domain = "b.com", username = "u", secretValue = pw, category = "Login"),
            com.example.data.local.VaultEntryEntity(id = 3, label = "C", domain = "c.com", username = "u", secretValue = pw, category = "Login")
        )
        val audit = com.example.services.PasswordGenerator.auditVault(entries)
        assertEquals(3, audit.totalCount)
        assertEquals(1, audit.weakCount)
        assertEquals(2, audit.reusedCount)
    }

    @Test
    fun watchlistSha256_producesStableHexDigest() {
        val h1 = com.example.services.WatchlistAndSyncHelper.sha256Hex("ritam")
        val h2 = com.example.services.WatchlistAndSyncHelper.sha256Hex("ritam")
        assertEquals(64, h1.length)
        assertEquals(h1, h2)
    }

    @Test
    fun tabPrivacyReport_computesAccurateGrades() {
        val activeProbeReport = com.example.browser.TabPrivacyReport(
            blockedHostCounts = mapOf("tracker.example" to 5),
            apiProbesBlocked = mapOf("sendBeacon" to 2)
        )
        assertEquals("A+", activeProbeReport.computeGrade(sslSecure = true, shieldsEnabled = true, strictLockdownsActive = 5))
        assertEquals("C", activeProbeReport.computeGrade(sslSecure = true, shieldsEnabled = false, strictLockdownsActive = 0))
        assertEquals("D", activeProbeReport.computeGrade(sslSecure = false, shieldsEnabled = false, strictLockdownsActive = 0))

        val cleanReport = com.example.browser.TabPrivacyReport()
        assertEquals("B", cleanReport.computeGrade(sslSecure = true, shieldsEnabled = false, strictLockdownsActive = 0))
    }

    @Test
    fun forceWebDarkScript_generatesSemanticDarkAndRestoreScripts() {
        val enableScript = com.example.privacy.RitamShieldsEngine.buildForceWebDarkScript(true)
        val disableScript = com.example.privacy.RitamShieldsEngine.buildForceWebDarkScript(false)
        assertTrue(enableScript.contains("ritam-force-dark-style"))
        assertTrue(enableScript.contains("isNativeDark"))
        assertTrue(enableScript.contains("darkenGradient"))
        assertTrue(disableScript.contains("data-ritam-orig-bgc"))
        assertTrue(disableScript.contains("data-ritam-orig-bgi"))
        assertTrue(disableScript.contains("removeAttribute('data-ritam-dark-mode')"))
    }

    @Test
    fun siteShortcutPlanner_fixesLegacyIconKeysAndRanksTopSitesFirst() {
        val pinned = listOf(
            com.example.data.local.BookmarkEntity(id = 1, title = "YouTube", url = "https://youtube.com", iconKey = "smart_display", isPinnedHome = true),
            com.example.data.local.BookmarkEntity(id = 2, title = "GitHub", url = "https://github.com", iconKey = "code", isPinnedHome = true),
            com.example.data.local.BookmarkEntity(id = 3, title = "DuckDuckGo", url = "https://duckduckgo.com", iconKey = "travel_explore", isPinnedHome = true),
            com.example.data.local.BookmarkEntity(id = 4, title = "Reddit", url = "https://reddit.com", iconKey = "forum", isPinnedHome = true)
        )
        val topUsage = listOf(
            com.example.data.local.SiteUsageEntity(domain = "github.com", title = "GitHub", url = "https://github.com", visitCount = 12, lastVisitedAt = 200L),
            com.example.data.local.SiteUsageEntity(domain = "arxiv.org", title = "arXiv Papers", url = "https://arxiv.org", visitCount = 5, lastVisitedAt = 150L)
        )

        val shortcuts = com.example.browser.SiteShortcutPlanner.mergePinnedAndTopSites(pinned, topUsage, maxSlots = 7)
        assertEquals(5, shortcuts.size)
        // Highest-visited sites come first
        assertEquals("github.com", shortcuts[0].domain)
        assertEquals("GH", shortcuts[0].iconKey)
        assertEquals("arxiv.org", shortcuts[1].domain)
        assertEquals("A", shortcuts[1].iconKey)

        // Legacy icon keys fixed to proper badges (never "sm", "co", "tr", "fo")
        val yt = shortcuts.first { it.domain == "youtube.com" }
        val dd = shortcuts.first { it.domain == "duckduckgo.com" }
        val rd = shortcuts.first { it.domain == "reddit.com" }
        assertEquals("YT", yt.iconKey)
        assertEquals("DD", dd.iconKey)
        assertEquals("RD", rd.iconKey)
    }

    @Test
    fun quickControlUsageSorter_placesMostUsedControlsFirst() {
        val original = listOf("find", "translate", "downloads", "devtools", "open_file")
        val usageMap = mapOf(
            "devtools" to com.example.data.local.QuickControlUsageEntity("devtools", usageCount = 9, lastUsedAt = 100L),
            "open_file" to com.example.data.local.QuickControlUsageEntity("open_file", usageCount = 4, lastUsedAt = 200L),
            "find" to com.example.data.local.QuickControlUsageEntity("find", usageCount = 9, lastUsedAt = 300L)
        )
        val sorted = com.example.browser.QuickControlUsageSorter.sortByUsage(original, { it }, usageMap)
        assertEquals(listOf("find", "devtools", "open_file", "translate", "downloads"), sorted)
    }

    @Test
    fun universalViewerHelper_classifiesAndParsesFormats() {
        assertEquals(com.example.services.UniversalFileType.IMAGE, com.example.services.UniversalViewerHelper.classifyType("photo.webp"))
        assertEquals(com.example.services.UniversalFileType.VIDEO, com.example.services.UniversalViewerHelper.classifyType("clip.mp4"))
        assertEquals(com.example.services.UniversalFileType.AUDIO, com.example.services.UniversalViewerHelper.classifyType("track.mp3"))
        assertEquals(com.example.services.UniversalFileType.PDF, com.example.services.UniversalViewerHelper.classifyType("spec.pdf"))
        assertEquals(com.example.services.UniversalFileType.WORD, com.example.services.UniversalViewerHelper.classifyType("report.docx"))
        assertEquals(com.example.services.UniversalFileType.PPT, com.example.services.UniversalViewerHelper.classifyType("deck.pptx"))
        assertEquals(com.example.services.UniversalFileType.EXCEL, com.example.services.UniversalViewerHelper.classifyType("budget.xlsx"))
        assertEquals(com.example.services.UniversalFileType.MARKDOWN, com.example.services.UniversalViewerHelper.classifyType("README.md"))

        // CSV quoted cell parsing
        val sheet = com.example.services.UniversalViewerHelper.parseDelimitedText(
            "Name,\"Role, Dept\",Score\n\"Ada, Lovelace\",Eng,99",
            delimiter = ','
        )
        assertEquals(2, sheet.rows.size)
        assertEquals("Role, Dept", sheet.rows[0][1])
        assertEquals("Ada, Lovelace", sheet.rows[1][0])

        // Markdown parsing
        val mdBlocks = com.example.services.UniversalViewerHelper.parseMarkdown("# Title\n> Quote line\n- Item A\n```kt\nprintln(1)\n```")
        assertTrue(mdBlocks.any { it is com.example.services.MarkdownBlock.Heading })
        assertTrue(mdBlocks.any { it is com.example.services.MarkdownBlock.Quote })
        assertTrue(mdBlocks.any { it is com.example.services.MarkdownBlock.Bullet })
        assertTrue(mdBlocks.any { it is com.example.services.MarkdownBlock.CodeBlock })
    }

    @Test
    fun universalViewerHelper_parsesInMemoryDocxPptxXlsxArchives() {
        fun zipBytes(vararg entries: Pair<String, String>): ByteArray {
            val baos = java.io.ByteArrayOutputStream()
            java.util.zip.ZipOutputStream(baos).use { zos ->
                entries.forEach { (name, xml) ->
                    zos.putNextEntry(java.util.zip.ZipEntry(name))
                    zos.write(xml.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }
            }
            return baos.toByteArray()
        }

        val docxBytes = zipBytes(
            "word/document.xml" to """
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:body>
                    <w:p><w:pPr><w:pStyle w:val="Heading1"/></w:pPr><w:r><w:t>Project Spec</w:t></w:r></w:p>
                    <w:p><w:r><w:t>Offline Word parser works cleanly.</w:t></w:r></w:p>
                  </w:body>
                </w:document>
            """.trimIndent()
        )
        val wordBlocks = com.example.services.UniversalViewerHelper.parseDocxBytes(docxBytes)
        assertTrue(wordBlocks.any { it.kind == com.example.services.WordBlockKind.HEADING1 && it.text == "Project Spec" })
        assertTrue(wordBlocks.any { it.text.contains("Offline Word parser works cleanly.") })

        val pptxBytes = zipBytes(
            "ppt/slides/slide1.xml" to """
                <p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"
                       xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
                  <p:cSld><p:spTree>
                    <p:sp><p:txBody><a:p><a:r><a:t>Quarter Review</a:t></a:r></a:p><a:p><a:r><a:t>Revenue grew 24%</a:t></a:r></a:p></p:txBody></p:sp>
                  </p:spTree></p:cSld>
                </p:sld>
            """.trimIndent()
        )
        val slides = com.example.services.UniversalViewerHelper.parsePptxBytes(pptxBytes)
        assertEquals(1, slides.size)
        assertEquals("Quarter Review", slides[0].title)
        assertTrue(slides[0].bullets.contains("Revenue grew 24%"))

        val xlsxBytes = zipBytes(
            "xl/workbook.xml" to """<workbook><sheets><sheet name="Summary"/></sheets></workbook>""",
            "xl/sharedStrings.xml" to """<sst><si><t>Metric</t></si><si><t>42</t></si></sst>""",
            "xl/worksheets/sheet1.xml" to """<worksheet><sheetData><row><c r="A1" t="s"><v>0</v></c><c r="B1" t="s"><v>1</v></c></row></sheetData></worksheet>"""
        )
        val sheets = com.example.services.UniversalViewerHelper.parseXlsxBytes(xlsxBytes)
        assertEquals(1, sheets.size)
        assertEquals("Summary", sheets[0].name)
        assertEquals(listOf("Metric", "42"), sheets[0].rows[0])
    }

    @Test
    fun webViewCacheMode_resolvesProperOfflineAndPrivateModes() {
        assertEquals(android.webkit.WebSettings.LOAD_NO_CACHE, com.example.browser.WebViewManager.resolveCacheMode(isPrivate = true, isOnline = true))
        assertEquals(android.webkit.WebSettings.LOAD_DEFAULT, com.example.browser.WebViewManager.resolveCacheMode(isPrivate = false, isOnline = true))
        assertEquals(android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK, com.example.browser.WebViewManager.resolveCacheMode(isPrivate = false, isOnline = false))
    }
}

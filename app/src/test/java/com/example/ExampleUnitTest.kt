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
}

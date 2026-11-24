package com.example.services

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.zip.ZipInputStream

enum class UniversalFileType(val label: String) {
    IMAGE("Image"),
    VIDEO("Video"),
    AUDIO("Audio"),
    PDF("PDF"),
    WORD("Word"),
    PPT("PowerPoint"),
    EXCEL("Excel"),
    MARKDOWN("Markdown"),
    TEXT("Text")
}

enum class WordBlockKind {
    HEADING1,
    HEADING2,
    PARAGRAPH,
    BULLET,
    TABLE
}

data class WordBlock(
    val kind: WordBlockKind,
    val text: String = "",
    val tableRows: List<List<String>> = emptyList()
)

data class PptSlide(
    val index: Int,
    val title: String,
    val bullets: List<String>,
    val notes: String = ""
)

data class ExcelSheet(
    val name: String,
    val rows: List<List<String>>,
    val maxCols: Int
)

sealed class MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data class Bullet(val text: String, val ordinalLabel: String? = null) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class Quote(val text: String) : MarkdownBlock()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock()
    data object Divider : MarkdownBlock()
}

data class UniversalViewerState(
    val title: String,
    val sourceLabel: String = "",
    val fileType: UniversalFileType,
    val localFile: File? = null,
    val sourceUrl: String = "",
    val mimeType: String = "",
    val fileSizeBytes: Long = 0L,
    val isLoading: Boolean = false,
    val error: String? = null,
    val pdfPageCount: Int = 0,
    val wordBlocks: List<WordBlock> = emptyList(),
    val pptSlides: List<PptSlide> = emptyList(),
    val excelSheets: List<ExcelSheet> = emptyList(),
    val markdownBlocks: List<MarkdownBlock> = emptyList(),
    val rawText: String = ""
)

object UniversalViewerHelper {

    private val VIEWABLE_DIRECT_EXTENSIONS = setOf(
        "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "csv", "md", "markdown"
    )

    fun isSupportedFilename(filenameOrUrl: String, mimeType: String = ""): Boolean {
        val ext = extractExtension(filenameOrUrl)
        val mime = mimeType.lowercase()
        if (mime.startsWith("image/") || mime.startsWith("video/") || mime.startsWith("audio/")) {
            return true
        }
        return ext in setOf(
            "png", "jpg", "jpeg", "webp", "gif", "bmp", "heic", "avif",
            "mp4", "mkv", "webm", "mov", "3gp", "m4v",
            "mp3", "m4a", "aac", "wav", "flac", "ogg", "opus",
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "csv", "tsv",
            "md", "markdown", "txt", "json", "xml", "log"
        ) || mime.contains("pdf") || mime.contains("word") || mime.contains("msword") ||
            mime.contains("powerpoint") || mime.contains("presentation") ||
            mime.contains("excel") || mime.contains("spreadsheet") ||
            mime.contains("markdown") || mime.startsWith("text/csv") || mime.startsWith("text/plain")
    }

    fun shouldOpenDirectlyInViewer(url: String): Boolean {
        val ext = extractExtension(url)
        return ext in VIEWABLE_DIRECT_EXTENSIONS
    }

    fun classifyType(filenameOrUrl: String, mimeType: String = ""): UniversalFileType {
        val ext = extractExtension(filenameOrUrl)
        val mime = mimeType.lowercase()
        return when {
            mime.startsWith("image/") || ext in setOf("png", "jpg", "jpeg", "webp", "gif", "bmp", "heic", "avif", "svg") ->
                UniversalFileType.IMAGE
            mime.startsWith("video/") || ext in setOf("mp4", "mkv", "webm", "mov", "3gp", "m4v", "avi") ->
                UniversalFileType.VIDEO
            mime.startsWith("audio/") || ext in setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus") ->
                UniversalFileType.AUDIO
            mime.contains("pdf") || ext == "pdf" ->
                UniversalFileType.PDF
            ext in setOf("doc", "docx", "rtf") || mime.contains("wordprocessingml") || mime.contains("msword") ->
                UniversalFileType.WORD
            ext in setOf("ppt", "pptx") || mime.contains("presentationml") || mime.contains("ms-powerpoint") ->
                UniversalFileType.PPT
            ext in setOf("xls", "xlsx", "csv", "tsv") || mime.contains("spreadsheetml") ||
                mime.contains("ms-excel") || mime.contains("csv") ->
                UniversalFileType.EXCEL
            ext in setOf("md", "markdown") || mime.contains("markdown") ->
                UniversalFileType.MARKDOWN
            else -> UniversalFileType.TEXT
        }
    }

    fun extractExtension(filenameOrUrl: String): String {
        val clean = filenameOrUrl.substringBefore("?").substringBefore("#").trim()
        val last = clean.substringAfterLast("/", clean)
        return last.substringAfterLast(".", "").lowercase()
    }

    suspend fun openLocalFile(
        context: Context,
        file: File,
        displayTitle: String = file.name,
        mimeType: String = ""
    ): UniversalViewerState = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            return@withContext UniversalViewerState(
                title = displayTitle,
                fileType = classifyType(displayTitle, mimeType),
                error = "File not found on storage."
            )
        }
        val detectedType = classifyType(file.name.ifBlank { displayTitle }, mimeType)
        buildStateFromFile(context, file, displayTitle, detectedType, sourceUrl = "", mimeType = mimeType)
    }

    suspend fun openUri(context: Context, uri: Uri): UniversalViewerState = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri).orEmpty()
            val resolvedName = queryDisplayName(context, uri)
                ?: uri.lastPathSegment?.substringAfterLast("/")?.takeIf { it.isNotBlank() }
                ?: "document"
            val extFromMime = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            val finalName = if (resolvedName.contains(".")) resolvedName
            else if (!extFromMime.isNullOrBlank()) "$resolvedName.$extFromMime"
            else resolvedName

            val cacheDir = RitamCacheManager.viewerCacheDir(context)
            val safeName = finalName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val hashedName = "${sha1Short(uri.toString())}_$safeName"
            val cachedFile = File(cacheDir, hashedName)
            resolver.openInputStream(uri)?.use { input ->
                cachedFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Unable to read selected file.")

            val fileType = classifyType(finalName, mimeType)
            buildStateFromFile(
                context = context,
                file = cachedFile,
                title = finalName,
                fileType = fileType,
                sourceUrl = uri.toString(),
                mimeType = mimeType
            )
        }.getOrElse { err ->
            UniversalViewerState(
                title = uri.lastPathSegment ?: "File",
                fileType = UniversalFileType.TEXT,
                error = err.message ?: "Unable to open selected file."
            )
        }
    }

    suspend fun openRemoteUrl(
        context: Context,
        url: String,
        mimeTypeHint: String = ""
    ): UniversalViewerState = withContext(Dispatchers.IO) {
        val rawName = Uri.parse(url).lastPathSegment?.takeIf { it.isNotBlank() } ?: "document"
        val fileType = classifyType(rawName, mimeTypeHint)
        runCatching {
            val dir = RitamCacheManager.viewerCacheDir(context)
            val safeExt = extractExtension(rawName).takeIf { it.isNotBlank() } ?: "bin"
            val safeBase = rawName.substringBeforeLast(".").replace(Regex("[^a-zA-Z0-9_-]"), "_").take(32)
            val cacheFile = File(dir, "${sha1Short(url)}_${safeBase}.$safeExt")
            if (!cacheFile.exists() || cacheFile.length() == 0L) {
                val request = Request.Builder().url(url).build()
                RitamCacheManager.sharedHttpClient(context).newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        throw IllegalStateException("HTTP ${resp.code} while loading file")
                    }
                    val body = resp.body ?: throw IllegalStateException("Empty response body")
                    cacheFile.outputStream().use { out ->
                        body.byteStream().copyTo(out)
                    }
                }
            }
            buildStateFromFile(
                context = context,
                file = cacheFile,
                title = rawName,
                fileType = fileType,
                sourceUrl = url,
                mimeType = mimeTypeHint
            )
        }.getOrElse { err ->
            UniversalViewerState(
                title = rawName,
                fileType = fileType,
                sourceUrl = url,
                error = err.message ?: "Unable to load remote file."
            )
        }
    }

    private suspend fun buildStateFromFile(
        context: Context,
        file: File,
        title: String,
        fileType: UniversalFileType,
        sourceUrl: String,
        mimeType: String
    ): UniversalViewerState {
        val sizeBytes = file.length()
        val ext = file.extension.lowercase()
        return when (fileType) {
            UniversalFileType.IMAGE,
            UniversalFileType.VIDEO,
            UniversalFileType.AUDIO -> UniversalViewerState(
                title = title,
                sourceLabel = file.name,
                fileType = fileType,
                localFile = file,
                sourceUrl = sourceUrl,
                mimeType = mimeType,
                fileSizeBytes = sizeBytes
            )

            UniversalFileType.PDF -> {
                val pages = PdfDocumentHelper.getPageCount(file)
                UniversalViewerState(
                    title = title,
                    sourceLabel = file.name,
                    fileType = UniversalFileType.PDF,
                    localFile = file,
                    sourceUrl = sourceUrl,
                    mimeType = "application/pdf",
                    fileSizeBytes = sizeBytes,
                    pdfPageCount = pages,
                    error = if (pages <= 0) "Unable to render PDF pages (file may be encrypted)." else null
                )
            }

            UniversalFileType.WORD -> {
                val blocks = runCatching {
                    val bytes = file.readBytes()
                    if (isZipSignature(bytes)) {
                        parseDocxBytes(bytes)
                    } else {
                        parseLegacyDocBytes(bytes, ext)
                    }
                }.getOrElse { emptyList() }
                val plainText = blocks.joinToString("\n\n") { block ->
                    if (block.kind == WordBlockKind.TABLE) {
                        block.tableRows.joinToString("\n") { row -> row.joinToString(" | ") }
                    } else block.text
                }
                UniversalViewerState(
                    title = title,
                    sourceLabel = file.name,
                    fileType = UniversalFileType.WORD,
                    localFile = file,
                    sourceUrl = sourceUrl,
                    mimeType = mimeType,
                    fileSizeBytes = sizeBytes,
                    wordBlocks = blocks,
                    rawText = plainText,
                    error = if (blocks.isEmpty()) "No readable document text found in file." else null
                )
            }

            UniversalFileType.PPT -> {
                val slides = runCatching {
                    val bytes = file.readBytes()
                    if (isZipSignature(bytes)) {
                        parsePptxBytes(bytes)
                    } else {
                        parseLegacyPptBytes(bytes)
                    }
                }.getOrElse { emptyList() }
                val plainText = slides.joinToString("\n\n") { s ->
                    buildString {
                        append("Slide ${s.index}: ${s.title}")
                        s.bullets.forEach { append("\n• $it") }
                        if (s.notes.isNotBlank()) append("\nNotes: ${s.notes}")
                    }
                }
                UniversalViewerState(
                    title = title,
                    sourceLabel = file.name,
                    fileType = UniversalFileType.PPT,
                    localFile = file,
                    sourceUrl = sourceUrl,
                    mimeType = mimeType,
                    fileSizeBytes = sizeBytes,
                    pptSlides = slides,
                    rawText = plainText,
                    error = if (slides.isEmpty()) "No slides could be extracted from presentation." else null
                )
            }

            UniversalFileType.EXCEL -> {
                val sheets = runCatching {
                    val bytes = file.readBytes()
                    when {
                        ext == "csv" || ext == "tsv" -> listOf(
                            parseDelimitedText(
                                bytes.toString(Charsets.UTF_8),
                                delimiter = if (ext == "tsv") '\t' else ',',
                                sheetName = file.nameWithoutExtension.ifBlank { "Sheet1" }
                            )
                        )
                        isZipSignature(bytes) -> parseXlsxBytes(bytes)
                        else -> parseLegacyXlsBytes(bytes)
                    }
                }.getOrElse { emptyList() }
                val plainText = sheets.joinToString("\n\n") { sheet ->
                    "=== ${sheet.name} ===\n" + sheet.rows.joinToString("\n") { it.joinToString("\t") }
                }
                UniversalViewerState(
                    title = title,
                    sourceLabel = file.name,
                    fileType = UniversalFileType.EXCEL,
                    localFile = file,
                    sourceUrl = sourceUrl,
                    mimeType = mimeType,
                    fileSizeBytes = sizeBytes,
                    excelSheets = sheets,
                    rawText = plainText,
                    error = if (sheets.isEmpty() || sheets.all { it.rows.isEmpty() }) {
                        "No spreadsheet rows could be extracted from file."
                    } else null
                )
            }

            UniversalFileType.MARKDOWN -> {
                val text = runCatching { file.readText(Charsets.UTF_8).take(350_000) }.getOrDefault("")
                val mdBlocks = parseMarkdown(text)
                UniversalViewerState(
                    title = title,
                    sourceLabel = file.name,
                    fileType = UniversalFileType.MARKDOWN,
                    localFile = file,
                    sourceUrl = sourceUrl,
                    mimeType = mimeType.ifBlank { "text/markdown" },
                    fileSizeBytes = sizeBytes,
                    markdownBlocks = mdBlocks,
                    rawText = text
                )
            }

            UniversalFileType.TEXT -> {
                val raw = runCatching { file.readText(Charsets.UTF_8).take(350_000) }.getOrDefault("")
                val pretty = StructuredViewerHelper.formatPrettyJson(raw) ?: raw
                UniversalViewerState(
                    title = title,
                    sourceLabel = file.name,
                    fileType = UniversalFileType.TEXT,
                    localFile = file,
                    sourceUrl = sourceUrl,
                    mimeType = mimeType.ifBlank { "text/plain" },
                    fileSizeBytes = sizeBytes,
                    rawText = pretty
                )
            }
        }
    }

    // ---------------- DOCX Parser ----------------

    fun parseDocxBytes(bytes: ByteArray): List<WordBlock> {
        val entries = readZipEntries(bytes, setOf("word/document.xml"))
        val docXml = entries["word/document.xml"] ?: return emptyList()
        val parser = newXmlParser(ByteArrayInputStream(docXml))
        val blocks = mutableListOf<WordBlock>()

        var inParagraph = false
        var inTable = false
        var inRow = false
        var inCell = false
        var paragraphStyle = ""
        var hasBullet = false
        val paraText = StringBuilder()
        val currentRow = mutableListOf<String>()
        val currentTableRows = mutableListOf<List<String>>()
        val cellText = StringBuilder()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val local = parser.name.substringAfter(":")
                    when (local) {
                        "tbl" -> {
                            inTable = true
                            currentTableRows.clear()
                        }
                        "tr" -> if (inTable) {
                            inRow = true
                            currentRow.clear()
                        }
                        "tc" -> if (inRow) {
                            inCell = true
                            cellText.setLength(0)
                        }
                        "p" -> {
                            inParagraph = true
                            paragraphStyle = ""
                            hasBullet = false
                            paraText.setLength(0)
                        }
                        "pStyle" -> if (inParagraph) {
                            val valAttr = findAttr(parser, "val")
                            if (!valAttr.isNullOrBlank()) paragraphStyle = valAttr
                        }
                        "numPr" -> if (inParagraph) {
                            hasBullet = true
                        }
                        "br" -> {
                            if (inCell) cellText.append("\n") else if (inParagraph) paraText.append("\n")
                        }
                        "tab" -> {
                            if (inParagraph && !inCell) paraText.append("    ")
                        }
                        "t" -> {
                            val txt = parser.nextText()
                            if (inCell) {
                                cellText.append(txt)
                            } else if (inParagraph) {
                                paraText.append(txt)
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val local = parser.name.substringAfter(":")
                    when (local) {
                        "p" -> {
                            if (inCell) {
                                if (cellText.isNotEmpty() && !cellText.endsWith("\n")) {
                                    cellText.append(" ")
                                }
                            } else if (inParagraph) {
                                val text = paraText.toString().trim()
                                if (text.isNotBlank()) {
                                    val kind = when {
                                        paragraphStyle.contains("Heading1", true) ||
                                            paragraphStyle.equals("Title", true) -> WordBlockKind.HEADING1
                                        paragraphStyle.contains("Heading", true) ||
                                            paragraphStyle.equals("Subtitle", true) -> WordBlockKind.HEADING2
                                        hasBullet || paragraphStyle.contains("List", true) -> WordBlockKind.BULLET
                                        else -> WordBlockKind.PARAGRAPH
                                    }
                                    blocks.add(WordBlock(kind = kind, text = text))
                                }
                            }
                            inParagraph = false
                        }
                        "tc" -> {
                            if (inRow) {
                                currentRow.add(cellText.toString().trim())
                            }
                            inCell = false
                        }
                        "tr" -> {
                            if (inTable && currentRow.isNotEmpty()) {
                                currentTableRows.add(currentRow.toList())
                            }
                            inRow = false
                        }
                        "tbl" -> {
                            if (currentTableRows.isNotEmpty()) {
                                blocks.add(
                                    WordBlock(
                                        kind = WordBlockKind.TABLE,
                                        tableRows = currentTableRows.toList()
                                    )
                                )
                            }
                            inTable = false
                        }
                    }
                }
            }
            event = parser.next()
        }
        return blocks
    }

    // ---------------- PPTX Parser ----------------

    fun parsePptxBytes(bytes: ByteArray): List<PptSlide> {
        val entries = readZipEntries(bytes) { name ->
            name.startsWith("ppt/slides/slide") && name.endsWith(".xml") ||
                name.startsWith("ppt/notesSlides/notesSlide") && name.endsWith(".xml")
        }
        val slideRegex = Regex("""ppt/slides/slide(\d+)\.xml""")
        val slideEntries = entries.entries
            .mapNotNull { (path, data) ->
                val num = slideRegex.matchEntire(path)?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
                num to data
            }
            .sortedBy { it.first }

        return slideEntries.map { (num, data) ->
            val paragraphs = extractPptXmlParagraphs(data)
            val notesBytes = entries["ppt/notesSlides/notesSlide$num.xml"]
            val notesText = notesBytes?.let { extractPptXmlParagraphs(it).filterNot { p -> p.all(Char::isDigit) }.joinToString("\n") }
                .orEmpty()
            val title = paragraphs.firstOrNull().orEmpty().ifBlank { "Slide $num" }
            val bullets = if (paragraphs.size > 1) paragraphs.drop(1) else emptyList()
            PptSlide(
                index = num,
                title = title,
                bullets = bullets,
                notes = notesText
            )
        }
    }

    private fun extractPptXmlParagraphs(xmlBytes: ByteArray): List<String> {
        val parser = newXmlParser(ByteArrayInputStream(xmlBytes))
        val paragraphs = mutableListOf<String>()
        var inPara = false
        val sb = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name.substringAfter(":")) {
                        "p" -> {
                            inPara = true
                            sb.setLength(0)
                        }
                        "t" -> if (inPara) {
                            sb.append(parser.nextText())
                        }
                        "br" -> if (inPara) sb.append("\n")
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.substringAfter(":") == "p") {
                        val line = sb.toString().trim()
                        if (line.isNotBlank()) paragraphs.add(line)
                        inPara = false
                    }
                }
            }
            event = parser.next()
        }
        return paragraphs
    }

    // ---------------- XLSX & CSV Parser ----------------

    fun parseXlsxBytes(bytes: ByteArray): List<ExcelSheet> {
        val entries = readZipEntries(bytes) { name ->
            name == "xl/sharedStrings.xml" ||
                name == "xl/workbook.xml" ||
                (name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml"))
        }
        val sharedStrings = entries["xl/sharedStrings.xml"]?.let { parseXlsxSharedStrings(it) } ?: emptyList()
        val sheetNames = entries["xl/workbook.xml"]?.let { parseXlsxSheetNames(it) } ?: emptyList()

        val sheetRegex = Regex("""xl/worksheets/sheet(\d+)\.xml""")
        val sheetFiles = entries.entries
            .mapNotNull { (path, data) ->
                val idx = sheetRegex.matchEntire(path)?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
                idx to data
            }
            .sortedBy { it.first }

        return sheetFiles.map { (idx, data) ->
            val rows = parseXlsxSheetRows(data, sharedStrings)
            val maxCols = rows.maxOfOrNull { it.size } ?: 0
            val padded = rows.map { row ->
                if (row.size < maxCols) row + List(maxCols - row.size) { "" } else row
            }
            val title = sheetNames.getOrNull(idx - 1)?.takeIf { it.isNotBlank() } ?: "Sheet $idx"
            ExcelSheet(name = title, rows = padded, maxCols = maxCols)
        }
    }

    private fun parseXlsxSharedStrings(xmlBytes: ByteArray): List<String> {
        val parser = newXmlParser(ByteArrayInputStream(xmlBytes))
        val strings = mutableListOf<String>()
        var inSi = false
        val current = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name.substringAfter(":")) {
                    "si" -> {
                        inSi = true
                        current.setLength(0)
                    }
                    "t" -> if (inSi) current.append(parser.nextText())
                }
                XmlPullParser.END_TAG -> if (parser.name.substringAfter(":") == "si") {
                    strings.add(current.toString())
                    inSi = false
                }
            }
            event = parser.next()
        }
        return strings
    }

    private fun parseXlsxSheetNames(xmlBytes: ByteArray): List<String> {
        val parser = newXmlParser(ByteArrayInputStream(xmlBytes))
        val names = mutableListOf<String>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name.substringAfter(":") == "sheet") {
                val name = findAttr(parser, "name")
                if (!name.isNullOrBlank()) names.add(name)
            }
            event = parser.next()
        }
        return names
    }

    private fun parseXlsxSheetRows(xmlBytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val parser = newXmlParser(ByteArrayInputStream(xmlBytes))
        val rows = mutableListOf<List<String>>()
        var currentRow = mutableListOf<String>()
        var inRow = false
        var cellType = ""
        var cellRef = ""
        var cellValue = ""
        var inlineValue = ""

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name.substringAfter(":")) {
                    "row" -> {
                        inRow = true
                        currentRow = mutableListOf()
                    }
                    "c" -> if (inRow) {
                        cellType = findAttr(parser, "t").orEmpty()
                        cellRef = findAttr(parser, "r").orEmpty()
                        cellValue = ""
                        inlineValue = ""
                    }
                    "v" -> if (inRow) cellValue = parser.nextText()
                    "t" -> if (inRow) inlineValue += parser.nextText()
                }
                XmlPullParser.END_TAG -> when (parser.name.substringAfter(":")) {
                    "c" -> if (inRow) {
                        val resolved = when (cellType) {
                            "s" -> cellValue.toIntOrNull()?.let { sharedStrings.getOrNull(it) } ?: cellValue
                            "inlineStr" -> inlineValue
                            "b" -> if (cellValue == "1") "TRUE" else "FALSE"
                            else -> if (cellValue.isNotEmpty()) cellValue else inlineValue
                        }.trim()
                        val colIdx = columnRefToIndex(cellRef)
                        if (colIdx >= 0) {
                            while (currentRow.size < colIdx) currentRow.add("")
                            if (colIdx < currentRow.size) currentRow[colIdx] = resolved
                            else currentRow.add(resolved)
                        } else {
                            currentRow.add(resolved)
                        }
                    }
                    "row" -> if (inRow) {
                        if (currentRow.any { it.isNotBlank() }) {
                            rows.add(currentRow)
                        }
                        inRow = false
                    }
                }
            }
            event = parser.next()
        }
        return rows.take(400)
    }

    fun parseDelimitedText(text: String, delimiter: Char = ',', sheetName: String = "Table"): ExcelSheet {
        val rows = mutableListOf<List<String>>()
        var currentRow = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                inQuotes -> {
                    if (ch == '"') {
                        if (i + 1 < text.length && text[i + 1] == '"') {
                            field.append('"')
                            i++
                        } else {
                            inQuotes = false
                        }
                    } else {
                        field.append(ch)
                    }
                }
                ch == '"' -> inQuotes = true
                ch == delimiter -> {
                    currentRow.add(field.toString().trim())
                    field.setLength(0)
                }
                ch == '\n' -> {
                    currentRow.add(field.toString().trim())
                    field.setLength(0)
                    if (currentRow.any { it.isNotEmpty() }) rows.add(currentRow)
                    currentRow = mutableListOf()
                }
                ch == '\r' -> Unit
                else -> field.append(ch)
            }
            i++
        }
        if (field.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(field.toString().trim())
            if (currentRow.any { it.isNotEmpty() }) rows.add(currentRow)
        }
        val maxCols = rows.maxOfOrNull { it.size } ?: 0
        val padded = rows.take(500).map { row ->
            if (row.size < maxCols) row + List(maxCols - row.size) { "" } else row
        }
        return ExcelSheet(name = sheetName, rows = padded, maxCols = maxCols)
    }

    // ---------------- Legacy Binary (.doc / .ppt / .xls) Fallback Extractors ----------------

    fun parseLegacyDocBytes(bytes: ByteArray, ext: String = "doc"): List<WordBlock> {
        val header = bytes.take(64).toByteArray().toString(Charsets.UTF_8)
        if (header.trimStart().startsWith("{\\rtf", ignoreCase = true)) {
            val stripped = stripRtfControlWords(bytes.toString(Charsets.UTF_8))
            return stripped.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { WordBlock(WordBlockKind.PARAGRAPH, it) }
        }
        if (header.contains("<html", true) || header.contains("<!doctype", true)) {
            val text = bytes.toString(Charsets.UTF_8)
                .replace(Regex("<(script|style)[^>]*>[\\s\\S]*?</\\1>", RegexOption.IGNORE_CASE), "")
                .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("</(p|div|li|h[1-6]|tr)>", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("<[^>]+>"), " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
            return text.lines()
                .map { it.replace(Regex("\\s+"), " ").trim() }
                .filter { it.isNotBlank() }
                .map { WordBlock(WordBlockKind.PARAGRAPH, it) }
        }
        val extractedRuns = extractReadableTextRuns(bytes, minRunLength = 8)
        return extractedRuns.mapIndexed { idx, run ->
            val kind = if (idx == 0 && run.length in 3..70) WordBlockKind.HEADING1 else WordBlockKind.PARAGRAPH
            WordBlock(kind, run)
        }
    }

    fun parseLegacyPptBytes(bytes: ByteArray): List<PptSlide> {
        // Attempt scanning raw PowerPoint binary TextCharsAtom (0x0FA0) / TextBytesAtom (0x0FA8)
        val slidesText = mutableListOf<String>()
        var i = 0
        while (i + 8 <= bytes.size) {
            val verInst = ((bytes[i + 1].toInt() and 0xFF) shl 8) or (bytes[i].toInt() and 0xFF)
            val recType = ((bytes[i + 3].toInt() and 0xFF) shl 8) or (bytes[i + 2].toInt() and 0xFF)
            val recLen = ByteBuffer.wrap(bytes, i + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
            if ((recType == 0x0FA0 || recType == 0x0FA8) && recLen in 2..65_536 && i + 8 + recLen <= bytes.size && (verInst and 0x0F) == 0) {
                val payload = bytes.copyOfRange(i + 8, i + 8 + recLen)
                val decoded = if (recType == 0x0FA0) {
                    String(payload, Charsets.UTF_16LE)
                } else {
                    String(payload, Charsets.ISO_8859_1)
                }
                val clean = decoded.replace('\r', '\n').trim()
                if (clean.length >= 2 && clean.any { it.isLetterOrDigit() }) {
                    slidesText.add(clean)
                }
                i += 8 + recLen
            } else {
                i++
            }
        }
        if (slidesText.isNotEmpty()) {
            return slidesText.chunked(3).mapIndexed { idx, chunk ->
                val title = chunk.first().lines().firstOrNull()?.take(90) ?: "Slide ${idx + 1}"
                val bullets = chunk.flatMap { it.lines() }.drop(1).map { it.trim() }.filter { it.isNotBlank() }
                PptSlide(index = idx + 1, title = title, bullets = bullets)
            }
        }
        val runs = extractReadableTextRuns(bytes, minRunLength = 6)
        return if (runs.isEmpty()) emptyList()
        else runs.chunked(5).mapIndexed { idx, chunk ->
            PptSlide(index = idx + 1, title = chunk.first(), bullets = chunk.drop(1))
        }
    }

    fun parseLegacyXlsBytes(bytes: ByteArray): List<ExcelSheet> {
        val textStart = bytes.take(128).toByteArray().toString(Charsets.UTF_8)
        if (textStart.contains("<table", true) || textStart.contains("<html", true)) {
            val rowMatches = Regex("<tr[^>]*>(.*?)</tr>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .findAll(bytes.toString(Charsets.UTF_8))
                .map { m ->
                    Regex("<t[dh][^>]*>(.*?)</t[dh]>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                        .findAll(m.groupValues[1])
                        .map { cell -> cell.groupValues[1].replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim() }
                        .toList()
                }
                .filter { it.isNotEmpty() }
                .toList()
            if (rowMatches.isNotEmpty()) {
                val maxCols = rowMatches.maxOfOrNull { it.size } ?: 0
                return listOf(ExcelSheet("Sheet 1", rowMatches.map { it + List(maxCols - it.size) { "" } }, maxCols))
            }
        }
        val runs = extractReadableTextRuns(bytes, minRunLength = 3)
        if (runs.isEmpty()) return emptyList()
        val rows = runs.chunked(4).map { chunk ->
            if (chunk.size < 4) chunk + List(4 - chunk.size) { "" } else chunk
        }
        return listOf(ExcelSheet(name = "Extracted Cells", rows = rows, maxCols = 4))
    }

    // ---------------- Markdown Parser ----------------

    fun parseMarkdown(markdown: String): List<MarkdownBlock> {
        val lines = markdown.replace("\r\n", "\n").lines()
        val blocks = mutableListOf<MarkdownBlock>()
        var i = 0
        while (i < lines.size) {
            val raw = lines[i]
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) {
                i++
                continue
            }
            if (trimmed.startsWith("```")) {
                val lang = trimmed.removePrefix("```").trim()
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                if (i < lines.size) i++
                blocks.add(MarkdownBlock.CodeBlock(language = lang, code = codeLines.joinToString("\n")))
                continue
            }
            if (trimmed.matches(Regex("^([-*_])\\1{2,}$"))) {
                blocks.add(MarkdownBlock.Divider)
                i++
                continue
            }
            val headingMatch = Regex("^(#{1,6})\\s+(.+)$").matchEntire(trimmed)
            if (headingMatch != null) {
                blocks.add(MarkdownBlock.Heading(headingMatch.groupValues[1].length, headingMatch.groupValues[2].trim()))
                i++
                continue
            }
            if (trimmed.startsWith(">")) {
                val quoteLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    quoteLines.add(lines[i].trim().removePrefix(">").trim())
                    i++
                }
                blocks.add(MarkdownBlock.Quote(quoteLines.joinToString("\n")))
                continue
            }
            // Pipe table check
            if (trimmed.startsWith("|") && i + 1 < lines.size &&
                lines[i + 1].trim().matches(Regex("^\\|?[\\s:\\-|]+\\|?$"))
            ) {
                val headers = splitMarkdownTableRow(trimmed)
                i += 2
                val rows = mutableListOf<List<String>>()
                while (i < lines.size && lines[i].trim().startsWith("|")) {
                    val row = splitMarkdownTableRow(lines[i].trim())
                    val padded = if (row.size < headers.size) row + List(headers.size - row.size) { "" } else row.take(headers.size)
                    rows.add(padded)
                    i++
                }
                blocks.add(MarkdownBlock.Table(headers, rows))
                continue
            }
            val bulletMatch = Regex("^[-*+]\\s+(.+)$").matchEntire(trimmed)
            if (bulletMatch != null) {
                blocks.add(MarkdownBlock.Bullet(bulletMatch.groupValues[1].trim(), ordinalLabel = "•"))
                i++
                continue
            }
            val orderedMatch = Regex("^(\\d+\\.)\\s+(.+)$").matchEntire(trimmed)
            if (orderedMatch != null) {
                blocks.add(MarkdownBlock.Bullet(orderedMatch.groupValues[2].trim(), ordinalLabel = orderedMatch.groupValues[1]))
                i++
                continue
            }
            // Paragraph accumulation
            val paraLines = mutableListOf(trimmed)
            i++
            while (i < lines.size) {
                val next = lines[i].trim()
                if (next.isEmpty() || next.startsWith("#") || next.startsWith("```") ||
                    next.startsWith(">") || next.matches(Regex("^([-*+]|\\d+\\.)\\s+.*"))
                ) {
                    break
                }
                paraLines.add(next)
                i++
            }
            blocks.add(MarkdownBlock.Paragraph(paraLines.joinToString(" ")))
        }
        return blocks
    }

    // ---------------- Helpers ----------------

    private fun splitMarkdownTableRow(line: String): List<String> =
        line.trim().removePrefix("|").removeSuffix("|").split("|").map { it.trim() }

    private fun stripRtfControlWords(rtf: String): String {
        return rtf
            .replace(Regex("""\\par\b"""), "\n")
            .replace(Regex("""\\line\b"""), "\n")
            .replace(Regex("""\\'([0-9a-fA-F]{2})""")) { m ->
                m.groupValues[1].toIntOrNull(16)?.toChar()?.toString().orEmpty()
            }
            .replace(Regex("""\\[a-zA-Z]+-?\d* ?"""), "")
            .replace("{", "")
            .replace("}", "")
            .trim()
    }

    private fun extractReadableTextRuns(bytes: ByteArray, minRunLength: Int): List<String> {
        val runs = LinkedHashSet<String>()
        // 1. ASCII/UTF-8 printable runs
        val asciiSb = StringBuilder()
        for (b in bytes) {
            val ch = (b.toInt() and 0xFF)
            if (ch in 32..126 || ch == 9 || ch == 10 || ch == 13) {
                asciiSb.append(ch.toChar())
            } else {
                flushTextRun(asciiSb, minRunLength, runs)
            }
        }
        flushTextRun(asciiSb, minRunLength, runs)

        // 2. UTF-16LE runs (standard Word/PPT/Excel legacy Unicode strings)
        val u16Sb = StringBuilder()
        var idx = 0
        while (idx + 1 < bytes.size) {
            val codeUnit = ((bytes[idx + 1].toInt() and 0xFF) shl 8) or (bytes[idx].toInt() and 0xFF)
            if (codeUnit in 32..126 || codeUnit == 9 || codeUnit == 10 || codeUnit == 13 || codeUnit in 0x00A0..0x2FFF) {
                u16Sb.append(codeUnit.toChar())
            } else {
                flushTextRun(u16Sb, minRunLength, runs)
            }
            idx += 2
        }
        flushTextRun(u16Sb, minRunLength, runs)

        val noiseWords = setOf(
            "Microsoft", "Word.Document", "Root Entry", "CompObj", "SummaryInformation",
            "DocumentSummaryInformation", "WordDocument", "1Table", "0Table", "Data",
            "Times New Roman", "Calibri", "Arial", "Normal.dotm"
        )
        return runs.filterNot { run ->
            noiseWords.any { run.equals(it, ignoreCase = true) } || run.count { it.isLetterOrDigit() } < 3
        }.take(250)
    }

    private fun flushTextRun(sb: StringBuilder, minLen: Int, out: MutableSet<String>) {
        if (sb.length >= minLen) {
            sb.toString().replace('\r', '\n').lines().forEach { line ->
                val trimmed = line.replace(Regex("\\s+"), " ").trim()
                if (trimmed.length >= minLen && trimmed.any { it.isLetter() }) {
                    out.add(trimmed)
                }
            }
        }
        sb.setLength(0)
    }

    private fun columnRefToIndex(ref: String): Int {
        if (ref.isBlank()) return -1
        var col = 0
        var seenLetter = false
        for (ch in ref) {
            if (ch.isLetter()) {
                seenLetter = true
                col = col * 26 + (ch.uppercaseChar() - 'A' + 1)
            } else break
        }
        return if (seenLetter) col - 1 else -1
    }

    private fun isZipSignature(bytes: ByteArray): Boolean =
        bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()

    private fun readZipEntries(
        bytes: ByteArray,
        targetNames: Set<String>
    ): Map<String, ByteArray> = readZipEntries(bytes) { it in targetNames }

    private fun readZipEntries(
        bytes: ByteArray,
        predicate: (String) -> Boolean
    ): Map<String, ByteArray> {
        val map = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && predicate(entry.name)) {
                    map[entry.name] = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return map
    }

    private fun newXmlParser(input: InputStream): XmlPullParser {
        val factory = XmlPullParserFactory.newInstance().apply {
            isNamespaceAware = true
        }
        return factory.newPullParser().apply {
            setInput(input, "UTF-8")
        }
    }

    private fun findAttr(parser: XmlPullParser, localName: String): String? {
        for (i in 0 until parser.attributeCount) {
            if (parser.getAttributeName(i).substringAfter(":") == localName) {
                return parser.getAttributeValue(i)
            }
        }
        return null
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
        }.getOrNull()
    }

    private fun sha1Short(input: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(input.toByteArray(Charsets.UTF_8))
        return digest.take(6).joinToString("") { "%02x".format(it) }
    }
}

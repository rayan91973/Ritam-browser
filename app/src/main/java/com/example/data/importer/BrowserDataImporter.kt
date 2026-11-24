package com.example.data.importer

import org.json.JSONArray
import org.json.JSONObject

data class ImportedBookmark(
    val url: String,
    val title: String,
    val folder: String? = null
)

data class BrowserImport(
    val sourceLabel: String,
    val bookmarks: List<ImportedBookmark>,
    val skippedEntries: Int
)

data class BrowserImportSummary(
    val sourceLabel: String,
    val bookmarksAdded: Int,
    val bookmarksSkipped: Int,
    val foldersCreated: Int
) {
    fun toMessage(): String = buildString {
        append("Imported ")
        append(bookmarksAdded)
        append(if (bookmarksAdded == 1) " bookmark" else " bookmarks")
        append(" from ")
        append(sourceLabel)
        if (foldersCreated > 0) append(" into $foldersCreated new folder${if (foldersCreated == 1) "" else "s"}")
        if (bookmarksSkipped > 0) append(" • skipped $bookmarksSkipped duplicate/invalid entr${if (bookmarksSkipped == 1) "y" else "ies"}")
    }
}

/**
 * Reads bookmark exports produced by other browsers, without needing access to their sandboxes.
 *
 * Supported inputs:
 *  - Netscape bookmark HTML: Chrome, Edge, Brave, Opera, Vivaldi, Safari, Firefox ("Export bookmarks")
 *  - Chrome/Chromium `Bookmarks` JSON (also Edge and other Chromium browsers)
 *  - Firefox JSON backup (`bookmarks-*.json`)
 *  - JSON arrays of `{ "url" | "uri", "title" | "name" }`
 *  - Plain text lists with one URL per line
 *
 * Folder names are preserved for HTML and JSON exports. Entries that are not http(s), and duplicate
 * URLs inside one file, are dropped and counted in [BrowserImport.skippedEntries].
 */
object BrowserDataImporter {

    private val httpUrlRegex = Regex("""(?i)\bhttps?://[^\s"'<>]+""")

    private val anchorRegex =
        Regex("""(?is)<A\s+[^>]*HREF\s*=\s*["']([^"']+)["'][^>]*>(.*?)</A\s*>""")

    private val folderRegex = Regex("""(?is)<H3[^>]*>(.*?)</H3\s*>""")

    fun parse(rawContent: String): BrowserImport {
        val trimmed = rawContent.trim().removePrefix("\uFEFF").trim()
        if (trimmed.isEmpty()) return BrowserImport("empty file", emptyList(), 0)
        return when {
            trimmed.startsWith("<") -> parseNetscapeHtml(trimmed)
            trimmed.startsWith("{") -> parseJsonObject(trimmed)
            trimmed.startsWith("[") -> parseJsonArray(trimmed)
            else -> parsePlainText(trimmed)
        }
    }

    private fun parseNetscapeHtml(html: String): BrowserImport {
        val bookmarks = LinkedHashMap<String, ImportedBookmark>()
        var skipped = 0
        val folderStack = ArrayDeque<String?>()
        var pendingFolder: String? = null

        for (rawLine in html.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            if (line.contains("<H3", ignoreCase = true)) {
                val text = folderRegex.find(line)?.groupValues?.get(1).orEmpty()
                pendingFolder = unescapeHtml(text).trim().ifBlank { null }
                continue
            }
            if (line.startsWith("<DL", ignoreCase = true) || line.contains("<DL>", ignoreCase = true)) {
                folderStack.addLast(pendingFolder)
                pendingFolder = null
                continue
            }
            if (line.startsWith("</DL", ignoreCase = true)) {
                if (folderStack.isNotEmpty()) folderStack.removeLast()
                continue
            }
            val anchor = anchorRegex.find(line)
            if (anchor != null) {
                val href = anchor.groupValues[1]
                val title = anchor.groupValues[2]
                val folder = folderStack.filterNotNull().joinToString("/").ifBlank { null }
                val added = addIfValid(bookmarks, href, title, folder)
                if (!added) skipped++
            }
        }
        return BrowserImport("browser bookmarks (HTML)", bookmarks.values.toList(), skipped)
    }

    private fun parseJsonObject(raw: String): BrowserImport {
        val root = runCatching { JSONObject(raw) }.getOrNull()
            ?: return BrowserImport("unreadable JSON", emptyList(), 0)
        val bookmarks = LinkedHashMap<String, ImportedBookmark>()
        var skipped = 0

        fun add(url: String?, title: String?, folder: String?) {
            if (url == null || !addIfValid(bookmarks, url, title.orEmpty(), folder)) skipped++
        }

        val roots = root.optJSONObject("roots")
        if (roots != null) {
            // Chrome / Chromium: {"roots":{"bookmark_bar":{...},"other":{...},"synced":{...}}}
            for (key in listOf("bookmark_bar", "other", "synced")) {
                val node = roots.optJSONObject(key) ?: continue
                val rootName = when (key) {
                    "bookmark_bar" -> "Bookmarks bar"
                    "other" -> "Other bookmarks"
                    else -> "Mobile bookmarks"
                }
                // Root nodes in Chrome exports usually carry the same name; use a fallback label
                // so an unnamed root still produces a readable folder.
                val nodeName = node.optString("name").trim()
                walkChromiumNode(
                    node,
                    folderPath = if (nodeName.isBlank()) rootName else null,
                    out = bookmarks,
                    onSkip = { skipped++ }
                )
            }
            return BrowserImport("Chrome, Edge, Brave, Opera or Vivaldi", bookmarks.values.toList(), skipped)
        }

        // Firefox backup: a places tree whose leaf type is "text/x-moz-place".
        if (root.optString("type") == "text/x-moz-place-container" || root.has("children")) {
            walkFirefoxNode(root, null, bookmarks, onSkip = { skipped++ })
            return BrowserImport("Firefox", bookmarks.values.toList(), skipped)
        }

        val array = root.optJSONArray("bookmarks")
        if (array != null) {
            for (i in 0 until array.length()) {
                val node = array.optJSONObject(i) ?: continue
                add(node.optString("url").ifBlank { node.optString("uri") },
                    node.optString("title").ifBlank { node.optString("name") }, null)
            }
            return BrowserImport("JSON bookmarks", bookmarks.values.toList(), skipped)
        }

        val singleUrl = root.optString("url").ifBlank { root.optString("uri") }
        if (singleUrl.isNotBlank()) {
            add(singleUrl, root.optString("title").ifBlank { root.optString("name") }, null)
            return BrowserImport("JSON bookmarks", bookmarks.values.toList(), skipped)
        }
        return BrowserImport("unrecognised JSON", bookmarks.values.toList(), skipped)
    }

    private fun walkChromiumNode(
        node: JSONObject,
        folderPath: String?,
        out: MutableMap<String, ImportedBookmark>,
        onSkip: () -> Unit
    ) {
        val type = node.optString("type")
        if (type == "url") {
            if (!addIfValid(out, node.optString("url"), node.optString("name"), folderPath)) onSkip()
            return
        }
        val name = node.optString("name").trim()
        val thisFolder = if (name.isNotBlank()) {
            listOfNotNull(folderPath, name).joinToString("/")
        } else {
            folderPath
        }
        val children = node.optJSONArray("children") ?: return
        for (i in 0 until children.length()) {
            val child = children.optJSONObject(i) ?: continue
            walkChromiumNode(child, thisFolder, out, onSkip)
        }
    }

    private fun walkFirefoxNode(
        node: JSONObject,
        folderPath: String?,
        out: MutableMap<String, ImportedBookmark>,
        onSkip: () -> Unit
    ) {
        val type = node.optString("type")
        if (type == "text/x-moz-place") {
            val url = node.optString("uri")
            if (!addIfValid(out, url, node.optString("title"), folderPath)) onSkip()
            return
        }
        val title = node.optString("title").trim()
        val internal = setOf("root", "menu", "toolbar", "unfiled", "mobile", "tags", "placesRoot")
        val nextFolder = if (title.isNotBlank() && title.lowercase() !in internal) {
            listOfNotNull(folderPath, title).joinToString("/")
        } else {
            folderPath
        }
        val children = node.optJSONArray("children") ?: return
        for (i in 0 until children.length()) {
            val child = children.optJSONObject(i) ?: continue
            walkFirefoxNode(child, nextFolder, out, onSkip)
        }
    }

    private fun parseJsonArray(raw: String): BrowserImport {
        val array = runCatching { JSONArray(raw) }.getOrNull()
            ?: return BrowserImport("unreadable JSON", emptyList(), 0)
        val bookmarks = LinkedHashMap<String, ImportedBookmark>()
        var skipped = 0
        for (i in 0 until array.length()) {
            val node = array.optJSONObject(i)
            if (node == null) {
                val plain = array.optString(i)
                if (plain.isNotBlank()) {
                    if (!addIfValid(bookmarks, plain, "", null)) skipped++
                }
                continue
            }
            val url = node.optString("url").ifBlank { node.optString("uri") }
            val title = node.optString("title").ifBlank { node.optString("name") }
            if (!addIfValid(bookmarks, url, title, null)) skipped++
        }
        return BrowserImport("JSON bookmarks", bookmarks.values.toList(), skipped)
    }

    private fun parsePlainText(raw: String): BrowserImport {
        val bookmarks = LinkedHashMap<String, ImportedBookmark>()
        var skipped = 0
        for (match in httpUrlRegex.findAll(raw)) {
            val url = match.value.trimEnd('.', ',', ';', ')', ']')
            if (!addIfValid(bookmarks, url, "", null)) skipped++
        }
        return BrowserImport("URL list", bookmarks.values.toList(), skipped)
    }

    private fun addIfValid(
        out: MutableMap<String, ImportedBookmark>,
        rawUrl: String,
        rawTitle: String,
        folder: String?
    ): Boolean {
        val url = rawUrl.trim()
        if (!url.startsWith("http://", ignoreCase = true) &&
            !url.startsWith("https://", ignoreCase = true)
        ) {
            return false
        }
        val key = canonicalKey(url)
        if (out.containsKey(key)) return false
        val title = unescapeHtml(rawTitle).trim().ifBlank { hostOf(url) }
        out[key] = ImportedBookmark(url = url, title = title, folder = folder?.trim()?.ifBlank { null })
        return true
    }

    /** Treats `https://a.com`, `https://a.com/` and `https://a.com/#top` as the same bookmark. */
    private fun canonicalKey(url: String): String =
        url.substringBefore('#').lowercase().trimEnd('/')

    private fun hostOf(url: String): String =
        url.substringAfter("://", url).substringBefore('/').removePrefix("www.")

    private fun unescapeHtml(value: String): String =
        value.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
}

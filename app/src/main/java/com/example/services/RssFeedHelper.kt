package com.example.services

import com.example.data.local.RssArticleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.util.concurrent.TimeUnit

data class ParsedRssFeed(
    val title: String,
    val feedUrl: String,
    val siteUrl: String,
    val articles: List<RssArticleEntity>
)

object RssFeedHelper {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun fetchAndParseFeed(feedUrl: String): Result<ParsedRssFeed> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(feedUrl)
                .header("User-Agent", "RitamBrowser/1.0 FeedReader")
                .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Feed returned HTTP ${response.code}")
                }
                val xml = response.body?.string() ?: throw IllegalStateException("Empty feed response")
                parseXml(xml, feedUrl)
            }
        }
    }

    fun parseXml(xml: String, feedUrl: String): ParsedRssFeed {
        val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml.trim()))

        var feedTitle = ""
        var siteUrl = ""
        var currentTag = ""
        var insideItem = false
        var itemTitle = ""
        var itemLink = ""
        var itemSummary = ""
        var itemDate = ""
        val articles = mutableListOf<RssArticleEntity>()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name.lowercase()
                    currentTag = name
                    if (name == "item" || name == "entry") {
                        insideItem = true
                        itemTitle = ""
                        itemLink = ""
                        itemSummary = ""
                        itemDate = ""
                    } else if (name == "link") {
                        val href = parser.getAttributeValue(null, "href")
                        if (!href.isNullOrBlank()) {
                            if (insideItem) itemLink = href else if (siteUrl.isBlank()) siteUrl = href
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim().orEmpty()
                    if (text.isNotEmpty()) {
                        if (!insideItem) {
                            when (currentTag) {
                                "title" -> if (feedTitle.isBlank()) feedTitle = text
                                "link" -> if (siteUrl.isBlank()) siteUrl = text
                            }
                        } else {
                            when (currentTag) {
                                "title" -> if (itemTitle.isBlank()) itemTitle = text
                                "link" -> if (itemLink.isBlank()) itemLink = text
                                "description", "summary", "content", "content:encoded" ->
                                    if (itemSummary.isBlank()) itemSummary = stripHtmlTags(text).take(220)
                                "pubdate", "updated", "published", "dc:date" ->
                                    if (itemDate.isBlank()) itemDate = text.take(24)
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val name = parser.name.lowercase()
                    if (name == "item" || name == "entry") {
                        insideItem = false
                        if (itemTitle.isNotBlank() && itemLink.startsWith("http")) {
                            articles.add(
                                RssArticleEntity(
                                    feedId = 0L,
                                    feedTitle = feedTitle.ifBlank { feedUrl },
                                    title = itemTitle,
                                    link = itemLink,
                                    summary = itemSummary,
                                    publishedLabel = itemDate
                                )
                            )
                        }
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        return ParsedRssFeed(
            title = feedTitle.ifBlank { feedUrl },
            feedUrl = feedUrl,
            siteUrl = siteUrl,
            articles = articles.take(40)
        )
    }

    private fun stripHtmlTags(raw: String): String =
        raw.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()
}

package com.example.browser

import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import java.util.UUID

data class ReaderArticle(
    val title: String,
    val subtitle: String = "",
    val category: String = "ARTICLE • READER MODE",
    val author: String = "",
    val siteName: String = "",
    val readTimeMinutes: Int = 5,
    val heroImageUrl: String? = null,
    val heroCaption: String? = null,
    val heroBadge: String? = null,
    val authorAvatarUrl: String? = null,
    val quote: String? = null,
    val paragraphs: List<String> = emptyList(),
    val secondaryImages: List<Pair<String, String>> = emptyList(),
    val isBuiltInShowcase: Boolean = false
)

data class TabState(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "",
    val title: String = "New Sanctuary",
    val domain: String = "",
    val isPrivate: Boolean = false,
    val isDesktopMode: Boolean = false,
    val isReaderMode: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val adsBlocked: Int = 0,
    val trackersBlocked: Int = 0,
    val blockedDomains: List<String> = emptyList(),
    val findMatchCurrent: Int = 0,
    val findMatchTotal: Int = 0,
    val sslSecure: Boolean = true,
    val snippet: String = "Ready to discover",
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val readerArticle: ReaderArticle? = null,
    val loadError: String? = null,
    // 18-Feature Additions
    val isPinned: Boolean = false,
    val groupName: String? = null,
    val zoomPercent: Int = 100,
    val isTranslated: Boolean = false,
    val originalUrlBeforeTranslate: String = "",
    val hasActiveMedia: Boolean = false,
    val isPlayingMedia: Boolean = false,
    // 19-Feature Power Additions
    val mediaSpeed: Float = 1.0f,
    val audioBoost: Float = 1.0f,
    val discoveredFeedUrl: String? = null,
    val isStandalonePwa: Boolean = false,
    // 9-Feature Privacy & Security Additions
    val privacyReport: TabPrivacyReport = TabPrivacyReport(),
    val activeSensorGrants: Set<String> = emptySet()
) {
    val isHome: Boolean get() = url.isBlank()
    val totalBlocked: Int get() = adsBlocked + trackersBlocked
}

data class TabPrivacyReport(
    val blockedHostCounts: Map<String, Int> = emptyMap(),
    val apiProbesBlocked: Map<String, Int> = emptyMap(),
    val unwrappedRedirects: Int = 0,
    val strippedParamsCount: Int = 0,
    val httpsUpgrades: Int = 0
) {
    val totalHostBlocks: Int get() = blockedHostCounts.values.sum()
    val totalApiBlocks: Int get() = apiProbesBlocked.values.sum()

    fun computeGrade(
        sslSecure: Boolean,
        shieldsEnabled: Boolean,
        strictLockdownsActive: Int = 4
    ): String {
        var score = 100
        if (!sslSecure) score -= 40
        if (!shieldsEnabled) score -= 25
        if (shieldsEnabled) {
            score += strictLockdownsActive.coerceIn(0, 6)
        } else if (blockedHostCounts.size > 3 || apiProbesBlocked.isNotEmpty()) {
            score -= 15
        }
        return when {
            score >= 96 && sslSecure && shieldsEnabled -> "A+"
            score >= 85 -> "A"
            score >= 70 -> "B"
            score >= 50 -> "C"
            else -> "D"
        }
    }
}

data class WebPermissionPromptRequest(
    val tabId: String,
    val origin: String,
    val domain: String,
    val permissionKeys: List<String>,
    val resourceLabels: List<String>,
    val rawPermissionRequest: PermissionRequest? = null,
    val geoOrigin: String? = null,
    val geoCallback: GeolocationPermissions.Callback? = null
)

data class SslWarningState(
    val tabId: String,
    val url: String,
    val primaryError: String
)

data class MaliciousWarningState(
    val tabId: String,
    val url: String,
    val domain: String
)

data class PageSourceDialogState(
    val title: String,
    val url: String,
    val htmlSource: String
)

data class ExternalAppRequest(
    val tabId: String,
    val appLabel: String,
    val targetUri: String,
    val fallbackWebUrl: String? = null
)

data class WebContextMenuRequest(
    val tabId: String,
    val linkUrl: String? = null,
    val imageUrl: String? = null,
    val titleText: String = ""
)

data class HighlightSelectionRequest(
    val tabId: String,
    val url: String,
    val domain: String,
    val title: String,
    val quoteText: String
)

data class ConsoleLogEntry(
    val level: String,
    val message: String,
    val sourceId: String,
    val lineNumber: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class NetworkLogEntry(
    val url: String,
    val host: String,
    val isBlocked: Boolean,
    val category: String,
    val timestamp: Long = System.currentTimeMillis()
)

object SanctuaryReaderSamples {
    // Hotlinked editorial assets directly from attached HTML reference
    val ARCHITECTURE_ESSAY = ReaderArticle(
        title = "Serene Architecture & Biophilic Design",
        subtitle = "How unbleached earthen materials and natural cadence are quietly reshaping human habitation in dense cities.",
        category = "ESSAY • ARCHITECTURAL PHILOSOPHY",
        author = "Elena Vance, RIBA",
        siteName = "Oxford Sanctuary Studies",
        readTimeMinutes = 6,
        heroImageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCHcmVGc5G6hRkV1XHSDCMnTzZRkNtfevAOHcGSRr9NkJ0BW54R2C-SikN_r3zPgXPy-0DfiLFtAq5U4yLBKOUwYVaWi39HWOHFb9d6rB1wSd66HTKAqIofmb85Nhthh0zyBvB7svBG-2dNraihBKjBB97gqhUYq2XOrgIHkZ6OP1mQQB6dCBGap0i0TH-QqnUuBScBoN5uIx7L6Chew0iUM4WwNXCzV_Rq3uzlcrxm",
        heroCaption = "Fig 1.1: Interior courtyard at Pavilion V balancing thermal mass with open breezeways.",
        heroBadge = "Pavilion V, Kyoto",
        authorAvatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuA-xsUhepN2p2s7wLFZ11fKmV1y6g8RepHAd5lrHTmvopx1sz53L49ZOeKbzK7OosJ5utJ-tquQa_l_H-zef2TkAr_u-p4KheJBSPgQPVeXF17ynTAA401tESvHsSES3z5l02hCm_dAF1VywHYFUCWn1rATqMFCx-jSnOw6m54bvBDak1kQ5r-IpzLz8Jgd7GJS5CxtrTyhHm4OdmQeU5MH3fZurtzfZwFIvr34v38n",
        quote = "“The eye seeks texture, the hand seeks warmth, and the nervous system craves rhythm rather than repetition.”",
        paragraphs = listOf(
            "To build with intention is to accept that buildings are breathing entities rather than static monuments. Contemporary urban density has long favored impervious surfaces—sheet glass, polished steel, and high-reflective synthetics—that unintentionally induce neuro-cognitive fatigue.",
            "Biophilic engineering replaces this friction with acoustic absorption and tactile presence. By introducing unbleached terracotta lime, porous limestone, and cross-laminated timber, spaces naturally regulate relative interior moisture while muting high-frequency transit noise from adjacent streets.",
            "The psychological dividend is immediate. Cortisol levels recorded in occupants residing within timber-earthen modules demonstrate a 14% mean reduction within the first seventy-two hours of sustained exposure. The built sanctuary becomes a sensory filter."
        ),
        secondaryImages = listOf(
            "Acoustic Travertine" to "https://lh3.googleusercontent.com/aida-public/AB6AXuCAV-f3wbq5Z5EhrWFhxaJK3Dikm8TM9EaB7nkFV829rsZttPRYwAEl_Sgpn5DgyoS-lvrrVTQvs0-erHXzSWV0BYwjO-0kSYgMy07S9o6R49H-1zagJw6WKUIQsKuge_tNZTNsY-d6EJGt_igidRgfN7IMQMb1knoa0dstj3kX6M_sPtb2_eFHLB7rKB2_aTx7zUSMNKyc4lFmaq8h9cemnJro4bXMqlMaCp4h81jq",
            "Solar Aperture" to "https://lh3.googleusercontent.com/aida-public/AB6AXuBL7PteW-QQRoLEiNA7AuLMa1sfnYIVaiIxJiU8O8tGLGZkDpPfHTKQmLXbrFBk31U171Lmk4G5c-vk6oyXXeXqCmh1EEvjN0lkb1Js2ETBon1fxArHXTJgGyMkAGdOI8JDPDQrw7EwiZv1n2sY6IZNS2Fj9Kex7d3m22OaJiY4sX8i_Ow6PPw1d_o6OI-S0Tmsd8b0InrdGhpnRdbosDZ7iFmnHgSBlnYjZ0qbmkGM"
        ),
        isBuiltInShowcase = true
    )

    const val READABILITY_EXTRACT_JS = """
        (function() {
            try {
                var title = document.title || '';
                var article = document.querySelector('article') || document.querySelector('main') || document.body;
                var h1 = document.querySelector('h1');
                if (h1 && h1.innerText && h1.innerText.trim().length > 4) {
                    title = h1.innerText.trim();
                }
                var paras = [];
                var nodes = article ? article.querySelectorAll('p') : [];
                for (var i = 0; i < nodes.length; i++) {
                    var txt = (nodes[i].innerText || '').trim();
                    if (txt.length > 40) {
                        paras.push(txt);
                    }
                    if (paras.length >= 24) break;
                }
                var img = document.querySelector('meta[property="og:image"]');
                var heroUrl = img ? img.getAttribute('content') : '';
                var desc = document.querySelector('meta[name="description"]');
                var subtitle = desc ? desc.getAttribute('content') : '';
                return JSON.stringify({
                    title: title,
                    subtitle: subtitle || '',
                    heroUrl: heroUrl || '',
                    paragraphs: paras
                });
            } catch (e) {
                return JSON.stringify({ title: document.title || '', subtitle: '', heroUrl: '', paragraphs: [] });
            }
        })();
    """
}

data class HomeSiteShortcut(
    val key: String,
    val title: String,
    val url: String,
    val domain: String,
    val iconKey: String,
    val visitCount: Long = 0L,
    val isPinned: Boolean = false,
    val isAnalyticsTop: Boolean = false
)

object SiteShortcutPlanner {
    fun mergePinnedAndTopSites(
        pinnedBookmarks: List<com.example.data.local.BookmarkEntity>,
        topSites: List<com.example.data.local.SiteUsageEntity>,
        maxSlots: Int = 7
    ): List<HomeSiteShortcut> {
        val topByDomain = topSites.associateBy { normalizeDomain(it.domain) }
        val result = mutableListOf<HomeSiteShortcut>()
        val seenDomains = mutableSetOf<String>()

        // 1. Place frequently visited analytics sites at the front
        topSites
            .filter { it.visitCount > 0L && normalizeDomain(it.domain).isNotBlank() }
            .sortedWith(compareByDescending<com.example.data.local.SiteUsageEntity> { it.visitCount }.thenByDescending { it.lastVisitedAt })
            .forEach { usage ->
                if (result.size >= maxSlots) return@forEach
                val norm = normalizeDomain(usage.domain)
                if (seenDomains.add(norm)) {
                    val matchingPinned = pinnedBookmarks.firstOrNull { extractNormalizedHost(it.url) == norm }
                    result.add(
                        HomeSiteShortcut(
                            key = "top_$norm",
                            title = matchingPinned?.title ?: cleanSiteTitle(usage.title, norm),
                            url = matchingPinned?.url ?: usage.url,
                            domain = norm,
                            iconKey = resolveBadgeKey(matchingPinned?.iconKey ?: "", matchingPinned?.title ?: usage.title, norm),
                            visitCount = usage.visitCount,
                            isPinned = matchingPinned != null,
                            isAnalyticsTop = true
                        )
                    )
                }
            }

        // 2. Fill remaining slots with pinned bookmarks not yet shown
        pinnedBookmarks.forEach { bm ->
            if (result.size >= maxSlots) return@forEach
            val norm = extractNormalizedHost(bm.url)
            if (norm.isBlank() || seenDomains.add(norm)) {
                val usage = topByDomain[norm]
                result.add(
                    HomeSiteShortcut(
                        key = "bm_${bm.id}",
                        title = bm.title,
                        url = bm.url,
                        domain = norm,
                        iconKey = resolveBadgeKey(bm.iconKey, bm.title, norm),
                        visitCount = usage?.visitCount ?: 0L,
                        isPinned = true,
                        isAnalyticsTop = (usage?.visitCount ?: 0L) > 0L
                    )
                )
            }
        }

        return result.take(maxSlots)
    }

    fun resolveBadgeKey(rawIconKey: String, title: String, domain: String): String {
        val trimmed = rawIconKey.trim()
        val mappedLegacy = when (trimmed.lowercase()) {
            "smart_display" -> "YT"
            "code" -> "GH"
            "travel_explore" -> "DD"
            "forum" -> "RD"
            "public", "" -> ""
            else -> if (trimmed.length <= 2 && trimmed.all { it.isLetterOrDigit() }) trimmed.uppercase() else ""
        }
        if (mappedLegacy.isNotBlank()) return mappedLegacy

        val cleanDomain = normalizeDomain(domain)
        return when {
            cleanDomain.contains("youtube") -> "YT"
            cleanDomain.contains("github") -> "GH"
            cleanDomain.contains("duckduckgo") -> "DD"
            cleanDomain.contains("reddit") -> "RD"
            cleanDomain.contains("wikipedia") -> "W"
            cleanDomain.contains("news.ycombinator") -> "Y"
            cleanDomain.contains("google") -> "G"
            else -> title.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString()
                ?: cleanDomain.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString()
                ?: "W"
        }
    }

    fun normalizeDomain(domain: String): String =
        domain.trim().lowercase().removePrefix("www.").removePrefix("m.")

    private fun extractNormalizedHost(url: String): String {
        val host = url
            .substringAfter("://", url)
            .substringBefore("/")
            .substringBefore("?")
            .substringBefore(":")
        return normalizeDomain(host)
    }

    private fun cleanSiteTitle(rawTitle: String, domain: String): String {
        val t = rawTitle.trim()
        if (t.isNotBlank() && !t.startsWith("http")) {
            return t.substringBefore(" - ").substringBefore(" | ").take(18).trim()
        }
        return domain.substringBefore(".").replaceFirstChar { it.uppercaseChar() }
    }
}

object QuickControlUsageSorter {
    fun <T> sortByUsage(
        items: List<T>,
        idOf: (T) -> String,
        usageMap: Map<String, com.example.data.local.QuickControlUsageEntity>
    ): List<T> {
        if (usageMap.isEmpty()) return items
        return items.withIndex()
            .sortedWith(
                compareByDescending<IndexedValue<T>> { usageMap[idOf(it.value)]?.usageCount ?: 0L }
                    .thenByDescending { usageMap[idOf(it.value)]?.lastUsedAt ?: 0L }
                    .thenBy { it.index }
            )
            .map { it.value }
    }
}


package com.example.browser

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
    val isPlayingMedia: Boolean = false
) {
    val isHome: Boolean get() = url.isBlank()
    val totalBlocked: Int get() = adsBlocked + trackersBlocked
}

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

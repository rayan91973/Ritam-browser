package com.example.privacy.adblock

/** Which kind of list a rule came from. Used to separate ad and tracker shield metrics. */
enum class AdCategory { AD, TRACKER }

/** Resource types as used by Adblock Plus `$type` options. */
enum class RequestType(val bit: Int) {
    DOCUMENT(1 shl 0),
    SUBDOCUMENT(1 shl 1),
    SCRIPT(1 shl 2),
    IMAGE(1 shl 3),
    STYLESHEET(1 shl 4),
    FONT(1 shl 5),
    MEDIA(1 shl 6),
    XMLHTTPREQUEST(1 shl 7),
    WEBSOCKET(1 shl 8),
    PING(1 shl 9),
    OBJECT(1 shl 10),
    OTHER(1 shl 11);

    companion object {
        fun fromToken(token: String): RequestType? = when (token) {
            "document" -> DOCUMENT
            "subdocument" -> SUBDOCUMENT
            "script" -> SCRIPT
            "image" -> IMAGE
            "stylesheet", "css" -> STYLESHEET
            "font" -> FONT
            "media" -> MEDIA
            "xmlhttprequest", "xhr" -> XMLHTTPREQUEST
            "websocket" -> WEBSOCKET
            "ping", "beacon" -> PING
            "object", "object-subrequest" -> OBJECT
            "other" -> OTHER
            else -> null
        }

        /** Best-effort resource type from the request Accept header (WebView exposes no type). */
        fun classify(accept: String?): RequestType {
            if (accept.isNullOrBlank()) return OTHER
            val a = accept.lowercase()
            return when {
                a.contains("text/css") -> STYLESHEET
                a.contains("image/") || a.contains("image/webp") -> IMAGE
                a.contains("javascript") || a.contains("ecmascript") -> SCRIPT
                a.contains("font/") || a.contains("woff") || a.contains("application/font") -> FONT
                a.contains("video/") || a.contains("audio/") -> MEDIA
                a.contains("text/html") || a.contains("application/xhtml") -> SUBDOCUMENT
                a.contains("json") || a.contains("xml") || a.contains("text/plain") -> XMLHTTPREQUEST
                else -> OTHER
            }
        }
    }
}

/** A single matching outcome, returned to the shields engine for metrics. */
data class FilterMatch(
    val category: AdCategory,
    val rule: String
)

/**
 * One Adblock Plus network filter.
 *
 * Two shapes are supported:
 *  - `hostOnly` rules (`||ads.example.com^`): matched by hostname suffix, the cheapest path.
 *  - pattern rules: matched with [AbpPattern], bucketed by a literal shortcut token.
 */
class NetworkRule(
    val pattern: String,
    val hostAnchor: Boolean,
    val startAnchor: Boolean,
    val requireEnd: Boolean,
    val hostOnly: String?,
    val shortcut: String,
    val category: AdCategory,
    val isRegex: Boolean = false,
    val includeDomains: Array<String>? = null,
    val excludeDomains: Array<String>? = null,
    /** 0 = any, 1 = third-party only, -1 = first-party only. */
    val thirdParty: Int = 0,
    val typeMask: Int = 0,
    val excludedTypeMask: Int = 0
) {
    val isUnconstrained: Boolean
        get() = includeDomains == null && excludeDomains == null &&
            thirdParty == 0 && typeMask == 0 && excludedTypeMask == 0

    fun matches(
        url: String,
        host: String,
        pageHost: String,
        type: RequestType,
        isThirdParty: Boolean
    ): Boolean {
        if (typeMask != 0 && (typeMask and type.bit) == 0) return false
        if (excludedTypeMask != 0 && (excludedTypeMask and type.bit) != 0) return false
        if (thirdParty > 0 && !isThirdParty) return false
        if (thirdParty < 0 && isThirdParty) return false
        includeDomains?.let { domains ->
            var allowed = false
            for (domain in domains) {
                if (AbpUrl.domainMatches(pageHost, domain)) {
                    allowed = true
                    break
                }
            }
            if (!allowed) return false
        }
        excludeDomains?.let { domains ->
            for (domain in domains) {
                if (AbpUrl.domainMatches(pageHost, domain)) return false
            }
        }
        if (isRegex) return true
        hostOnly?.let { only ->
            return host == only || host.endsWith(".$only")
        }
        return AbpPattern.matches(url, pattern, hostAnchor, startAnchor, requireEnd)
    }
}

/** URL helpers shared by the matcher and the engine. */
object AbpUrl {
    fun hostOf(url: String): String {
        val scheme = url.indexOf("://")
        val start = if (scheme >= 0) scheme + 3 else 0
        var end = url.length
        for (i in start until url.length) {
            val c = url[i]
            if (c == '/' || c == '?' || c == '#') {
                end = i
                break
            }
        }
        var host = url.substring(start, end)
        val at = host.lastIndexOf('@')
        if (at >= 0) host = host.substring(at + 1)
        val colon = host.indexOf(':')
        if (colon >= 0) host = host.substring(0, colon)
        return host.removePrefix("www.")
    }

    /** `a.example.com` matches `example.com`; wildcard entries such as `example.*` are honoured. */
    fun domainMatches(host: String, domain: String): Boolean {
        if (host.isEmpty() || domain.isEmpty()) return false
        val d = domain.lowercase()
        if (d.contains('*')) {
            return wildcardDomainMatch(host, d)
        }
        return host == d || host.endsWith(".$d")
    }

    private fun wildcardDomainMatch(host: String, pattern: String): Boolean {
        var hi = 0
        var pi = 0
        var starPi = -1
        var starHi = 0
        while (hi < host.length) {
            if (pi < pattern.length && (pattern[pi] == host[hi])) {
                pi++
                hi++
            } else if (pi < pattern.length && pattern[pi] == '*') {
                starPi = pi++
                starHi = hi
            } else if (starPi >= 0) {
                pi = starPi + 1
                hi = ++starHi
            } else {
                return false
            }
        }
        while (pi < pattern.length && pattern[pi] == '*') pi++
        return pi == pattern.length
    }

    /** Registrable-domain comparison, good enough for `$third-party` decisions. */
    fun siteOf(host: String): String {
        val clean = host.removePrefix("www.")
        val parts = clean.split('.')
        if (parts.size <= 2) return clean
        val last = parts[parts.size - 1]
        val secondLast = parts[parts.size - 2]
        return if (last.length == 2 && secondLast.length <= 3) {
            parts.subList(parts.size - 3, parts.size).joinToString(".")
        } else {
            parts.subList(parts.size - 2, parts.size).joinToString(".")
        }
    }

    fun isThirdParty(requestHost: String, pageHost: String): Boolean {
        if (pageHost.isEmpty()) return true
        return siteOf(requestHost) != siteOf(pageHost)
    }
}

/** Adblock Plus pattern matching: `*` wildcard, `^` separator, `||` / `|` anchors. */
object AbpPattern {
    fun isSeparator(c: Char): Boolean = when (c) {
        in 'a'..'z', in '0'..'9' -> false
        '_', '-', '.', '%' -> false
        else -> true
    }

    fun matches(
        url: String,
        pattern: String,
        hostAnchor: Boolean,
        startAnchor: Boolean,
        requireEnd: Boolean
    ): Boolean {
        if (pattern.isEmpty()) return false
        if (hostAnchor) {
            val scheme = url.indexOf("://")
            val hostStart = if (scheme >= 0) scheme + 3 else 0
            var hostEnd = url.length
            for (i in hostStart until url.length) {
                val c = url[i]
                if (c == '/' || c == '?' || c == '#') {
                    hostEnd = i
                    break
                }
            }
            var start = hostStart
            while (start <= hostEnd) {
                if (matchFrom(pattern, url, start, requireEnd)) return true
                val dot = url.indexOf('.', start)
                if (dot < 0 || dot >= hostEnd) break
                start = dot + 1
            }
            return false
        }
        if (startAnchor) return matchFrom(pattern, url, 0, requireEnd)
        // Unanchored filters match anywhere in the URL; a leading wildcard expresses that.
        return matchFrom("*$pattern", url, 0, requireEnd)
    }

    /**
     * Glob match starting exactly at [from]. Unless [requireEnd] is set the pattern only has to
     * match a prefix of the remainder, which is the ABP "substring" semantics.
     */
    private fun matchFrom(pattern: String, text: String, from: Int, requireEnd: Boolean): Boolean {
        var pi = 0
        var ti = from
        var starPi = -1
        var starTi = from
        while (true) {
            if (pi == pattern.length) {
                return !requireEnd || ti == text.length
            }
            val c = pattern[pi]
            if (c == '*') {
                starPi = pi
                pi++
                starTi = ti
                continue
            }
            if (c == '^') {
                if (ti == text.length) {
                    pi++
                    continue
                }
                if (isSeparator(text[ti])) {
                    pi++
                    ti++
                    continue
                }
            } else if (ti < text.length && text[ti] == c) {
                pi++
                ti++
                continue
            }
            if (starPi >= 0 && starTi < text.length) {
                pi = starPi + 1
                ti = ++starTi
                continue
            }
            return false
        }
    }

    /** Longest alphanumeric run, truncated; used as the bucket key for fast lookup. */
    fun shortcutOf(pattern: String): String {
        var bestStart = -1
        var bestLen = 0
        var i = 0
        while (i < pattern.length) {
            while (i < pattern.length && !isAlnum(pattern[i])) i++
            val start = i
            while (i < pattern.length && isAlnum(pattern[i])) i++
            val len = i - start
            if (len > bestLen) {
                bestLen = len
                bestStart = start
            }
        }
        if (bestLen < 3 || bestStart < 0) return ""
        return pattern.substring(bestStart, bestStart + minOf(bestLen, 8))
    }

    fun isAlnum(c: Char): Boolean = c in 'a'..'z' || c in '0'..'9'
}

/** One element-hiding rule (`example.com##.ad-slot`). */
class CosmeticEntry(val selector: String, val excludeDomains: Array<String>?)

/** Parsed cosmetic rules from all enabled lists. */
class CosmeticRules {
    val generic = ArrayList<CosmeticEntry>()
    val byDomain = HashMap<String, MutableList<CosmeticEntry>>()
    val exceptionsByDomain = HashMap<String, MutableSet<String>>()
    val genericExceptions = HashSet<String>()
}

/** Result of parsing one filter list file. */
class ParsedFilterList(
    val blockRules: List<NetworkRule>,
    val exceptionRules: List<NetworkRule>,
    val cosmeticRules: CosmeticRules,
    /** `@@||site^$document` — whitelists the whole page. */
    val pageWhitelist: Set<String>,
    /** `@@||site^$generichide` — disables generic element hiding on the page. */
    val genericHideWhitelist: Set<String>,
    val networkCount: Int,
    val cosmeticCount: Int
)

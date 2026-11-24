package com.example.privacy.adblock

import java.util.concurrent.ConcurrentHashMap

/**
 * Compiled, immutable ad/tracker blocking engine.
 *
 * Requests are matched in this order:
 *  1. page-wide `$document` exceptions
 *  2. `@@` exception rules
 *  3. blocking rules: hostname rules first (hash lookups), then pattern rules bucketed by a
 *     literal shortcut token, then rules without any usable token, then regex rules.
 */
class AdBlockEngine private constructor(
    private val blocks: RuleIndex,
    private val exceptions: RuleIndex,
    private val pageWhitelist: Set<String>,
    private val genericHideWhitelist: Set<String>,
    private val cosmetic: CosmeticRules,
    val networkRuleCount: Int,
    val cosmeticRuleCount: Int
) {
    private val cosmeticCache = ConcurrentHashMap<String, String>()
    private val genericHideCache = ConcurrentHashMap<String, Boolean>()

    val isUsable: Boolean get() = networkRuleCount > 0

    fun findBlockingRule(
        requestUrl: String,
        pageHost: String,
        type: RequestType
    ): FilterMatch? {
        val url = requestUrl.lowercase()
        val requestHost = AbpUrl.hostOf(url)
        if (requestHost.isEmpty()) return null
        val page = pageHost.lowercase().removePrefix("www.")

        if (pageWhitelist.isNotEmpty() && isWhitelisted(page)) return null

        val isThirdParty = AbpUrl.isThirdParty(requestHost, page)
        if (exceptions.find(url, requestHost, page, type, isThirdParty) != null) return null
        return blocks.find(url, requestHost, page, type, isThirdParty)
    }

    fun isWhitelisted(pageHost: String): Boolean {
        if (pageWhitelist.isEmpty()) return false
        for (domain in pageWhitelist) {
            if (AbpUrl.domainMatches(pageHost, domain)) return true
        }
        return false
    }

    /** True when the site disables generic element hiding via `@@||site^$generichide`. */
    fun allowsGenericCosmetic(pageHost: String): Boolean {
        if (genericHideWhitelist.isEmpty()) return true
        val host = pageHost.lowercase().removePrefix("www.")
        return genericHideCache.getOrPut(host) {
            genericHideWhitelist.none { AbpUrl.domainMatches(host, it) }
        }
    }

    /**
     * Element-hiding CSS for a page: domain specific selectors first, then the most common
     * generic ones (capped so stylesheet parsing stays fast on mobile WebView).
     */
    fun cosmeticCss(pageHost: String, allowGeneric: Boolean = true): String {
        val host = pageHost.lowercase().removePrefix("www.")
        if (host.isEmpty()) return ""
        val cacheKey = if (allowGeneric) host else "$host|no-generic"
        cosmeticCache[cacheKey]?.let { return it }

        val selectors = LinkedHashSet<String>(64)
        var cursor = host
        while (true) {
            cosmetic.byDomain[cursor]?.let { entries ->
                for (entry in entries) {
                    if (entry.excludeDomains == null ||
                        entry.excludeDomains.none { AbpUrl.domainMatches(host, it) }
                    ) {
                        selectors.add(entry.selector)
                    }
                }
            }
            val dot = cursor.indexOf('.')
            if (dot < 0) break
            cursor = cursor.substring(dot + 1)
        }

        val removed = HashSet<String>(8)
        cursor = host
        while (true) {
            cosmetic.exceptionsByDomain[cursor]?.let { removed.addAll(it) }
            val dot = cursor.indexOf('.')
            if (dot < 0) break
            cursor = cursor.substring(dot + 1)
        }
        if (removed.isNotEmpty()) selectors.removeAll(removed)

        if (allowGeneric) {
            var added = 0
            for (entry in cosmetic.generic) {
                if (added >= GENERIC_SELECTOR_CAP) break
                if (entry.excludeDomains != null &&
                    entry.excludeDomains.any { AbpUrl.domainMatches(host, it) }
                ) continue
                if (cosmetic.genericExceptions.contains(entry.selector)) continue
                if (selectors.add(entry.selector)) added++
            }
        }

        val css = if (selectors.isEmpty()) {
            ""
        } else {
            buildString(selectors.size * 32) {
                append(selectors.joinToString(",\n"))
                append("\n{ display: none !important; }\n")
            }
        }
        if (cosmeticCache.size > COSMETIC_CACHE_LIMIT) cosmeticCache.clear()
        cosmeticCache[cacheKey] = css
        return css
    }

    private class RuleIndex {
        private val plainHosts = HashMap<String, AdCategory>(4096)
        private val hostRules = HashMap<String, MutableList<NetworkRule>>(8192)
        private val buckets = HashMap<String, MutableList<NetworkRule>>(4096)
        private val always = ArrayList<NetworkRule>(64)
        private val regexRules = ArrayList<Pair<Regex, NetworkRule>>(8)

        fun add(rule: NetworkRule) {
            when {
                rule.isRegex -> {
                    if (regexRules.size < REGEX_RULE_LIMIT) {
                        val compiled = runCatching {
                            Regex(rule.pattern.substring(1, rule.pattern.length - 1))
                        }.getOrNull() ?: return
                        regexRules.add(compiled to rule)
                    }
                }
                rule.hostOnly != null -> {
                    if (rule.isUnconstrained) {
                        plainHosts[rule.hostOnly] = rule.category
                    } else {
                        hostRules.getOrPut(rule.hostOnly) { ArrayList(2) }.add(rule)
                    }
                }
                rule.shortcut.isEmpty() -> always.add(rule)
                else -> buckets.getOrPut(rule.shortcut) { ArrayList(1) }.add(rule)
            }
        }

        fun find(
            url: String,
            host: String,
            pageHost: String,
            type: RequestType,
            isThirdParty: Boolean
        ): FilterMatch? {
            var boundary = host
            while (true) {
                plainHosts[boundary]?.let { return FilterMatch(it, "||$boundary^") }
                val dot = boundary.indexOf('.')
                if (dot < 0) break
                boundary = boundary.substring(dot + 1)
            }

            boundary = host
            while (true) {
                hostRules[boundary]?.let { rules ->
                    for (rule in rules) {
                        if (rule.matches(url, host, pageHost, type, isThirdParty)) {
                            return FilterMatch(rule.category, "||$boundary^")
                        }
                    }
                }
                val dot = boundary.indexOf('.')
                if (dot < 0) break
                boundary = boundary.substring(dot + 1)
            }

            var i = 0
            val length = url.length
            while (i < length) {
                while (i < length && !AbpPattern.isAlnum(url[i])) i++
                val start = i
                while (i < length && AbpPattern.isAlnum(url[i])) i++
                val pieceLength = i - start
                if (pieceLength >= 3) {
                    val maxLen = minOf(8, pieceLength)
                    var len = 3
                    while (len <= maxLen) {
                        buckets[url.substring(start, start + len)]?.let { rules ->
                            for (rule in rules) {
                                if (rule.matches(url, host, pageHost, type, isThirdParty)) {
                                    return FilterMatch(rule.category, rule.pattern)
                                }
                            }
                        }
                        len++
                    }
                }
                if (i >= length) break
                i++
            }

            for (rule in always) {
                if (rule.matches(url, host, pageHost, type, isThirdParty)) {
                    return FilterMatch(rule.category, rule.pattern)
                }
            }

            for ((regex, rule) in regexRules) {
                if (regex.containsMatchIn(url) && rule.matches(url, host, pageHost, type, isThirdParty)) {
                    return FilterMatch(rule.category, rule.pattern)
                }
            }
            return null
        }
    }

    companion object {
        const val GENERIC_SELECTOR_CAP = 1500
        private const val COSMETIC_CACHE_LIMIT = 128
        private const val REGEX_RULE_LIMIT = 250

        val EMPTY = AdBlockEngine(
            blocks = RuleIndex(),
            exceptions = RuleIndex(),
            pageWhitelist = emptySet(),
            genericHideWhitelist = emptySet(),
            cosmetic = CosmeticRules(),
            networkRuleCount = 0,
            cosmeticRuleCount = 0
        )

        fun build(lists: List<ParsedFilterList>): AdBlockEngine {
            if (lists.isEmpty()) return EMPTY
            val blocks = RuleIndex()
            val exceptions = RuleIndex()
            val pageWhitelist = HashSet<String>()
            val genericHideWhitelist = HashSet<String>()
            val cosmetic = CosmeticRules()
            var networkCount = 0
            var cosmeticCount = 0

            for (list in lists) {
                for (rule in list.blockRules) blocks.add(rule)
                for (rule in list.exceptionRules) exceptions.add(rule)
                networkCount += list.networkCount
                cosmeticCount += list.cosmeticCount
                pageWhitelist.addAll(list.pageWhitelist)
                genericHideWhitelist.addAll(list.genericHideWhitelist)
                cosmetic.generic.addAll(list.cosmeticRules.generic)
                cosmetic.genericExceptions.addAll(list.cosmeticRules.genericExceptions)
                for ((domain, entries) in list.cosmeticRules.byDomain) {
                    cosmetic.byDomain.getOrPut(domain) { ArrayList(2) }.addAll(entries)
                }
                for ((domain, selectors) in list.cosmeticRules.exceptionsByDomain) {
                    cosmetic.exceptionsByDomain.getOrPut(domain) { HashSet() }.addAll(selectors)
                }
            }
            return AdBlockEngine(
                blocks = blocks,
                exceptions = exceptions,
                pageWhitelist = pageWhitelist,
                genericHideWhitelist = genericHideWhitelist,
                cosmetic = cosmetic,
                networkRuleCount = networkCount,
                cosmeticRuleCount = cosmeticCount
            )
        }
    }
}

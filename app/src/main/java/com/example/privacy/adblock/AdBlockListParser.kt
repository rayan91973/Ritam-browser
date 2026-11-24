package com.example.privacy.adblock

/**
 * Parser for Adblock Plus / uBlock Origin filter syntax as used by EasyList and EasyPrivacy.
 *
 * Supported: `||`, `|`, `^`, `*` anchors, `@@` exceptions, `$` options (third-party, domain,
 * resource types, document, generichide, badfilter), element hiding (`##`, domain scoped,
 * `~` domain exclusions) and element hiding exceptions (`#@#`).
 *
 * Deliberately skipped: popup rules (cannot be enforced at request level and would over-block),
 * CSP/removeparam/rewrite/method rules, procedural cosmetics and scriptlets. Ad and tracker
 * blocking does not need them.
 */
object AdBlockListParser {

    private val COSMETIC_MARKERS = listOf("#@$#", "#$?#", "#$#", "#@#", "#?#", "##")

    fun parse(text: String, category: AdCategory): ParsedFilterList {
        val lines = text.lineSequence()

        // First pass: collect $badfilter targets so the rules they cancel are never compiled.
        val badfilterTargets = HashSet<String>(64)
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || line[0] == '!' || line[0] == '[') continue
            if (isCosmetic(line)) continue
            badfilterKey(line)?.let { badfilterTargets.add(it) }
        }

        val blockRules = ArrayList<NetworkRule>(2048)
        val exceptionRules = ArrayList<NetworkRule>(512)
        val cosmetic = CosmeticRules()
        val pageWhitelist = HashSet<String>(16)
        val genericHideWhitelist = HashSet<String>(16)
        var networkCount = 0
        var cosmeticCount = 0

        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line[0] == '!' || line[0] == '[') continue

            val marker = cosmeticMarker(line)
            if (marker != null) {
                if (parseCosmeticRule(line, marker, cosmetic)) cosmeticCount++
                continue
            }

            val isException = line.startsWith("@@")
            if (isException && badfilterTargets.isNotEmpty()) {
                val key = normalizedKey(line)
                if (key != null && badfilterTargets.contains(key)) continue
            } else if (badfilterTargets.isNotEmpty() && line.contains('$')) {
                val key = normalizedKey(line)
                if (key != null && badfilterTargets.contains(key)) continue
            }

            val parsed = parseNetworkRule(line, category) ?: continue
            networkCount++
            if (parsed.pageWhitelistDomain != null) {
                pageWhitelist.add(parsed.pageWhitelistDomain)
            }
            if (parsed.genericHideDomain != null) {
                genericHideWhitelist.add(parsed.genericHideDomain)
            }
            parsed.rule?.let { if (isException) exceptionRules.add(it) else blockRules.add(it) }
        }

        return ParsedFilterList(
            blockRules = blockRules,
            exceptionRules = exceptionRules,
            cosmeticRules = cosmetic,
            pageWhitelist = pageWhitelist,
            genericHideWhitelist = genericHideWhitelist,
            networkCount = networkCount,
            cosmeticCount = cosmeticCount
        )
    }

    private fun isCosmetic(line: String): Boolean = cosmeticMarker(line) != null

    private fun cosmeticMarker(line: String): String? {
        var bestIndex = -1
        var bestMarker: String? = null
        for (marker in COSMETIC_MARKERS) {
            val idx = line.indexOf(marker)
            if (idx >= 0 && (bestIndex < 0 || idx < bestIndex)) {
                bestIndex = idx
                bestMarker = marker
            }
        }
        return bestMarker
    }

    private class ParsedNetwork(
        val rule: NetworkRule?,
        val pageWhitelistDomain: String? = null,
        val genericHideDomain: String? = null
    )

    private fun parseNetworkRule(line: String, category: AdCategory): ParsedNetwork? {
        var body = line
        val isException = body.startsWith("@@")
        if (isException) body = body.substring(2)

        // Split options from the pattern. A `/.../` rule is only a regex when it is closed by
        // `/` (optionally followed by `$`); plain paths such as `/ads/banner*.gif` are not.
        var patternPart = body
        var optionPart: String? = null
        val dollar = body.indexOf('$')
        val regexEnd = if (body.startsWith("/") && body.length > 2) body.lastIndexOf('/') else -1
        val isRegexForm = regexEnd > 0 &&
            (regexEnd == body.length - 1 || body[regexEnd + 1] == '$')
        if (isRegexForm) {
            patternPart = body.substring(0, regexEnd + 1)
            optionPart = if (regexEnd + 1 < body.length) {
                body.substring(regexEnd + 1).removePrefix("$").ifEmpty { null }
            } else {
                null
            }
        } else if (dollar >= 0) {
            patternPart = body.substring(0, dollar)
            optionPart = body.substring(dollar + 1)
        }
        if (patternPart.isEmpty()) return null

        val options = parseOptions(optionPart, isException) ?: return null
        if (options.badfilterOnly) return null

        val isRegex = patternPart.length > 2 && patternPart.startsWith("/") && patternPart.endsWith("/")
        if (!isRegex) {
            if (patternPart.any { it.isWhitespace() }) return null
        }
        val pattern = if (isRegex) patternPart else patternPart.lowercase()

        // `@@||site^$document` whitelists an entire page, not just the top level document.
        if (isException && options.documentOnly) {
            val domain = hostOfAnchor(pattern)
            if (domain != null) return ParsedNetwork(null, pageWhitelistDomain = domain)
        }
        if (isException && options.genericHide) {
            val domain = hostOfAnchor(pattern)
            if (domain != null) return ParsedNetwork(null, genericHideDomain = domain)
        }

        var hostAnchor = false
        var startAnchor = false
        var requireEnd = false
        var remaining = pattern

        if (remaining.startsWith("||")) {
            hostAnchor = true
            remaining = remaining.substring(2)
        } else if (remaining.startsWith("|")) {
            startAnchor = true
            remaining = remaining.substring(1)
        }
        if (remaining.endsWith("|")) {
            requireEnd = true
            remaining = remaining.dropLast(1)
        }
        if (remaining.isEmpty()) return null

        var hostOnly: String? = null
        if (hostAnchor && !isRegex) {
            val candidate = remaining.removeSuffix("^")
            if (candidate.isNotEmpty() && candidate.none { it == '*' || it == '^' || it == '/' || it == '$' }) {
                hostOnly = candidate
            }
        }

        val shortcut = if (isRegex || hostOnly != null) "" else AbpPattern.shortcutOf(remaining)
        if (!isRegex && hostOnly == null && shortcut.isEmpty() && options.typeMask == 0 && options.includeDomains == null) {
            // Extremely short generic patterns match so often that honouring them hurts more
            // (false positives on ordinary pages) than it helps.
            if (remaining.length < 5) return null
        }

        val rule = NetworkRule(
            pattern = remaining,
            hostAnchor = hostAnchor,
            startAnchor = startAnchor,
            requireEnd = requireEnd,
            hostOnly = hostOnly,
            shortcut = shortcut,
            category = category,
            isRegex = isRegex,
            includeDomains = options.includeDomains?.toTypedArray(),
            excludeDomains = options.excludeDomains?.toTypedArray(),
            thirdParty = options.thirdParty,
            typeMask = options.typeMask,
            excludedTypeMask = options.excludedTypeMask
        )
        return ParsedNetwork(rule)
    }

    private fun hostOfAnchor(pattern: String): String? {
        if (!pattern.startsWith("||")) return null
        val candidate = pattern.removePrefix("||")
            .substringBefore('^')
            .substringBefore('/')
            .substringBefore('$')
            .lowercase()
        if (candidate.isBlank()) return null
        if (candidate.any { it == '*' || it == '|' }) return null
        return candidate
    }

    private class Options {
        var typeMask = 0
        var excludedTypeMask = 0
        var thirdParty = 0
        var includeDomains: MutableList<String>? = null
        var excludeDomains: MutableList<String>? = null
        var documentOnly = false
        var genericHide = false
        var badfilterOnly = false
    }

    /** Returns null when the rule cannot be enforced safely and must be skipped. */
    private fun parseOptions(raw: String?, isException: Boolean): Options? {
        val options = Options()
        if (raw.isNullOrBlank()) return options
        for (rawToken in raw.split(',')) {
            val token = rawToken.trim()
            if (token.isEmpty()) continue
            val negated = token.startsWith("~")
            val name = (if (negated) token.substring(1) else token)
                .substringBefore('=')
                .lowercase()
            val value = token.substringAfter('=', "")
            when (name) {
                "third-party", "3p" -> options.thirdParty = if (negated) -1 else 1
                "domain", "from" -> parseDomains(value, options)
                "badfilter" -> if (!isException) options.badfilterOnly = true
                "document", "doc" -> if (!negated) options.documentOnly = true
                "generichide", "ghide" -> options.genericHide = true
                "important", "match-case", "dont-vary", "all", "denyallow" -> Unit
                "redirect", "redirect-rule", "empty", "mp4", "noopjs", "noopcss", "nooptext",
                "noopframe", "noopvmap", "noopvast", "1x1-transparent.gif", "2x2-transparent.png",
                "3x2-transparent.png", "32x32-transparent.png", "amazon-adsystem.com" -> Unit
                // Cannot be enforced at the network layer, or would over-block if treated as a
                // plain block rule (popup rules often anchor a whole site).
                "popup", "csp", "removeparam", "rewrite", "method", "elemhide", "ehide",
                "specifichide", "shide", "jsinject", "urltransform", "replace", "inline-script",
                "inline-font", "permissions", "header" -> return null
                "first-party", "1p" -> options.thirdParty = -1
                else -> {
                    val type = RequestType.fromToken(name)
                    if (type != null) {
                        if (negated) {
                            options.excludedTypeMask = options.excludedTypeMask or type.bit
                        } else {
                            options.typeMask = options.typeMask or type.bit
                        }
                    } else if (value.isEmpty()) {
                        // Unknown option with no value: refuse the rule rather than guess.
                        return null
                    }
                }
            }
        }
        if (options.documentOnly) {
            if (options.typeMask == 0) {
                options.typeMask = RequestType.DOCUMENT.bit
            }
        }
        return options
    }

    private fun parseDomains(value: String, options: Options) {
        if (value.isBlank()) return
        for (part in value.split('|')) {
            val entry = part.trim().lowercase()
            if (entry.isEmpty()) continue
            if (entry.startsWith("~")) {
                val domain = entry.substring(1)
                if (domain.isNotEmpty()) {
                    (options.excludeDomains ?: ArrayList<String>().also { options.excludeDomains = it })
                        .add(domain)
                }
            } else {
                (options.includeDomains ?: ArrayList<String>().also { options.includeDomains = it })
                    .add(entry)
            }
        }
    }

    private fun badfilterKey(line: String): String? {
        if (!line.contains('$')) return null
        if (isCosmetic(line)) return null
        val dollar = line.indexOf('$')
        val options = line.substring(dollar + 1).split(',')
        if (options.none { it.equals("badfilter", ignoreCase = true) }) return null
        val remaining = options
            .filterNot { it.equals("badfilter", ignoreCase = true) }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .sorted()
        val pattern = line.substring(0, dollar).lowercase()
        return if (remaining.isEmpty()) pattern else pattern + "$" + remaining.joinToString(",")
    }

    private fun normalizedKey(line: String): String? {
        if (!line.contains('$')) return null
        val dollar = line.indexOf('$')
        val options = line.substring(dollar + 1)
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.equals("badfilter", ignoreCase = true) }
            .sorted()
        val pattern = line.substring(0, dollar).lowercase()
        return if (options.isEmpty()) pattern else pattern + "$" + options.joinToString(",")
    }

    private fun parseCosmeticRule(line: String, marker: String, into: CosmeticRules): Boolean {
        val idx = line.indexOf(marker)
        if (idx < 0) return false
        val domainPart = line.substring(0, idx).trim()
        val selector = line.substring(idx + marker.length).trim()
        if (selector.isEmpty()) return false
        // Style injection, procedural and snippet rules are not supported by plain CSS.
        if (marker.contains('$') || marker.contains('?')) return false
        if (selector.startsWith("+js(")) return false
        if (selector.contains(":has-text(") || selector.contains(":matches-") ||
            selector.contains(":style(") || selector.contains(":remove(") ||
            selector.contains(":remove-attr(") || selector.contains(":remove-class(") ||
            selector.contains(":upward(") || selector.contains(":min-text-length(") ||
            selector.contains(":watch-attr(") || selector.contains(":others(") ||
            selector.startsWith("{") || selector.contains("^^")
        ) {
            return false
        }
        val isException = marker.contains('@')

        val include = ArrayList<String>(2)
        val exclude = ArrayList<String>(2)
        if (domainPart.isNotEmpty()) {
            for (part in domainPart.split(',')) {
                val entry = part.trim().lowercase()
                if (entry.isEmpty()) continue
                if (entry.startsWith("~")) {
                    val d = entry.substring(1)
                    if (d.isNotEmpty()) exclude.add(d)
                } else if (entry.contains('*')) {
                    // Entity rules (`google.*`) are not modelled; skip entirely to stay safe.
                    return false
                } else {
                    include.add(entry)
                }
            }
        }

        if (isException) {
            if (include.isEmpty()) {
                into.genericExceptions.add(selector)
            } else {
                for (domain in include) {
                    into.exceptionsByDomain.getOrPut(domain) { HashSet() }.add(selector)
                }
            }
            return true
        }

        val entry = CosmeticEntry(selector, if (exclude.isEmpty()) null else exclude.toTypedArray())
        if (include.isEmpty()) {
            into.generic.add(entry)
        } else {
            for (domain in include) {
                into.byDomain.getOrPut(domain) { ArrayList(2) }.add(entry)
            }
        }
        return true
    }
}

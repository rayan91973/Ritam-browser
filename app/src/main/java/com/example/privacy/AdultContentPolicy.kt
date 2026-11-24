package com.example.privacy

/**
 * Central policy for pornographic / nude content.
 *
 * Used in three places so the rule set cannot drift:
 *  - typed omnibox input and search queries (navigating is refused with an explanation),
 *  - top-level navigations and redirects,
 *  - sub-resource blocking inside [RitamShieldsEngine].
 *
 * Matching is deliberately conservative: unambiguous terms block on their own, ambiguous words
 * ("sex", "adult") only block with adult context or a short query, and common safe phrases
 * ("sex education", "Essex", "unisex", …) are exempt.
 */
object AdultContentPolicy {

    data class Verdict(val matched: String, val reason: String)

    /** Hosts that serve adult content. Matched against the host and all parent domains. */
    private val blockedDomains = hashSetOf(
        "pornhub.com", "xvideos.com", "xnxx.com", "xhamster.com", "xhamster2.com", "redtube.com",
        "youporn.com", "brazzers.com", "stripchat.com", "chaturbate.com", "livejasmin.com",
        "cam4.com", "bongacams.com", "camsoda.com", "spankbang.com", "eporner.com", "hqporner.com",
        "tnaflix.com", "youjizz.com", "motherless.com", "porn.com", "sex.com", "beeg.com",
        "txxx.com", "tube8.com", "nhentai.net", "hentaihaven.xxx", "hanime.tv", "gelbooru.com",
        "rule34.xxx", "rule34paheal.net", "e621.net", "sankakucomplex.com", "danbooru.donmai.us",
        "onlyfans.com", "fansly.com", "manyvids.com", "fapello.com", "thothub.tv", "erome.com",
        "javhd.com", "javbus.com", "missav.com", "supjav.com", "netflav.com", "jav.guru",
        "adultfriendfinder.com", "ashleymadison.com", "seeking.com", "spicytrash.com",
        "pornpics.com", "sexvid.xxx", "ixxx.com", "alohatube.com", "drtuber.com", "sunporno.com",
        "nuvid.com", "tubegalore.com", "anysex.com", "hclips.com", "porntrex.com", "porngo.com",
        "javmix.tv", "keporn.com", "bokepindonesia.com", "desisex.com", "indiansex.com",
        "hentaimama.io", "nhentai.xxx", "tsumino.com", "fakku.net", "literotica.com",
        "fetlife.com", "bdsmlr.com", "xnxx.tv", "xvideo.xxx", "pornhat.com", "porn300.com",
        "pornone.com", "pornhd.com", "pornheed.com", "porn00.org", "porn2play.com",
        "sexu.com", "sexvid.io", "sexvid.porn", "sexvideo.xxx", "nudevista.com", "nuvid.net"
    )

    /** Terms that unambiguously mean adult content. Word-boundary matched. */
    private val strongTerms = listOf(
        "porn", "porno", "pornography", "xxx", "hentai", "nsfw", "nude", "nudes", "naked",
        "camgirl", "camsex", "sexcam", "sexchat", "sexdoll", "onlyfans", "fansly", "rule34",
        "r34", "xvideos", "xnxx", "xhamster", "pornhub", "redtube", "youporn", "brazzers",
        "milf", "blowjob", "handjob", "cumshot", "creampie", "gangbang", "bukkake",
        "livejasmin", "chaturbate", "bongacams", "stripchat", "spankbang", "eporner",
        "youjizz", "motherless", "hentaihaven", "nhentai", "gelbooru", "javhd", "javbus",
        "missav", "beeg", "txxx", "alohatube", "drtuber", "sunporno", "tubegalore",
        "pornpics", "porntrex", "fapello", "thothub", "erome", "bdsm", "threesome"
    )

    /** Ambiguous terms: only block with adult context, or when the query is short. */
    private val weakTerms = listOf(
        "sex", "adult", "erotic", "erotica", "escort", "stripper", "orgy", "anal", "pussy",
        "tits", "boobs", "fetish", "camsex", "nudity", "sensual", "hookup"
    )

    /** Phrases that must never trip the filter even though they contain a weak/strong word. */
    private val safePhrases = listOf(
        "sex education", "sex ed", "sexual health", "sexual education", "sexual assault",
        "sexism", "sexist", "sextortion", "sex chromosome", "sexually transmitted",
        "sex offender", "sex discrimination", "essex", "sussex", "middlesex", "unisex",
        "sextant", "sextet", "sexagenarian", "adult education", "adult literacy",
        "young adult", "adulting", "naked mole rat"
    )

    private val adultContext = listOf(
        "video", "videos", "tube", "cam", "cams", "pics", "pic", "photos", "photo", "gifs",
        "gif", "movie", "movies", "chat", "story", "stories", "comic", "comics", "manga",
        "game", "games", "massage", "dating", "hookup", "watch", "stream", "hd", "free"
    )

    private fun hostMatches(host: String): String? {
        val clean = host.lowercase().removePrefix("www.").trimEnd('.')
        if (clean.isBlank()) return null
        var current = clean
        while (true) {
            if (blockedDomains.contains(current)) return current
            val dot = current.indexOf('.')
            if (dot < 0) return null
            current = current.substring(dot + 1)
        }
    }

    /** True for a bare term occurrence delimited by non-alphanumeric characters. */
    private fun containsWord(haystack: String, needle: String): Boolean {
        var index = haystack.indexOf(needle)
        while (index >= 0) {
            val before = if (index == 0) ' ' else haystack[index - 1]
            val afterIndex = index + needle.length
            val after = if (afterIndex >= haystack.length) ' ' else haystack[afterIndex]
            if (!before.isLetterOrDigit() && !after.isLetterOrDigit()) return true
            index = haystack.indexOf(needle, index + 1)
        }
        return false
    }

    private fun normalize(text: String): String =
        text.lowercase().replace(Regex("[_\\-+.,;:!?/\\\\|(){}\\[\\]'\"<>@#$%^&*=~`]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    /** Checks typed input: a URL, a bare domain, or a search query. */
    fun checkInput(rawInput: String): Verdict? {
        val input = rawInput.trim()
        if (input.isBlank()) return null
        if (input.startsWith("ritam://") || input.startsWith("about:")) return null

        val urlLike = input.contains("://") || (!input.contains(' ') && input.contains('.'))
        if (urlLike) {
            val url = if (input.contains("://")) input else "https://$input"
            checkUrl(url)?.let { return it }
        }
        return checkText(unwrapSearchQuery(input))
    }

    /** Checks a full URL, including its host and decoded path/query text. */
    fun checkUrl(url: String): Verdict? {
        val lower = url.lowercase()
        if (lower.startsWith("ritam://") || lower.startsWith("about:") || lower.startsWith("data:")) return null
        val host = runCatching { java.net.URI(url).host }.getOrNull().orEmpty()
        hostMatches(host)?.let { return Verdict(it, "This website is blocked as adult content.") }

        val path = runCatching { java.net.URI(url).rawPath.orEmpty() + " " + (java.net.URI(url).rawQuery ?: "") }
            .getOrDefault("")
            .let { java.net.URLDecoder.decode(it, "UTF-8") }
        return checkText("$host $path")
    }

    /** Checks arbitrary text (a search query, title or path). */
    fun checkText(text: String): Verdict? {
        val normalized = normalize(text)
        if (normalized.isBlank()) return null

        // Remove safe phrases first so "naked mole rat" and "sex education" cannot trip the filter,
        // while an additional adult term in the same query is still caught.
        var working = normalized
        for (phrase in safePhrases) {
            if (containsWord(working, phrase)) working = working.replace(phrase, " ")
        }
        if (working.isBlank()) return null

        strongTerms.firstOrNull { containsWord(working, it) }?.let {
            return Verdict(it, "This content is not allowed on this platform.")
        }

        val weakHits = weakTerms.filter { containsWord(working, it) }
        if (weakHits.isEmpty()) return null
        val tokenCount = working.split(' ').count { it.isNotBlank() }
        val hasAdultContext = adultContext.any { containsWord(working, it) }
        return if (tokenCount <= 3 || hasAdultContext) {
            Verdict(weakHits.first(), "This content is not allowed on this platform.")
        } else null
    }

    /** Fast host-only check used by the network blocker. */
    fun isBlockedHost(host: String): Boolean = hostMatches(host) != null

    private val adultHints = listOf(
        "porn", "xxx", "nude", "naked", "hentai", "nsfw", "sex", "adult", "camgirl",
        "onlyfans", "redtube", "xvideo", "brazzers", "bdsm", "erotic", "milf", "rule34"
    )

    /**
     * Cheap substring pre-filter so the network hot path only pays for full policy evaluation when
     * a URL could plausibly be adult content.
     */
    fun hasAdultHint(url: String): Boolean = adultHints.any { url.contains(it, ignoreCase = true) }

    /** Extracts the query from known search-engine URLs so result pages are judged by the query. */
    private fun unwrapSearchQuery(input: String): String {
        val match = Regex("[?&](?:q|query|search|p|text)=([^&]+)", RegexOption.IGNORE_CASE).find(input)
        if (match != null) {
            return runCatching { java.net.URLDecoder.decode(match.groupValues[1], "UTF-8") }.getOrDefault(match.groupValues[1])
        }
        return input
    }
}

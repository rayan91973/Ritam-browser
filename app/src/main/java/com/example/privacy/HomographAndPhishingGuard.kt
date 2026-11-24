package com.example.privacy

import android.net.Uri
import java.net.IDN

data class HomographWarningState(
    val tabId: String,
    val url: String,
    val displayHost: String,
    val asciiHost: String,
    val reason: String
)

object HomographAndPhishingGuard {

    private val protectedBrandRoots = listOf(
        "google", "apple", "microsoft", "paypal", "amazon", "github", "gitlab",
        "chase", "wellsfargo", "bankofamerica", "coinbase", "binance",
        "instagram", "facebook", "whatsapp", "netflix", "proton", "dropbox"
    )

    fun inspectUrl(url: String, tabId: String = ""): HomographWarningState? {
        if (url.isBlank()) return null
        val lower = url.trim().lowercase()
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) return null

        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
        val userInfo = uri.userInfo
        val host = uri.host?.trim()?.lowercase()?.removePrefix("www.") ?: return null
        if (host.isBlank()) return null

        // 1. Userinfo "@" credential masking (e.g. https://paypal.com@evil.com)
        if (!userInfo.isNullOrBlank() && (userInfo.contains(".") || userInfo.length >= 4)) {
            return HomographWarningState(
                tabId = tabId,
                url = url,
                displayHost = "$userInfo@$host",
                asciiHost = host,
                reason = "This URL embeds '@' before the true destination ($host), a common phishing trick to disguise websites."
            )
        }

        // 2. Punycode / IDN "xn--" or mixed non-ASCII Unicode lookalike characters
        val asciiHost = runCatching { IDN.toASCII(host) }.getOrDefault(host).lowercase()
        val unicodeHost = runCatching { IDN.toUnicode(asciiHost) }.getOrDefault(host)
        val hasPunycode = asciiHost.contains("xn--") || host.contains("xn--")
        val hasNonAscii = host.any { it.code > 127 } || unicodeHost.any { it.code > 127 }

        if (hasPunycode || hasNonAscii) {
            val scripts = classifyScripts(unicodeHost)
            val mixedScripts = scripts.size > 1
            val hasConfusable = containsConfusableScriptChars(unicodeHost)
            if (mixedScripts || hasConfusable || hasPunycode) {
                val reason = when {
                    mixedScripts -> "Mixed-alphabet homograph detected ($unicodeHost → $asciiHost). Lookalike Unicode characters may imitate a trusted brand."
                    hasConfusable -> "Non-Latin lookalike script detected ($unicodeHost → $asciiHost)."
                    else -> "Internationalized Punycode domain ($asciiHost) decoded as $unicodeHost."
                }
                return HomographWarningState(
                    tabId = tabId,
                    url = url,
                    displayHost = unicodeHost,
                    asciiHost = asciiHost,
                    reason = reason
                )
            }
        }

        // 3. Common digit/leet lookalike brand typosquatting on suspicious TLDs
        val firstLabel = host.substringBefore(".")
        val suspiciousTlds = listOf(".tk", ".ml", ".ga", ".cf", ".gq", ".xyz", ".top", ".click")
        if (suspiciousTlds.any { host.endsWith(it) }) {
            val normalizedLeet = firstLabel
                .replace('0', 'o')
                .replace('1', 'l')
                .replace('3', 'e')
                .replace('5', 's')
            val matchedBrand = protectedBrandRoots.firstOrNull { brand ->
                normalizedLeet.contains(brand) && !host.endsWith("$brand.com")
            }
            if (matchedBrand != null) {
                return HomographWarningState(
                    tabId = tabId,
                    url = url,
                    displayHost = host,
                    asciiHost = asciiHost,
                    reason = "Suspicious domain closely imitates '$matchedBrand' on a high-risk top-level domain."
                )
            }
        }

        return null
    }

    private fun classifyScripts(host: String): Set<String> {
        val scripts = HashSet<String>()
        for (ch in host) {
            if (!ch.isLetter()) continue
            val block = Character.UnicodeBlock.of(ch)
            when (block) {
                Character.UnicodeBlock.BASIC_LATIN,
                Character.UnicodeBlock.LATIN_1_SUPPLEMENT,
                Character.UnicodeBlock.LATIN_EXTENDED_A,
                Character.UnicodeBlock.LATIN_EXTENDED_B -> scripts.add("LATIN")
                Character.UnicodeBlock.CYRILLIC,
                Character.UnicodeBlock.CYRILLIC_SUPPLEMENTARY -> scripts.add("CYRILLIC")
                Character.UnicodeBlock.GREEK -> scripts.add("GREEK")
                Character.UnicodeBlock.ARMENIAN -> scripts.add("ARMENIAN")
                Character.UnicodeBlock.CHEROKEE -> scripts.add("CHEROKEE")
                else -> scripts.add(block?.toString() ?: "OTHER")
            }
        }
        return scripts
    }

    private fun containsConfusableScriptChars(host: String): Boolean {
        for (ch in host) {
            val block = Character.UnicodeBlock.of(ch)
            if (block == Character.UnicodeBlock.CYRILLIC ||
                block == Character.UnicodeBlock.GREEK ||
                block == Character.UnicodeBlock.CHEROKEE ||
                block == Character.UnicodeBlock.ARMENIAN
            ) {
                return true
            }
        }
        return false
    }
}

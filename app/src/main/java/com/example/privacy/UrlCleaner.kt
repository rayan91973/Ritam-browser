package com.example.privacy

import android.net.Uri

object UrlCleaner {

    private val trackingParams = hashSetOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "utm_id", "utm_source_platform", "utm_creative_format", "utm_marketing_tactic",
        "fbclid", "gclid", "gclsrc", "dclid", "wbraid", "gbraid",
        "msclkid", "yclid", "twclid", "ttclid", "li_fat_id",
        "mc_cid", "mc_eid", "igshid", "igsh", "si",
        "_hsenc", "_hsmi", "hsctatracking", "vero_id",
        "mkt_tok", "spm", "scm", "ref_src", "ref_url",
        "oly_anon_id", "oly_enc_id", "__s", "s_kwcid"
    )

    data class CleanResult(
        val cleanedUrl: String,
        val removedCount: Int
    )

    data class UnwrapResult(
        val unwrappedUrl: String,
        val wrapperHost: String?,
        val wasUnwrapped: Boolean
    )

    private val knownRedirectWrapperParams = listOf(
        "url", "u", "q", "dest", "destination", "target", "redirect", "redirect_url", "to", "r"
    )

    private val knownRedirectWrapperHosts = hashSetOf(
        "l.facebook.com", "lm.facebook.com", "m.facebook.com",
        "l.instagram.com", "out.reddit.com", "click.redditmail.com",
        "safelinks.protection.outlook.com", "vk.com", "youtube.com",
        "www.youtube.com", "www.google.com", "google.com"
    )

    fun unwrapRedirectUrl(rawUrl: String): UnwrapResult {
        var current = rawUrl.trim()
        var wrapperHost: String? = null
        var hops = 0
        while (hops < 3) {
            val uri = runCatching { Uri.parse(current) }.getOrNull() ?: break
            val host = uri.host?.lowercase() ?: break
            val path = uri.path?.lowercase() ?: ""
            val isExplicitWrapper = host in knownRedirectWrapperHosts && (
                path.startsWith("/url") ||
                    path.startsWith("/l.php") ||
                    path.startsWith("/away.php") ||
                    path.startsWith("/redirect") ||
                    host.startsWith("l.") ||
                    host.startsWith("lm.") ||
                    host.startsWith("out.") ||
                    host.contains("safelinks")
                )
            var extracted: String? = null
            if (isExplicitWrapper) {
                for (param in knownRedirectWrapperParams) {
                    val candidate = uri.getQueryParameter(param)?.trim()
                    if (!candidate.isNullOrBlank() &&
                        (candidate.startsWith("http://", true) || candidate.startsWith("https://", true))
                    ) {
                        val candidateHost = runCatching { Uri.parse(candidate).host?.lowercase() }.getOrNull()
                        if (candidateHost != null && candidateHost != host) {
                            extracted = candidate
                            break
                        }
                    }
                }
            } else {
                // Also catch explicit ?redirect=https://... or ?redirect_url=https://... wrappers
                for (param in listOf("redirect", "redirect_url", "dest", "destination")) {
                    val candidate = uri.getQueryParameter(param)?.trim()
                    if (!candidate.isNullOrBlank() &&
                        (candidate.startsWith("http://", true) || candidate.startsWith("https://", true))
                    ) {
                        val candidateHost = runCatching { Uri.parse(candidate).host?.lowercase() }.getOrNull()
                        if (candidateHost != null && candidateHost != host) {
                            extracted = candidate
                            break
                        }
                    }
                }
            }
            if (extracted == null) break
            if (wrapperHost == null) wrapperHost = host.removePrefix("www.")
            current = extracted
            hops++
        }
        return UnwrapResult(
            unwrappedUrl = current,
            wrapperHost = wrapperHost,
            wasUnwrapped = hops > 0
        )
    }

    fun cleanUrl(rawUrl: String): CleanResult {
        if (rawUrl.isBlank() || (!rawUrl.startsWith("http://", true) && !rawUrl.startsWith("https://", true))) {
            return CleanResult(rawUrl, 0)
        }
        return runCatching {
            val uri = Uri.parse(rawUrl)
            val paramNames = uri.queryParameterNames
            if (paramNames.isEmpty()) {
                return@runCatching CleanResult(rawUrl, 0)
            }
            val builder = uri.buildUpon().clearQuery()
            var removed = 0
            paramNames.forEach { name ->
                val lower = name.lowercase()
                if (lower in trackingParams || lower.startsWith("utm_")) {
                    removed++
                } else {
                    uri.getQueryParameters(name).forEach { value ->
                        builder.appendQueryParameter(name, value)
                    }
                }
            }
            if (removed == 0) {
                CleanResult(rawUrl, 0)
            } else {
                CleanResult(builder.build().toString(), removed)
            }
        }.getOrElse {
            CleanResult(rawUrl, 0)
        }
    }

    fun stripIfEnabled(rawUrl: String, enabled: Boolean): String {
        return if (enabled) cleanUrl(rawUrl).cleanedUrl else rawUrl
    }
}

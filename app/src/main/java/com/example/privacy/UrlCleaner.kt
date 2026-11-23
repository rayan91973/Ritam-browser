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

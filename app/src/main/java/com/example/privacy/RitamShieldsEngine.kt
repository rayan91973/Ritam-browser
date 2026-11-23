package com.example.privacy

import android.net.Uri
import android.webkit.WebResourceResponse
import com.example.data.local.WebsiteSettingsEntity
import com.example.data.preferences.BrowserPreferences
import com.example.data.repository.BrowserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap

enum class BlockCategory {
    AD,
    TRACKER,
    MALICIOUS,
    ALLOWED
}

data class BlockDecision(
    val isBlocked: Boolean,
    val category: BlockCategory,
    val matchedRule: String? = null,
    val estimatedBytesSaved: Long = 0L
) {
    fun toEmptyWebResponse(): WebResourceResponse = WebResourceResponse(
        "text/plain",
        "utf-8",
        204,
        "Blocked by Ritam Shields",
        emptyMap(),
        ByteArrayInputStream(ByteArray(0))
    )
}

data class SessionShieldMetrics(
    val adsBlocked: Long = 0L,
    val trackersBlocked: Long = 0L,
    val bytesSaved: Long = 0L,
    val lastBlockedDomain: String? = null
)

class RitamShieldsEngine(
    private val repository: BrowserRepository,
    private val dohResolver: DohResolver? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var currentPrefs: BrowserPreferences = BrowserPreferences()

    @Volatile
    private var easyListEnabled: Boolean = true

    @Volatile
    private var easyPrivacyEnabled: Boolean = true

    @Volatile
    private var peterLoweEnabled: Boolean = true

    @Volatile
    private var threatIntelEnabled: Boolean = true

    private val siteExceptions = ConcurrentHashMap<String, WebsiteSettingsEntity>()
    private val customBlockDomains = HashSet<String>()
    private val customAllowDomains = HashSet<String>()
    private val customUrlTokens = ArrayList<String>()

    private val _sessionMetrics = MutableStateFlow(SessionShieldMetrics())
    val sessionMetrics: StateFlow<SessionShieldMetrics> = _sessionMetrics.asStateFlow()

    // Curated EasyList & Peter Lowe ad-serving domains
    private val adDomains = hashSetOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adservice.google.com", "pagead2.googlesyndication.com",
        "amazon-adsystem.com", "adnxs.com", "adsrvr.org", "criteo.com",
        "criteo.net", "taboola.com", "outbrain.com", "pubmatic.com",
        "rubiconproject.com", "openx.net", "casalemedia.com", "moatads.com",
        "advertising.com", "admob.com", "ads.yahoo.com", "ads.twitter.com",
        "adform.net", "bidswitch.net", "media.net", "revcontent.com",
        "mgid.com", "zemanta.com", "yieldmo.com", "smartadserver.com",
        "2mdn.net", "flashtalking.com", "serving-sys.com", "tradedoubler.com",
        "zedo.com", "popads.net", "propellerads.com", "adcolony.com",
        "applovin.com", "unityads.unity3d.com", "vungle.com", "inmobi.com",
        "adroll.com", "ads.linkedin.com", "adskeeper.com", "buysellads.com",
        "carbonads.net", "doubleverify.com", "everesttech.net", "exponential.com",
        "gumgum.com", "indexww.com", "lijit.com", "mathtag.com", "nativo.com",
        "outbrainimg.com", "sharethrough.com", "sonobi.com", "spotxchange.com",
        "tapad.com", "teads.tv", "triplelift.com", "undertone.com", "adsafeprotected.com"
    )

    // Curated EasyPrivacy tracker & fingerprinting domains
    private val trackerDomains = hashSetOf(
        "google-analytics.com", "ssl.google-analytics.com", "googletagmanager.com",
        "analytics.google.com", "scorecardresearch.com", "quantserve.com",
        "hotjar.com", "mixpanel.com", "segment.io", "segment.com",
        "amplitude.com", "fullstory.com", "logrocket.com", "mouseflow.com",
        "crazyegg.com", "clarity.ms", "newrelic.com", "nr-data.net",
        "chartbeat.com", "chartbeat.net", "omtrdc.net", "demdex.net",
        "krxd.net", "bluekai.com", "exelator.com", "addthis.com",
        "sharethis.com", "branch.io", "app.link", "adjust.com",
        "appsflyer.com", "kochava.com", "pixel.facebook.com",
        "connect.facebook.net", "analytics.twitter.com", "tr.snapchat.com",
        "analytics.tiktok.com", "bat.bing.com", "px.ads.linkedin.com",
        "sb.scorecardresearch.com", "fpjs.io", "fingerprintjs.com"
    )

    // Known threat-intelligence test / malicious domains (never flags ordinary sites)
    private val maliciousDomains = hashSetOf(
        "malware.testing.google.test",
        "testsafebrowsing.appspot.com",
        "phishing-example.invalid",
        "ianfette.org",
        "evil-tracker-demo.invalid"
    )

    private val adUrlSubstrings = listOf(
        "/pagead/", "/adsense/", "/adserver/", "/doubleclick/",
        "banner_ad", "popunder", "sponsored-ads", "/prebid/"
    )

    private val trackerUrlSubstrings = listOf(
        "/gtag/js", "/analytics.js", "/fbevents.js", "/pixel?",
        "/collect?v=", "fingerprint.min.js", "/telemetry/beacon"
    )

    init {
        scope.launch {
            repository.customFilterRules.collect { rules ->
                synchronized(customBlockDomains) {
                    customBlockDomains.clear()
                    customAllowDomains.clear()
                    customUrlTokens.clear()
                    rules.filter { it.enabled }.forEach { entity ->
                        parseAdblockRule(entity.rule)
                    }
                }
            }
        }
        scope.launch {
            repository.filterLists.collect { lists ->
                lists.forEach { list ->
                    when {
                        list.name.contains("EasyList", ignoreCase = true) -> easyListEnabled = list.enabled
                        list.name.contains("EasyPrivacy", ignoreCase = true) -> easyPrivacyEnabled = list.enabled
                        list.name.contains("Peter Lowe", ignoreCase = true) -> peterLoweEnabled = list.enabled
                        list.name.contains("Threat", ignoreCase = true) -> threatIntelEnabled = list.enabled
                    }
                }
            }
        }
        scope.launch {
            repository.websiteSettings.collect { list ->
                siteExceptions.clear()
                list.forEach { siteExceptions[it.domain.lowercase()] = it }
            }
        }
    }

    fun updatePreferences(preferences: BrowserPreferences) {
        currentPrefs = preferences
    }

    private fun parseAdblockRule(rawRule: String) {
        val rule = rawRule.trim().lowercase()
        if (rule.isEmpty() || rule.startsWith("!") || rule.startsWith("[")) return
        when {
            rule.startsWith("@@||") -> {
                val domain = rule.removePrefix("@@||").substringBefore("^").substringBefore("/")
                if (domain.isNotBlank()) customAllowDomains.add(domain)
            }
            rule.startsWith("||") -> {
                val domain = rule.removePrefix("||").substringBefore("^").substringBefore("/")
                if (domain.isNotBlank()) customBlockDomains.add(domain)
            }
            else -> customUrlTokens.add(rule)
        }
    }

    fun isMaliciousUrl(url: String): Boolean {
        if (!currentPrefs.maliciousProtectionEnabled || !threatIntelEnabled) return false
        val host = extractHost(url) ?: return false
        return matchesDomainSet(host, maliciousDomains)
    }

    fun maybeUpgradeToHttps(url: String, pageDomain: String?): String {
        val siteSetting = pageDomain?.lowercase()?.let { siteExceptions[it] }
        val httpsOnly = siteSetting?.httpsOnly ?: currentPrefs.httpsOnlyEnabled
        if (!httpsOnly) return url
        if (!url.startsWith("http://", ignoreCase = true)) return url
        val host = extractHost(url) ?: return url
        if (host == "localhost" || host == "127.0.0.1" || host.startsWith("192.168.") || host.startsWith("10.")) {
            return url
        }
        return "https://" + url.removePrefix("http://")
    }

    fun isShieldsEnabledForDomain(domain: String): Boolean {
        val setting = siteExceptions[domain.lowercase()]
        return setting?.shieldsEnabled ?: true
    }

    fun shouldApplyCosmeticAdBlocker(pageDomain: String): Boolean {
        if (!currentPrefs.adBlockingEnabled || !currentPrefs.cosmeticAdBlockingEnabled) return false
        val clean = pageDomain.lowercase().removePrefix("www.")
        return isShieldsEnabledForDomain(clean)
    }

    fun shouldBlockCookieBanners(pageDomain: String): Boolean {
        if (!currentPrefs.blockCookieBanners) return false
        val clean = pageDomain.lowercase().removePrefix("www.")
        return isShieldsEnabledForDomain(clean)
    }

    fun getZoomForDomain(pageDomain: String): Int {
        val clean = pageDomain.lowercase().removePrefix("www.")
        return siteExceptions[clean]?.zoomPercent?.coerceIn(50, 200) ?: 100
    }

    fun shouldForceDarkForDomain(pageDomain: String): Boolean {
        val clean = pageDomain.lowercase().removePrefix("www.")
        val override = siteExceptions[clean]?.forceDarkOverride ?: -1
        return when (override) {
            1 -> true
            0 -> false
            else -> currentPrefs.forceWebDarkMode
        }
    }

    fun cosmeticAdBlockerScript(): String = """
        (function() {
            try {
                if (document.getElementById('ritam-adblock-style')) return;
                var style = document.createElement('style');
                style.id = 'ritam-adblock-style';
                style.textContent = 'ins.adsbygoogle, [id^="google_ads_"], [id^="div-gpt-ad"], .adsbygoogle, .ad-banner, .ad-container, .sponsored-ad, [data-ad-slot], [data-google-query-id], iframe[src*="doubleclick.net"], iframe[src*="googlesyndication.com"] { display: none !important; visibility: hidden !important; height: 0 !important; min-height: 0 !important; }';
                (document.head || document.documentElement).appendChild(style);
            } catch (e) {}
        })();
    """.trimIndent()

    fun cookieConsentBlockerScript(): String = """
        (function() {
            try {
                if (!document.getElementById('ritam-cookie-block-style')) {
                    var style = document.createElement('style');
                    style.id = 'ritam-cookie-block-style';
                    style.textContent = '#onetrust-banner-sdk, #onetrust-consent-sdk, .onetrust-pc-dark-filter, #CybotCookiebotDialog, .cc-window, .fc-consent-root, .qc-cmp2-container, [id*="cookie-banner"], [class*="cookie-banner"], [id*="consent-banner"], [class*="consent-banner"], [aria-label*="cookie" i][role="dialog"] { display: none !important; visibility: hidden !important; opacity: 0 !important; pointer-events: none !important; }';
                    (document.head || document.documentElement).appendChild(style);
                }
                var rejectSelectors = [
                    '#onetrust-reject-all-handler',
                    '#CybotCookiebotDialogBodyButtonDecline',
                    'button[aria-label*="Reject" i]',
                    'button[id*="reject" i]',
                    'button[class*="reject" i]'
                ];
                for (var i = 0; i < rejectSelectors.length; i++) {
                    var btn = document.querySelector(rejectSelectors[i]);
                    if (btn && typeof btn.click === 'function') { btn.click(); break; }
                }
            } catch (e) {}
        })();
    """.trimIndent()

    fun forceWebDarkScript(enabled: Boolean): String = if (enabled) {
        """
        (function() {
            try {
                var existing = document.getElementById('ritam-force-dark-style');
                if (!existing) {
                    var s = document.createElement('style');
                    s.id = 'ritam-force-dark-style';
                    s.textContent = 'html { filter: invert(0.92) hue-rotate(180deg) !important; background-color: #111413 !important; } img, video, picture, svg, canvas, iframe { filter: invert(1) hue-rotate(180deg) !important; }';
                    (document.head || document.documentElement).appendChild(s);
                }
            } catch (e) {}
        })();
        """.trimIndent()
    } else {
        """
        (function() {
            try {
                var el = document.getElementById('ritam-force-dark-style');
                if (el && el.parentNode) el.parentNode.removeChild(el);
            } catch (e) {}
        })();
        """.trimIndent()
    }

    fun shouldInterceptRequest(
        requestUrl: String,
        pageDomain: String,
        isPrivate: Boolean
    ): BlockDecision {
        val host = extractHost(requestUrl) ?: return BlockDecision(false, BlockCategory.ALLOWED)
        val cleanPageDomain = pageDomain.lowercase().removePrefix("www.")

        // Per-website exception check
        val siteSetting = siteExceptions[cleanPageDomain]
        if (siteSetting != null && !siteSetting.shieldsEnabled) {
            return BlockDecision(false, BlockCategory.ALLOWED)
        }

        // Custom exception rule @@||domain^
        synchronized(customBlockDomains) {
            if (matchesDomainSet(host, customAllowDomains)) {
                return BlockDecision(false, BlockCategory.ALLOWED)
            }
            if (matchesDomainSet(host, customBlockDomains)) {
                return recordBlock(cleanPageDomain, host, BlockCategory.AD, "custom-domain:$host", 28_000L, isPrivate)
            }
            val lowerUrl = requestUrl.lowercase()
            val matchedToken = customUrlTokens.firstOrNull { lowerUrl.contains(it) }
            if (matchedToken != null) {
                return recordBlock(cleanPageDomain, host, BlockCategory.AD, "custom-rule:$matchedToken", 24_000L, isPrivate)
            }
        }

        // Never block first-party main host resources unless explicitly in custom rules
        if (host == cleanPageDomain || host.endsWith(".$cleanPageDomain")) {
            return BlockDecision(false, BlockCategory.ALLOWED)
        }

        // Ad blocking check
        if (currentPrefs.adBlockingEnabled && (easyListEnabled || peterLoweEnabled)) {
            if (matchesDomainSet(host, adDomains)) {
                return recordBlock(cleanPageDomain, host, BlockCategory.AD, "domain:$host", 48_000L, isPrivate)
            }
            val lowerUrl = requestUrl.lowercase()
            val adToken = adUrlSubstrings.firstOrNull { lowerUrl.contains(it) }
            if (adToken != null) {
                return recordBlock(cleanPageDomain, host, BlockCategory.AD, "url:$adToken", 36_000L, isPrivate)
            }
        }

        // Tracker blocking check
        if (currentPrefs.trackerBlockingEnabled && (easyPrivacyEnabled || peterLoweEnabled)) {
            if (matchesDomainSet(host, trackerDomains)) {
                return recordBlock(cleanPageDomain, host, BlockCategory.TRACKER, "tracker:$host", 18_500L, isPrivate)
            }
            val lowerUrl = requestUrl.lowercase()
            val trackerToken = trackerUrlSubstrings.firstOrNull { lowerUrl.contains(it) }
            if (trackerToken != null) {
                return recordBlock(cleanPageDomain, host, BlockCategory.TRACKER, "tracker-url:$trackerToken", 14_200L, isPrivate)
            }
        }

        // Encrypted DNS (DoH) blocklist enforcement (cached check + async warm-up)
        val dnsProv = currentPrefs.dnsProvider
        if (dnsProv != DnsProvider.SYSTEM && dohResolver != null) {
            if (dohResolver.isCachedBlockedByDns(host, dnsProv)) {
                return recordBlock(cleanPageDomain, host, BlockCategory.TRACKER, "doh:${dnsProv.name}", 12_000L, isPrivate)
            }
            if (dnsProv == DnsProvider.ADGUARD || dnsProv == DnsProvider.QUAD9) {
                scope.launch {
                    dohResolver.resolveHost(host, dnsProv, currentPrefs.customDohUrl)
                }
            }
        }

        return BlockDecision(false, BlockCategory.ALLOWED)
    }

    private fun recordBlock(
        pageDomain: String,
        blockedHost: String,
        category: BlockCategory,
        rule: String,
        estimatedBytes: Long,
        isPrivate: Boolean
    ): BlockDecision {
        val current = _sessionMetrics.value
        _sessionMetrics.value = current.copy(
            adsBlocked = if (category == BlockCategory.AD) current.adsBlocked + 1 else current.adsBlocked,
            trackersBlocked = if (category == BlockCategory.TRACKER) current.trackersBlocked + 1 else current.trackersBlocked,
            bytesSaved = current.bytesSaved + estimatedBytes,
            lastBlockedDomain = blockedHost
        )

        if (!isPrivate && pageDomain.isNotBlank()) {
            val typeStr = if (category == BlockCategory.TRACKER) "TRACKER" else "AD"
            scope.launch {
                repository.recordBlockedStat(pageDomain, typeStr, estimatedBytes)
            }
        }

        return BlockDecision(
            isBlocked = true,
            category = category,
            matchedRule = rule,
            estimatedBytesSaved = estimatedBytes
        )
    }

    private fun extractHost(url: String): String? {
        return runCatching {
            Uri.parse(url).host?.lowercase()?.removePrefix("www.")
        }.getOrNull()
    }

    private fun matchesDomainSet(host: String, set: Set<String>): Boolean {
        if (set.contains(host)) return true
        var idx = host.indexOf('.')
        while (idx >= 0 && idx + 1 < host.length) {
            val parent = host.substring(idx + 1)
            if (set.contains(parent)) return true
            idx = host.indexOf('.', idx + 1)
        }
        return false
    }
}

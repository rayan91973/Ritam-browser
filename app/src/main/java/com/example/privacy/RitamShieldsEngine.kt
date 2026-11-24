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
    private val customCssSelectors = ArrayList<Pair<String?, String>>()

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

    private val adultDomains = hashSetOf(
        "pornhub.com", "xvideos.com", "xnxx.com", "redtube.com", "youporn.com",
        "brazzers.com", "stripchat.com", "camsoda.com", "livejasmin.com",
        "chaturbate.com", "adultfriendfinder.com", "xhamster.com", "eporner.com",
        "spankbang.com", "hqporner.com", "tnaflix.com", "nuvid.com", "sunporno.com",
        "tubegalore.com", "motherless.com", "anysex.com", "drtuber.com", "youjizz.com",
        "hentaihaven.red", "nhentai.net", "gelbooru.com", "rule34.xxx"
    )

    private val adUrlSubstrings = listOf(
        "/pagead/", "/adsense/", "/adserver/", "/doubleclick/",
        "banner_ad", "popunder", "sponsored-ads", "/prebid/"
    )

    private val trackerUrlSubstrings = listOf(
        "/gtag/js", "/analytics.js", "/fbevents.js", "/pixel?",
        "/collect?v=", "fingerprint.min.js", "/telemetry/beacon"
    )

    private val adultUrlSubstrings = listOf(
        "/porn", "/sex", "/nude", "/xxx", "/adult", "/cammodel",
        "/nsfw", "/erotic", "pornstar", "stripchat", "chaturbate"
    )

    init {
        scope.launch {
            repository.customFilterRules.collect { rules ->
                synchronized(customBlockDomains) {
                    customBlockDomains.clear()
                    customAllowDomains.clear()
                    customUrlTokens.clear()
                    customCssSelectors.clear()
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
        val trimmed = rawRule.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("!") || trimmed.startsWith("[")) return
        val cosmeticIdx = trimmed.indexOf("##")
        if (cosmeticIdx >= 0) {
            val domainPart = trimmed.substring(0, cosmeticIdx).trim().lowercase().takeIf { it.isNotBlank() }
            val selector = trimmed.substring(cosmeticIdx + 2).trim()
            if (selector.isNotEmpty()) {
                customCssSelectors.add(domainPart to selector)
            }
            return
        }
        val rule = trimmed.lowercase()
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

    fun isEphemeralContainerForDomain(domain: String): Boolean {
        val clean = domain.lowercase().removePrefix("www.")
        val siteSetting = if (clean.isNotBlank()) siteExceptions[clean] else null
        return siteSetting?.isEphemeralContainer ?: currentPrefs.ephemeralContainerDefault
    }

    fun getSiteSettingForDomain(domain: String): WebsiteSettingsEntity? {
        val clean = domain.lowercase().removePrefix("www.")
        return if (clean.isNotBlank()) siteExceptions[clean] else null
    }

    fun strictReferrerScript(): String = """
        (function() {
            try {
                if (!document.getElementById('ritam-strict-referrer')) {
                    var meta = document.querySelector('meta[name="referrer"]');
                    if (!meta) {
                        meta = document.createElement('meta');
                        meta.name = 'referrer';
                        (document.head || document.documentElement).appendChild(meta);
                    }
                    meta.id = 'ritam-strict-referrer';
                    meta.setAttribute('content', 'strict-origin-when-cross-origin');
                }
                var anchors = document.querySelectorAll('a[href^="http"]');
                for (var i = 0; i < anchors.length; i++) {
                    var a = anchors[i];
                    if (a.hostname && a.hostname !== location.hostname) {
                        var rel = (a.getAttribute('rel') || '').toLowerCase();
                        if (rel.indexOf('noreferrer') < 0) {
                            a.setAttribute('rel', (rel + ' noreferrer noopener').trim());
                        }
                    }
                }
            } catch (e) {}
        })();
    """.trimIndent()

    fun webRtcLeakBlockerScript(): String = """
        (function() {
            try {
                if (window.__ritamWebRtcGuarded) return;
                window.__ritamWebRtcGuarded = true;
                var notify = function(label) {
                    if (window.RitamBridge && window.RitamBridge.onPrivacyApiBlocked) {
                        window.RitamBridge.onPrivacyApiBlocked(location.hostname || '', label);
                    }
                };
                var sanitizeCandidate = function(candidateStr) {
                    if (!candidateStr) return true;
                    if (candidateStr.indexOf('.local') >= 0) return false;
                    if (/\b(?:10\.\d{1,3}|192\.168|172\.(?:1[6-9]|2\d|3[01]))\.\d{1,3}\.\d{1,3}\b/.test(candidateStr)) {
                        return false;
                    }
                    return true;
                };
                var wrapRtc = function(OrigRtc) {
                    if (!OrigRtc) return OrigRtc;
                    var Wrapped = function(config, constraints) {
                        var pc = new OrigRtc(config, constraints);
                        var origAddEventListener = pc.addEventListener.bind(pc);
                        pc.addEventListener = function(type, listener, options) {
                            if (type === 'icecandidate' && typeof listener === 'function') {
                                var wrappedListener = function(ev) {
                                    if (ev && ev.candidate && ev.candidate.candidate && !sanitizeCandidate(ev.candidate.candidate)) {
                                        notify('WebRTC Local IP Probe');
                                        return;
                                    }
                                    listener.call(this, ev);
                                };
                                return origAddEventListener(type, wrappedListener, options);
                            }
                            return origAddEventListener(type, listener, options);
                        };
                        var userHandler = null;
                        try {
                            Object.defineProperty(pc, 'onicecandidate', {
                                configurable: true,
                                get: function() { return userHandler; },
                                set: function(fn) {
                                    userHandler = fn;
                                    pc.addEventListener('icecandidate', function(ev) {
                                        if (typeof userHandler === 'function') userHandler.call(pc, ev);
                                    });
                                }
                            });
                        } catch (e) {}
                        return pc;
                    };
                    Wrapped.prototype = OrigRtc.prototype;
                    return Wrapped;
                };
                if (window.RTCPeerConnection) {
                    window.RTCPeerConnection = wrapRtc(window.RTCPeerConnection);
                }
                if (window.webkitRTCPeerConnection) {
                    window.webkitRTCPeerConnection = wrapRtc(window.webkitRTCPeerConnection);
                }
            } catch (e) {}
        })();
    """.trimIndent()

    fun covertApiLockdownScript(): String = """
        (function() {
            try {
                if (window.__ritamCovertLocked) return;
                window.__ritamCovertLocked = true;
                var notify = function(label) {
                    if (window.RitamBridge && window.RitamBridge.onPrivacyApiBlocked) {
                        window.RitamBridge.onPrivacyApiBlocked(location.hostname || '', label);
                    }
                };
                if (navigator.sendBeacon) {
                    var origBeacon = navigator.sendBeacon.bind(navigator);
                    navigator.sendBeacon = function(url, data) {
                        try {
                            var target = new URL(url, location.href);
                            if (target.hostname && target.hostname !== location.hostname) {
                                notify('Cross-Site Telemetry Beacon');
                                return true;
                            }
                        } catch (e) {}
                        return origBeacon(url, data);
                    };
                }
                if (navigator.getBattery) {
                    navigator.getBattery = function() {
                        notify('Battery API Fingerprinting');
                        return Promise.reject(new DOMException('Blocked by Ritam Privacy Shield', 'NotAllowedError'));
                    };
                }
                if (navigator.vibrate) {
                    navigator.vibrate = function() {
                        notify('Silent Haptic Probe');
                        return false;
                    };
                }
                var origWinAddEvent = window.addEventListener.bind(window);
                window.addEventListener = function(type, listener, options) {
                    if (type === 'devicemotion' || type === 'deviceorientation' || type === 'deviceorientationabsolute') {
                        notify('Motion/Orientation Sensor Probe');
                        return;
                    }
                    return origWinAddEvent(type, listener, options);
                };
                if (navigator.clipboard && navigator.clipboard.read) {
                    navigator.clipboard.read = function() {
                        notify('Silent Clipboard Read');
                        return Promise.reject(new DOMException('Clipboard read blocked', 'NotAllowedError'));
                    };
                }
                var openCount = 0;
                var lastReset = Date.now();
                var origOpen = window.open;
                window.open = function(u, t, f) {
                    var now = Date.now();
                    if (now - lastReset > 4000) { openCount = 0; lastReset = now; }
                    openCount++;
                    if (openCount > 2) {
                        notify('Popup Flood Attempt');
                        return null;
                    }
                    return origOpen ? origOpen.apply(window, arguments) : null;
                };
            } catch (e) {}
        })();
    """.trimIndent()

    fun cosmeticAdBlockerScript(pageDomain: String = ""): String {
        val cleanHost = pageDomain.lowercase().removePrefix("www.")
        val extraSelectors = synchronized(customBlockDomains) {
            customCssSelectors.mapNotNull { (domain, selector) ->
                if (domain == null || cleanHost == domain || cleanHost.endsWith(".$domain")) selector else null
            }.distinct()
        }
        val baseSelectors = "ins.adsbygoogle, [id^=\"google_ads_\"], [id^=\"div-gpt-ad\"], .adsbygoogle, .ad-banner, .ad-container, .sponsored-ad, [data-ad-slot], [data-google-query-id], iframe[src*=\"doubleclick.net\"], iframe[src*=\"googlesyndication.com\"]"
        val combined = if (extraSelectors.isEmpty()) baseSelectors else "$baseSelectors, ${extraSelectors.joinToString(", ")}"
        val escapedCss = "$combined { display: none !important; visibility: hidden !important; height: 0 !important; min-height: 0 !important; }"
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", " ")
        return """
        (function() {
            try {
                var style = document.getElementById('ritam-adblock-style');
                if (!style) {
                    style = document.createElement('style');
                    style.id = 'ritam-adblock-style';
                    (document.head || document.documentElement).appendChild(style);
                }
                style.textContent = '$escapedCss';
            } catch (e) {}
        })();
        """.trimIndent()
    }

    fun fingerprintRandomizerScript(): String = """
        (function() {
            try {
                if (window.__ritamFpRandomized) return;
                window.__ritamFpRandomized = true;
                var noise = Math.floor(Math.random() * 5) - 2;
                var origToDataURL = HTMLCanvasElement.prototype.toDataURL;
                HTMLCanvasElement.prototype.toDataURL = function() {
                    try {
                        var ctx = this.getContext('2d');
                        if (ctx && this.width > 0 && this.height > 0) {
                            ctx.fillStyle = 'rgba(128,128,128,0.01)';
                            ctx.fillRect(0, 0, 1, 1);
                        }
                    } catch (e) {}
                    return origToDataURL.apply(this, arguments);
                };
                if (window.CanvasRenderingContext2D) {
                    var origGetImageData = CanvasRenderingContext2D.prototype.getImageData;
                    CanvasRenderingContext2D.prototype.getImageData = function() {
                        var img = origGetImageData.apply(this, arguments);
                        if (img && img.data && img.data.length > 3) {
                            img.data[0] = (img.data[0] + noise + 256) % 256;
                        }
                        return img;
                    };
                }
                var fakeCores = [4, 6, 8][Math.floor(Math.random() * 3)];
                try {
                    Object.defineProperty(navigator, 'hardwareConcurrency', { get: function() { return fakeCores; } });
                } catch (e) {}
                if (window.AudioBuffer && AudioBuffer.prototype.getChannelData) {
                    var origAudio = AudioBuffer.prototype.getChannelData;
                    AudioBuffer.prototype.getChannelData = function() {
                        var buf = origAudio.apply(this, arguments);
                        if (buf && buf.length > 0) {
                            buf[0] = buf[0] + (noise * 1e-7);
                        }
                        return buf;
                    };
                }
            } catch (e) {}
        })();
    """.trimIndent()

    fun elementPickerScript(): String = """
        (function() {
            try {
                if (window.__ritamPickerActive) return;
                window.__ritamPickerActive = true;
                var buildSelector = function(el) {
                    if (!el || el === document.body || el === document.documentElement) return '';
                    if (el.id && /^[a-zA-Z_-][a-zA-Z0-9_-]*$/.test(el.id)) return '#' + el.id;
                    var tag = el.tagName.toLowerCase();
                    if (el.classList && el.classList.length > 0) {
                        var cls = Array.prototype.slice.call(el.classList, 0, 2)
                            .filter(function(c) { return /^[a-zA-Z_-][a-zA-Z0-9_-]*$/.test(c); });
                        if (cls.length > 0) return tag + '.' + cls.join('.');
                    }
                    return tag;
                };
                var handler = function(ev) {
                    ev.preventDefault();
                    ev.stopPropagation();
                    ev.stopImmediatePropagation();
                    document.removeEventListener('click', handler, true);
                    window.__ritamPickerActive = false;
                    var sel = buildSelector(ev.target);
                    if (sel) {
                        ev.target.style.setProperty('display', 'none', 'important');
                        if (window.RitamBridge && window.RitamBridge.onElementPicked) {
                            window.RitamBridge.onElementPicked(sel);
                        }
                    } else if (window.RitamBridge && window.RitamBridge.onElementPicked) {
                        window.RitamBridge.onElementPicked('');
                    }
                };
                document.addEventListener('click', handler, true);
            } catch (e) {}
        })();
    """.trimIndent()

    fun clipboardGuardScript(): String = """
        (function() {
            try {
                if (window.__ritamClipboardGuarded) return;
                window.__ritamClipboardGuarded = true;
                if (navigator.clipboard && navigator.clipboard.readText) {
                    var origRead = navigator.clipboard.readText.bind(navigator.clipboard);
                    navigator.clipboard.readText = function() {
                        if (window.RitamBridge && window.RitamBridge.onClipboardReadAttempt) {
                            window.RitamBridge.onClipboardReadAttempt(location.hostname || '');
                        }
                        return origRead();
                    };
                }
            } catch (e) {}
        })();
    """.trimIndent()

    fun feedDiscoveryScript(): String = """
        (function() {
            try {
                var links = document.querySelectorAll('link[rel*="alternate"]');
                for (var i = 0; i < links.length; i++) {
                    var t = (links[i].getAttribute('type') || '').toLowerCase();
                    if (t.indexOf('rss') >= 0 || t.indexOf('atom') >= 0 || t.indexOf('xml') >= 0) {
                        var href = links[i].href;
                        if (href) return href;
                    }
                }
                return '';
            } catch (e) { return ''; }
        })();
    """.trimIndent()

    fun selectedTextScript(): String = """
        (function() {
            try {
                var sel = window.getSelection ? String(window.getSelection()).trim() : '';
                if (sel.length > 0) return sel.slice(0, 1200);
                var p = document.querySelector('article p, main p, p');
                return p && p.innerText ? p.innerText.trim().slice(0, 600) : '';
            } catch (e) { return ''; }
        })();
    """.trimIndent()

    fun mediaSpeedBoostScript(speed: Float, gain: Float): String {
        val safeSpeed = speed.coerceIn(0.5f, 3.0f)
        val safeGain = gain.coerceIn(1.0f, 2.5f)
        return """
        (function() {
            try {
                var media = document.querySelectorAll('video, audio');
                for (var i = 0; i < media.length; i++) {
                    var el = media[i];
                    el.playbackRate = $safeSpeed;
                    if (!el.__ritamGainNode && $safeGain > 1.01) {
                        var AudioCtx = window.AudioContext || window.webkitAudioContext;
                        if (AudioCtx) {
                            var ctx = window.__ritamAudioCtx || (window.__ritamAudioCtx = new AudioCtx());
                            var src = ctx.createMediaElementSource(el);
                            var gainNode = ctx.createGain();
                            src.connect(gainNode);
                            gainNode.connect(ctx.destination);
                            el.__ritamGainNode = gainNode;
                        }
                    }
                    if (el.__ritamGainNode) {
                        el.__ritamGainNode.gain.value = $safeGain;
                    }
                }
            } catch (e) {}
        })();
        """.trimIndent()
    }

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

    fun forceWebDarkScript(enabled: Boolean): String = buildForceWebDarkScript(enabled)

    companion object {
        fun buildForceWebDarkScript(enabled: Boolean): String = if (enabled) {
        """
        (function() {
            try {
                function parseRgba(str) {
                    if (!str || str === 'transparent' || str === 'none') return null;
                    var m = str.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*([\d.]+))?\)/i);
                    if (!m) return null;
                    return { r: +m[1], g: +m[2], b: +m[3], a: m[4] !== undefined ? +m[4] : 1 };
                }
                function lum(c) {
                    if (!c) return 1;
                    return (0.2126 * c.r + 0.7152 * c.g + 0.0722 * c.b) / 255;
                }
                function rgbToHsl(r, g, b) {
                    r /= 255; g /= 255; b /= 255;
                    var max = Math.max(r, g, b), min = Math.min(r, g, b);
                    var h = 0, s = 0, l = (max + min) / 2;
                    if (max !== min) {
                        var d = max - min;
                        s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
                        switch (max) {
                            case r: h = (g - b) / d + (g < b ? 6 : 0); break;
                            case g: h = (b - r) / d + 2; break;
                            case b: h = (r - g) / d + 4; break;
                        }
                        h /= 6;
                    }
                    return { h: Math.round(h * 360), s: Math.round(s * 100), l: Math.round(l * 100) };
                }
                function isGradientLight(bgImg) {
                    var matches = bgImg.match(/rgba?\(\d+,\s*\d+,\s*\d+(?:,\s*[\d.]+)?\)/gi);
                    if (!matches || matches.length === 0) return false;
                    var sum = 0, count = 0;
                    for (var k = 0; k < matches.length; k++) {
                        var c = parseRgba(matches[k]);
                        if (c && c.a > 0.15) { sum += lum(c); count++; }
                    }
                    return count > 0 && (sum / count) > 0.42;
                }
                function darkenGradient(bgImg) {
                    return bgImg.replace(/rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*([\d.]+))?\)/gi, function(full, r, g, b, a) {
                        var c = { r: +r, g: +g, b: +b, a: a !== undefined ? +a : 1 };
                        if (lum(c) <= 0.35) return full;
                        var hsl = rgbToHsl(c.r, c.g, c.b);
                        var targetL = Math.max(8, Math.min(18, Math.round(hsl.l * 0.18)));
                        var targetS = Math.min(hsl.s, 28);
                        return a !== undefined
                            ? 'hsla(' + hsl.h + ',' + targetS + '%,' + targetL + '%,' + a + ')'
                            : 'hsl(' + hsl.h + ',' + targetS + '%,' + targetL + '%)';
                    });
                }

                var metaCs = document.querySelector('meta[name="color-scheme"]');
                var csContent = metaCs ? (metaCs.getAttribute('content') || '').trim().toLowerCase() : '';
                var htmlBg = parseRgba(getComputedStyle(document.documentElement).backgroundColor);
                var bodyBg = document.body ? parseRgba(getComputedStyle(document.body).backgroundColor) : null;
                var rootBg = (bodyBg && bodyBg.a > 0.2) ? bodyBg : (htmlBg && htmlBg.a > 0.2 ? htmlBg : null);
                var isNativeDark = csContent === 'dark' || csContent === 'only dark' || (rootBg && lum(rootBg) < 0.30);

                var styleId = 'ritam-force-dark-style';
                var styleEl = document.getElementById(styleId);
                if (!styleEl) {
                    styleEl = document.createElement('style');
                    styleEl.id = styleId;
                    (document.head || document.documentElement).appendChild(styleEl);
                }

                if (isNativeDark) {
                    styleEl.textContent = ':root { color-scheme: dark !important; }';
                    document.documentElement.setAttribute('data-ritam-dark-mode', 'native');
                    return;
                }

                document.documentElement.setAttribute('data-ritam-dark-mode', 'adaptive');
                styleEl.textContent = [
                    ':root { color-scheme: dark !important; }',
                    'html, body { background-color: #101412 !important; color: #e6ece8 !important; }',
                    'a { color: #7ac4a2; }',
                    'input, textarea, select { background-color: #1b221e !important; color: #e9efeb !important; border-color: #34403a !important; }',
                    'input::placeholder, textarea::placeholder { color: #8c9992 !important; }',
                    'pre, code, kbd, samp { background-color: #101412 !important; color: #d8e6de !important; border-color: #2b3530 !important; }',
                    'hr { border-color: #2b3530 !important; }',
                    'img, video, canvas { filter: brightness(0.94) contrast(1.02); }',
                    '::selection { background: #2d5a46 !important; color: #ffffff !important; }'
                ].join('\n');

                function trackOrig(el, prop, attrName) {
                    if (!el.hasAttribute(attrName)) {
                        el.setAttribute(attrName, el.style.getPropertyValue(prop) || '');
                    }
                }

                function adaptElement(el) {
                    if (!el || el.nodeType !== 1) return;
                    var tag = (el.tagName || '').toUpperCase();
                    if (tag === 'IMG' || tag === 'VIDEO' || tag === 'PICTURE' || tag === 'CANVAS' || tag === 'SOURCE' || tag === 'SCRIPT' || tag === 'STYLE' || tag === 'LINK') return;

                    if (tag === 'SVG' || tag === 'PATH' || tag === 'CIRCLE' || tag === 'RECT' || tag === 'POLYGON' || tag === 'LINE') {
                        var csSvg = getComputedStyle(el);
                        var fc = parseRgba(csSvg.fill);
                        if (fc && fc.a > 0.2 && lum(fc) < 0.28) {
                            var hslF = rgbToHsl(fc.r, fc.g, fc.b);
                            trackOrig(el, 'fill', 'data-ritam-orig-fill');
                            el.style.setProperty('fill', hslF.s > 25 ? ('hsl(' + hslF.h + ',' + Math.min(hslF.s, 65) + '%,64%)') : '#d3ddd7', 'important');
                        }
                        var sc = parseRgba(csSvg.stroke);
                        if (sc && sc.a > 0.2 && lum(sc) < 0.28) {
                            trackOrig(el, 'stroke', 'data-ritam-orig-stroke');
                            el.style.setProperty('stroke', '#d3ddd7', 'important');
                        }
                        return;
                    }

                    var cs = getComputedStyle(el);
                    var bg = parseRgba(cs.backgroundColor);
                    var bgImg = cs.backgroundImage || 'none';
                    var hasUrlImg = bgImg.indexOf('url(') !== -1;
                    var hasGradient = !hasUrlImg && bgImg.indexOf('gradient(') !== -1;

                    var bgIsDark = (bg && bg.a > 0.35 && lum(bg) <= 0.36) || (hasGradient && !isGradientLight(bgImg));
                    var bgIsLight = bg && bg.a > 0.15 && lum(bg) > 0.42;

                    if (hasGradient && isGradientLight(bgImg)) {
                        trackOrig(el, 'background-image', 'data-ritam-orig-bgi');
                        el.style.setProperty('background-image', darkenGradient(bgImg), 'important');
                    } else if (!hasUrlImg && bgIsLight) {
                        trackOrig(el, 'background-color', 'data-ritam-orig-bgc');
                        var l = lum(bg);
                        var isStickyOrHeader = cs.position === 'sticky' || cs.position === 'fixed' || tag === 'HEADER' || tag === 'NAV';
                        var darkSurface = '#171c19';
                        if (tag === 'BODY' || tag === 'HTML' || tag === 'MAIN') {
                            darkSurface = '#101412';
                        } else if (isStickyOrHeader) {
                            darkSurface = bg.a < 0.96 ? 'rgba(20, 25, 22, 0.92)' : '#151a17';
                        } else if (tag === 'TH' || tag === 'THEAD') {
                            darkSurface = '#1e2521';
                        } else if (l > 0.94) {
                            darkSurface = '#181d1a';
                        } else if (l > 0.85) {
                            darkSurface = '#1d2420';
                        } else {
                            darkSurface = '#232c27';
                        }
                        el.style.setProperty('background-color', darkSurface, 'important');
                    }

                    var col = parseRgba(cs.color);
                    if (col && !bgIsDark && lum(col) < 0.55) {
                        trackOrig(el, 'color', 'data-ritam-orig-color');
                        var hsl = rgbToHsl(col.r, col.g, col.b);
                        var newColor;
                        if (hsl.s < 28 || hsl.l < 16) {
                            newColor = lum(col) < 0.22 ? '#edf2ef' : '#b8c3bd';
                        } else {
                            var targetL = tag === 'A' ? 72 : Math.max(hsl.l, 70);
                            newColor = 'hsl(' + hsl.h + ', ' + Math.min(Math.max(hsl.s, 45), 82) + '%, ' + targetL + '%)';
                        }
                        el.style.setProperty('color', newColor, 'important');
                    }

                    ['border-top-color', 'border-right-color', 'border-bottom-color', 'border-left-color'].forEach(function(prop) {
                        var camel = prop.replace(/-([a-z])/g, function(_, ch) { return ch.toUpperCase(); });
                        var bCol = parseRgba(cs[camel]);
                        if (bCol && bCol.a > 0.1 && lum(bCol) > 0.55) {
                            trackOrig(el, prop, 'data-ritam-orig-' + prop);
                            el.style.setProperty(prop, '#2b342f', 'important');
                        }
                    });
                }

                function runPass() {
                    if (document.documentElement.getAttribute('data-ritam-dark-mode') !== 'adaptive') return;
                    var all = document.querySelectorAll('body, body *');
                    var limit = Math.min(all.length, 2500);
                    for (var i = 0; i < limit; i++) {
                        adaptElement(all[i]);
                    }
                }

                runPass();

                if (!window.__ritamDarkObserver && document.body) {
                    var timer = null;
                    window.__ritamDarkObserver = new MutationObserver(function() {
                        if (document.documentElement.getAttribute('data-ritam-dark-mode') !== 'adaptive') return;
                        if (timer) return;
                        timer = setTimeout(function() {
                            timer = null;
                            runPass();
                        }, 150);
                    });
                    window.__ritamDarkObserver.observe(document.body, { childList: true, subtree: true });
                }
            } catch (e) {}
        })();
        """.trimIndent()
    } else {
        """
        (function() {
            try {
                document.documentElement.removeAttribute('data-ritam-dark-mode');
                var el = document.getElementById('ritam-force-dark-style');
                if (el && el.parentNode) el.parentNode.removeChild(el);
                var restore = function(attr, prop) {
                    var nodes = document.querySelectorAll('[' + attr + ']');
                    for (var i = 0; i < nodes.length; i++) {
                        var orig = nodes[i].getAttribute(attr);
                        if (orig) nodes[i].style.setProperty(prop, orig);
                        else nodes[i].style.removeProperty(prop);
                        nodes[i].removeAttribute(attr);
                    }
                };
                restore('data-ritam-orig-bgc', 'background-color');
                restore('data-ritam-orig-bgi', 'background-image');
                restore('data-ritam-orig-color', 'color');
                restore('data-ritam-orig-fill', 'fill');
                restore('data-ritam-orig-stroke', 'stroke');
                restore('data-ritam-orig-border-top-color', 'border-top-color');
                restore('data-ritam-orig-border-right-color', 'border-right-color');
                restore('data-ritam-orig-border-bottom-color', 'border-bottom-color');
                restore('data-ritam-orig-border-left-color', 'border-left-color');
            } catch (e) {}
        })();
        """.trimIndent()
        }
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

        // Adult & Nude Content blocking check
        if (matchesDomainSet(host, adultDomains)) {
            return recordBlock(cleanPageDomain, host, BlockCategory.MALICIOUS, "adult-domain:$host", 50_000L, isPrivate)
        }
        val lowerUrl = requestUrl.lowercase()
        val adultToken = adultUrlSubstrings.firstOrNull { lowerUrl.contains(it) }
        if (adultToken != null && (lowerUrl.contains("porn") || lowerUrl.contains("sex") || lowerUrl.contains("nude") || lowerUrl.contains("xxx"))) {
            return recordBlock(cleanPageDomain, host, BlockCategory.MALICIOUS, "adult-keyword:$adultToken", 40_000L, isPrivate)
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

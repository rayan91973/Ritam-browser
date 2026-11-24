package com.example.browser

/**
 * Page scripts for sites whose ads cannot be removed by network rules alone.
 *
 * YouTube serves ads from its own origin and injects them through the player, so filter lists never
 * see an "ad" request. Two layers are used, mirroring how desktop blockers handle YouTube:
 *  1. strip ad metadata out of `/youtubei/v1/player` responses before the player parses them,
 *  2. skip/neutralise any ad that still appears, and hide ad surfaces in the DOM.
 */
object SiteAdBlockScripts {

    fun isYouTubeHost(host: String): Boolean {
        val clean = host.lowercase().removePrefix("www.")
        return clean == "youtube.com" || clean.endsWith(".youtube.com") ||
            clean == "youtu.be" || clean == "youtube-nocookie.com" ||
            clean.endsWith(".youtube-nocookie.com")
    }

    /**
     * Idempotent, defensive and cheap: a 500 ms tick plus a MutationObserver, both no-ops when the
     * page has no ad surface. All failures are swallowed so a YouTube change can never break the
     * page, only stop ad skipping.
     */
    val youTubeAdBlockScript: String = """
        (function() {
            if (window.__ritamYouTubeAdBlock) return;
            window.__ritamYouTubeAdBlock = true;
            try { console.log('[Ritam] YouTube ad filter active'); } catch (e) {}

            function scrub(text) {
                if (typeof text !== 'string' || text.length === 0) return text;
                if (text.indexOf('"adPlacements"') < 0 &&
                    text.indexOf('"adSlots"') < 0 &&
                    text.indexOf('"playerAds"') < 0 &&
                    text.indexOf('"adBreakHeartbeatParams"') < 0) return text;
                try {
                    var data = JSON.parse(text);
                    var targets = [data, data.playerResponse, data.playerAdParams];
                    for (var i = 0; i < targets.length; i++) {
                        var target = targets[i];
                        if (!target || typeof target !== 'object') continue;
                        delete target.adPlacements;
                        delete target.adSlots;
                        delete target.playerAds;
                        delete target.adBreakHeartbeatParams;
                        delete target.adParams;
                    }
                    var output = JSON.stringify(data);
                    try {
                        if (!window.__ritamAdMetaLogged) {
                            window.__ritamAdMetaLogged = true;
                            console.log('[Ritam] stripped YouTube ad metadata from player response');
                        }
                    } catch (e) {}
                    return output;
                } catch (e) {
                    return text;
                }
            }

            // --- Layer 1a: XHR responses ---
            try {
                var nativeOpen = XMLHttpRequest.prototype.open;
                var nativeSend = XMLHttpRequest.prototype.send;
                XMLHttpRequest.prototype.open = function(method, url) {
                    try { this.__ritamPlayerUrl = String(url || ''); } catch (e) {}
                    return nativeOpen.apply(this, arguments);
                };
                XMLHttpRequest.prototype.send = function() {
                    var xhr = this;
                    try {
                        xhr.addEventListener('readystatechange', function() {
                            if (xhr.readyState !== 4) return;
                            var url = xhr.__ritamPlayerUrl || '';
                            if (url.indexOf('/youtubei/v1/player') < 0) return;
                            if (xhr.responseType && xhr.responseType !== 'text' && xhr.responseType !== 'json') return;
                            try {
                                var raw = xhr.responseType === 'json' ? JSON.stringify(xhr.response) : xhr.responseText;
                                var clean = scrub(raw);
                                if (clean === raw) return;
                                Object.defineProperty(xhr, 'responseText', { configurable: true, value: clean });
                                if (xhr.responseType === '') {
                                    Object.defineProperty(xhr, 'response', { configurable: true, value: clean });
                                }
                            } catch (e) {}
                        });
                    } catch (e) {}
                    return nativeSend.apply(this, arguments);
                };
            } catch (e) {}

            // --- Layer 1b: fetch responses ---
            try {
                var nativeFetch = window.fetch;
                window.fetch = function(input, init) {
                    var url = typeof input === 'string' ? input : ((input && input.url) || '');
                    var promise = nativeFetch.apply(this, arguments);
                    if (url.indexOf('/youtubei/v1/player') < 0) return promise;
                    return promise.then(function(response) {
                        try {
                            return response.clone().text().then(function(body) {
                                var clean = scrub(body);
                                if (clean === body) return response;
                                var headers = new Headers(response.headers);
                                headers.delete('content-encoding');
                                headers.delete('content-length');
                                return new Response(clean, {
                                    status: response.status,
                                    statusText: response.statusText,
                                    headers: headers
                                });
                            }).catch(function() { return response; });
                        } catch (e) {
                            return response;
                        }
                    });
                };
            } catch (e) {}

            // --- Layer 2: DOM clean-up and ad skipping ---
            var hideSelectors = [
                '.ytp-ad-module', '.ytp-ad-overlay-container', '.ytp-ad-overlay-slot',
                '.ytp-ad-player-overlay', '.ytp-ad-progress-list', '.ytp-ad-image-overlay',
                '.ytp-featured-product', '.ytp-suggested-action', '.ytp-ad-text-overlay',
                '#player-ads', '#masthead-ad', '#panels ytd-engagement-panel-section-list-renderer[target-id="engagement-panel-ads"]',
                'ytd-ad-slot-renderer', 'ytd-in-feed-ad-layout-renderer',
                'ytd-promoted-sparkles-web-renderer', 'ytd-promoted-video-renderer',
                'ytd-display-ad-renderer', 'ytd-banner-promo-renderer',
                'ytd-statement-banner-renderer', 'ytd-companion-slot-renderer',
                'ytd-action-companion-ad-renderer', 'ytd-player-legacy-desktop-watch-ads-renderer',
                'ytd-merch-shelf-renderer',
                // Mobile web (m.youtube.com) renders "ytm-" elements instead of desktop "ytd-".
                'ytm-promoted-sparkles-web-renderer', 'ytm-ad-slot-renderer',
                'ytm-in-feed-ad-layout-renderer', 'ytm-display-ad-renderer',
                'ytm-promoted-video-renderer', 'ytm-companion-slot',
                'ytm-banner-promo-renderer', 'ytm-statement-banner-renderer',
                'ytm-companion-slot-renderer', 'ytm-ad-action-companion-renderer',
                'ytm-merch-shelf-renderer', '.ytm-promoted-sparkles-web-renderer'
            ];
            var removeSelectors = [
                'ytd-ad-slot-renderer', 'ytd-in-feed-ad-layout-renderer',
                'ytd-promoted-sparkles-web-renderer', 'ytd-promoted-video-renderer',
                'ytd-display-ad-renderer', 'ytd-banner-promo-renderer',
                'ytd-statement-banner-renderer', 'ytd-companion-slot-renderer',
                'ytd-action-companion-ad-renderer',
                'ytm-promoted-sparkles-web-renderer', 'ytm-ad-slot-renderer',
                'ytm-in-feed-ad-layout-renderer', 'ytm-display-ad-renderer',
                'ytm-promoted-video-renderer', 'ytm-companion-slot',
                'ytm-banner-promo-renderer', 'ytm-statement-banner-renderer'
            ];

            // Buttons whose visible label is "Skip" but whose class changes between player builds.
            function textButtons() {
                var found = [];
                try {
                    var scope = document.querySelector('#movie_player, .html5-video-player, ytm-player, .player-container');
                    if (!scope) return found;
                    var nodes = scope.querySelectorAll('button, [role="button"], .ytp-ad-skip-button, [class*="skip-button"]');
                    for (var i = 0; i < nodes.length; i++) {
                        var el = nodes[i];
                        var label = ((el.getAttribute && (el.getAttribute('aria-label') || el.getAttribute('data-title-no-tooltip'))) || '').toLowerCase();
                        var text = ((el.textContent || '').trim()).toLowerCase();
                        if (label.indexOf('skip') === 0 || label === 'skip ad' || text === 'skip ad' || text === 'skip ads' || text === 'skip') {
                            found.push(el);
                        }
                    }
                } catch (e) {}
                return found;
            }

            var savedRate = 1;
            var lastAdVideo = null;

            function isAdPlaying() {
                try {
                    if (document.querySelector('.ad-showing') ||
                        document.querySelector('.ytp-ad-player-overlay-layout') ||
                        document.querySelector('.ytp-ad-module .ytp-ad-player-overlay') ||
                        document.querySelector('.ytp-ad-skip-button-modern, .ytp-ad-skip-button-container, .ytp-skip-ad-button')) {
                        return true;
                    }
                    // Mobile feed ads play inside their own container rather than #movie_player.
                    var feed = document.querySelector('ytm-companion-slot video, .ytp-ad-player-overlay-layout video');
                    return !!(feed && !feed.paused);
                } catch (e) {
                    return false;
                }
            }

            function activeVideo() {
                return document.querySelector('video.html5-main-video') ||
                    document.querySelector('#movie_player video') ||
                    document.querySelector('video');
            }

            function skipAd() {
                var buttons = document.querySelectorAll(
                    '.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, ' +
                    '.ytp-ad-skip-button-container button, .ytp-ad-overlay-close-button'
                );
                for (var i = 0; i < buttons.length; i++) {
                    try { buttons[i].click(); } catch (e) {}
                }
                var textHits = textButtons();
                for (var t = 0; t < textHits.length; t++) {
                    try { textHits[t].click(); } catch (e) {}
                }
                var video = activeVideo();
                if (!video) return;
                try {
                    if (!window.__ritamAdSkipLogged) {
                        window.__ritamAdSkipLogged = true;
                        console.log('[Ritam] skipping YouTube ad');
                    }
                    if (lastAdVideo !== video) { savedRate = 1; lastAdVideo = video; }
                    if (isFinite(video.duration) && video.duration > 0) {
                        if (video.currentTime < video.duration - 0.15) video.currentTime = video.duration;
                    }
                    if (video.playbackRate < 8) video.playbackRate = 8;
                } catch (e) {}
            }

            function restorePlayback() {
                var video = activeVideo();
                if (!video || video === lastAdVideo) {
                    if (video && video.playbackRate > 1.05 && !isAdPlaying()) {
                        try { video.playbackRate = savedRate > 1.05 ? savedRate : 1; } catch (e) {}
                    }
                }
            }

            function tick() {
                try {
                    for (var i = 0; i < hideSelectors.length; i++) {
                        var nodes = document.querySelectorAll(hideSelectors[i]);
                        for (var j = 0; j < nodes.length; j++) {
                            nodes[j].style.setProperty('display', 'none', 'important');
                        }
                    }
                    for (var k = 0; k < removeSelectors.length; k++) {
                        var ads = document.querySelectorAll(removeSelectors[k]);
                        for (var m = ads.length - 1; m >= 0; m--) {
                            if (ads[m].parentNode) ads[m].parentNode.removeChild(ads[m]);
                        }
                    }
                    if (isAdPlaying()) {
                        skipAd();
                    } else {
                        window.__ritamAdSkipLogged = false;
                        restorePlayback();
                    }
                } catch (e) {}
            }

            function start() {
                tick();
                try {
                    var observer = new MutationObserver(function() { tick(); });
                    observer.observe(document.documentElement || document, { childList: true, subtree: true });
                } catch (e) {}
                setInterval(tick, 500);
            }

            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', start, { once: true });
            } else {
                start();
            }
        })();
    """.trimIndent()

    /**
     * Keeps media playing while the app is in the background or in PiP.
     *
     * The page is told it is always visible (many players pause on `visibilitychange`) and a
     * watchdog restarts a video that the player paused on its own. Explicit user actions are
     * respected through `__ritamUserMediaActionAt`, which native code updates before pausing.
     */
    fun mediaKeepAliveScript(backgroundMode: Boolean): String = """
        (function() {
            try {
                window.__ritamBackgroundMode = ${if (backgroundMode) "true" else "false"};
                if (window.__ritamMediaGuardInstalled) return;
                window.__ritamMediaGuardInstalled = true;

                try {
                    Object.defineProperty(document, 'hidden', { configurable: true, get: function() { return false; } });
                    Object.defineProperty(document, 'visibilityState', { configurable: true, get: function() { return 'visible'; } });
                    Object.defineProperty(document, 'webkitHidden', { configurable: true, get: function() { return false; } });
                    Object.defineProperty(document, 'webkitVisibilityState', { configurable: true, get: function() { return 'visible'; } });
                } catch (e) {}

                function userPausedRecently() {
                    return !!(window.__ritamUserMediaActionAt &&
                        (Date.now() - window.__ritamUserMediaActionAt) < 1500);
                }

                function playingVideo() {
                    var media = document.querySelectorAll('video, audio');
                    for (var i = 0; i < media.length; i++) {
                        if (!media[i].paused && !media[i].ended) return media[i];
                    }
                    return null;
                }

                function rememberTarget() {
                    var v = playingVideo();
                    if (v) window.__ritamLastMediaTarget = v;
                }

                ['play', 'playing'].forEach(function(evt) {
                    document.addEventListener(evt, rememberTarget, true);
                });

                // Restart a player-initiated pause while backgrounded / in PiP.
                document.addEventListener('pause', function(e) {
                    if (!e || !e.target || e.target.ended) return;
                    if (!window.__ritamBackgroundMode || userPausedRecently()) return;
                    var target = e.target;
                    setTimeout(function() {
                        try {
                            if (window.__ritamBackgroundMode && !userPausedRecently() && target.paused && !target.ended) {
                                target.play().catch(function() {});
                            }
                        } catch (err) {}
                    }, 120);
                }, true);

                // Keep the native media session/foreground service in sync. Playback state changes
                // do not always mutate the DOM, so event-driven reporting alone leaves it stale.
                function reportMedia() {
                    try {
                        var media = document.querySelectorAll('video, audio');
                        var playing = false;
                        for (var i = 0; i < media.length; i++) {
                            if (!media[i].paused && !media[i].ended) { playing = true; break; }
                        }
                        if (window.RitamBridge && window.RitamBridge.onMediaStateChanged) {
                            window.RitamBridge.onMediaStateChanged(media.length > 0, playing);
                        }
                    } catch (e) {}
                }
                setInterval(reportMedia, 2000);
                setTimeout(reportMedia, 300);

                // Some players pause repeatedly; nudge once a second while backgrounded.
                setInterval(function() {
                    if (!window.__ritamBackgroundMode || userPausedRecently()) return;
                    var v = playingVideo();
                    if (v || !window.__ritamLastMediaTarget) return;
                    var target = window.__ritamLastMediaTarget;
                    if (target && target.paused && !target.ended) {
                        target.play().catch(function() {});
                    }
                }, 1000);
            } catch (e) {}
        })();
    """.trimIndent()
}

# How Ritam Browser Works (`working.md`)

This document explains the internal runtime mechanics of Ritam Browser across the browser engine, privacy pipeline, media/document handlers, and persistence layers.

---

## 1. Multi-Tab WebView Engine & LRU Pool

Creating an Android `WebView` for dozens of background tabs consumes excessive native memory. `WebViewManager` (`com.example.browser.WebViewManager`) solves this with a bounded **Least Recently Used (LRU) pool of 5 active WebView instances**:

1. **Tab Creation & Switching**: Each `TabState` holds lightweight metadata (`id`, `url`, `title`, `domain`, `isPrivate`, `isDesktopMode`, `isReaderMode`, `isPinned`, `groupName`, `zoomPercent`, `lastAccessedAt`). When a tab becomes active, `getOrCreateWebView(tabId, isPrivate, isDesktopMode)` either reuses its live `WebView` or creates one configured for that tab's privacy and viewport profile.
2. **LRU Eviction**: When the pool exceeds `5` active `WebView` instances, the least-recently-used non-pinned background tab's `WebView` is safely detached, paused, and destroyed while its `TabState` (URL, title, scroll/zoom metadata) remains intact in memory so re-selecting it reloads seamlessly.
3. **Desktop Mode Fix**: Enabling Desktop Mode sets a desktop Linux Chrome User-Agent string *and* configures `settings.useWideViewPort = true`, `settings.loadWithOverviewMode = true`, and injects a viewport `<meta name="viewport" content="width=1280, initial-scale=0.5">` normalization script on page completion so responsive sites genuinely switch to desktop layouts.
4. **Auto-Hide Scroll Tracking**: `WebView.setOnScrollChangeListener` reports vertical scroll deltas to `MainViewModel`. When `autoHideToolbarsOnScroll` is enabled, downward scrolling beyond a small threshold smoothly hides the omnibox and bottom navigation dock; upward scrolling or tapping the reveal pill restores them.

---

## 2. RITAM Shields Privacy Pipeline

`RitamShieldsEngine` (`com.example.privacy.RitamShieldsEngine`) executes three layers of protection on every navigation and sub-resource fetch:

### Layer A — Pre-Navigation & URL Hygiene
- **Tracking Parameter Stripping (`UrlCleaner`)**: Before loading user-entered URLs, copying addresses, sharing QR codes, or saving to the Reading List, `UrlCleaner.cleanUrl()` strips tracking parameters (`utm_*`, `fbclid`, `gclid`, `wbraid`, `gbraid`, `msclkid`, `igshid`, `igsh`, `si`, `mc_cid`, `mc_eid`, etc.) while keeping functional query arguments intact.
- **HTTPS-Only Upgrades**: Plain `http://` URLs are transparently upgraded to `https://` unless disabled per-site or globally.
- **Malicious Domain Guard**: Hostnames matching known phishing/malware signatures halt navigation before content loads and present `MaliciousDomainDialog`.

### Layer B — Sub-Resource Interception (`shouldInterceptRequest`)
Every network request triggered inside `WebView` runs through `RitamShieldsEngine.shouldInterceptRequest()`:
1. Checks per-site exceptions in `WebsiteSettingsEntity` (if Shields are paused for a domain, interception yields).
2. Evaluates subscribed filter lists (`FilterListEntity`) and user **Custom Filter Rules** (`CustomFilterRuleEntity` supporting Adblock `||domain^` and `@@||exception^` rules).
3. Checks cached Encrypted DNS (DoH) sinkhole status (`DohResolver.isCachedBlockedByDns()`).
4. Blocked requests return an empty `WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))` and increment session + Room `BlockingStatsEntity` counters (`AD`, `TRACKER`, `MALICIOUS`, `HTTPS`) and estimated bytes saved.

### Layer C — Post-Load DOM Cosmetic Filtering, Cookie Blocking & Force Dark
On `onPageFinished` / `onPageCommitVisible`, `WebViewManager` injects curated CSS + JavaScript payloads:
- **Cosmetic Ad-Blocker**: Collapses ad containers (`ins.adsbygoogle`, `[id*="ad-slot"]`, sponsored cards) so pages render cleanly without empty gaps.
- **Cookie & Consent Popup Auto-Blocker**: Hides major CMP overlay selectors (OneTrust, Cookiebot, Quantcast, TrustArc) and dispatches safe clicks to "Reject All" / "Necessary Only" buttons.
- **Force Dark Mode for Webpages**: Resolves `WebsiteSettingsEntity.forceDarkOverride` (`1` = Force On, `0` = Force Off, `-1` = Follow global `forceWebDarkMode`). When enabled, injects an intelligent invert/hue-rotate stylesheet preserving photos, `<video>`, and `<canvas>` elements.

---

## 3. Encrypted DNS (DoH) Resolver

`DohResolver` (`com.example.privacy.DohResolver`) queries RFC 8484 DNS-over-HTTPS JSON endpoints over `OkHttpClient` (4s connect/read timeout) supporting:
- **System Default** (standard Android resolver)
- **Cloudflare `1.1.1.1`** (`https://cloudflare-dns.com/dns-query`)
- **Quad9 `9.9.9.9`** (`https://dns.quad9.net:5053/dns-query`)
- **AdGuard DNS** (`https://dns.adguard-dns.com/resolve`)
- **Google Public DNS** (`https://dns.google/resolve`)
- **Custom DoH URL** configured by the user

Results and lookup latencies (`latencyMs`) are cached in a `ConcurrentHashMap`. If AdGuard/Quad9 sinkholes a tracker domain (NXDOMAIN or `0.0.0.0`/`::`), Shields treat that domain as DNS-blocked.

---

## 4. Incognito Mode & Biometric Locking

- **Ephemeral Isolation**: Private tabs flag `isPrivate = true`. History, search queries, and persistent WebStorage/cookies are excluded from Room writes. Closing private tabs or tapping **Burn** destroys their WebViews and clears volatile cache/cookies.
- **Hardware Authentication (`BiometricAuthHelper`)**: Uses Android's `BiometricManager` + `BiometricPrompt` (supporting strong biometrics and device PIN/pattern/password fallback).
  - **Incognito Lock**: When `biometricIncognitoLock` is enabled, viewing any private tab shows `BiometricLockScreenOverlay` until unlocked. Navigating away from the app (`MainActivity.onStop()`) calls `viewModel.relockSensitiveAreas()`.
  - **Vault Lock**: Stored credentials and developer keys in `VaultEntryEntity` remain masked behind an `"Unlock Vault with Biometrics"` prompt until authenticated.

---

## 5. Reading Experience: Reader Mode, Translator, PDF Viewer & Read Aloud

- **Reader Mode (`ReaderModeView`)**: Extracts the page `<article>` / `<p>` nodes via JS and renders an editorial serif layout with adjustable font size (`13sp–24sp`).
- **Reading List ("Read Later")**: Persists curated URLs, titles, excerpts, and estimated read times in Room (`reading_list` table) with unread/done filters.
- **Page & Text Translator (`TranslationHelper`)**: Offers 1-tap full-page Google Translate proxy navigation (with **Restore Original**) plus inline quick-snippet translation via MyMemory REST API across 9 languages.
- **Built-in PDF Viewer (`PdfDocumentHelper` + `PdfViewerDialog`)**: Intercepts `.pdf` links and downloaded PDFs, caches remote streams safely in app cache, and renders crisp pages via Android `PdfRenderer` with zoom (`1.0x–2.5x`) and night contrast inversion.
- **Read Aloud (`ReadAloudController`)**: Uses Android `TextToSpeech` with sentence/paragraph queue stepping, Play/Pause, Previous/Next paragraph, and speed cycling (`0.8x` to `2.0x`), plus optional active playback notifications.

---

## 6. Media, PiP, QR & Developer Inspector

- **Fullscreen Video & Picture-in-Picture**: `WebChromeClient.onShowCustomView` elevates HTML5 fullscreen `<video>` playback into a full-bleed Compose host. When media is playing and the user leaves the app (`onUserLeaveHint`) or taps **PiP Mode** in Quick Controls, `MainActivity` enters Android `PictureInPictureParams` (16:9).
- **Offline QR Code Generator (`QrCodeGenerator`)**: Pure Kotlin Byte-mode QR encoder (Versions 1–10, ECC Level L, Reed-Solomon + best mask scoring) renders `Bitmap` QR codes with zero external dependencies.
- **Camera QR Scanner (`QrScannerScreen`)**: Uses CameraX + Google ML Kit Barcode Scanning under runtime `CAMERA` permission to scan URLs directly into navigation.
- **DevTools Inspector (`DevToolsSheet`)**: Captures `WebChromeClient.onConsoleMessage` logs, records live blocked/allowed `NetworkLogEntry` traffic, evaluates arbitrary JS expressions via `evaluateJavascript`, and dumps `document.cookie` and `localStorage` pairs.

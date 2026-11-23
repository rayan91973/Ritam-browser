# Complete Features List (`featureslist.md`)

Ritam Browser combines core mobile web browsing, aggressive on-device privacy engineering, editorial reading tools, and power-user utilities.

---

## 1. Core Browser Engine
- **Fast Multi-Tab Browsing**: Lightweight tab state backed by a 5-slot LRU `WebView` pool to prevent memory bloat.
- **Omnibox Search & Direct Navigation**: Unified pill address bar with HTTPS lock indicator, live session Shield blocked counter badge, Reader Mode toggle, and reload/stop button.
- **Live Search Engine Selector**: Switch directly inside the search pill between **DuckDuckGo**, **Brave Search**, **Google**, **Startpage**, **Qwant**, and **Bing**.
- **Omnibox Quick Actions**: Instant **Copy Clean Address**, **Edit Address**, **Share QR Code**, and **Add to Reading List** pills when the omnibox overlay opens on an active webpage.
- **Desktop View Mode**: Full Desktop Chrome User-Agent combined with `useWideViewPort` and injected responsive viewport normalization.
- **Find in Page**: Live match-highlighting search bar (`WebView.findAllAsync` + `findNext`) with up/down stepping.
- **Gesture Navigation**:
  - Swipe bottom dock horizontally for Back/Forward navigation.
  - Swipe the omnibox pill horizontally to switch adjacent tabs.
  - Pull down on the omnibox to reload the active page.
  - Double-tap the omnibox pill to jump smoothly to the top of the page.

---

## 2. RITAM Shields & Privacy Suite
- **Ad & Tracker Blocker**: Request-level blocking across EasyList-style ad networks, analytics beacons, and third-party scripts with live Data Saved & Trackers Blocked KPIs.
- **Cosmetic Ad-Blocker (CSS)**: Automatically collapses empty ad wrappers and sponsored DOM slots after load.
- **Cookie & Consent Popup Auto-Blocker**: Suppresses CMP cookie banners and triggers automated "Reject All" interactions.
- **Tracking-Parameter Cleaner (`UrlCleaner`)**: Strips `utm_*`, `fbclid`, `gclid`, `msclkid`, `igshid`, `si`, and telemetry query parameters cleanly from links.
- **Encrypted DNS (DoH) Selector**: Route hostnames through **Cloudflare 1.1.1.1**, **Quad9**, **AdGuard DNS**, **Google Public DNS**, or a **Custom DoH** JSON endpoint with built-in latency/IP verification testing.
- **HTTPS-Only Upgrade & SSL Error Guard**: Automatically upgrades HTTP navigations and blocks invalid SSL certificates without silent bypasses.
- **Malicious Domain Protection**: Halts navigation to known phishing/malware signatures before content renders.
- **External App Launch Guard**: Confirms `intent://` and external app deep links with fallback-to-browser support.
- **Per-Site Permissions Dialog**: Toggle Shields, JavaScript, HTTPS-Only, Per-Site Page Zoom (`50%–200%`), and Per-Site Force Dark Override (`Follow / Off / On`).
- **Custom Filter Rules & Subscribed Lists**: Toggle curated filter lists and author custom Adblock syntax rules (`||domain^` and `@@||exception^`).

---

## 3. Incognito, Biometric Security & Local Vault
- **Distinct Incognito Visual Identity**: Dedicated dark slate aesthetic with custom **Incognito Spects** emblem across the top bar, omnibox, tab switcher, and quick controls; zero history/cookies persisted.
- **Biometric Incognito Lock**: Require Android `BiometricPrompt` authentication when opening or returning to Incognito tabs.
- **Local Passwords & API Keys Vault**: Device-only Room vault with **Biometric Vault Lock**, inline credential auto-fill toggle, and masked secret protection.
- **Multi-Profile Personas**: Switch between **Personal**, **Work**, and **Anonymous** profile identities.
- **One-Tap "Burn Session" & Wipe-on-Exit**: Animated instant wipe of all open tabs, history, cache, and cookies, plus optional automatic wipe on app exit.

---

## 4. Tab Management & Workspace Productivity
- **Tab Groups & Pinned Tabs**: Organize tabs into custom or preset groups (`Work`, `Research`, `Reading`, `Shopping`) and pin vital tabs at the top.
- **Undo Close Tab**: Reopen accidentally closed tabs immediately from the Snackbar **Undo** button or **Reopen** control in the Tab Drawer.
- **Tab Search & Auto-Close Inactive Tabs**: Filter open tabs by title/URL/domain and automatically or manually sweep stale tabs (`7 / 14 / 30 days`).
- **Bottom Address Bar Mode**: Optional ergonomic bottom-docked omnibox directly above the navigation bar.
- **Auto-Hide Toolbars on Scroll**: Distraction-free immersive viewport that collapses toolbars while scrolling down articles.

---

## 5. Reading, Documents, Media & Translation
- **Editorial Reader Mode**: Clutter-free distraction-free article reader with adjustable font size (`13sp–24sp`).
- **Reading List ("Read Later")**: Dedicated Library tab for saved articles with unread/done filtering, excerpts, and estimated read times.
- **Read Aloud (TTS)**: Built-in Text-to-Speech narration with Play/Pause, paragraph navigation, and speed cycling (`0.8x–2.0x`).
- **Built-in Page & Text Translator**: 1-tap full-page translation (with Original restore) plus instant snippet translation across 9 languages.
- **Built-in PDF Viewer**: In-app `PdfRenderer` document reader with zoom controls, night contrast invert mode, and external viewer handoff for both web `.pdf` links and downloaded PDFs.
- **Print, View Source & Save Offline**: System print preview (`PrintManager`), formatted HTML source viewer (`PageSourceDialog`), and offline `.mht`/`.html` webpage saving into Downloads.
- **Picture-in-Picture (PiP) & Media Controls**: Fullscreen HTML5 `<video>`, floating Play/Pause pill, and Android PiP minimization.

---

## 6. QR Utilities, Library, Notifications & Widgets
- **Offline QR Code Generator & Camera QR Scanner**: Share clean URLs as crisp scannable QR bitmaps or scan codes via CameraX + ML Kit.
- **Link Long-Press Context Menu**: Long-press webpage links or images to open in New/Private tab, save to Reading List, copy clean URL, or generate a QR code.
- **Full Library Hub**: Browse **History** (time filters, Markdown export, multi-select delete), **Downloads** (real `StatFs` storage breakdown, category chips, open/share), **Bookmarks** (folders, pin-to-home, Netscape HTML import/export), and **Read Later**.
- **Notification Controls & Home Screen Widget**: Fine-grained alert toggles (`POST_NOTIFICATIONS`) plus an Android Home Screen Search/Voice/Incognito widget (`RitamSearchWidgetProvider`).
- **Developer Console & Network Inspector (`DevTools`)**: Live JS console logs, interactive JS evaluator, blocked/allowed network request inspector, and `document.cookie` + `localStorage` viewer.

# Ritam Browser — Feature Status Report

Audit date: 2026-11-24 · Base commit: `cdbef63` (+ local changes described at the end)

## How this was verified

| Symbol | Meaning |
| --- | --- |
| ✅ | Covered by an automated test that passes in this repository |
| 🟢 | Implemented and wired end-to-end in code (UI → ViewModel → engine/service); not exercised at runtime in this audit |
| 🟡 | Partial: the feature exists but a meaningful part is missing, limited, or unverified |
| 🔴 | Missing or dead: advertised or exposed in UI, but no working implementation |

Method: full read of the 60 Kotlin files (~29 k LOC), call-graph tracing from UI entry points, and
`gradle :app:testDebugUnitTest` (43 tests). Runtime smoke testing on an emulator was limited to
install + cold start — the headless emulator had no DNS, so no live page could load. Nothing below is
claimed as runtime-verified unless it is marked ✅.

**Totals: 43 tests green.** After the fixes in this pass, the settings pipeline is test-covered.

> The existing product docs (`featureslist.md`, `working.md`, `README.md` in `docs/`) describe intent
> and in-app catalog copy. Where they disagree with this report, this report reflects the code as it
> exists at the commit above.

---

## 1. Core browsing

| Feature | Status | Evidence / notes |
| --- | --- | --- |
| Tab management (new/close/switch/undo/pin/groups, inactive-tab cleanup) | 🟢 | `MainViewModel` 848–993, `TabSwitcherSheet` |
| Split-screen dual tabs | 🟢 | `splitTab` state consumed by `MainActivity` (line 339) |
| Private / incognito tabs | 🟢 | `TabState.isPrivate`, HC stats skipped |
| Omnibox with 5 search engines, URL heuristics | 🟢 | `SearchEngine.buildQueryUrl`, `navigateToInput` |
| Voice search, QR search | 🟢 | `MainActivity.launchVoiceRecognition`, `QrScannerScreen` |
| Desktop mode + per-tab zoom | 🟢 | `WebViewManager.applyDesktopUserAgent`, `zoomByTab` |
| Find in page | 🟢 | `WebViewManager` findAll + `MainViewModel.findNext` |
| Reader mode, font size, bionic text, column width | 🟢 | `ReaderModeView`, reader prefs are read |
| SSL error + malicious-site warnings | 🟢 | `WebViewManager.onReceivedSslError`, warning dialogs |
| Session restore on startup | 🔴 | `restoreTabsOnStartup` is persisted but never consumed; tabs live only in memory. The in-app catalog claims “Session Restore & Crash Recovery”. |
| Omni command palette | 🟢 | `CommandPaletteDialog`, `setCommandPaletteOpen` |
| Back/forward/reload/home gestures, pull-to-refresh | 🟢 | gesture prefs are read in `WebViewManager` |

## 2. Privacy & shields

| Feature | Status | Evidence / notes |
| --- | --- | --- |
| ABP ad blocking (EasyList, EasyPrivacy, Peter Lowe) | ✅ | 14 matcher tests + 3 real-list tests; 110,497 network rules |
| Cosmetic element hiding | ✅ | CSS assertions in `RealListAdBlockTest` |
| Offline bundled lists + 24 h upstream refresh | ✅ | `AdBlockFilterStore`; refresh failure handling added |
| Custom filter rules (`||`, `@@`, `##`, tokens) | 🟢 | `parseAdblockRule`, streamed from Room |
| Per-site shields + HTTPS-only exceptions | 🟢 | `WebsiteSettingsEntity`, `shouldInterceptRequest` |
| Ad/tracker toggles and all 57 settings | ✅ | `BrowserPreferencesTest` — **these were broken; see “Fixed in this pass”** |
| HTTPS upgrade | 🟢 | `maybeUpgradeToHttps` |
| Third-party cookie blocking | 🟡 | Toggle now applies; enforcement relies on WebView cookie policy only |
| Fingerprint randomizer / WebRTC / covert-API lockdown / strict referrer | 🟡 | User agent + JS injection hardening; not a real fingerprinting defence (native WebRTC still present) |
| DoH resolver (Cloudflare/Quad9/AdGuard/Google/Custom) | 🟡 | `DohResolver` resolves and caches per request, but WebView still performs its own system-DNS resolution — DoH is a block check, not encrypted transport for page loads |
| Threat intelligence (phishing) | 🟡 | Only 5 test domains, no feed |
| Adult content filter | 🟡 | Always on (`RitamShieldsEngine` 1051–1058), no setting; keyword rules can hit legitimate URLs containing “sex” |
| Clear-on-exit, burn session, ephemeral containers | 🟢 | `wipeOnExitIfEnabled`, `burnSession` |
| Screen security flag (FLAG_SECURE) | 🟢 | `MainActivity` 378–380 |

## 3. Content tools

| Feature | Status | Evidence / notes |
| --- | --- | --- |
| PDF viewer (PdfRenderer, zoom, invert) | 🟢 | `PdfViewerDialog`, `PdfDocumentHelper` |
| Universal viewer: DOCX/PPTX/XLSX/CSV/JSON/MD | ✅ | `ExampleUnitTest` parses in-memory archives (now under Robolectric) |
| RSS reader (subscribe, parse, read state) | 🟢 | `RssFeedHelper`, `LibraryTab.FEEDS` |
| Read-aloud (TTS + media notification) | 🟢 | `ReadAloudController`, `MediaSessionHelper` |
| Page translation | 🟡 | `TranslationHelper` uses the free MyMemory API (rate-limited, 6 s timeouts, no key/offline fallback); page-level translation is a URL/JS hand-off |
| Watchlist / page-diff tracker | 🟢 | SHA-256 content hash compare in `MainViewModel` 2223–2260 |
| Highlights + export | 🟢 | `HighlightDao`, Library tab |
| Reading list | 🟢 | Room + Library |
| Userscripts (URL-pattern matched injection) | 🟢 | `WebViewManager` 1025–1043 |
| DevTools: console, network log, DOM source, JS eval, cookies, localStorage | 🟢 | `DevToolsSheet`, `runDevJs` |
| HAR export | 🔴 | Network inspector exists; no HAR file export |
| Offline page save / MHTML export | 🔴 | No implementation; catalog claims “Offline MHTML Web Archive Exporter” |
| AI page summarizer & Q&A | 🔴 | No AI dependency or API call exists (Firebase AI deps are commented out); catalog advertises it |
| Ad zapper / element picker | 🔴 | Catalog entry only |
| Audio booster / equalizer | 🔴 | Catalog entry only |
| QR code generator for page/URL | ✅ | `ExampleUnitTest` validates matrix output |

## 4. Library & storage

| Feature | Status | Evidence / notes |
| --- | --- | --- |
| History (filters, delete, bulk delete, clear) | 🟢 | `LibraryScreen`, `HistoryDao` |
| Bookmarks + folders + HTML import/export | 🟢 | `BrowserRepository.importBookmarksFromHtml`, export via SAF |
| Downloads (queue, pause/resume via HTTP Range, cancel, open, share) | 🟢 | `BrowserDownloadManager` |
| Storage stats + cache clearing | 🟢 | `RitamCacheManager`, `refreshStorageStats` |
| PWA shortcuts | 🟢 | `PwaShortcutHelper`, intent handling |
| Home-screen search widget | 🟢 | `RitamSearchWidgetProvider` |

## 5. Personalisation & accessibility

| Feature | Status | Evidence / notes |
| --- | --- | --- |
| 4 themes, accents, OLED black, font pairs, density, corner/omnibox shape | 🟢 | All read from DataStore and applied in `Theme.kt` |
| Force-dark on websites | 🟢 | `forceWebDarkScript` + luminance analysis |
| Bottom omnibox, auto-hide toolbars, swipe gestures | 🟢 | prefs consumed in Compose/WebView |
| Split screen, PiP, media speed/boost, fullscreen video | 🟢 | `MediaSessionHelper`, PiP params in `MainActivity` |

## 6. Security & biometrics

| Feature | Status | Evidence / notes |
| --- | --- | --- |
| Biometric vault lock | 🔴 | `BiometricAuthHelper` and `BiometricLockScreenOverlay` are **never called**; `isVaultUnlocked` is never read by UI; `vaultEntries` never rendered. The toggle now persists, but there is no prompt and no vault screen. |
| Biometric incognito lock | 🔴 | Same: setting + lock screen exist, overlay is unused |
| Password generator | 🔴 | `PasswordGenerator` has zero call sites |
| Encrypted vault storage | 🟡 | `CryptoVaultHelper` is used by a save path, but entries are unreachable from the UI |
| Autofill setting | 🔴 | Toggle exists; `autofillEnabled` has no consumer |
| Site permissions manager | 🟢 | `WebsiteSettingsDao`, permission prompt flow |

## Defects found and fixed in this pass

1. **Dead settings pipeline (high impact).** `PreferencesRepository.preferences` persisted 57 keys but
   re-read ~25 of them as hardcoded literals (`adBlockingEnabled = true`, `dnsProvider = SYSTEM`,
   `biometricIncognitoLock = false`, …). Turning off ad blocking, trackers, HTTPS-only, JS, secure
   DNS or changing the profile had no effect, and switches snapped back. All keys are now read; a
   regression test (`BrowserPreferencesTest`, 3 cases) fails if literals come back.
2. **Unprotected coroutine failures in the shields engine.** Initial filter loading and Room stream
   collectors ran inside bare `launch` blocks, so any I/O exception would reach the default uncaught
   exception handler and crash the app. They are now guarded and logged.
3. **Silent filter-list failures.** Asset load, download, validation and write errors were all
   discarded with `getOrNull()`. Each stage now logs a specific reason (release builds included) so a
   broken list update is diagnosable.
4. **Six duplicate OkHttp clients.** RSS, watchlist, translation, structured viewer, downloads and the
   filter store each built their own client (own pool, dispatcher, threads). They now derive from
   `RitamHttp.base` and share one pool/dispatcher; DoH keeps an isolated client on purpose.
5. **Crash-prone force-unwraps** in `UniversalViewerDialog` (`loadError!!`, `bmp!!`) replaced with
   local vals and smart casts.
6. **Broken test suite.** `ExampleRobolectricTest` requested SDK 36 (needs JDK 21, build runs JDK 17)
   and `ExampleUnitTest` used Android’s `XmlPullParser` without Robolectric. Both failures predate
   this audit and now pass; the suite is trustworthy again.

## Remaining tech debt (ranked by risk)

| # | Issue | Where | Why it matters |
| --- | --- | --- | --- |
| 1 | `MainViewModel` is a 2,686-line god object with ~200 settings pass-throughs, tab state, media, devtools, vault and download control | `ui/MainViewModel.kt` | Every UI change risks unrelated regressions; untestable as a unit |
| 2 | `fallbackToDestructiveMigration` is enabled even though migrations 1→5 exist | `data/local/RitamDatabase.kt:225` | A schema mistake silently wipes user history/bookmarks |
| 3 | `RitamShieldsEngine` is 1,125 lines mixing hardcoded ad lists, adult filtering, JS payloads, DoH and cosmetics | `privacy/` | Duplicates the filter engine, hard to audit; adult list is unreachable from settings |
| 4 | No tab/session persistence layer | `MainViewModel` tabs | Session restore cannot be implemented without it |
| 5 | Seed rule counts are stale literals (412/286/194) | `RitamDatabase.seedInitialData` | Settings show wrong numbers until the first sync |
| 6 | Zero instrumentation/UI tests; Robolectric used only for 5 tests | `app/src/test` | Compose flows, WebView wiring and intents are unverified |
| 7 | WebView settings and JS payloads are duplicated across manager/engine | `WebViewManager`, `RitamShieldsEngine` | Two sources of truth for hardening scripts |
| 8 | `syncFilterListRuleCount` dead code | `WatchlistAndSyncHelper` | Confusing leftover |

## Future scope

1. **Settings/config layer:** move option state into a `SettingsController` and split `MainViewModel`
   by feature (tabs, library, media, shields). Prerequisite for maintainable growth.
2. **Real session persistence:** serialize `TabState` to DataStore/Room, restore on cold start, and
   use it for crash recovery.
3. **Biometrics for real:** route vault/incognito through `BiometricAuthHelper`, render
   `BiometricLockScreenOverlay`, and add a vault screen using `CryptoVaultHelper` + `vaultEntries`.
   Wire or remove `PasswordGenerator` and `autofillEnabled`.
4. **DNS that actually protects page loads:** the app-level DoH check cannot change WebView's
   resolver; real options are a per-app VPN/DNS service or removing the “Secure DNS” claim for page
   traffic.
5. **Background work with WorkManager:** daily filter refresh, RSS refresh, watchlist re-checks —
   currently only run when the app is open.
6. **In-app feature catalog accuracy:** correct or remove the AI summarizer, MHTML export, equalizer,
   ad zapper and session-restore entries so the docs match the binary.
7. **CI:** GitHub Actions running `testDebugUnitTest` + `assembleRelease` on JDK 17, plus instrumented
   tests on an API 34 emulator with a DNS-capable network.

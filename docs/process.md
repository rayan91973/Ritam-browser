# Processes & Lifecycle Pipelines (`process.md`)

This document describes the operational workflows, request lifecycles, data management processes, and development/release loops in Ritam Browser.

---

## 1. URL Resolution & Navigation Process

When a user enters text in the omnibox, taps a bookmark, scans a QR code, or clicks a widget action:

```
User Input / Intent / QR Scan
        │
        ▼
[1] Malicious Check ──(Blocked Host)──► MaliciousDomainDialog ("Return Safely")
        │
       Safe
        │
        ▼
[2] Tracking Stripper (if stripTrackingParams = true)
    UrlCleaner.cleanUrl() removes utm_*, fbclid, gclid, etc.
        │
        ▼
[3] HTTPS-Only Upgrade (if enabled for global/site)
    http:// ──► https://
        │
        ▼
[4] Search vs. Direct URL Routing (`looksLikeUrl()`)
    ├── Contains spaces / no TLD ──► SearchEngine.buildQueryUrl(query)
    └── Valid domain or scheme    ──► Load URL directly
        │
        ▼
[5] PDF Interception Check
    Ends with .pdf? ──► Download to cache ──► Open PdfViewerDialog
        │
       HTML/Web
        │
        ▼
[6] WebView Load (`WebViewManager.loadUrl`)
    ├── Apply Per-Site Zoom (`zoomPercent`) & Desktop UA
    ├── Intercept sub-resources (`RitamShieldsEngine`)
    └── On Finish: Inject Cosmetic Ad-Block + Cookie Reject + Force Dark CSS
        │
        ▼
[7] Persistence (Non-Private Tabs Only)
    Record visit in Room (`history` & `search_history` tables)
```

---

## 2. External App & Deep-Link Interception Process

When a webpage redirects to an `intent://`, `mailto:`, `tel:`, `market://`, or custom app scheme:
1. `WebViewClient.shouldOverrideUrlLoading` intercepts non-`http(s)` schemes.
2. If `confirmExternalAppLaunch` is enabled (default), `WebViewManager` extracts a readable app label and any embedded `S.browser_fallback_url` from Android `intent://` syntax.
3. `MainViewModel` sets `externalAppRequest`, presenting `ExternalAppConfirmDialog`:
   - **Open App** -> Dispatches Android `ACTION_VIEW` Intent with `FLAG_ACTIVITY_NEW_TASK`.
   - **Load Web Page** -> Loads `fallbackWebUrl` safely inside the active browser tab.
   - **Stay Here** -> Dismisses cleanly without leaving Ritam Browser.

---

## 3. Tab Lifecycle & Undo/Auto-Close Process

1. **Tab Creation**: `createNewTab(isPrivate)` initializes a `TabState` inheriting `defaultDesktopMode` when regular, or clean ephemeral defaults when private.
2. **Closing & Undo Stack**: Closing a regular tab records a snapshot in `recentlyClosedStack` and triggers a short Snackbar with an **Undo** action (`undoCloseTab()`), as well as a **Reopen** button inside `TabSwitcherOverlay`. Private tabs are never pushed to the Undo stack.
3. **Pinned Tabs Protection**: Pinned tabs (`isPinned = true`) float to the top of the Tab Drawer and are immune to **Close All Regular Tabs** and **Sweep Inactive** sweeps.
4. **Inactive Tab Cleanup**: When `autoCloseTabsDays` (`7`, `14`, or `30`) is configured, non-pinned regular tabs whose `lastAccessedAt` is older than the threshold are flagged as `INACTIVE` in the Tab Drawer and swept automatically at launch or manually via **Sweep Inactive**.

---

## 4. Database Schema Migration Process (Room v1 -> v2)

Persistence uses `RitamDatabase` (`com.example.data.local.RitamDatabase`):
- **Version 1**: `history`, `bookmarks`, `bookmark_folders`, `downloads`, `website_settings` (`domain`, `shieldsEnabled`, `javascriptEnabled`, `desktopMode`, `cookiesAllowed`, `httpsOnly`, `updatedAt`), `filter_lists`, `custom_filters`, `blocking_stats`, `search_history`, `vault_entries`.
- **Version 2 (`MIGRATION_1_2`)**:
  1. `ALTER TABLE website_settings ADD COLUMN zoomPercent INTEGER NOT NULL DEFAULT 100`
  2. `ALTER TABLE website_settings ADD COLUMN forceDarkOverride INTEGER NOT NULL DEFAULT -1`
  3. `CREATE TABLE IF NOT EXISTS reading_list (...)` with unique index on `url` and index on `savedAt`.

Schema defaults are seeded lazily (`BrowserRepository.ensureSeedData()`) on app startup for default bookmark folders, starter Sanctuary bookmarks, and built-in filter lists.

---

## 5. Session Burn & Wipe-on-Exit Process

- **Manual Burn (`burnSession()`)**:
  1. Displays animated `BurnSessionOverlay` (`700ms` sequence).
  2. Stops `ReadAloudController` TTS speech and pauses all WebView audio/video (`pauseAllMedia()`).
  3. Destroys every live `WebView` instance in `WebViewManager`.
  4. Clears `recentlyClosedStack`, DevTools console/network logs, Room `history` and `search_history`, and WebView cache/cookies/WebStorage.
  5. Spawns a single fresh Home tab.
- **Automatic Wipe-on-Exit (`wipeOnExitIfEnabled()`)**: Triggered in `MainActivity.onStop()` when `isFinishing == true` and `preferences.clearOnExit == true`.

---

## 6. Build & Verification Process

1. **Code Edit Cycle**: Modify Kotlin/Compose files cleanly using constructor DI and Room/DataStore flows.
2. **Compilation Check**: Execute `compile_applet` (or `gradle assembleDebug`) to verify Kotlin, KSP (Room + Moshi codegen), resource merging, and manifest processing.
3. **Unit & Regression Tests**: Execute `gradle :app:testDebugUnitTest --tests com.example.ExampleUnitTest` for fast JVM verification of domain classifiers, QR encoding, DoH providers, and language mappings (and `gradle :app:testDebugUnitTest` when Robolectric artifacts are cached).
4. **APK Packaging**: Debug builds sign with `debug.keystore`; release builds sign with `release` `signingConfigs` driven by environment variables (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`).

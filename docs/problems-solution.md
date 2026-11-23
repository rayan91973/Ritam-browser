# Problems & Solutions Log (`problems-solution.md`)

Key technical issues encountered while building Ritam Browser and how each was solved cleanly.

---

## 1. Desktop Mode Only Changed User-Agent String
- **Problem**: Many responsive websites inspect CSS viewport width (`window.innerWidth`) rather than just the HTTP `User-Agent` header. Setting a desktop UA alone still rendered mobile layouts inside `WebView`.
- **Solution**: Updated `WebViewManager.applyWebSettings()` and `setDesktopMode()` to simultaneously apply a desktop Linux Chrome User-Agent, enable `settings.useWideViewPort = true` and `settings.loadWithOverviewMode = true`, and inject a JavaScript `<meta name="viewport" content="width=1280, initial-scale=0.5">` normalization script on `onPageFinished` before triggering reload.

---

## 2. Multi-Tab Memory Bloat from Keeping All WebViews Alive
- **Problem**: Each Android `WebView` spins up heavyweight Chromium renderer resources. Keeping 10+ background `WebView` objects attached degrades scrolling FPS and risks OutOfMemory errors.
- **Solution**: Implemented a bounded **5-slot LRU `WebView` pool** in `WebViewManager`. Tab metadata (`TabState`) stays lightweight in `MainViewModel`, while `WebViewManager` evicts and destroys the oldest background `WebView` when exceeding 5 instances and lazily restores tabs upon selection.

---

## 3. Accidental Overwrite When Tapping the Address Bar on Active Webpages
- **Problem**: When a user taps the omnibox on an open page just to copy or tweak the URL, immediately replacing the field with a blank query forces them to cancel or retype.
- **Solution**: Added a contextual action strip at the top of `OmniboxSearchOverlay` featuring **Copy Address** (automatically stripped via `UrlCleaner`), **Edit Address** (populates cursor at end for quick query/path edits), **QR Code**, and **Read Later**.

---

## 4. Incognito Mode Was Hard to Distinguish at a Glance
- **Problem**: Tinting a single pill slightly darker left users unsure whether a tab was private or regular.
- **Solution**: Designed a dedicated `IncognitoColors` dark obsidian slate palette, created the custom `IncognitoSpectsEmblem` Canvas graphic, added a persistent top **Incognito Identity Bar** on private tabs, styled private rows separately in `TabSwitcherOverlay`, and gated access with optional `BiometricPrompt` locking.

---

## 5. Offline QR Code Sharing Without Bloating APK Dependencies
- **Problem**: Adding heavyweight barcode encoding libraries just to generate shareable URL QR codes increases APK size and dependency surface.
- **Solution**: Authored a clean, self-contained **pure-Kotlin QR Code encoder (`QrCodeGenerator`)** supporting Byte mode, ECC Level L, Reed-Solomon error correction, Versions 1–10, and penalty-scored mask selection—tested via `ExampleUnitTest`.

---

## 6. Viewing PDFs Without Requiring Storage Permissions or External Apps
- **Problem**: Web links ending in `.pdf` or downloaded PDF files typically fail in Android `WebView` or require third-party apps, while broad storage permissions violate Google Play policies.
- **Solution**: Built `PdfDocumentHelper` + `PdfViewerDialog` powered by Android's native `android.graphics.pdf.PdfRenderer`. Remote `.pdf` navigations download securely into `context.cacheDir` via `OkHttpClient`, while downloaded files open directly from app-private storage with zoom and night-contrast inversion, plus optional `FileProvider` external handoff.

---

## 7. Tracking Parameters Surviving Copy, Share, and Reading List Actions
- **Problem**: Navigating or sharing links copied from search results or social feeds often carries `utm_*`, `fbclid`, `gclid`, and `si` trackers.
- **Solution**: Built `UrlCleaner.cleanUrl()` and wired it automatically into navigation resolution, clean URL copying, QR code generation, link long-press actions, and Reading List persistence.

---

## 8. Schema Evolution from Room v1 to v2 Without Destructive Wipe
- **Problem**: Adding `ReadingListItemEntity` plus `zoomPercent` and `forceDarkOverride` columns to `website_settings` would crash existing installs without a proper migration.
- **Solution**: Defined explicit `MIGRATION_1_2` SQL statements in `RitamDatabase` (`ALTER TABLE website_settings ADD COLUMN ...` and `CREATE TABLE IF NOT EXISTS reading_list ...` with matching indices) so user bookmarks, vault items, and filter rules remain intact across upgrades.

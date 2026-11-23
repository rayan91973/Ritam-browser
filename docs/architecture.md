# Architecture Overview (`architecture.md`)

Ritam Browser follows an **Offline-First Clean MVVM + Repository Architecture** tailored for Android Jetpack Compose and multi-tab WebView state isolation.

---

## 1. High-Level Layer Diagram

```
┌───────────────────────────────────────────────────────────────────────────┐
│                         UI Layer (Jetpack Compose)                        │
│  MainActivity.kt (NavHost, PiP, Biometric Host, Lifecycle Lock/Wipe)      │
│  Screens: BrowserScreen, LibraryScreen, SettingsAnalyticsScreen,          │
│           ReaderModeView, QrScannerScreen                                 │
│  Components: OmniboxPill, BottomNavDock, QuickControlsSheet,              │
│              TabSwitcherOverlay, DevToolsSheet, PdfViewerDialog, Dialogs  │
└───────────────────────────────────┬───────────────────────────────────────┘
                                    │ StateFlow / Events
                                    ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                    Presentation Layer (`MainViewModel`)                   │
│  Coordinates activeTab, tabs, undo stack, ReadAloud, DevTools logs,       │
│  PDF viewer state, Translate dialog, DoH latency test, Burn sequence      │
└───────────┬───────────────────────────────┬───────────────────────────────┘
            │                               │
            ▼                               ▼
┌───────────────────────────┐   ┌───────────────────────────────────────────┐
│   Browser & Privacy Core  │   │      Domain / Services / Repositories     │
│  - WebViewManager (LRU 5) │   │  - BrowserRepository (Room DAOs)          │
│  - RitamShieldsEngine     │   │  - PreferencesRepository (DataStore)      │
│  - UrlCleaner             │   │  - BrowserDownloadManager / PdfDocHelper  │
│  - DohResolver (OkHttp)   │   │  - ReadAloudController / TranslationHelper│
└───────────────────────────┘   │  - BiometricAuthHelper / QrCodeGenerator  │
                                └───────────────────┬───────────────────────┘
                                                    │
                                                    ▼
                                ┌───────────────────────────────────────────┐
                                │   Persistence Layer (Device-Only Local)   │
                                │  - Room Database (`RitamDatabase` v2)     │
                                │  - Preferences DataStore (`ritam_prefs`)  │
                                └───────────────────────────────────────────┘
```

---

## 2. Package Breakdown (`com.example`)

| Package | Key Files | Responsibility |
| :--- | :--- | :--- |
| `com.example` | `RitamApplication.kt`, `MainActivity.kt` | Manual DI (`RitamContainer`), Compose `NavHost`, PiP handling, lifecycle biometric lock & wipe-on-exit. |
| `com.example.browser` | `BrowserModels.kt`, `WebViewManager.kt` | `TabState`, DevTools & warning models, 5-slot LRU `WebView` pool, JS/CSS injections, print/source/save-page exporters. |
| `com.example.privacy` | `RitamShieldsEngine.kt`, `UrlCleaner.kt`, `DohResolver.kt` | Request blocking, cosmetic/cookie/dark-mode JS/CSS generation, query parameter sanitizer, RFC 8484 DoH JSON resolver. |
| `com.example.data.local` | `Entities.kt`, `Daos.kt`, `RitamDatabase.kt` | Room v2 entities, DAOs, and `MIGRATION_1_2`. |
| `com.example.data.preferences` | `BrowserPreferences.kt`, `PreferencesRepository.kt` | Typed `BrowserPreferences` model (theme, font, topology, shields, DoH, tabs, translation) backed by DataStore. |
| `com.example.data.repository` | `BrowserRepository.kt` | Unified data operations, default seed data, Netscape HTML bookmark import/export, Markdown history export. |
| `com.example.services` | `BiometricAuthHelper`, `BrowserDownloadManager`, `PdfDocumentHelper`, `QrCodeGenerator`, `ReadAloudController`, `RitamNotificationManager`, `TranslationHelper` | Hardware/OS integrations, download/storage metrics, `PdfRenderer` wrapper, pure-Kotlin QR encoder, TTS narration, notifications, translation API. |
| `com.example.ui` | `MainViewModel.kt` | Single source of truth exposing reactive `StateFlow`s to Compose UI. |
| `com.example.ui.screens` | `BrowserScreen`, `LibraryScreen`, `SettingsAnalyticsScreen`, `SettingsSections`, `ReaderModeView`, `QrScannerScreen` | Top-level screen composables and modular settings cards. |
| `com.example.ui.components` | `OmniboxPill`, `BottomNavDock`, `QuickControlsSheet`, `TabSwitcherSheet`, `DevToolsSheet`, `PdfViewerDialog`, `FeatureDialogs`, `BrowserDialogs`, `PageToolsOverlays`, `LotusEmblem`, `IncognitoAndEngineIcons` | Reusable M3 components, overlays, dialogs, and custom vector artwork. |
| `com.example.ui.theme` | `Color.kt`, `Theme.kt`, `Type.kt` | Palette system (Serene Sand, Onyx Forest, Solarized Warm, System Follow), Accent tones, `LocalRitamExtendedColors`, bundled `.ttf` typography. |
| `com.example.widget` | `RitamSearchWidgetProvider.kt` | Android Home Screen widget dispatching Search, Voice, and Private Tab intents. |

---

## 3. Database Schema (`RitamDatabase` v2)

| Table | Entity | Purpose |
| :--- | :--- | :--- |
| `history` | `HistoryEntity` | Timestamped page visits with title, URL, and domain (never written for private tabs). |
| `bookmarks` | `BookmarkEntity` | Saved pages with folder reference and `isPinnedHome` flag. |
| `bookmark_folders` | `BookmarkFolderEntity` | User-created and default bookmark folders. |
| `downloads` | `DownloadEntity` | Transfer status, `DownloadCategory`, local file path, MIME type, byte totals. |
| `website_settings` | `WebsiteSettingsEntity` | Per-domain overrides: `shieldsEnabled`, `javascriptEnabled`, `desktopMode`, `httpsOnly`, `zoomPercent`, `forceDarkOverride`. |
| `reading_list` | `ReadingListItemEntity` | Read Later queue (`url`, `title`, `domain`, `excerpt`, `readTimeMinutes`, `isRead`). |
| `filter_lists` | `FilterListEntity` | Subscribed shield filter lists with rule counts and enable toggle. |
| `custom_filters` | `CustomFilterRuleEntity` | User-defined Adblock syntax filter rules. |
| `blocking_stats` | `BlockingStatsEntity` | Daily domain-level counters for blocked Ads, Trackers, Malicious hosts, HTTPS upgrades, and bytes saved. |
| `search_history` | `SearchHistoryEntity` | Recent omnibox searches for quick suggestions. |
| `vault_entries` | `VaultEntryEntity` | Device-local saved logins, API keys, and passkey labels. |

---

## 4. Navigation Routes (`RitamRoutes`)

Managed inside `MainActivity.kt` using Compose Navigation:
- `browser` — Primary `BrowserScreen` (Home sanctuary / Active WebView / Reader Mode).
- `library/{tab}` — `LibraryScreen` routed directly to `ALL`, `HISTORY`, `DOWNLOADS`, `BOOKMARKS`, or `READING_LIST`.
- `settings/{section}` — `SettingsAnalyticsScreen` focused on `ALL`, `ANALYTICS`, `APPEARANCE`, `SHIELDS`, `NOTIFICATIONS`, `GESTURES`, `VAULT`, or `BROWSER`.
- `qr_scanner` — Fullscreen CameraX + ML Kit QR barcode scanner.

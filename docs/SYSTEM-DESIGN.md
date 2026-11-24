# Ritam Browser — System Design (implementation view)

Ritam is a single-module Android app (Kotlin, Jetpack Compose, Room, DataStore, OkHttp, WebView).
There is no DI framework: `AppContainer` in `RitamApplication.kt` is the composition root and the
application-scoped singletons hang off it.

```
Compose UI (ui/screens, ui/components)
        │  state  ↑            ↓ events
        └── MainViewModel (AndroidViewModel, ui/MainViewModel.kt)
                 │
   ┌─────────────┼───────────────┬──────────────────┬────────────────┐
BrowserRepository  WebViewManager  RitamShieldsEngine  Platform services
(Room DAOs)        (WebView pool)  (filter/DNS/shields) (downloads, media,
                                                          cache, viewers)
```

## Module map

| Package | Responsibility |
| --- | --- |
| `com.example` | `MainActivity` (single activity), `RitamApplication` + `AppContainer` |
| `browser` | `WebViewManager` (per-tab `WebView` lifecycle, clients, JS injection), `BrowserModels` (`TabState`, `BlockDecision`, …) |
| `data.local` | Room database, entities, DAOs, migrations, first-run seed data |
| `data.repository` | `BrowserRepository` — the only writer of Room data; exposes `Flow`s |
| `data.preferences` | DataStore-backed `BrowserPreferences`, enums for theming/search |
| `privacy` | `RitamShieldsEngine` (request interception, DNS, cosmetics), `DohResolver`, `UrlCleaner`, homograph guard |
| `privacy.adblock` | ABP filter engine: parser, rule model, compiled index, list store |
| `services` | Downloads, PDF/Office/structured viewers, read-aloud (TTS), media session, notifications, QR, RSS, translation, cache, PWA shortcuts |
| `ui` | `MainViewModel` plus screens/sheets/dialogs |
| `widget` | Home-screen search widget provider |

The dependency direction is one-way: UI → ViewModel → repository/engine/services. Services never
reference the UI; the engine never touches Compose.

## Request path (the hot path)

1. `WebViewManager` installs a `WebViewClient` per tab. `shouldInterceptRequest` routes to
   `RitamShieldsEngine.shouldInterceptRequest(url, pageDomain, isPrivate, requestType)`.
2. `requestType` comes from the request's `Accept` header (`RequestType.classify`), so the engine
   can apply `$script`, `$image`, `$xmlhttprequest` etc. without guessing from the URL.
3. Decision order in the engine:
   per-site exception → custom allow rules → filter-list page whitelist (`@@…$document`) → ABP
   network rules → curated ad/tracker fallbacks → adult filter → DoH block check.
4. A block returns a synthetic `WebResourceResponse` (empty `204`), records one `BlockingStatsEntity`
   row and increments in-memory session metrics. No network I/O happens for blocked requests.
5. Cosmetic filtering is injected as a stylesheet on page commit/finish; the engine compiles
   domain-scoped rules for the page plus a capped set of generic rules, cached per domain.

The ABP engine is immutable once built (`AdBlockEngine`), so it is safe to read from the WebView
thread. Rebuilds are published through a `@Volatile` field; in-flight lookups keep the old snapshot.

### Filter list lifecycle

`AdBlockFilterStore` loads bundled assets from `app/src/main/assets/filters` on first launch, then
refreshes from upstream every 24 h into `filesDir/filterlists`. A downloaded copy is used only if it
is a plausible list (>20 KB, contains `##`/`||`, not HTML), so a captive-portal page can never
replace a working list. The bundled copy is the offline fallback.

## Concurrency

- `WebViewManager`, `MainViewModel` and UI run on the main thread; WebView interception callbacks
  run on a WebView worker thread.
- `RitamShieldsEngine` owns a `CoroutineScope(SupervisorJob() + Dispatchers.IO)` for stream
  collection and filter-list compilation. Every collector is wrapped so a database or I/O failure
  cannot take the process down.
- Shared mutable state in the engine is `ConcurrentHashMap` / `@Volatile` / a synchronized rule set.
- Network helpers are `suspend` + `Dispatchers.IO`, returning `Result` where callers need to react.

## Persistence

- **Room** (`ritam_browser.db`, schema v5): history, bookmarks/folders, downloads, website settings,
  filter lists, custom rules, blocking stats, search history, vault, reading list, userscripts,
  watchlist, highlights, RSS, site usage, quick-control usage. Migrations 1→2…4→5 exist, but the
  builder also enables `fallbackToDestructiveMigration` (see tech debt in `FEATURE_STATUS.md`).
- **DataStore** (`ritam_preferences`): every user setting, exposed as a single
  `Flow<BrowserPreferences>`. `BrowserPreferences` is the only settings model the engine and UI read.
- **Files**: download cache, PDF/viewer cache, refreshed filter lists, clipboard-free temp files.

## Error-handling conventions

- `com.example.core.RitamLog` is the logging facade. Debug builds log everything; release builds
  keep warnings/errors only, so page URLs cannot leak into logcat on shipped builds.
- `RitamLog.guarded(tag, action) { … }` wraps background work that must never crash the process
  (filter loading, stream collection).
- Best-effort OS interactions (WebView settings, TTS, notifications) use `runCatching` with a log
  line; user-facing failures go through `MainViewModel.showMessage` / dialog state.
- Network helpers return `Result<T>` and callers surface failures in the UI.
- Injected JavaScript intentionally uses empty `try/catch` — a page-script failure must not break
  the page. Those are not swallowed Kotlin errors.

## HTTP

`com.example.core.RitamHttp.base` is a single process-wide `OkHttpClient`; feature helpers derive
clients with `RitamHttp.builder()` (timeouts/redirects differ) while sharing its connection pool and
dispatcher. `DohResolver` deliberately keeps a private client: DNS traffic is a privacy boundary and
should not share sockets with content requests.

## Build and verification

No Gradle wrapper is committed. Use a local Gradle 9.3.1 install:

```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
KEYSTORE_PATH=ritam-release.jks STORE_PASSWORD=… KEY_PASSWORD=… gradle :app:assembleRelease
```

- `:app:testDebugUnitTest` runs 43 JVM tests. Robolectric-based tests are pinned to SDK 34 so the
  suite runs on JDK 17 (SDK 36 requires JDK 21).
- Release signing reads `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`.
- Filter lists are compiled at runtime; parsing ~3.5 MB of text takes ~0.4 s on a desktop JVM and is
  done once on a background thread.

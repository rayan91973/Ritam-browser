# Ritam Browser

A privacy-focused Android browser built with Kotlin, Jetpack Compose and WebView. It combines a
Brave-style filter-list ad blocker (EasyList, EasyPrivacy, Peter Lowe), DoH lookups, per-site
shields, a reader/translation toolkit, media handling and a full library of bookmarks, history,
downloads, RSS and read-later.

- **[System design](docs/SYSTEM-DESIGN.md)** — module map, request path, concurrency, persistence,
  error-handling conventions (implementation view).
- **[Feature status report](docs/FEATURE_STATUS.md)** — audit of what works, what is partial, what is
  missing, known defects and future scope.
- **[Product docs](docs/architecture.md)** — the original design documents (`architecture.md`,
  `featureslist.md`, `working.md`, `problems-solution.md`, …). These describe intent; where they
  disagree with the audit, the audit reflects the code.

## Requirements

- JDK 17
- Android SDK with platform 34+ (`ANDROID_HOME` set) and build-tools 34+
- Gradle 9.3.1 (no wrapper is committed)

## Build

```bash
# debug
gradle :app:assembleDebug

# release (signing values come from the environment)
KEYSTORE_PATH=ritam-release.jks \
STORE_PASSWORD=your-store-password \
KEY_PASSWORD=your-key-password \
gradle :app:assembleRelease
```

Outputs land in `app/build/outputs/apk/`.

> The original upload keystore password for this project is not available in the repository, so
> release builds made here are signed with `ritam-release.jks` (alias `upload`). Any previously
> installed build signed with a different key must be uninstalled first:
> `adb uninstall com.aistudio.ritambrowser.vkmxqr`.

## Test

```bash
gradle :app:testDebugUnitTest
```

43 JVM tests: ABP engine (14), real filter-list blocking (3), settings round-trip (3), helper
utilities and docx/pptx/xlsx parsing (13), shields engine + Room/persistence (10). Robolectric tests
are pinned to SDK 34 so they run on JDK 17.

## Project layout

```
app/src/main/java/com/example/
  browser/      WebView pool, tab models
  data/         Room database + DAOs, repository, DataStore preferences
  privacy/      shields engine, DoH, URL hygiene, ABP ad blocker (privacy/adblock)
  services/     downloads, viewers, TTS, media, notifications, RSS, cache, QR
  ui/           MainViewModel, Compose screens/components, theme
  widget/       home-screen search widget
app/src/main/assets/filters/   bundled EasyList / EasyPrivacy / Peter Lowe lists
```

## Ad blocking

Filter lists are bundled for first launch and offline use, then refreshed from upstream every 24 h
into app storage. Rules support ABP syntax (anchors, `*`, `^`, options, `@@` exceptions, cosmetic
`##`/`##+js`-free hiding); ~110 k network rules and ~24 k cosmetic rules are indexed at build time
(host hash sets plus token-bucketed pattern rules). Blocked requests never leave the device.

Not implemented: `$popup`, `$csp`, `$removeparam`, `$rewrite` and scriptlet injection — these are
deliberately skipped because they over-block or need a JS engine.

## License / attribution

Filter lists belong to their respective projects: [EasyList](https://easylist.to/),
[EasyPrivacy](https://easylist.to/), and [Peter Lowe’s list](https://pgl.yoyo.org/adservers/).

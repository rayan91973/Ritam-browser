# Ritam Browser — Documentation Hub

**Ritam Browser** (`com.aistudio.ritambrowser.vkmxqr`) is a fast, lightweight, privacy-first Android web browser built with **Kotlin**, **Jetpack Compose (Material 3)**, **Android WebView**, **Room**, and **DataStore**. Its integrated privacy engine (**RITAM Shields**) blocks ads, trackers, cookie consent banners, malicious domains, and URL tracking parameters on-device while offering a calm, editorial reading aesthetic.

---

## Documentation Index

| Document | Purpose |
| :--- | :--- |
| [**Architecture (`architecture.md`)**](./architecture.md) | MVVM + Repository layers, `RitamContainer` DI, Room v2 database schema, DataStore preferences, WebView LRU cache, and Navigation routes. |
| [**How It Works (`working.md`)**](./working.md) | Deep-dive into WebView lifecycle, RITAM Shields interception, DoH resolution, Reader Mode, Biometric lock, PiP, PDF renderer, DevTools, and TTS Read Aloud. |
| [**Processes & Pipelines (`process.md`)**](./process.md) | Request/navigation lifecycle, data persistence & Room migration workflows, Incognito isolation, and build/test verification pipelines. |
| [**Complete Features List (`featureslist.md`)**](./featureslist.md) | Full catalog of core browser capabilities, RITAM Shields, productivity tools, media handling, and developer features. |
| [**UI/UX Guide (`ui-ux.md`)**](./ui-ux.md) | Information architecture, user flows, gesture system, bottom omnibox ergonomics, Incognito visual identity, and accessibility rules. |
| [**Design System (`design.md`)**](./design.md) | Color palettes, accent matrix, typography pairings, corner/omnibox/density topologies, and M3 component specifications. |
| [**Setup & Build Guide (`setup.md`)**](./setup.md) | SDK requirements, Gradle Kotlin DSL toolchain, manifest permissions, build commands, and test execution. |
| [**Problems & Solutions (`problems-solution.md`)**](./problems-solution.md) | Real engineering challenges resolved during development (Desktop UA + viewport, LRU WebView pool, cookie/consent blocking, zero-dep QR, offline PDF rendering). |
| [**Privacy & Security (`privacy-security.md`)**](./privacy-security.md) | Threat model, least-privilege Play-compliant permissions, biometric lock gating, external app launch guard, and local enclave design. |
| [**Testing Strategy (`testing.md`)**](./testing.md) | JVM unit tests, Robolectric Room + Shields verification, Compose `testTag` conventions, and CI-friendly commands. |
| [**Future Scope (`future-scope.md`)**](./future-scope.md) | Planned technical milestones including WebExtensions/userscript support, E2EE sync, WebAuthn hardware passkeys, and foldable/tablet adaptive panes. |

---

## Tech Stack Snapshot

- **Language & Async**: Kotlin (`2.4.21` Compose plugin), Coroutines & Flow (`1.10.2`)
- **UI**: Jetpack Compose (`BOM 2024.09.00`), Material Design 3, Navigation Compose (`2.8.9`), Coil (`2.7.0`)
- **Browser Engine**: Android `WebView` with custom `WebViewClient`, `WebChromeClient`, and 5-slot LRU instance pool (`WebViewManager`)
- **Persistence**: Room (`2.7.0`, schema v2 with KSP) + Jetpack Preferences DataStore (`1.1.7`)
- **Networking**: OkHttp (`4.10.0`) for Encrypted DNS-over-HTTPS (DoH), PDF cache fetching, and snippet translation
- **Hardware & System APIs**: CameraX (`1.5.0`) + ML Kit Barcode Scanning (`17.3.0`) for QR Scanner, Android `BiometricPrompt`, Android `PdfRenderer`, `TextToSpeech`, Picture-in-Picture (`PictureInPictureParams`), PrintManager, and `AppWidgetProvider`
- **Target Platform**: Android (`minSdk = 26`, `targetSdk = 36`, `compileSdk = 36.1`)

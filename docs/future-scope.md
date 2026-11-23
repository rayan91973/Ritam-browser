# Future Scope & Roadmap (`future-scope.md`)

While Ritam Browser delivers a complete offline-first privacy browser on top of Android `WebView`, several architectural evolutions are planned for future milestones.

---

## 1. Engine & Networking Evolution
- **Dedicated GeckoView / Embedded Chromium Content Shell Option**: Abstract `WebViewManager` behind a `BrowserEngineAdapter` interface so future builds can swap in GeckoView or a custom Chromium build with full `Network` header interception, WebRTC IP-leak blocking, and per-tab container isolation.
- **Full DoH Tunneling / Custom Local DNS Socket**: Route *all* `WebView` DNS lookups through an encrypted local DoH resolver socket (or `VpnService`-free `WebViewAssetLoader` + OkHttp response bridge) rather than relying solely on host reputation + cached DoH sinkhole lookups.
- **Automated Filter List Subscriptions**: Background `WorkManager` periodic updates for EasyList, EasyPrivacy, Fanboy's Annoyance List, and Peter Lowe's list with compact bloom-filter/trie indexing.

---

## 2. Extensions & Power-User Capabilities
- **WebExtensions / Userscript Engine**: Sandboxed content-script registry allowing users to install Greasemonkey/Tampermonkey-compatible scripts scoped per domain.
- **Multi-Format Export & Backup**: Encrypted `.ritam` vault archive export combining bookmarks, custom filter rules, site zoom/dark preferences, and reading list items.
- **Developer Tools Remote Profiling**: HAR network trace export from `DevToolsSheet` and live DOM node inspector.

---

## 3. Adaptive Multi-Form-Factor & Ecosystem Features
- **Canonical Tablet & Foldable Support**: Dual-pane List-Detail layout in `LibraryScreen` (bookmarks/history list beside live preview) and `NavigationRail` chrome when `WindowWidthSizeClass.Expanded` is active.
- **Android Credential Provider Service (`AutofillService`)**: Expose the encrypted Room vault through Android's system Autofill framework beyond inline WebView hints.
- **FIDO2 / Passkey Hardware Binding**: Real Android Credential Manager `CreatePublicKeyCredentialRequest` integration for WebAuthn keys.
- **Zero-Knowledge End-to-End Encrypted Sync**: Optional peer-to-peer QR pairing or client-side AES-GCM encrypted sync of bookmarks and reading lists across devices.

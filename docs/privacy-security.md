# Privacy & Security Model (`privacy-security.md`)

Ritam Browser is architected around **local-first data sovereignty**—user history, credentials, filter rules, and blocking telemetry never leave the device.

---

## 1. Core Security Guarantees

1. **Zero Cloud Telemetry / Zero Remote Analytics**: All blocked-ad counters, data-saved calculations, and heap/memory gauges are computed locally via `RitamShieldsEngine` and Android's `Debug` / `Runtime` APIs.
2. **Strict Incognito Isolation**:
   - Private tabs bypass Room `history` and `search_history` inserts.
   - `WebStorage` and cookies are swept when closing private sessions or burning sessions.
3. **Hardware Biometric & Credential Gating (`BiometricAuthHelper`)**:
   - Uses `androidx.biometric` (`BIOMETRIC_STRONG or DEVICE_CREDENTIAL`) via `BiometricPrompt`.
   - Protects both **Incognito Tabs** and **Vault Secrets** (`VaultEntryEntity`), automatically re-locking inside `MainActivity.onStop()`.
4. **Zero-Bypass SSL Error Handling**: `WebViewClient.onReceivedSslError` immediately cancels failing requests and displays `SslErrorDialog` without offering dangerous "Proceed anyway" bypasses on untrusted certs.
5. **External App Launch Protection**: `intent://` and non-HTTP schemes require explicit user confirmation (`ExternalAppConfirmDialog`), preventing silent drive-by app launches.

---

## 2. Google Play Policy & Permission Hygiene

- **No Broad Storage Permissions**: Never requests `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, `READ_MEDIA_IMAGES`, or `READ_MEDIA_VIDEO`. Bookmarks/history import & export rely on Android's scoped Storage Access Framework (`ActivityResultContracts.CreateDocument` / `OpenDocument`).
- **Scoped File Sharing**: Screenshots, PDFs, and downloaded files shared externally use `androidx.core.content.FileProvider` (`${applicationId}.fileprovider`) with temporary `FLAG_GRANT_READ_URI_PERMISSION`.
- **Just-in-Time Runtime Permissions**:
  - `CAMERA` requested only inside `QrScannerScreen`.
  - `POST_NOTIFICATIONS` requested only when notifications are enabled or tested in Settings.

---

## 3. Encrypted DNS (DoH) Privacy

- DNS-over-HTTPS requests go directly over TLS (`https://`) to the user's chosen provider (`Cloudflare`, `Quad9`, `AdGuard`, `Google`, or `Custom`) using `OkHttpClient` rather than plaintext UDP port 53 during DoH lookups/checks.

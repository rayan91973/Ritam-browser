# Testing Strategy & Verification (`testing.md`)

Ritam Browser uses fast local JVM unit tests and Robolectric integration tests to verify core logic without requiring an Android emulator or ADB.

---

## 1. Test Suites

### Pure-JVM Unit Tests (`app/src/test/java/com/example/ExampleUnitTest.kt`)
Fast, zero-network verification of deterministic logic:
- **`DownloadCategory.classify()`**: Validates MIME-type and extension classification across Documents, Media, Archives, and Images.
- **`QrCodeGenerator.generateMatrix()`**: Verifies square QR matrix generation, minimum v1 dimensions (`>= 21×21`), and finder pattern module placement.
- **`DnsProvider` Endpoints**: Ensures all encrypted DNS providers expose valid `https://` RFC 8484 endpoints and `SYSTEM` remains blank.
- **`TranslationHelper`**: Validates supported language resolution (`en`, `ja`, `hi`, etc.).

### Robolectric Integration Tests (`app/src/test/java/com/example/ExampleRobolectricTest.kt`)
Runs against an in-memory Room `RitamDatabase` and `RitamShieldsEngine`:
- **Resource Integrity**: Verifies `R.string.app_name == "Ritam"`.
- **RITAM Shields Engine**: Tests ad blocking (`googlesyndication.com`), tracker blocking (`google-analytics.com`), first-party allowlisting, phishing domain interception (`testsafebrowsing.appspot.com`), and HTTPS-Only URL upgrading.
- **Bookmarks Netscape HTML Round-Trip**: Tests adding bookmarks, exporting valid Netscape HTML, and re-importing HTML bookmark files into Room.

---

## 2. Running Tests

### Run Pure-JVM Unit Tests
```bash
gradle :app:testDebugUnitTest --tests com.example.ExampleUnitTest
```

### Run Full Unit Test Suite (when Robolectric runtime artifacts are cached)
```bash
gradle :app:testDebugUnitTest
```

---

## 3. UI Testability Conventions

All primary interactive Compose surfaces include explicit `Modifier.testTag(...)` identifiers (`snake_case`), including:
- `omnibox_pill`, `omnibox_input_field`, `omnibox_copy_address_button`, `omnibox_edit_address_button`
- `bottom_nav_dock`, `quick_controls_button`, `tab_switcher_button`
- `library_tab_*`, `settings_chip_*`
- `incognito_biometric_lock_overlay`, `unlock_incognito_button`
- `devtools_sheet`, `pdf_viewer_dialog`, `undo_close_tab_button`

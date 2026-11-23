# Setup & Build Guide (`setup.md`)

Follow this guide to configure, build, and test **Ritam Browser** locally or in the AI Studio Android build environment.

---

## 1. Environment & SDK Requirements

| Component | Version / Configuration |
| :--- | :--- |
| **Application ID** | `com.aistudio.ritambrowser.vkmxqr` |
| **Namespace** | `com.example` |
| **Compile SDK** | `36` (Minor API level `1`) |
| **Target SDK** | `36` |
| **Min SDK** | `26` (Android 8.0 Oreo — required for adaptive icons, Notification channels, PiP, `PdfRenderer`) |
| **Java Compatibility** | Java `11` (`JavaVersion.VERSION_11`) |
| **AGP** | `9.1.1` |
| **Kotlin Compose Plugin** | `2.4.21` |
| **KSP** | `2.3.5` (Room compiler & Moshi codegen) |

> **Important Note on `local.properties`:** Do **NOT** create a `local.properties` file in cloud/AI Studio environments; `ANDROID_SDK_ROOT` is pre-configured by the environment. Use `gradle` directly instead of `./gradlew`.

---

## 2. Project Configuration Files

- `/settings.gradle.kts`: Sets `rootProject.name = "Ritam Browser"` and includes `:app`.
- `/gradle/libs.versions.toml`: Central Version Catalog managing Compose BOM, Room, DataStore, OkHttp, CameraX, ML Kit, Coil, Moshi, and Robolectric.
- `/app/build.gradle.kts`: Configures `signingConfigs` (`release` and `debugConfig`), KSP annotation processors (`androidx.room:room-compiler`, `moshi-kotlin-codegen`), and Secrets Gradle Plugin reading `.env` / `.env.example`.
- `/metadata.json`: Tracks platform identity (`Ritam Browser`) synchronized with `/app/src/main/res/values/strings.xml`.

---

## 3. Declared Permissions (`AndroidManifest.xml`)

Ritam Browser adheres to Google Play least-privilege guidelines:

| Permission | Type | Usage |
| :--- | :--- | :--- |
| `INTERNET` | Normal | Core web browsing, DoH DNS queries, snippet translation, PDF caching. |
| `ACCESS_NETWORK_STATE` | Normal | Checking network connectivity prior to downloads/lookups. |
| `USE_BIOMETRIC` | Normal | Device hardware / credential authentication for Incognito Lock & Vault Lock. |
| `CAMERA` | Runtime | Explicitly requested only when the user opens the QR Code Scanner (`QrScannerScreen`). |
| `POST_NOTIFICATIONS` | Runtime (Android 13+) | Explicitly requested when the user enables download/Read Aloud/shield alerts or taps Grant Permission in Settings. |

*Zero broad media/storage permissions (`READ_EXTERNAL_STORAGE`, `READ_MEDIA_IMAGES`, etc.) are requested.* Document export/import uses Android's Storage Access Framework (`CreateDocument` / `OpenDocument`).

---

## 4. Building & Running Commands

### Check Compilation & Generate Debug APK
```bash
gradle assembleDebug
```
*(In AI Studio, use the `compile_applet` tool.)*

### Run Pure-JVM Unit Tests
```bash
gradle :app:testDebugUnitTest --tests com.example.ExampleUnitTest
```

### Run All Unit & Robolectric Tests (requires Android SDK/Robolectric jars cached)
```bash
gradle :app:testDebugUnitTest
```

---

## 5. Secrets & API Keys
- No external API key is required for core browsing, Shields ad-blocking, DoH resolution, offline QR generation, PDF viewing, or biometric locking.
- `.env.example` provides the template structure used by the Secrets Gradle Plugin if optional third-party keys are ever added. Never commit real credentials or modify `debug.keystore*`.

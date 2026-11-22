# TeleBridge SMS — Native Android (Kotlin + Jetpack Compose)

A privacy-first native Android application built with **Kotlin**, **Jetpack Compose**, **Material 3**, and **Android Keystore (`EncryptedSharedPreferences`)**, preserving the TeleBridge SMS Forwarder bilingual (Bangla & English) dashboard design.

## Key Architecture & Security Guarantees
- **Package Name:** `com.telebridge.sms`
- **Zero Secrets in APK:** No Telegram Bot Tokens, API keys, or webhook secrets are hardcoded in `BuildConfig`, XML resources, or Kotlin source files. Runtime relay URLs and webhook secrets entered by the user are stored encrypted on-device via `EncryptedSharedPreferences`.
- **Sample-Only Testing & Preview:** Built-in sample SMS presets (`ParcelTrack`, `OpsMonitor`, `ClinicDesk`) allow rule-matching and relay testing without touching real user messages.
- **No Real SMS / OTP Harvesting:** `AndroidManifest.xml` intentionally does **not** request `READ_SMS`, `RECEIVE_SMS`, or `BIND_NOTIFICATION_LISTENER_SERVICE`. Real SMS, OTPs, banking alerts, and private notifications are never read or auto-forwarded.
- **Responsive UI & Splash Screen:** Uses `androidx.core:core-splashscreen` (`Theme.TeleBridge.Splash`), custom vector adaptive launcher icons, `NavigationBar` on phones, and `NavigationRail` on tablets/foldables (`>= 600dp`).

## Opening & Building in Android Studio
1. Open the `android/` folder in **Android Studio** (Ladybug / Koala or newer).
2. Allow Gradle to sync dependencies from `gradle/libs.versions.toml` (requires JDK 17 and Android SDK Platform 35).
3. Run `./gradlew assembleDebug` or click **Run** on an Android emulator or device (`minSdk = 24`, `targetSdk = 35`).

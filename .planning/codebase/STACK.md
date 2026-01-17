# Technology Stack

**Analysis Date:** 2026-01-17

## Languages

**Primary:**
- Kotlin 1.9.0 - All application code (`gradle/libs.versions.toml`)

**Secondary:**
- Java 11 - JVM target and compilation compatibility (`app/build.gradle.kts`)

## Runtime

**Environment:**
- Android API 34 (minSdk) - Minimum SDK requirement
- Android API 35 (targetSdk/compileSdk) - Target and compile SDK
- Android Runtime (ART)

**Build System:**
- Gradle with Android Gradle Plugin 8.13.2 (`gradle/libs.versions.toml`)
- KSP (Kotlin Symbol Processing) 1.9.0-1.0.13 - Annotation processing

**Package Manager:**
- Gradle with version catalog (`gradle/libs.versions.toml`)
- Lockfile: Not used (Gradle manages versions via catalog)

## Frameworks

**Core:**
- Jetpack Compose (BOM 2024.08.00) - Modern declarative UI (`app/build.gradle.kts`)
- Room 2.6.1 - SQLite database abstraction (`app/build.gradle.kts`)
- Navigation Compose 2.7.7 - In-app navigation (`app/build.gradle.kts`)
- ViewModel 2.7.0 - Lifecycle-aware state management (`app/build.gradle.kts`)
- WorkManager 2.9.1 - Background task scheduling (`app/build.gradle.kts`)
- DataStore 1.1.1 - Preferences data storage (`app/build.gradle.kts`)

**Testing:**
- JUnit 4.13.2 - Unit testing framework (`gradle/libs.versions.toml`)
- MockK 1.13.8 - Kotlin mocking library (`gradle/libs.versions.toml`)
- Turbine 1.0.0 - Flow testing library (`gradle/libs.versions.toml`)
- Robolectric 4.11.1 - Android unit testing framework (`gradle/libs.versions.toml`)
- kotlinx-coroutines-test 1.7.3 - Coroutine testing utilities (`gradle/libs.versions.toml`)

**Build/Dev:**
- Spotless 6.25.0 - Code formatting with ktlint (`gradle/libs.versions.toml`)
- ProGuard - Release APK minification (`app/build.gradle.kts`)

## Key Dependencies

**Critical:**
- jBCrypt 0.4 - Password hashing (bcrypt) (`gradle/libs.versions.toml`)
  - Used in: `app/src/main/java/dev/notyouraverage/smscourier/security/SecurityManager.kt`
- Android Keystore API - Hardware-backed encryption
  - Used in: `app/src/main/java/dev/notyouraverage/smscourier/security/KeystoreEncryptionManager.kt`
- Standard Java Crypto (javax.crypto) - AES-256-GCM/CBC, HMAC-SHA256, PBKDF2
  - Used in: `app/src/main/java/dev/notyouraverage/smscourier/security/MessageEncryption.kt`

**Infrastructure:**
- Room - Local SQLite database via `SmsCourierDatabase.kt`
- Lifecycle Runtime 2.8.4 - Lifecycle awareness (`app/build.gradle.kts`)
- Core KTX 1.13.1 - Kotlin extensions for Android (`app/build.gradle.kts`)

## Configuration

**Environment:**
- No environment variables required at runtime
- Version code/name from environment at build time (`VERSION_CODE`, `VERSION_NAME`)
- Keystore properties from `release-keystore.properties` (gitignored)

**Build:**
- `app/build.gradle.kts` - App build configuration
- `gradle/libs.versions.toml` - Centralized dependency versions
- `gradle.properties` - Gradle system properties
- `app/spotless.gradle` - Code formatting rules

## Platform Requirements

**Development:**
- Any platform with Android Studio and JDK 11+
- No external dependencies (offline-first app)

**Production:**
- Android 14+ (API 34+) devices with SMS capability
- Telephony hardware required for SMS forwarding
- Distributed as signed APK or via Play Store

---

*Stack analysis: 2026-01-17*
*Update after major dependency changes*

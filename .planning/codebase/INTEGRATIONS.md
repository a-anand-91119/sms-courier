# External Integrations

**Analysis Date:** 2026-01-17

## APIs & External Services

**External APIs:**
- None - This is an offline-first app with no network APIs

**Third-party Services:**
- None - All communication is device-to-device via SMS

## Data Storage

**Databases:**
- Room (SQLite) - Primary data store
  - Database: `app/src/main/java/dev/notyouraverage/smscourier/data/SmsCourierDatabase.kt`
  - Name: `sms_courier_database`
  - Version: 5 (with 4 migrations)
  - Entities:
    - `PairedDevice` - Device pairing records
    - `ForwardingSession` - Forwarding session records
  - DAOs: `PairedDeviceDao`, `ForwardingSessionDao`

**File Storage:**
- None - No file-based storage

**Caching:**
- In-memory only (MasterService maps for transient state)
- No persistent caching layer

## Authentication & Identity

**Auth Provider:**
- Custom SMS-based authentication
  - Password hashing: bcrypt with cost factor 12
  - Challenge-response: HMAC-SHA256
  - Implementation: `app/src/main/java/dev/notyouraverage/smscourier/security/SecurityManager.kt`

**Device Pairing:**
- Mutual authentication between SOURCE and TARGET devices
- Password set during pairing approval on TARGET
- Auth key (SHA256 of password) stored on both devices

**No OAuth/External Auth:**
- No third-party identity providers
- No cloud authentication

## Android Platform APIs

**SMS APIs:**
- `android.provider.Telephony` - SMS receipt handling
- `android.telephony.SmsManager` - SMS sending
- Location: `app/src/main/java/dev/notyouraverage/smscourier/receivers/SmsReceiver.kt`, `services/SmsSender.kt`

**Broadcast System:**
- `BroadcastReceiver` for SMS interception
- Action: `android.provider.Telephony.Sms.Intents.SMS_RECEIVED_ACTION`
- Custom intent actions for internal communication

**Foreground Services:**
- Type: `remoteMessaging`
- Location: `app/src/main/java/dev/notyouraverage/smscourier/services/foreground/MasterService.kt`
- Permissions: FOREGROUND_SERVICE, FOREGROUND_SERVICE_REMOTE_MESSAGING, POST_NOTIFICATIONS

**Notifications API:**
- Used for pairing requests and forwarding status
- Location: `app/src/main/java/dev/notyouraverage/smscourier/notifications/PairingNotificationManager.kt`
- Channels defined in `MainApplication.kt`

## Security Services

**Android KeyStore:**
- Provider: `"AndroidKeyStore"`
- Algorithm: AES-256-GCM
- Key alias: `"smscourier_storage_key"`
- Location: `app/src/main/java/dev/notyouraverage/smscourier/security/KeystoreEncryptionManager.kt`
- Purpose: Encrypt sensitive keys at rest in database

**Java Crypto:**
- `javax.crypto.Cipher` - AES encryption
- `javax.crypto.Mac` - HMAC-SHA256
- `javax.crypto.SecretKeyFactory` - PBKDF2 key derivation
- `java.security.MessageDigest` - SHA-256 hashing

## Monitoring & Observability

**Error Tracking:**
- None - Local logging only (Android Log)

**Analytics:**
- None

**Logs:**
- Android Log (stdout/logcat)
- TAG pattern: `"SMSC:<ClassName>"`
- No external log aggregation

## CI/CD & Deployment

**Hosting:**
- Local APK distribution or Play Store
- Signed APK build configuration in `app/build.gradle.kts`

**CI Pipeline:**
- GitHub Actions (implied by CLAUDE.md git conventions)
- Build commands documented in CLAUDE.md

## Environment Configuration

**Development:**
- No environment variables required at runtime
- Build-time: `VERSION_CODE`, `VERSION_NAME` from environment
- Keystore: `release-keystore.properties` (gitignored)

**Production:**
- Signed APK with release keystore
- ProGuard minification enabled for release builds
- No server-side configuration

## Protocol: SMS Command System

**Custom Protocol:**
- All commands prefixed with `SMSC`
- Parsing: `app/src/main/java/dev/notyouraverage/smscourier/commands/CommandParser.kt`
- Patterns: `app/src/main/java/dev/notyouraverage/smscourier/commands/CommandPatterns.kt`

**Commands:**
| Command | Format | Direction |
|---------|--------|-----------|
| PAIR_REQUEST | `SMSC PAIR_REQUEST` | SOURCE → TARGET |
| PAIR_APPROVED | `SMSC PAIR_APPROVED` | TARGET → SOURCE |
| PAIR_REJECTED | `SMSC PAIR_REJECTED` | TARGET → SOURCE |
| UNPAIR | `SMSC UNPAIR` | Either |
| AUTH_REQUEST | `SMSC AUTH_REQUEST` | SOURCE → TARGET |
| AUTH_CHALLENGE | `SMSC AUTH_CHALLENGE <nonce>` | TARGET → SOURCE |
| START_FORWARD | `SMSC START_FORWARD <response> [duration]` | SOURCE → TARGET |
| STOP_FORWARD | `SMSC STOP_FORWARD` | SOURCE → TARGET |
| FWD | `SMSC FWD <+sender> message` | TARGET → SOURCE |
| FWDE | `SMSC FWDE <+sender> <encrypted>` | TARGET → SOURCE |

## Hardware Requirements

**Telephony Hardware:**
- SMS capability required for core functionality
- Declared as optional in manifest (allows installation without SMS hardware)
- Location: `app/src/main/AndroidManifest.xml`

## Summary

This is a **fully offline application** with:
- No external APIs or cloud services
- Device-to-device communication via SMS only
- Local SQLite storage via Room
- Android Keystore for secure key storage
- Standard Android platform APIs (SMS, notifications, services)

---

*Integration audit: 2026-01-17*
*Update when adding/removing external services*

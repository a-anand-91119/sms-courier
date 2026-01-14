# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run all unit tests
./gradlew test

# Run Android instrumented tests
./gradlew connectedAndroidTest

# Format code with Spotless
./gradlew spotlessApply

# Check code formatting
./gradlew spotlessCheck
```

## Project Overview

SMS Courier is an Android app for forwarding SMS messages between two devices via SMS commands. It enables a "source" device to receive SMS messages forwarded from a "target" device using a secure pairing system.

**Key terminology:**
- **Source device**: The device that requests to receive forwarded SMS messages
- **Target device**: The device that forwards its incoming SMS messages to the source

## Architecture

### SMS Command Protocol

All commands are SMS messages prefixed with `SMSC`. The protocol supports:

| Command | Format | Description |
|---------|--------|-------------|
| Pair Request | `SMSC PAIR_REQUEST` | Initiate pairing |
| Pair Approved | `SMSC PAIR_APPROVED` | Accept pairing |
| Pair Rejected | `SMSC PAIR_REJECTED` | Reject pairing |
| Unpair | `SMSC UNPAIR` | Remove pairing |
| Start Forward | `SMSC START_FORWARD <password> [duration]` | Begin forwarding session |
| Stop Forward | `SMSC STOP_FORWARD` | End forwarding session |
| Forward Data | `SMSC FWD <+sender> message` | Forwarded SMS content |

Command parsing is handled in `commands/CommandParser.kt` using regex patterns defined in `commands/CommandPatterns.kt`.

### Service Architecture

- **MasterService** (`services/foreground/MasterService.kt`): Foreground service that orchestrates all SMS operations. Registers the SMS receiver, manages forwarding sessions, and coordinates between components.
- **SmsReceiver** (`receivers/SmsReceiver.kt`): BroadcastReceiver that intercepts all incoming SMS, parses commands, and routes to MasterService.
- **SmsCommandHandler** (`handlers/SmsCommandHandler.kt`): Core business logic for handling all SMSC commands (pairing, forwarding, authentication).
- **SmsSender** (`services/SmsSender.kt`): Handles outgoing SMS for commands and forwarded messages.

### Data Layer

Room database (`data/SmsCourierDatabase.kt`) with two entities:
- **PairedDevice**: Stores paired device info, roles (SOURCE/TARGET), pairing status, password hash (bcrypt), and lockout info for failed auth attempts.
- **ForwardingSession**: Tracks active/past forwarding sessions with duration, expiration, and message counts.

Repository pattern: `PairedDeviceRepository` and `ForwardingSessionRepository` wrap DAO operations.

### Security

- **SecurityManager** (`security/SecurityManager.kt`): Handles password validation using bcrypt, tracks failed attempts, and implements device lockout.
- Passwords are set during pairing approval and validated on START_FORWARD commands.

### UI Layer

Jetpack Compose with Navigation Compose. Screens are in `composables/screens/`, ViewModels in `viewmodels/`, routes defined in `navigation/Screen.kt`.

## Code Style

- Kotlin with ktlint via Spotless plugin
- Run `./gradlew spotlessApply` before committing
- Spotless ratchets from `origin/main` (only checks changed files)

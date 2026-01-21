# Architecture

This document describes the architecture of SMS Courier for developers who want to understand or contribute to the codebase.

## Overview

SMS Courier forwards SMS messages between two paired Android devices using an SMS-based command protocol. No internet connection or cloud servers are required.

**Key terminology:**
- **Source device**: The device that requests and receives forwarded SMS messages
- **Target device**: The device that forwards its incoming SMS messages to the source

## SMS Command Protocol

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

## Service Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     MasterService                        │
│                 (Foreground Service)                     │
│  - Orchestrates all SMS operations                       │
│  - Manages forwarding sessions                           │
│  - Coordinates between components                        │
└─────────────────────────────────────────────────────────┘
           │                              │
           ▼                              ▼
┌─────────────────────┐      ┌─────────────────────┐
│    SmsReceiver      │      │   SmsCommandHandler │
│ (BroadcastReceiver) │      │  (Business Logic)   │
│ - Intercepts SMS    │      │ - Pairing logic     │
│ - Parses commands   │      │ - Auth logic        │
│ - Routes to service │      │ - Forwarding logic  │
└─────────────────────┘      └─────────────────────┘
                                       │
                                       ▼
                             ┌─────────────────────┐
                             │     SmsSender       │
                             │ - Sends commands    │
                             │ - Forwards messages │
                             └─────────────────────┘
```

### Key Components

| Component | Location | Responsibility |
|-----------|----------|----------------|
| **MasterService** | `services/foreground/MasterService.kt` | Foreground service that orchestrates all SMS operations |
| **SmsReceiver** | `receivers/SmsReceiver.kt` | BroadcastReceiver that intercepts incoming SMS and parses commands |
| **SmsCommandHandler** | `handlers/SmsCommandHandler.kt` | Core business logic for all SMSC commands |
| **SmsSender** | `services/SmsSender.kt` | Handles outgoing SMS for commands and forwarded messages |

## Data Layer

Room database (`data/SmsCourierDatabase.kt`) with two main entities:

### PairedDevice

Stores paired device information:
- Phone number and display name
- Role (SOURCE or TARGET)
- Pairing status (PENDING, APPROVED, REJECTED)
- Password hash (bcrypt)
- Lockout info for failed auth attempts

### ForwardingSession

Tracks forwarding sessions:
- Session duration and expiration
- Active/inactive status
- Message counts
- Associated device

### Repositories

- `PairedDeviceRepository` - CRUD operations for paired devices
- `ForwardingSessionRepository` - Session management

## Security

### Password Handling

- Passwords are hashed using bcrypt (cost factor 12)
- Never stored in plaintext
- Set during pairing approval
- Validated on START_FORWARD commands

### Device Lockout

- Tracks failed authentication attempts
- Progressive lockout after configurable max attempts
- Lockout duration is configurable

### Message Encryption

- Optional AES-256-CBC encryption for forwarded messages
- PBKDF2 key derivation (10,000 iterations)
- Unique salt and IV per session

All security operations are handled by `SecurityManager` (`security/SecurityManager.kt`).

## UI Layer

Built with Jetpack Compose and Navigation Compose.

| Directory | Contents |
|-----------|----------|
| `composables/screens/` | Screen composables (Home, Settings, Pairing, etc.) |
| `viewmodels/` | ViewModels for each screen |
| `navigation/Screen.kt` | Navigation route definitions |

### MVVM Pattern

Each screen follows the MVVM pattern:
1. **Screen** (Composable) - UI rendering
2. **ViewModel** - State management and business logic
3. **Repository** - Data access

## Settings

User preferences are persisted using DataStore (`SettingsRepository`):

- Notification preferences
- Default forwarding duration
- Theme selection
- Security settings (lockout duration, max attempts, etc.)

## Testing

- Unit tests: `./gradlew test`
- Instrumented tests: `./gradlew connectedAndroidTest`
- Integration test base class: `IntegrationTestBase`

See the test directories for examples of testing patterns used in the project.

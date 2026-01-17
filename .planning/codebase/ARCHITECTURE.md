# Architecture

**Analysis Date:** 2026-01-17

## Pattern Overview

**Overall:** Clean Architecture with MVVM + Service Layer

**Key Characteristics:**
- Single foreground service orchestrating all SMS operations
- MVVM pattern for UI with reactive state flows
- Repository pattern for data access
- SMS-based command protocol for device communication
- Offline-first (no network APIs)

## Layers

**Presentation Layer:**
- Purpose: Render UI and handle user interaction
- Contains: Jetpack Compose screens, ViewModels
- Location: `app/src/main/java/dev/notyouraverage/smscourier/composables/`, `viewmodels/`
- Depends on: Repositories, SmsSender
- Used by: User via MainActivity

**Domain Layer:**
- Purpose: Core business logic and command processing
- Contains: SmsCommandHandler, SecurityManager, command parsing
- Location: `app/src/main/java/dev/notyouraverage/smscourier/handlers/`, `security/`, `commands/`
- Depends on: Repositories, SmsSender
- Used by: MasterService

**Service Layer:**
- Purpose: Orchestrate SMS operations and manage background work
- Contains: MasterService (foreground), SmsReceiver (broadcast), SmsSender
- Location: `app/src/main/java/dev/notyouraverage/smscourier/services/`, `receivers/`
- Depends on: Domain layer, Data layer
- Used by: Android system, UI layer

**Data Layer:**
- Purpose: Persist and retrieve data
- Contains: Room database, DAOs, Repositories
- Location: `app/src/main/java/dev/notyouraverage/smscourier/data/`, `repository/`
- Depends on: Room, Android Keystore
- Used by: Domain layer, ViewModels

**Security Layer:**
- Purpose: Handle authentication, encryption, password hashing
- Contains: SecurityManager, MessageEncryption, KeystoreEncryptionManager
- Location: `app/src/main/java/dev/notyouraverage/smscourier/security/`
- Depends on: Android Keystore, Java crypto
- Used by: Domain layer, Data layer

## Data Flow

**SMS Reception Flow:**

1. Android system delivers SMS via broadcast
2. SmsReceiver parses message for SMSC prefix
3. If SMSC command → Intent to MasterService with PROCESS_COMMAND
4. MasterService routes to SmsCommandHandler
5. Handler executes business logic (pairing, forwarding, auth)
6. Repository updates database
7. ViewModel observes changes via Flow
8. UI updates reactively

**Pairing Flow (Two-way):**

1. SOURCE sends `SMSC PAIR_REQUEST` via SmsSender
2. TARGET's SmsReceiver routes to SmsCommandHandler.handlePairRequest()
3. PairingNotificationManager shows approval notification
4. User approves → PairingRequestsScreen creates password
5. TARGET sends `SMSC PAIR_APPROVED` with password hash stored
6. SOURCE receives → updates device status to APPROVED

**Forwarding Session Flow:**

1. SOURCE initiates auth request via ForwardingControlViewModel
2. MasterService sends `SMSC AUTH_REQUEST`
3. TARGET generates nonce, sends `SMSC AUTH_CHALLENGE <nonce>`
4. SOURCE computes HMAC response, sends `SMSC START_FORWARD <response> <duration>`
5. TARGET validates, creates ForwardingSession
6. Subsequent SMS on TARGET → encrypted and forwarded to SOURCE

**State Management:**
- Reactive Flows from database (StateFlow with SharingStarted.WhileSubscribed)
- In-memory maps for transient state (sessionHandlers, pendingAuthRequests, pendingChallenges)
- ViewModels combine flows using `combine()`

## Key Abstractions

**Repository:**
- Purpose: Encapsulate data access with business logic
- Examples: `PairedDeviceRepository`, `ForwardingSessionRepository`
- Location: `app/src/main/java/dev/notyouraverage/smscourier/repository/`
- Pattern: Wraps DAO with phone normalization, key encryption

**ViewModel:**
- Purpose: Manage UI state with lifecycle awareness
- Examples: `HomeViewModel`, `ForwardingControlViewModel`, `PairedDevicesViewModel`
- Location: `app/src/main/java/dev/notyouraverage/smscourier/viewmodels/`
- Pattern: Factory injection, StateFlow for state, viewModelScope for coroutines

**ParsedCommand:**
- Purpose: Type-safe representation of SMSC commands
- Examples: `PairRequest`, `PairApproved`, `StartForward`, `ForwardedData`
- Location: `app/src/main/java/dev/notyouraverage/smscourier/commands/ParsedCommand.kt`
- Pattern: Sealed class hierarchy with data classes

**Manager:**
- Purpose: Handle cross-cutting concerns
- Examples: `SecurityManager`, `KeystoreEncryptionManager`, `PairingNotificationManager`
- Pattern: Singleton-like (companion object methods or object)

## Entry Points

**Application Entry:**
- Location: `app/src/main/java/dev/notyouraverage/smscourier/applications/MainApplication.kt`
- Triggers: App launch
- Responsibilities: Create notification channels

**Activity Entry:**
- Location: `app/src/main/java/dev/notyouraverage/smscourier/activities/MainActivity.kt`
- Triggers: Home icon, deep links from notifications
- Responsibilities: Set up Compose UI, request permissions, start MasterService

**Service Entry:**
- Location: `app/src/main/java/dev/notyouraverage/smscourier/services/foreground/MasterService.kt`
- Triggers: MainActivity startService(), Intent actions
- Responsibilities: Orchestrate all SMS operations, run in foreground

**Receiver Entry:**
- Location: `app/src/main/java/dev/notyouraverage/smscourier/receivers/SmsReceiver.kt`
- Triggers: Incoming SMS (android.provider.Telephony.SMS_RECEIVED)
- Responsibilities: Parse commands, route to MasterService, forward messages

## Error Handling

**Strategy:** Log and fail gracefully with user notification

**Patterns:**
- Try-catch at service boundaries with Log.e()
- Null checks with early return
- Sealed class results for typed errors
- User-visible toasts/notifications for failures

## Cross-Cutting Concerns

**Logging:**
- TAG pattern: "SMSC:<ClassName>"
- Log levels: d (debug), i (info), w (warning), e (error)
- Structured logging with context variables

**Validation:**
- Phone number normalization at repository boundary
- Command parsing with regex patterns
- Password validation with bcrypt

**Encryption:**
- At-rest: Android Keystore AES-256-GCM for stored keys
- In-transit: AES-256-CBC with PBKDF2 for forwarded messages
- Authentication: bcrypt for passwords, HMAC-SHA256 for challenge-response

---

*Architecture analysis: 2026-01-17*
*Update when major patterns change*

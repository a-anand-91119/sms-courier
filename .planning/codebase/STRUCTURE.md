# Codebase Structure

**Analysis Date:** 2026-01-17

## Directory Layout

```
SMSCourier/
├── app/
│   ├── build.gradle.kts        # App build configuration
│   ├── spotless.gradle         # Code formatting rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── java/dev/notyouraverage/smscourier/
│       │       ├── activities/     # Android Activities
│       │       ├── applications/   # Application class
│       │       ├── commands/       # SMS command protocol
│       │       ├── composables/    # Jetpack Compose UI
│       │       ├── constants/      # App-wide constants
│       │       ├── data/           # Room database layer
│       │       ├── enums/          # Enumerations
│       │       ├── handlers/       # Business logic handlers
│       │       ├── models/         # Data models
│       │       ├── navigation/     # Navigation Compose
│       │       ├── notifications/  # Notification management
│       │       ├── receivers/      # BroadcastReceivers
│       │       ├── repository/     # Repository pattern
│       │       ├── security/       # Security & cryptography
│       │       ├── services/       # Services (foreground/background)
│       │       ├── ui/             # UI theming
│       │       ├── viewmodels/     # MVVM ViewModels
│       │       └── workers/        # WorkManager jobs
│       └── test/                   # Unit tests
├── gradle/
│   └── libs.versions.toml      # Version catalog
├── build.gradle.kts            # Root build configuration
├── settings.gradle.kts         # Gradle settings
└── CLAUDE.md                   # AI assistant instructions
```

## Directory Purposes

**activities/**
- Purpose: Android Activity classes
- Contains: `MainActivity.kt` (single activity, Compose-based)
- Key files: `MainActivity.kt` - Entry point, permission requests, deep linking

**applications/**
- Purpose: Application class initialization
- Contains: `MainApplication.kt`
- Key files: Creates notification channels on app startup

**commands/**
- Purpose: SMS command protocol parsing
- Contains: Parser, patterns, parsed command types
- Key files:
  - `CommandParser.kt` - Parse SMSC messages from SMS
  - `CommandPatterns.kt` - Regex patterns for commands
  - `ParsedCommand.kt` - Sealed class hierarchy for typed commands

**composables/**
- Purpose: Jetpack Compose UI components
- Contains: Screen composables
- Subdirectories: `screens/` - Full-screen UI components
- Key files:
  - `screens/HomeScreen.kt` - Main dashboard
  - `screens/AddDeviceScreen.kt` - Pair new device
  - `screens/PairedDevicesScreen.kt` - List paired devices
  - `screens/PairingRequestsScreen.kt` - Incoming pairing requests
  - `screens/ForwardingControlScreen.kt` - Control forwarding sessions

**constants/**
- Purpose: App-wide constant definitions
- Contains: `Constants.kt`
- Key files: Notification channel IDs, other app constants

**data/**
- Purpose: Room database and data access
- Contains: Database, DAOs, entities, converters
- Subdirectories:
  - `dao/` - Data Access Objects
  - `entities/` - Room entity classes
- Key files:
  - `SmsCourierDatabase.kt` - Room database (v5 with migrations)
  - `dao/PairedDeviceDao.kt` - Query paired devices
  - `dao/ForwardingSessionDao.kt` - Query forwarding sessions
  - `entities/PairedDevice.kt` - Device pairing record
  - `entities/ForwardingSession.kt` - Forwarding session record
  - `Converters.kt` - Room type converters

**enums/**
- Purpose: Enumeration types
- Contains: `SmsCommand.kt`, `DeviceRole.kt`, `PairingStatus.kt`

**handlers/**
- Purpose: Core business logic handlers
- Contains: `SmsCommandHandler.kt`
- Key files: `SmsCommandHandler.kt` - Process all SMSC commands

**models/**
- Purpose: Data transfer objects
- Contains: `SmsMessageData.kt`

**navigation/**
- Purpose: Navigation Compose routing
- Contains: `NavGraph.kt`, `Screen.kt`
- Key files:
  - `NavGraph.kt` - Navigation graph definition
  - `Screen.kt` - Screen route definitions (sealed class)

**notifications/**
- Purpose: Notification management
- Contains: `PairingNotificationManager.kt`
- Key files: Show pairing/forwarding notifications

**receivers/**
- Purpose: Android BroadcastReceivers
- Contains: SMS receiver, action receivers
- Key files:
  - `SmsReceiver.kt` - Intercept incoming SMS
  - `PairingActionReceiver.kt` - Handle notification approve/reject
  - `ServiceNotificationReceiver.kt` - Handle service notification actions

**repository/**
- Purpose: Repository pattern (data access abstraction)
- Contains: `PairedDeviceRepository.kt`, `ForwardingSessionRepository.kt`
- Key files: Wrap DAOs with business logic, key encryption

**security/**
- Purpose: Security and cryptography
- Contains: Password hashing, encryption, challenge-response
- Key files:
  - `SecurityManager.kt` - bcrypt, HMAC, device lockout
  - `MessageEncryption.kt` - AES-256-CBC message encryption
  - `KeystoreEncryptionManager.kt` - Android Keystore wrapper

**services/**
- Purpose: Android Services
- Subdirectories:
  - `foreground/` - Foreground service
  - `background/` - Background service (legacy)
- Key files:
  - `foreground/MasterService.kt` - Main orchestrator service
  - `SmsSender.kt` - Outgoing SMS handler

**ui/**
- Purpose: Jetpack Compose theming
- Subdirectories: `theme/`
- Key files: `Theme.kt`, `Color.kt`, `Type.kt`

**viewmodels/**
- Purpose: MVVM ViewModels
- Contains: State management for each screen
- Key files:
  - `HomeViewModel.kt` - Dashboard state
  - `PairingRequestsViewModel.kt` - Pairing requests state
  - `AddDeviceViewModel.kt` - Add device state
  - `ForwardingControlViewModel.kt` - Forwarding control state
  - `PairedDevicesViewModel.kt` - Paired devices list state

**workers/**
- Purpose: WorkManager background jobs
- Contains: `SmsWorker.kt` (future use)

## Key File Locations

**Entry Points:**
- `app/src/main/java/dev/notyouraverage/smscourier/activities/MainActivity.kt` - UI entry
- `app/src/main/java/dev/notyouraverage/smscourier/applications/MainApplication.kt` - App init
- `app/src/main/java/dev/notyouraverage/smscourier/services/foreground/MasterService.kt` - Service

**Configuration:**
- `app/build.gradle.kts` - App build configuration
- `gradle/libs.versions.toml` - Dependency versions
- `app/spotless.gradle` - Code formatting
- `app/src/main/AndroidManifest.xml` - Android manifest

**Core Logic:**
- `app/src/main/java/dev/notyouraverage/smscourier/handlers/SmsCommandHandler.kt` - Command processing
- `app/src/main/java/dev/notyouraverage/smscourier/services/foreground/MasterService.kt` - Orchestration
- `app/src/main/java/dev/notyouraverage/smscourier/security/SecurityManager.kt` - Authentication

**Testing:**
- `app/src/test/java/dev/notyouraverage/smscourier/` - Unit tests
- `app/src/test/java/dev/notyouraverage/smscourier/TestFixtures.kt` - Test data builders
- `app/src/test/java/dev/notyouraverage/smscourier/MainCoroutineRule.kt` - Test utilities

**Documentation:**
- `CLAUDE.md` - AI assistant instructions
- `.planning/` - Project planning documents

## Naming Conventions

**Files:**
- PascalCase.kt for all Kotlin classes
- Test files: `*Test.kt` suffix alongside or in test/ tree

**Directories:**
- lowercase for all directories
- Plural names for collections: `composables/`, `viewmodels/`, `services/`

**Special Patterns:**
- `*ViewModel.kt` - ViewModels with Factory companion
- `*Repository.kt` - Repository classes
- `*Manager.kt` - Manager/utility classes
- `*Receiver.kt` - BroadcastReceivers
- `*Screen.kt` - Composable screen functions

## Where to Add New Code

**New Feature (UI + Business Logic):**
- Screen: `composables/screens/`
- ViewModel: `viewmodels/`
- Navigation: Update `navigation/NavGraph.kt`

**New Command Type:**
- Pattern: `commands/CommandPatterns.kt`
- Parser: `commands/CommandParser.kt`
- Command class: `commands/ParsedCommand.kt`
- Handler: `handlers/SmsCommandHandler.kt`

**New Database Entity:**
- Entity: `data/entities/`
- DAO: `data/dao/`
- Repository: `repository/`
- Database: Add to `SmsCourierDatabase.kt` with migration

**New Background Task:**
- Worker: `workers/`
- Schedule from MasterService or ViewModel

**New Notification:**
- Manager: `notifications/`
- Receiver: `receivers/` (if action handling needed)

## Special Directories

**.planning/**
- Purpose: Project planning and codebase documentation
- Source: Generated by /gsd:map-codebase and planning tools
- Committed: Yes

**app/build/**
- Purpose: Build output and generated code
- Source: Gradle build process
- Committed: No (in .gitignore)

---

*Structure analysis: 2026-01-17*
*Update when directory structure changes*

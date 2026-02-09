# SMS Courier

## What This Is

SMS Courier is an Android app for forwarding SMS messages between two paired devices using secure SMS-based commands. A "source" device can receive SMS messages forwarded from a "target" device through a secure pairing system with password-based authentication. The app provides comprehensive device management with session history, message-level storage, export capabilities, and bidirectional visibility.

## Core Value

Reliable, secure SMS forwarding between paired devices with minimal user intervention.

## Current State (v0.0.64 Shipped)

**Just shipped:** Device Management & Visibility milestone with session history, export, and bidirectional indicators.

**Parallel:** Play Store Launch (Phase 4 in progress - awaiting Google review)

## Requirements

### Validated

- SMS-based command protocol with SMSC prefix
- Secure pairing workflow (request -> approve -> password)
- Bcrypt password hashing with device lockout
- Foreground service for reliable message forwarding
- Room database for paired device and session management (v7)
- Jetpack Compose UI with Navigation Compose
- CI/CD pipeline with Play Store deployment
- Comprehensive test coverage (343 tests)
- Settings screen with DataStore persistence — v0.0.63
- Notification persistence toggle — v0.0.63
- Default forwarding duration setting — v0.0.63
- Theme selection with immediate application — v0.0.63
- About section with version, privacy policy, support links — v0.0.63
- Permission status with fix navigation — v0.0.63
- Advanced security settings (lockout, attempts, timeouts) — v0.0.63
- Reactive settings via Flow collection in services — v0.0.63
- ✓ ForwardedMessage table with FK CASCADE — v0.0.64
- ✓ Soft delete for PairedDevice (archive history) — v0.0.64
- ✓ Transaction-based message storage with counters — v0.0.64
- ✓ Device History screen with active/removed sections — v0.0.64
- ✓ Session History with Paging 3 and session/contact toggle — v0.0.64
- ✓ MessageDetailBottomSheet with expandable rows — v0.0.64
- ✓ Export functionality (CSV/JSON/TXT via SAF) — v0.0.64
- ✓ Bidirectional visibility indicators (↑↓⇅) — v0.0.64
- ✓ SessionBreakdownBottomSheet with stop actions — v0.0.64
- ✓ Configurable history retention (7-90 days or Forever) — v0.0.64
- ✓ Auto-cleanup with WorkManager (7-day periodic) — v0.0.64

### Active

**Next milestone TBD** — Define via `/gsd:new-milestone`

### Out of Scope

- Message format customization — deferred to future milestone
- Notification grouping — deferred to future milestone
- In-app notification channel controls — use Android system settings
- Contact name lookup for message history — requires READ_CONTACTS permission, gauge demand first
- Delivery tracking for forwarded messages — deferred to future milestone
- PIN/fingerprint authentication for history access — deferred unless compliance required
- Message content encryption at rest — high complexity, defer unless compliance required
- Cross-device history sync — privacy concerns, intentionally avoided

## Context

**Current state:**
- v0.0.64 shipped with full device management features
- 22,261 lines of Kotlin code
- Room database v7 with message storage
- Play Store closed testing complete, awaiting beta/production access
- 343 tests passing

**Technical environment:**
- Kotlin with Jetpack Compose
- Room database (version 7)
- DataStore Preferences for settings persistence
- WorkManager for background cleanup
- Paging 3 for message lists
- Material 3 with dynamic colors
- Target API 35

## Constraints

- **Play Store timeline:** Awaiting Google review for beta/production access
- **No breaking changes:** Maintain stability during production rollout

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| DataStore vs SharedPreferences | Modern, coroutine-native, type-safe | ✓ Good |
| Settings UI library | Full control over Material3 styling | ✓ Good |
| Settings architecture | Reactive updates to services | ✓ Good |
| Security defaults | Match existing SecurityManager constants | ✓ Good |
| Unpair auto-archives (ARCH-05) | Safer default preserves history | ✓ Good |
| Foreign key CASCADE | Automatic message cleanup when session deleted | ✓ Good |
| Soft delete for PairedDevice | Preserve history after unpair | ✓ Good |
| Paging 3 for message lists | Scalability at high message counts | ✓ Good |
| Storage Access Framework | Cross-API export compatibility | ✓ Good |
| Non-blocking storage failures | Graceful degradation | ✓ Good |
| WorkManager for cleanup | Battery-efficient background processing | ✓ Good |

---
*Last updated: 2026-02-09 after v0.0.64 milestone*

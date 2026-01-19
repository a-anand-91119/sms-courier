# SMS Courier

## What This Is

SMS Courier is an Android app for forwarding SMS messages between two paired devices using secure SMS-based commands. A "source" device can receive SMS messages forwarded from a "target" device through a secure pairing system with password-based authentication.

## Core Value

Reliable, secure SMS forwarding between paired devices with minimal user intervention.

## Current Milestone: Play Store Launch

**Goal:** Complete closed testing period and launch publicly on Google Play Store.

**Status:** Phase 3 in progress (12 testers enrolled, 14-day waiting period)

## Requirements

### Validated

- SMS-based command protocol with SMSC prefix
- Secure pairing workflow (request -> approve -> password)
- Bcrypt password hashing with device lockout
- Foreground service for reliable message forwarding
- Room database for paired device and session management
- Jetpack Compose UI with Navigation Compose
- CI/CD pipeline with Play Store deployment
- Comprehensive test coverage (343 tests)
- Settings screen with DataStore persistence (v0.0.63)
- Notification persistence toggle (v0.0.63)
- Default forwarding duration setting (v0.0.63)
- Theme selection with immediate application (v0.0.63)
- About section with version, privacy policy, support links (v0.0.63)
- Permission status with fix navigation (v0.0.63)
- Advanced security settings (lockout, attempts, timeouts) (v0.0.63)
- Reactive settings via Flow collection in services (v0.0.63)

### Active

(Play Store Launch milestone - no code requirements)

### Out of Scope

- Message format customization — deferred to future milestone
- Data management (clear history) — deferred until device/SMS history feature
- Notification grouping — deferred to future milestone
- In-app notification channel controls — use Android system settings

## Context

**Current state:**
- App functionally complete with settings feature shipped (v0.0.63)
- Play Store closed testing in progress (12 testers, 14-day wait)
- 343 tests passing with comprehensive coverage
- All features validated, ready for production release

**Technical environment:**
- Kotlin with Jetpack Compose
- Room database (version 5)
- DataStore Preferences for settings persistence
- Material 3 with dynamic colors
- Target API 35

## Constraints

- **Play Store timeline:** 14-day closed testing period required before open beta
- **No breaking changes:** Maintain stability during dogfooding

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| DataStore vs SharedPreferences | Modern, coroutine-native, type-safe | DataStore Preferences |
| Settings UI library | Full control over Material3 styling | Custom Compose components |
| Settings architecture | Reactive updates to services | SettingsRepository with Flow properties |
| Security defaults | Match existing SecurityManager constants | Conservative defaults |

---
*Last updated: 2026-01-19 after v0.0.63 milestone shipped*

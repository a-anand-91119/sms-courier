# SMS Courier

## What This Is

SMS Courier is an Android app for forwarding SMS messages between two paired devices using secure SMS-based commands. A "source" device can receive SMS messages forwarded from a "target" device through a secure pairing system with password-based authentication.

## Core Value

Reliable, secure SMS forwarding between paired devices with minimal user intervention.

## Current Milestone: v0.0.64 Device Management & Visibility

**Goal:** Enhanced device management with session history and bidirectional forwarding visibility

**Target features:**
- Device history screen (all devices - active & removed)
- Session history with message-level details
- Export history (CSV, JSON, TXT)
- Archive management for removed devices
- Bidirectional forwarding visibility (↑↓⇅ indicators)
- Session breakdown and directional indicators
- Configurable history retention and auto-cleanup

**Parallel:** Play Store Launch (Phase 4 in progress - awaiting Google review)

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

**v0.0.64 Device Management & Visibility:**
- Device history with session tracking and message storage
- Bidirectional forwarding visibility on home screen
- Archive management for removed devices
- Export functionality (CSV, JSON, TXT formats)
- Configurable history retention and auto-cleanup
- Session/contact view toggle with persistence
- Directional indicators on paired devices list

### Out of Scope

- Message format customization — deferred to future milestone
- Notification grouping — deferred to future milestone
- In-app notification channel controls — use Android system settings
- Contact name lookup for message history — deferred to future milestone
- Delivery tracking for forwarded messages — deferred to future milestone
- PIN/fingerprint authentication for history access — deferred to future milestone

## Context

**Current state:**
- App functionally complete with settings feature shipped (v0.0.63)
- Play Store closed testing complete, applied for beta/production access
- 343 tests passing with comprehensive coverage
- Starting v0.0.64 milestone: Enhanced device management and visibility features

**Technical environment:**
- Kotlin with Jetpack Compose
- Room database (version 5)
- DataStore Preferences for settings persistence
- Material 3 with dynamic colors
- Target API 35

## Constraints

- **Play Store timeline:** Awaiting Google review for beta/production access
- **No breaking changes:** Maintain stability during dogfooding

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| DataStore vs SharedPreferences | Modern, coroutine-native, type-safe | DataStore Preferences |
| Settings UI library | Full control over Material3 styling | Custom Compose components |
| Settings architecture | Reactive updates to services | SettingsRepository with Flow properties |
| Security defaults | Match existing SecurityManager constants | Conservative defaults |
| Unpair auto-archives (ARCH-05) | Safer default preserves history; user can delete via Archive Management | Auto-archive without choice dialog |

---
*Last updated: 2026-02-08 — v0.0.64 audit complete, pending manual verification*

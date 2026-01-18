# SMS Courier

## What This Is

SMS Courier is an Android app for forwarding SMS messages between two paired devices using secure SMS-based commands. A "source" device can receive SMS messages forwarded from a "target" device through a secure pairing system with password-based authentication.

## Core Value

Reliable, secure SMS forwarding between paired devices with minimal user intervention.

## Current Milestone: v0.0.63 Settings

**Goal:** Add a settings screen with user-configurable preferences for notifications, service behavior, security, and app appearance.

**Target features:**
- Persistent notification toggle (recreate on dismiss vs stay dismissed)
- Auto-start on boot
- Default forwarding duration
- Theme selection (Light/Dark/System)
- About section (version, privacy policy, support)
- Permission status with fix links
- Advanced settings (security timeouts, pairing rate limits)

## Requirements

### Validated

- SMS-based command protocol with SMSC prefix
- Secure pairing workflow (request -> approve -> password)
- Bcrypt password hashing with device lockout
- Foreground service for reliable message forwarding
- Room database for paired device and session management
- Jetpack Compose UI with Navigation Compose
- CI/CD pipeline with Play Store deployment
- Comprehensive test coverage (301 tests)

### Active

- [ ] Settings screen with main and advanced sections
- [ ] Notification persistence toggle
- [ ] Auto-start on boot functionality
- [ ] Default forwarding duration setting
- [ ] Theme selection
- [ ] About section
- [ ] Permission status display

### Out of Scope

- Message format customization — deferred to future milestone
- Data management (clear history) — deferred until device/SMS history feature
- Notification grouping — deferred to future milestone
- In-app notification channel controls — use Android system settings

## Context

**Current state:**
- App functionally complete and approved for Play Store closed testing
- 12 testers enrolled, 14-day waiting period in progress
- 301 tests passing with comprehensive coverage
- Play Store Launch milestone running in parallel (Phases 3-4)

**Technical environment:**
- Kotlin with Jetpack Compose
- Room database (version 5)
- Material 3 with dynamic colors
- Target API 35

## Constraints

- **Parallel milestone:** Play Store Launch is still active (dogfooding phase)
- **No breaking changes:** Settings must not disrupt existing functionality
- **SharedPreferences/DataStore:** Settings persistence needs lightweight storage

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| SharedPreferences vs DataStore | TBD during implementation | — Pending |
| Settings UI library | Compose Preferences vs custom | — Pending |

---
*Last updated: 2026-01-18 after v0.0.63 milestone initialization*

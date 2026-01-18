---
phase: 10-integration-infrastructure
plan: 02
subsystem: testing
tags: [kotlin, junit, robolectric, test-fixtures, integration-tests]

dependency-graph:
  requires: [10-01]
  provides: [scenario-builders, test-state-setup]
  affects: [10-03, 11-*]

tech-stack:
  added: []
  patterns:
    - extension-functions-on-test-base
    - scenario-builder-pattern
    - bcrypt-in-tests

file-tracking:
  key-files:
    created:
      - app/src/test/java/dev/notyouraverage/smscourier/integration/ScenarioBuilders.kt
    modified:
      - app/src/test/java/dev/notyouraverage/smscourier/integration/IntegrationTestBase.kt

decisions:
  - id: internal-repositories
    choice: "Use internal instead of protected for deviceRepository and sessionRepository"
    reason: "Extension functions in separate object need access to repositories"
    impact: "Repositories accessible within test package (acceptable for test code)"

metrics:
  duration: 3m
  completed: 2026-01-18
---

# Phase 10 Plan 02: Scenario Builders Summary

One-liner: Extension functions on IntegrationTestBase for one-call setup of approved devices, active sessions, and pairing states with automatic bcrypt hashing.

## What Was Built

### ScenarioBuilders.kt (187 lines)
Object containing 7 extension functions on IntegrationTestBase:

| Function | Purpose | State Created |
|----------|---------|---------------|
| `setupApprovedSourceDevice` | Create approved SOURCE device | bcrypt hash, auth key, inserted to DB |
| `setupApprovedTargetDevice` | Create approved TARGET device | bcrypt hash, auth key, encryption key |
| `setupActiveForwardingSession` | Create device with active session | SOURCE + active ForwardingSession |
| `setupPendingPairingRequest` | Create pending received state | TARGET, PENDING_RECEIVED status |
| `setupPendingSent` | Create pending sent state | SOURCE, PENDING_SENT status |
| `setupLockedDevice` | Create locked device | failedAttempts=5, lockedUntil set |
| `setupBidirectionalPairing` | Create both roles | SOURCE and TARGET for same phone |

All builders:
- Use `SecurityManager.hashPassword()` for bcrypt hashing
- Use `SecurityManager.deriveAuthKey()` for auth key derivation
- Insert devices into repositories automatically
- Accept optional parameters for customization

## Technical Details

### Access Modifier Change
Changed `deviceRepository` and `sessionRepository` in IntegrationTestBase from `protected` to `internal`:
- `protected` restricts to subclasses only
- Extension functions in an `object` are not subclasses
- `internal` allows same-module access (acceptable for test code)

### Usage Pattern
```kotlin
class MyTest : IntegrationTestBase() {
    @Test
    fun `test with approved device`() = runTest {
        // One-call setup with automatic hashing
        val device = setupApprovedSourceDevice("+1234567890", "password123")

        // device now has:
        // - bcrypt passwordHash
        // - SHA256 authKey
        // - inserted into deviceRepository
    }
}
```

## Commits

| Hash | Type | Description |
|------|------|-------------|
| bad6694 | feat | Create ScenarioBuilders with 7 extension functions |
| 1a9800d | fix | Make repositories internal for extension function access |

## Verification Results

- [x] Both files compile: `./gradlew compileDebugUnitTestKotlin` passes
- [x] All 7 extension functions present and documented
- [x] All functions use SecurityManager for password/auth key handling
- [x] All functions insert data into repositories
- [x] TestFixtures compatible (has all required fields)
- [x] ScenarioBuilders.kt exceeds 80 line minimum (187 lines)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Changed repository access modifiers**
- **Found during:** Task 1
- **Issue:** Extension functions in `object ScenarioBuilders` cannot access `protected` members of `IntegrationTestBase`
- **Fix:** Changed `deviceRepository` and `sessionRepository` from `protected` to `internal`
- **Files modified:** IntegrationTestBase.kt
- **Commit:** 1a9800d

## Next Phase Readiness

**Unblocked:**
- Plan 10-03 can create validation tests using ScenarioBuilders
- Phase 11 E2E tests can use all scenario builders

**Available scenarios:**
- Approved pairing in both directions
- Active forwarding sessions
- Pending pairing states
- Locked device testing
- Bidirectional pairing

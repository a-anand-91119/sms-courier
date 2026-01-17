# Codebase Concerns

**Analysis Date:** 2026-01-17

## Tech Debt

**Phone Number Normalization Incomplete:**
- Issue: Basic regex normalization doesn't follow E.164 standard
- Files: `app/src/main/java/dev/notyouraverage/smscourier/repository/PairedDeviceRepository.kt` (lines 136-142)
- Why: Rapid development, deferred proper implementation
- Impact: Same device could be paired under multiple number formats; matching issues
- Fix approach: Integrate libphonenumber library for proper E.164 normalization
- Note: Code comment acknowledges this: "In production, use libphonenumber for proper E.164 normalization"

**Large Service File:**
- Issue: MasterService is 579 lines with multiple concerns mixed
- Files: `app/src/main/java/dev/notyouraverage/smscourier/services/foreground/MasterService.kt`
- Why: Central orchestrator grew organically
- Impact: Difficult to maintain, test, and reason about; high coupling
- Fix approach: Extract domain-specific handlers (PairingManager, ForwardingManager, AuthManager)

**Large UI Files:**
- Issue: Screen composables are 500+ lines each
- Files:
  - `app/src/main/java/dev/notyouraverage/smscourier/composables/screens/ForwardingControlScreen.kt` (544 lines)
  - `app/src/main/java/dev/notyouraverage/smscourier/composables/screens/HomeScreen.kt` (505 lines)
- Why: UI logic and state management co-located
- Impact: Hard to test, reuse, and maintain
- Fix approach: Extract smaller composable components, separate state handling

**Intent Actions Not Centralized:**
- Issue: Multiple constant definitions scattered across files
- Files: `MasterService.kt`, `PairingNotificationManager.kt`, `PairingActionReceiver.kt`
- Why: Features added incrementally
- Impact: Risk of typos, harder to find all actions
- Fix approach: Move all Intent actions to `Constants.kt`

## Known Bugs

**No known bugs at this time.**

The codebase appears well-maintained with comprehensive error handling.

## Security Considerations

**Encryption Key in Memory:**
- Risk: `pendingChallenges` map holds nonces in plaintext memory
- Files: `app/src/main/java/dev/notyouraverage/smscourier/security/SecurityManager.kt` (line 70)
- Current mitigation: Challenge expiry (5 minutes)
- Recommendations: Clear sensitive data on app suspension; consider Android's EncryptedSharedPreferences for temporary storage

**Password in Intent Extras:**
- Risk: Password passed through Intent to MasterService
- Files: `app/src/main/java/dev/notyouraverage/smscourier/viewmodels/ForwardingControlViewModel.kt` (lines 65-71)
- Current mitigation: Intent is local, not exported
- Recommendations: Consider in-memory secure storage; clear after use

**Auth Key String Storage:**
- Risk: Auth key (SHA256 hash of password) stored as plaintext string in entity
- Files: `app/src/main/java/dev/notyouraverage/smscourier/data/entities/PairedDevice.kt` (line 60)
- Current mitigation: Database encrypted key column uses Keystore
- Recommendations: Document secure deletion requirements; consider memory protection

## Performance Bottlenecks

**Database Calls in BroadcastReceiver Loop:**
- Problem: For each active session, makes individual database queries
- Files: `app/src/main/java/dev/notyouraverage/smscourier/receivers/SmsReceiver.kt` (lines 140-172)
- Measurement: Not profiled; potential issue with many active sessions
- Cause: N+1 query pattern when multiple sessions exist
- Improvement path: Cache active sessions in MasterService; batch queries

**Pending Challenges Cleanup:**
- Problem: Linear scan on every `generateChallenge()` call
- Files: `app/src/main/java/dev/notyouraverage/smscourier/security/SecurityManager.kt` (lines 169-172)
- Measurement: Not profiled; low concern with typical usage
- Cause: `cleanupExpiredChallenges()` called synchronously
- Improvement path: Use scheduled cleanup task instead of per-call cleanup

## Fragile Areas

**BroadcastReceiver Lifecycle:**
- Files: `app/src/main/java/dev/notyouraverage/smscourier/receivers/SmsReceiver.kt`
- Why fragile: `handleRegularSms` launches coroutine but receiver can be destroyed before completion
- Common failures: Forwarding may fail silently if receiver destroyed mid-operation
- Safe modification: Use `goAsync()` to extend receiver lifecycle during async operations
- Test coverage: No dedicated tests for SmsReceiver

**Session State Maps:**
- Files: `app/src/main/java/dev/notyouraverage/smscourier/services/foreground/MasterService.kt` (lines 65-68)
- Why fragile: `sessionHandlers`, `pendingAuthRequests`, `pendingChallenges` are mutable maps accessed from multiple coroutines
- Common failures: Concurrent modification possible during auth flow or session expiry
- Safe modification: Use `ConcurrentHashMap` or synchronize access
- Test coverage: Limited integration testing

**Timeout Handler Cleanup:**
- Files: `app/src/main/java/dev/notyouraverage/smscourier/services/foreground/MasterService.kt` (lines 256-286)
- Why fragile: Runnable stored in map may not be removed if session ends unexpectedly
- Common failures: Memory leak if service crashes; orphaned timeouts
- Safe modification: Ensure cleanup in all termination paths; add defensive cleanup in onDestroy

## Scaling Limits

**Active Sessions:**
- Current capacity: Designed for few active sessions (typical personal use)
- Limit: Database queries per SMS could slow with many sessions
- Symptoms at limit: SMS forwarding delays
- Scaling path: Cache sessions in memory; implement session limits

**SMS Rate:**
- Current capacity: Android system handles queuing
- Limit: SMS sending rate limited by carrier (varies)
- Symptoms at limit: Delayed or failed message delivery
- Scaling path: Queue messages; implement backoff; notify user of delays

## Dependencies at Risk

**No critical dependency risks identified.**

- jBCrypt 0.4: Stable, widely used
- Room, Compose, Navigation: Google-maintained, actively updated
- MockK: Actively maintained

## Missing Critical Features

**No Missing Critical Features:**
The app appears feature-complete for its intended purpose.

**Nice-to-have (not blocking):**
- Message delivery confirmation
- Retry mechanism for failed forwards
- Message history/logging UI

## Test Coverage Gaps

**SmsReceiver Not Tested:**
- What's not tested: BroadcastReceiver SMS interception and routing
- Files: `app/src/main/java/dev/notyouraverage/smscourier/receivers/SmsReceiver.kt`
- Risk: Core functionality could break unnoticed; critical component
- Priority: High
- Difficulty to test: Requires Robolectric with SMS intent simulation

**Database Migration Tests Missing:**
- What's not tested: Schema migrations (4 migrations exist)
- Files: `app/src/main/java/dev/notyouraverage/smscourier/data/SmsCourierDatabase.kt` (lines 33-92)
- Risk: Migration failures on app update could lose user data
- Priority: Medium
- Difficulty to test: Room provides testing utilities; straightforward setup

**Integration Tests Missing:**
- What's not tested: Full pairing and forwarding flows end-to-end
- Risk: Component interactions could fail even if unit tests pass
- Priority: Medium
- Difficulty to test: Requires mock SMS system or instrumented tests

## Documentation Gaps

**Security Architecture:**
- Missing: Threat model documentation
- Missing: Key rotation procedures
- Missing: Recovery process if Keystore inaccessible

**Challenge-Response Flow:**
- Missing: State machine documentation for auth flow
- Missing: Race condition handling between SOURCE and TARGET

---

*Concerns audit: 2026-01-17*
*Update as issues are fixed or new ones discovered*

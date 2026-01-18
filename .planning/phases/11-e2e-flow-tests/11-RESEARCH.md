# Phase 11: End-to-End Flow Tests - Research

**Researched:** 2026-01-18
**Domain:** Android integration testing with JUnit 4, Robolectric, Room
**Confidence:** HIGH

## Summary

Phase 11 builds end-to-end flow tests using the infrastructure established in Phase 10. The codebase has a complete integration test setup with `IntegrationTestBase` (provides Robolectric + Room + real repositories + real SecurityManager), `CapturingSmsSender` (intercepts SMS for verification), and `ScenarioBuilders` (one-call test state setup).

The primary technical constraint is that this project uses **JUnit 4.13.2**, not JUnit 5. The CONTEXT.md requested `@Nested` inner classes for test organization, but this is a JUnit 5 feature. The alternative is to organize tests using conventional JUnit 4 patterns: separate test classes per flow with comment blocks for grouping.

**Primary recommendation:** Use separate test classes per major flow (PairingFlowIntegrationTest, ForwardingFlowIntegrationTest, SecurityIntegrationTest, EdgeCaseIntegrationTest), with clear comment-based section markers instead of @Nested.

## Standard Stack

The established testing libraries for this project:

### Core (Already in Place)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| JUnit 4 | 4.13.2 | Test framework | Project standard, all tests use it |
| Robolectric | 4.11.1 | Android test environment | Enables unit tests with Android APIs |
| MockK | 1.13.8 | Mocking | Kotlin-native mocking library |
| kotlinx-coroutines-test | 1.7.3 | Coroutine testing | `runTest` for suspend functions |
| Room | 2.6.1 | In-memory database | Test isolation with real database behavior |

### Supporting (From Phase 10)
| Class | Purpose | When to Use |
|-------|---------|-------------|
| IntegrationTestBase | Abstract base with full DI | Extend for all integration tests |
| CapturingSmsSender | Captures outgoing SMS | Verify SMS commands sent |
| ScenarioBuilders | Pre-built test states | Setup complex initial states |
| IntegrationTest annotation | CI filtering marker | All integration test classes |
| TestFixtures | Factory methods | Create test entities |

### Not Available
| Feature | Why Not | Alternative |
|---------|---------|-------------|
| @Nested (JUnit 5) | Project uses JUnit 4 | Comment-based grouping, separate test classes |
| @DisplayName (JUnit 5) | JUnit 4 limitation | Backtick method names |

## Architecture Patterns

### Recommended Test Class Structure
```
app/src/test/java/dev/notyouraverage/smscourier/integration/
    IntegrationTestBase.kt          # Abstract base (exists)
    CapturingSmsSender.kt           # SMS capture (exists)
    ScenarioBuilders.kt             # State setup (exists)
    IntegrationTest.kt              # Marker annotation (exists)
    InfrastructureValidationTest.kt # Validation (exists)
    PairingFlowIntegrationTest.kt   # NEW: Full pairing flow tests
    ForwardingFlowIntegrationTest.kt # NEW: Full forwarding flow tests
    SecurityIntegrationTest.kt      # NEW: Auth lockout, challenge-response
    EdgeCaseIntegrationTest.kt      # NEW: Malformed commands, unknown devices
```

### Pattern 1: Integration Test Structure
**What:** Extend IntegrationTestBase, use ScenarioBuilders for setup
**When to use:** All integration tests
**Example:**
```kotlin
// Source: IntegrationTestBase.kt, InfrastructureValidationTest.kt
@IntegrationTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@ExperimentalCoroutinesApi
class PairingFlowIntegrationTest : IntegrationTestBase() {

    // ==================== Happy Path ====================

    @Test
    fun `givenNoExistingDevice_whenPairRequestReceived_thenDeviceCreatedWithPendingReceived`() = runTest {
        // Action: Simulate receiving PAIR_REQUEST
        commandHandler.handlePairRequest("+1234567890")
        waitForAsync()

        // Verify database state
        val device = deviceRepository.getByPhoneNumberAndRole("+1234567890", DeviceRole.TARGET)
        assertNotNull(device)
        assertEquals(PairingStatus.PENDING_RECEIVED, device?.status)
    }

    // ==================== Error Cases ====================

    @Test
    fun `givenUnknownDevice_whenStartForwardReceived_thenNoSessionCreated`() = runTest {
        // No device setup - simulates unknown sender
        // ...
    }
}
```

### Pattern 2: Multi-Step Flow Testing
**What:** Test complete flow from initiation to completion
**When to use:** Testing end-to-end user flows
**Example:**
```kotlin
// Source: InfrastructureValidationTest.kt pattern
@Test
fun `fullPairingFlow_initiate_approve_verify`() = runTest {
    val sourcePhone = "+1111111111"
    val targetPhone = "+2222222222"
    val password = "testPassword123"

    // Step 1: Initiate pairing (source side)
    commandHandler.initiatePairing(targetPhone)
    waitForAsync()

    // Checkpoint 1: Verify PAIR_REQUEST sent and device created
    capturingSmsSender.assertSent(targetPhone, Regex("SMSC PAIR_REQUEST"))
    val sourceDevice = deviceRepository.getByPhoneNumberAndRole(targetPhone, DeviceRole.SOURCE)
    assertNotNull(sourceDevice)
    assertEquals(PairingStatus.PENDING_SENT, sourceDevice?.status)

    // Clear for next step
    capturingSmsSender.clear()

    // Step 2: Receive PAIR_REQUEST on target (simulated)
    commandHandler.handlePairRequest(sourcePhone)
    waitForAsync()

    // Checkpoint 2: Target device created
    val targetDevice = deviceRepository.getByPhoneNumberAndRole(sourcePhone, DeviceRole.TARGET)
    assertNotNull(targetDevice)
    assertEquals(PairingStatus.PENDING_RECEIVED, targetDevice?.status)

    // Step 3: Approve pairing (target side)
    commandHandler.approvePairing(sourcePhone, password)
    waitForAsync()

    // Checkpoint 3: Verify PAIR_APPROVED sent, status updated, auth key set
    capturingSmsSender.assertSent(sourcePhone, Regex("SMSC PAIR_APPROVED"))
    val approvedDevice = deviceRepository.getByPhoneNumberAndRole(sourcePhone, DeviceRole.TARGET)
    assertEquals(PairingStatus.APPROVED, approvedDevice?.status)
    assertNotNull(approvedDevice?.passwordHash)
    assertNotNull(approvedDevice?.authKey)
}
```

### Pattern 3: Challenge-Response Authentication Flow
**What:** Test the complete auth flow: AUTH_REQUEST -> AUTH_CHALLENGE -> START_FORWARD
**When to use:** Testing forwarding session start
**Example:**
```kotlin
@Test
fun `fullAuthFlow_challenge_response_sessionStart`() = runTest {
    val phone = "+1234567890"
    val password = "testPassword123"

    // Setup: Approved TARGET device
    val device = setupApprovedTargetDevice(phone, password)

    // Step 1: Handle AUTH_REQUEST (generates challenge)
    commandHandler.handleAuthRequest(phone)
    waitForAsync()

    // Verify challenge sent
    val challengeMsg = capturingSmsSender.findByPrefix("SMSC AUTH_CHALLENGE")
    assertEquals(1, challengeMsg.size)
    val nonce = challengeMsg[0].message.removePrefix("SMSC AUTH_CHALLENGE ").trim()

    // Step 2: Compute correct HMAC response
    val authKey = SecurityManager.deriveAuthKey(password)
    val response = SecurityManager.computeHmac(authKey, nonce)

    // Step 3: Handle START_FORWARD with correct response
    commandHandler.handleStartForward(phone, response, 30)
    waitForAsync()

    // Verify session created
    val sessions = sessionRepository.getActiveSessionsList()
    assertEquals(1, sessions.size)
    assertEquals(phone, sessions[0].devicePhoneNumber)
    assertTrue(sessions[0].isActive)
}
```

### Anti-Patterns to Avoid
- **Mocking in integration tests:** Use real repositories and SecurityManager; only mock notificationManager
- **Testing single commands:** Unit tests already cover single commands; integration tests are for flows
- **Ignoring waitForAsync():** Always call after async operations to ensure database writes complete
- **Not clearing capturingSmsSender:** Call `clear()` between flow steps if verifying intermediate states

## Don't Hand-Roll

Problems that have existing solutions in the infrastructure:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Password hashing | Manual bcrypt calls | `SecurityManager.hashPassword()` | Consistent salt handling |
| Auth key derivation | Custom SHA-256 | `SecurityManager.deriveAuthKey()` | Matches production code |
| HMAC computation | Manual crypto | `SecurityManager.computeHmac()` | Exactly matches validation logic |
| Test device setup | Manual entity creation | `ScenarioBuilders.setupApproved*` | Handles all fields correctly |
| SMS verification | Custom list checking | `capturingSmsSender.assertSent()` | Better error messages |
| Async waiting | Thread.sleep | `waitForAsync()` | 500ms standard wait |

**Key insight:** The infrastructure exists because getting password hashes, auth keys, and HMAC responses exactly right is critical. Using ScenarioBuilders and SecurityManager's static methods ensures test data matches production behavior.

## Common Pitfalls

### Pitfall 1: JUnit 5 Features in JUnit 4
**What goes wrong:** Using `@Nested`, `@DisplayName`, `@BeforeEach` annotations
**Why it happens:** CONTEXT.md requested @Nested, but project uses JUnit 4
**How to avoid:** Use comment-based grouping and backtick method names
**Warning signs:** Compilation errors mentioning "cannot find symbol"

### Pitfall 2: Challenge Expiry in Tests
**What goes wrong:** Challenge expires during test execution (2 minute expiry)
**Why it happens:** Real time elapses between challenge generation and validation
**How to avoid:** Keep challenge-response sequences in single test method; don't delay between steps
**Warning signs:** `validateChallengeAndAuthenticate` returns `NoPendingChallenge`

### Pitfall 3: Phone Number Normalization
**What goes wrong:** Tests fail because repository normalizes phone numbers
**Why it happens:** Repository adds "+" prefix if missing
**How to avoid:** Always use E.164 format with "+" prefix in tests (e.g., "+1234567890")
**Warning signs:** `getByPhoneNumberAndRole` returns null unexpectedly

### Pitfall 4: Forgetting waitForAsync()
**What goes wrong:** Database state not updated when assertions run
**Why it happens:** Repository operations use `Dispatchers.IO`
**How to avoid:** Always call `waitForAsync()` after `commandHandler.*` methods
**Warning signs:** Assertions fail intermittently; works with added delays

### Pitfall 5: Role Confusion in Pairing
**What goes wrong:** Testing wrong device role after pairing
**Why it happens:** When A initiates pairing with B:
  - A stores B as SOURCE (A receives from B)
  - B stores A as TARGET (B forwards to A)
**How to avoid:** Document role expectations in comments; use descriptive variable names
**Warning signs:** Device lookup returns null; wrong status

### Pitfall 6: Lockout State Persistence
**What goes wrong:** Lockout state persists across tests
**Why it happens:** In-memory database is recreated per test, but `lockedUntil` is timestamp-based
**How to avoid:** Use `setupLockedDevice()` with explicit `lockDurationMs`; set future lockout
**Warning signs:** Unexpected lockout in unrelated tests

## Code Examples

Verified patterns from existing codebase:

### ScenarioBuilder Usage
```kotlin
// Source: ScenarioBuilders.kt
import dev.notyouraverage.smscourier.integration.ScenarioBuilders.setupApprovedSourceDevice
import dev.notyouraverage.smscourier.integration.ScenarioBuilders.setupApprovedTargetDevice
import dev.notyouraverage.smscourier.integration.ScenarioBuilders.setupActiveForwardingSession
import dev.notyouraverage.smscourier.integration.ScenarioBuilders.setupPendingPairingRequest
import dev.notyouraverage.smscourier.integration.ScenarioBuilders.setupLockedDevice
import dev.notyouraverage.smscourier.integration.ScenarioBuilders.setupBidirectionalPairing

// Available builders:
// setupApprovedSourceDevice(phone, password) - they receive our forwarded messages
// setupApprovedTargetDevice(phone, password) - we receive forwarded messages from them
// setupActiveForwardingSession(phone, duration, password) - returns Pair<device, session>
// setupPendingPairingRequest(phone) - waiting for user approval
// setupPendingSent(phone) - waiting for remote approval
// setupLockedDevice(phone, password, lockDurationMs) - locked from failed auth
// setupBidirectionalPairing(phone, password) - both SOURCE and TARGET roles
```

### CapturingSmsSender Assertions
```kotlin
// Source: CapturingSmsSender.kt
// Verify specific message sent
capturingSmsSender.assertSent("+1234567890", Regex("SMSC PAIR_APPROVED"))

// Verify exact message count
capturingSmsSender.assertMessageCount(1)

// Verify no messages sent
capturingSmsSender.assertNothingSent()

// Filter messages by prefix
val forwardedMessages = capturingSmsSender.findByPrefix("SMSC FWD")
val challenges = capturingSmsSender.findByPrefix("SMSC AUTH_CHALLENGE")

// Filter by recipient
val messagesToAlice = capturingSmsSender.findByPhoneNumber("+1111111111")

// Get all messages for inspection
val allMessages = capturingSmsSender.sentMessages
```

### SecurityManager Static Methods
```kotlin
// Source: SecurityManager.kt
// Hash password for storage (on TARGET)
val passwordHash = SecurityManager.hashPassword("password123")
// passwordHash.hash = bcrypt hash string
// passwordHash.salt = bcrypt salt string

// Derive auth key for HMAC (both sides)
val authKey = SecurityManager.deriveAuthKey("password123")
// authKey = Base64-encoded SHA-256 of password

// Compute HMAC response (on SOURCE)
val response = SecurityManager.computeHmac(authKey, nonce)
// response = Base64-encoded HMAC-SHA256

// Generate nonce (for manual challenge testing)
val nonce = SecurityManager.generateNonce()
// nonce = Base64-encoded 16 random bytes
```

### Complete Flow Test Example
```kotlin
// Source: Synthesized from InfrastructureValidationTest.kt patterns
@Test
fun `givenApprovedDevice_whenFullAuthFlow_thenSessionCreatedAndMessagesForwarded`() = runTest {
    val targetPhone = "+1234567890"
    val password = "testPassword123"

    // Setup: Approved TARGET device (we forward TO them)
    setupApprovedTargetDevice(targetPhone, password)

    // === AUTH FLOW ===
    // Step 1: Receive AUTH_REQUEST
    commandHandler.handleAuthRequest(targetPhone)
    waitForAsync()

    // Extract nonce from sent challenge
    val challengeMessages = capturingSmsSender.findByPrefix("SMSC AUTH_CHALLENGE")
    assertEquals(1, challengeMessages.size)
    val nonce = challengeMessages[0].message.substringAfter("AUTH_CHALLENGE ").trim()
    capturingSmsSender.clear()

    // Step 2: Compute and send correct response
    val authKey = SecurityManager.deriveAuthKey(password)
    val response = SecurityManager.computeHmac(authKey, nonce)
    commandHandler.handleStartForward(targetPhone, response, 30)
    waitForAsync()

    // Verify session created
    val sessions = sessionRepository.getActiveSessionsList()
    assertEquals(1, sessions.size)
    assertEquals(targetPhone, sessions[0].devicePhoneNumber)
    assertTrue(sessions[0].isActive)
    assertEquals(30, sessions[0].durationMinutes)

    // === FORWARDING FLOW ===
    // Step 3: Receive regular SMS, verify forwarding
    commandHandler.handleIncomingSms("+5555555555", "Your OTP is 123456")
    waitForAsync()

    // Verify forwarded message sent
    val forwardedMessages = capturingSmsSender.findByPrefix("SMSC FWD")
    assertEquals(1, forwardedMessages.size)
    assertEquals(targetPhone, forwardedMessages[0].phoneNumber)
    assertTrue(forwardedMessages[0].message.contains("+5555555555"))
    assertTrue(forwardedMessages[0].message.contains("123456"))

    // Verify message count incremented
    val updatedSession = sessionRepository.getSessionById(sessions[0].id)
    assertEquals(1, updatedSession?.messagesForwarded)

    // === STOP FLOW ===
    capturingSmsSender.clear()
    commandHandler.handleStopForward(targetPhone)
    waitForAsync()

    // Verify session ended
    val endedSession = sessionRepository.getSessionById(sessions[0].id)
    assertFalse(endedSession?.isActive ?: true)
    assertEquals("REMOTE", endedSession?.stoppedBy)
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Password auth | Challenge-response HMAC | v0.1 | Legacy commands still parsed but no longer work for auth |
| Single pairing role | Bidirectional pairing | v0.1 | Same phone can be both SOURCE and TARGET |
| Generic UNPAIR | Role-specific UNPAIR | v0.1 | `SMSC UNPAIR SOURCE` or `SMSC UNPAIR TARGET` |
| Plain FWD | Encrypted FWDE | v0.1 | Messages encrypted with auth key |

**Deprecated/outdated:**
- `SMSC start <password>`: Legacy command, auth fails
- `SMSC stop <password>`: Still works for stopping (doesn't need auth)
- Plain password in START_FORWARD: Now requires HMAC response

## Open Questions

Things that couldn't be fully resolved:

1. **Time simulation for session expiry**
   - What we know: Sessions have `expiresAt` timestamp; `expireSessions()` checks against `System.currentTimeMillis()`
   - What's unclear: Whether to inject time provider or use short durations
   - Recommendation: For Phase 11, use short durations (1 minute) and verify immediate behavior; defer time injection to future phase if needed

2. **Race condition testing specifics**
   - What we know: CONTEXT.md requests "rapid successive commands"
   - What's unclear: How to reliably simulate race conditions in single-threaded Robolectric
   - Recommendation: Test idempotency (same command twice) and state consistency (interleaved commands); true concurrency testing may require instrumented tests

3. **Multi-device session forwarding**
   - What we know: `handleIncomingSms` forwards to ALL active sessions
   - What's unclear: Maximum practical device count for tests
   - Recommendation: Test with 3 devices (meaningful multi-device without test bloat)

## Sources

### Primary (HIGH confidence)
- IntegrationTestBase.kt - Actual implementation examined
- CapturingSmsSender.kt - Actual implementation examined
- ScenarioBuilders.kt - Actual implementation examined
- InfrastructureValidationTest.kt - Working test patterns
- SmsCommandHandler.kt - Full flow logic
- SecurityManager.kt - Auth implementation
- CommandPatterns.kt / CommandParser.kt - Message format

### Secondary (MEDIUM confidence)
- SmsCommandHandlerTest.kt - Unit test patterns (mocking style)
- TestFixtures.kt - Entity factory methods

### Tertiary (LOW confidence)
- None - all research based on actual codebase inspection

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Examined actual dependencies in build.gradle.kts and libs.versions.toml
- Architecture: HIGH - Examined all Phase 10 infrastructure files
- Pitfalls: HIGH - Identified from code analysis (phone normalization, role model, challenge expiry)
- Code examples: HIGH - Synthesized from working code in repository

**Research date:** 2026-01-18
**Valid until:** 60 days (stable infrastructure, no expected changes)

---

*Phase: 11-e2e-flow-tests*
*Research completed: 2026-01-18*

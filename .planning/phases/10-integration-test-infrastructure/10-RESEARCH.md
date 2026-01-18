# Phase 10: Integration Test Infrastructure - Research

**Researched:** 2026-01-18
**Domain:** Android JVM integration testing with Robolectric, Room, and MockK
**Confidence:** HIGH

## Summary

This research establishes the infrastructure patterns for integration testing of multi-component flows in SMS Courier. The project already has a solid foundation from Phase 9 with Robolectric (4.11.1), Room in-memory database, and MockK (1.13.8) patterns established in SmsReceiverTest.kt.

The integration test infrastructure will build on existing patterns while adding:
1. A mock SmsSender for capturing outgoing messages without actual SMS transmission
2. Scenario-based test fixtures for common states (paired devices, active sessions)
3. CI configuration for dedicated integration test stage running in parallel with unit tests

**Primary recommendation:** Use existing test utilities (MainCoroutineRule, TestFixtures) and extend them with scenario builders and a capturing SmsSender mock. Keep tests in the same source set as unit tests but mark with `@IntegrationTest` annotation for CI filtering.

## Standard Stack

The established libraries/tools for this domain:

### Core (Already in Project)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Robolectric | 4.11.1 | Android component simulation | Industry standard for JVM Android tests |
| Room | 2.6.1 | Database (in-memory for tests) | Already using in-memory pattern in Phase 9 |
| MockK | 1.13.8 | Kotlin-first mocking | Already using throughout codebase |
| kotlinx-coroutines-test | 1.7.3 | Coroutine testing utilities | Standard for Kotlin async testing |
| JUnit 4 | 4.13.2 | Test framework | Already in use throughout |
| Turbine | 1.0.0 | Flow testing | Already in use for ViewModel tests |

### Supporting (Already in Project)
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| androidx.test.ext:junit | 1.2.1 | AndroidJUnit extensions | Context access in Robolectric |
| RuntimeEnvironment | (Robolectric) | Application context | Getting context for in-memory Room |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| JUnit 4 | JUnit 5 | JUnit 5 has better nested tests but requires migration |
| MockK slot | MockK MutableList | MutableList captures multiple calls, slot for single |
| @IntegrationTest annotation | Separate source set | Source set requires gradle config changes; annotation simpler |

**Installation:**
No additional dependencies needed. All required libraries are already in the project.

## Architecture Patterns

### Recommended Test Structure
```
app/src/test/java/dev/notyouraverage/smscourier/
├── MainCoroutineRule.kt          # Existing - coroutine dispatcher rule
├── TestFixtures.kt               # Existing - data factories
├── integration/                  # NEW - integration test infrastructure
│   ├── IntegrationTestBase.kt    # Base class for integration tests
│   ├── CapturingSmsSender.kt     # Mock SmsSender that captures messages
│   ├── ScenarioBuilders.kt       # Pre-built test scenarios
│   └── IntegrationTest.kt        # Marker annotation for CI filtering
├── integration/flows/            # Phase 11 - actual flow tests go here
│   ├── PairingFlowTest.kt
│   └── ForwardingFlowTest.kt
└── ... (existing test files)
```

### Pattern 1: Integration Test Base Class
**What:** Abstract base class providing common setup for integration tests
**When to use:** All integration tests that test multi-component flows
**Example:**
```kotlin
// Source: Established from Phase 9 SmsReceiverTest patterns
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@ExperimentalCoroutinesApi
abstract class IntegrationTestBase {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    protected lateinit var context: Context
    protected lateinit var database: SmsCourierDatabase
    protected lateinit var deviceRepository: PairedDeviceRepository
    protected lateinit var sessionRepository: ForwardingSessionRepository
    protected lateinit var capturingSmsSender: CapturingSmsSender
    protected lateinit var securityManager: SecurityManager
    protected lateinit var commandHandler: SmsCommandHandler

    @Before
    open fun setup() {
        context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(
            context,
            SmsCourierDatabase::class.java
        ).allowMainThreadQueries().build()

        deviceRepository = PairedDeviceRepository(database.pairedDeviceDao())
        sessionRepository = ForwardingSessionRepository(database.forwardingSessionDao())
        capturingSmsSender = CapturingSmsSender()
        securityManager = SecurityManager(deviceRepository)
        // commandHandler setup with capturing sender
    }

    @After
    open fun tearDown() {
        database.close()
    }
}
```

### Pattern 2: Capturing SmsSender (Mock Boundary)
**What:** A mock SmsSender that captures all outgoing messages for verification
**When to use:** All integration tests that verify SMS output
**Example:**
```kotlin
// Source: Pattern derived from MockK capture mechanics
class CapturingSmsSender : SmsSender(mockk(relaxed = true)) {

    data class SentMessage(
        val phoneNumber: String,
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val _sentMessages = mutableListOf<SentMessage>()
    val sentMessages: List<SentMessage> get() = _sentMessages.toList()

    override fun send(phoneNumber: String, message: String) {
        _sentMessages.add(SentMessage(phoneNumber, message))
    }

    fun clear() = _sentMessages.clear()

    fun findByType(commandPrefix: String): List<SentMessage> =
        sentMessages.filter { it.message.startsWith(commandPrefix) }

    fun assertSent(phoneNumber: String, messagePattern: Regex) {
        assertTrue(
            "Expected message matching $messagePattern to $phoneNumber",
            sentMessages.any {
                it.phoneNumber == phoneNumber && messagePattern.matches(it.message)
            }
        )
    }

    fun assertNothingSent() {
        assertTrue("Expected no messages sent, but got: $sentMessages", sentMessages.isEmpty())
    }
}
```

### Pattern 3: Scenario Builders
**What:** Factory methods that create complete test scenarios
**When to use:** Setting up complex state (paired devices with sessions)
**Example:**
```kotlin
// Source: Extension of existing TestFixtures pattern
object ScenarioBuilders {

    /**
     * Creates a device paired as SOURCE (we send forwarding commands to them)
     * - Device in APPROVED status
     * - Password hash and auth key set
     * - Ready to start forwarding
     */
    suspend fun IntegrationTestBase.setupApprovedSourceDevice(
        phoneNumber: String = "+1234567890",
        password: String = "testPassword123"
    ): PairedDevice {
        val passwordHash = SecurityManager.hashPassword(password)
        val authKey = SecurityManager.deriveAuthKey(password)

        val device = createTestDevice(
            phoneNumber = phoneNumber,
            role = DeviceRole.SOURCE,
            status = PairingStatus.APPROVED,
            passwordHash = passwordHash.hash,
            passwordSalt = passwordHash.salt,
            authKey = authKey
        )
        deviceRepository.insert(device)
        return device
    }

    /**
     * Creates a device paired as TARGET (they send forwarding commands to us)
     * - Device in APPROVED status
     * - Password hash and auth key set
     * - Ready to receive forwarding requests
     */
    suspend fun IntegrationTestBase.setupApprovedTargetDevice(
        phoneNumber: String = "+1234567890",
        password: String = "testPassword123"
    ): PairedDevice {
        val passwordHash = SecurityManager.hashPassword(password)
        val authKey = SecurityManager.deriveAuthKey(password)

        val device = createTestDevice(
            phoneNumber = phoneNumber,
            role = DeviceRole.TARGET,
            status = PairingStatus.APPROVED,
            passwordHash = passwordHash.hash,
            passwordSalt = passwordHash.salt,
            authKey = authKey
        )
        deviceRepository.insert(device)
        return device
    }

    /**
     * Creates a paired device with an active forwarding session
     */
    suspend fun IntegrationTestBase.setupActiveForwardingSession(
        phoneNumber: String = "+1234567890",
        durationMinutes: Int = 30,
        password: String = "testPassword123"
    ): Pair<PairedDevice, ForwardingSession> {
        val device = setupApprovedSourceDevice(phoneNumber, password)
        val encryptionKey = SecurityManager.deriveAuthKey(password)
        val sessionId = sessionRepository.startSession(phoneNumber, durationMinutes, encryptionKey)
        val session = sessionRepository.getSessionById(sessionId)!!
        return device to session
    }

    /**
     * Creates a pending pairing request scenario
     */
    suspend fun IntegrationTestBase.setupPendingPairingRequest(
        phoneNumber: String = "+1234567890"
    ): PairedDevice {
        val device = createTestDevice(
            phoneNumber = phoneNumber,
            role = DeviceRole.TARGET,
            status = PairingStatus.PENDING_RECEIVED
        )
        deviceRepository.insert(device)
        return device
    }
}
```

### Pattern 4: Marker Annotation for Test Filtering
**What:** Custom annotation to mark integration tests for CI filtering
**When to use:** Distinguishing integration tests from unit tests in CI
**Example:**
```kotlin
// Source: Standard JUnit4 custom annotation pattern
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class IntegrationTest

// Usage in test class
@IntegrationTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PairingFlowIntegrationTest : IntegrationTestBase() {
    // ...
}
```

### Anti-Patterns to Avoid
- **Testing MasterService directly for command flows:** MasterService has complex lifecycle; test SmsCommandHandler instead for business logic
- **Sharing database state between tests:** Always use fresh in-memory database per test
- **Real time delays in tests:** Use TestDispatcher's virtual time, never Thread.sleep()
- **Testing private methods directly:** Build on Phase 9's reflection approach only when necessary; prefer testing through public APIs

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| In-memory database | Custom mock DB | Room.inMemoryDatabaseBuilder | Handles all SQLite behavior correctly |
| Coroutine testing | Manual thread coordination | MainCoroutineRule + runTest | Handles dispatcher replacement automatically |
| Message capture | List + manual tracking | CapturingSmsSender class | Centralizes verification logic, type-safe |
| Time control | Manual clock tracking | ShadowSystemClock or Clock interface | Robolectric provides this; avoid System.currentTimeMillis() |
| Test data | Inline object creation | TestFixtures factories | Consistent defaults, less test noise |

**Key insight:** The project already has Room in-memory database patterns from Phase 9; the SmsCourierDatabase singleton requires careful handling but works with Robolectric's application context.

## Common Pitfalls

### Pitfall 1: Database Singleton Contamination
**What goes wrong:** Tests share database state through SmsCourierDatabase singleton
**Why it happens:** SmsCourierDatabase uses singleton pattern with INSTANCE variable
**How to avoid:** Clear database between tests in @After, or use separate Room.inMemoryDatabaseBuilder instances
**Warning signs:** Tests pass individually but fail when run together

### Pitfall 2: Coroutine Dispatcher Mismatch
**What goes wrong:** Tests hang or time out waiting for coroutines
**Why it happens:** Production code uses Dispatchers.IO but tests use TestDispatcher
**How to avoid:** Either inject dispatchers for testability, or use CountDownLatch pattern from Phase 9 for async operations
**Warning signs:** Tests timeout, or succeed with Thread.sleep but fail without

### Pitfall 3: Shadow vs Mock Confusion
**What goes wrong:** Robolectric shadows and MockK mocks conflict
**Why it happens:** Both try to intercept the same calls
**How to avoid:** Use shadows for Android framework classes, mocks for your own classes
**Warning signs:** Unexpected null returns, shadow methods not being called

### Pitfall 4: Test Pollution from Static State
**What goes wrong:** MasterService.isRunning or similar statics leak between tests
**Why it happens:** Static variables aren't reset by JUnit
**How to avoid:** Reset statics in @After, or mock classes that use statics
**Warning signs:** Test order dependency, different results in isolation vs suite

### Pitfall 5: Notification Manager Mocking Complexity
**What goes wrong:** PairingNotificationManager calls cause test failures
**Why it happens:** Real NotificationManager requires Android system services
**How to avoid:** Use relaxed mock for PairingNotificationManager (already done in existing tests)
**Warning signs:** NullPointerException in notification-related code paths

## Code Examples

Verified patterns from project and official sources:

### Sample Integration Test (Infrastructure Validation)
```kotlin
// Source: Phase 10 deliverable - proves infrastructure works
@IntegrationTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@ExperimentalCoroutinesApi
class IntegrationInfrastructureTest : IntegrationTestBase() {

    @Test
    fun `infrastructure validates full stack works`() = runTest {
        // Setup: Create paired device with active session
        val (device, session) = setupActiveForwardingSession(
            phoneNumber = "+1234567890",
            durationMinutes = 30
        )

        // Verify database state
        val retrievedDevice = deviceRepository.getByPhoneNumberAndRole(
            "+1234567890",
            DeviceRole.SOURCE
        )
        assertNotNull(retrievedDevice)
        assertEquals(PairingStatus.APPROVED, retrievedDevice?.status)

        // Verify session state
        val activeSessions = sessionRepository.getActiveSessionsList()
        assertEquals(1, activeSessions.size)
        assertEquals("+1234567890", activeSessions[0].devicePhoneNumber)

        // Execute: Process incoming SMS through handler
        commandHandler.handleIncomingSms("+5555555555", "Test OTP: 123456")

        // Verify: SmsSender captured the forwarded message
        assertEquals(1, capturingSmsSender.sentMessages.size)
        capturingSmsSender.assertSent(
            "+1234567890",
            Regex("SMSC FWD.*123456.*")
        )
    }

    @Test
    fun `capturing sender records all command types`() = runTest {
        val targetPhone = "+1234567890"

        // Send various commands
        capturingSmsSender.sendPairRequest(targetPhone)
        capturingSmsSender.sendAuthChallenge(targetPhone, "nonce123")
        capturingSmsSender.sendPairApproved(targetPhone)

        // Verify all captured
        assertEquals(3, capturingSmsSender.sentMessages.size)
        assertEquals(1, capturingSmsSender.findByType("SMSC PAIR_REQUEST").size)
        assertEquals(1, capturingSmsSender.findByType("SMSC AUTH_CHALLENGE").size)
        assertEquals(1, capturingSmsSender.findByType("SMSC PAIR_APPROVED").size)
    }

    @Test
    fun `scenario builder creates ready-to-forward state`() = runTest {
        // This validates the scenario builder creates correct state
        val device = setupApprovedTargetDevice("+1234567890", "password123")

        assertNotNull(device.passwordHash)
        assertNotNull(device.authKey)
        assertEquals(PairingStatus.APPROVED, device.status)
        assertEquals(DeviceRole.TARGET, device.role)

        // Verify we can generate and validate challenges
        assertFalse(securityManager.isDeviceLocked(device))
        val nonce = securityManager.generateChallenge("+1234567890")
        assertTrue(securityManager.hasPendingChallenge("+1234567890"))
    }
}
```

### Async Waiting Pattern (from Phase 9)
```kotlin
// Source: SmsReceiverTest.kt - proven pattern for async coroutine waiting
private fun waitForAsync() {
    val latch = CountDownLatch(1)
    latch.await(500, TimeUnit.MILLISECONDS)
}
```

### TestDispatcher with StandardTestDispatcher for Time Control
```kotlin
// Source: kotlinx-coroutines-test documentation
@get:Rule
val mainCoroutineRule = MainCoroutineRule(StandardTestDispatcher())

@Test
fun `session expires after duration`() = runTest {
    // Create session with 30 minute duration
    val (device, session) = setupActiveForwardingSession(durationMinutes = 30)

    // Advance time by 30 minutes
    advanceTimeBy(30 * 60 * 1000L)
    advanceUntilIdle()

    // Verify session expired
    val currentSession = sessionRepository.getActiveSessionForDevice(device.phoneNumber)
    // Note: Session expiry logic would need to run on TestDispatcher for this to work
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| TestCoroutineDispatcher (deprecated) | StandardTestDispatcher/UnconfinedTestDispatcher | kotlinx-coroutines-test 1.6 | Use runTest{} not runBlockingTest{} |
| Separate androidTest for integration | Robolectric JVM tests | Robolectric 4.0+ | Much faster, no emulator needed |
| java-test-fixtures plugin | AGP testFixtures block | AGP 8.5 | Still experimental for Kotlin |
| Manual Room transaction testing | Room testing artifact | Room 2.4+ | Built-in migration testing |

**Deprecated/outdated:**
- `runBlockingTest{}`: Replaced by `runTest{}` in kotlinx-coroutines-test 1.6+
- `TestCoroutineDispatcher`: Replaced by StandardTestDispatcher/UnconfinedTestDispatcher

## Open Questions

Things that couldn't be fully resolved:

1. **Session Expiry Testing**
   - What we know: Sessions have `expiresAt` timestamp checked in MasterService
   - What's unclear: Best approach to test time-based expiry (Robolectric ShadowSystemClock vs Clock interface injection)
   - Recommendation: Defer to Phase 11; infrastructure should support both approaches

2. **MasterService Integration**
   - What we know: MasterService uses Robolectric.buildService() successfully in existing tests
   - What's unclear: Whether full command flow through MasterService is testable without starting foreground service
   - Recommendation: Focus integration tests on SmsCommandHandler; MasterService tested through SmsReceiver routing in Phase 9

3. **Test Parallelism in CI**
   - What we know: GitLab supports `parallel: N` for test jobs
   - What's unclear: Optimal number of parallel jobs for this test suite size
   - Recommendation: Start with parallel unit and integration jobs (2 jobs), tune based on actual runtime

## Sources

### Primary (HIGH confidence)
- [Android Developer Testing Docs](https://developer.android.com/training/testing/local-tests/robolectric) - Robolectric strategies
- [Room Testing Documentation](https://developer.android.com/training/data-storage/room/testing-db) - In-memory database testing
- [kotlinx-coroutines-test API](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/) - TestDispatcher, runTest
- [Android Kotlin Coroutines Testing](https://developer.android.com/kotlin/coroutines/test) - Official testing guide
- [MockK Documentation](https://mockk.io/) - Verification and capture patterns

### Secondary (MEDIUM confidence)
- [GitLab CI Android Setup](https://about.gitlab.com/blog/setting-up-gitlab-ci-for-android-projects/) - CI configuration patterns
- [Test Fixtures in Android](https://medium.com/@theilacker/implementing-test-fixtures-in-kotlin-android-project-say-goobdye-to-sharedtest-folder-ec51f396f56f) - Fixture sharing patterns
- [Robolectric Best Practices](https://robolectric.org/best-practices/) - Service testing, thread management

### Tertiary (LOW confidence)
- Various Medium articles on Room testing - Cross-verified with official docs
- GitLab CI parallel testing examples - Specific to different project sizes

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All libraries already in project and working
- Architecture: HIGH - Building on proven Phase 9 patterns
- Pitfalls: HIGH - Based on actual issues encountered in Phase 9
- CI integration: MEDIUM - GitLab patterns documented, specific tuning TBD

**Research date:** 2026-01-18
**Valid until:** 2026-02-18 (30 days - stable patterns, libraries not fast-moving)

# Phase 24: Reproduce UAT Issues - Research

**Researched:** 2026-02-16
**Domain:** Android unit testing with Kotlin, JUnit, MockK for test-driven development
**Confidence:** HIGH

## Summary

Phase 24 requires writing 7 failing tests that prove UAT bugs exist before production code is changed. The tests must be written so they fail now but pass after fix phases (25-28) without test modifications. This is the standard Red-Green-Refactor TDD approach applied to bug fixing.

The codebase already has excellent test infrastructure in place:
- JUnit 4 for test framework with MainCoroutineRule for coroutine testing
- MockK for mocking with relaxed mocks and coEvery/coVerify for suspending functions
- Turbine for testing Flow emissions
- Robolectric for tests requiring Android context
- TestFixtures helper for creating test data
- Existing patterns: unit tests for ViewModels, repository tests through the repository layer

The primary research question is how to organize and tag these UAT reproduction tests so they can be run together as a smoke-test suite to verify which issues remain unfixed.

**Primary recommendation:** Use JUnit @Tag annotation to mark all UAT tests with a common tag (e.g., `@Tag("uat-v0.0.65")`) and optionally individual issue tags (e.g., `@Tag("HOME-01")`). Let tests fail actively rather than @Ignore them—the active failures provide better feedback and match the Red phase of TDD.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| JUnit 4 | (via libs.junit) | Test framework | Already used throughout codebase, provides @Test, @Before, assertions |
| MockK | (via libs.mockk) | Kotlin mocking | Already used for all mocking, Kotlin-native with coEvery/coVerify for suspend functions |
| Turbine | (via libs.turbine) | Flow testing | Already used in ViewModelTest files for testing StateFlow emissions |
| kotlinx-coroutines-test | (via libs) | Coroutine testing | Provides runTest, TestDispatcher, already integrated via MainCoroutineRule |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Robolectric | (via libs.robolectric) | Android context simulation | When test needs Android APIs (Context, Intent, etc.) - already used in repository tests |
| JUnit Rules | JUnit 4 | Test setup/teardown | MainCoroutineRule already exists for coroutine dispatcher management |

### Testing Patterns Already in Use
- **ViewModelTest pattern**: Mock repositories, use Turbine to test StateFlow, verify ViewModel state transformations
- **Repository pattern**: Test through repository layer (not DAO directly), use real business logic with mocked DAOs
- **TestFixtures**: Centralized test data creation with `createTestDevice()`, `createTestSession()`
- **relaxed = true**: MockK relaxed mocks for dependencies that aren't the focus of the test

## Architecture Patterns

### Recommended Test Organization

Based on existing codebase patterns and UAT requirements:

```
app/src/test/java/dev/notyouraverage/smscourier/
├── viewmodels/
│   ├── HomeViewModelTest.kt              # Add HOME-01 test here
│   ├── ForwardingControlViewModelTest.kt # Add SESS-01/02/03 tests here
│   └── DeviceHistoryViewModelTest.kt     # Add DEVH-02 test here
├── repository/
│   ├── ForwardingSessionRepositoryTest.kt # Add DEVH-01 test here (or new file)
│   └── PairedDeviceRepositoryTest.kt     # May need for DEVH-01
├── handlers/
│   └── PairingApprovalHandlerTest.kt     # NEW: Add NOTF-01 test here
└── uat/
    └── UatTestSuite.kt                   # OPTIONAL: Test suite runner
```

### Pattern 1: ViewModel Test with Flow Assertions

**What:** Test ViewModel state transformations by mocking repositories and asserting on StateFlow emissions
**When to use:** HOME-01, SESS-01/02 (UI state bugs)
**Example:**
```kotlin
// Source: Existing HomeViewModelTest.kt pattern
@Test
fun `TARGET device shows Forwarding label for active sessions`() = runTest {
    val targetDevice = createTestDevice(role = DeviceRole.TARGET)
    val session = createTestSession(devicePhoneNumber = targetDevice.phoneNumber)

    approvedDevicesFlow.value = listOf(targetDevice)
    activeSessionsFlow.value = listOf(session)

    viewModel.homeState.test {
        val state = awaitItem()

        // HOME-01: Should show "Forwarding to" not "Receiving"
        assertEquals(Direction.RECEIVING_FROM, state.directionalStatus.activeSessions[0].direction)
        // ^^ This assertion should FAIL because current code uses wrong label

        cancelAndIgnoreRemainingEvents()
    }
}
```

### Pattern 2: Repository Layer Test with Mocked DAO

**What:** Test repository business logic with mocked DAO, verify data transformations and flows
**When to use:** DEVH-01 (data layer bug)
**Example:**
```kotlin
// Source: Adapted from ForwardedMessageRepositoryTest.kt
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PairedDeviceRepositoryTest {
    @MockK
    private lateinit var deviceDao: PairedDeviceDao

    @Test
    fun `getArchivedDevices returns devices with isArchived true including session data`() = runTest {
        // DEVH-01: Verify archived devices retain history access
        val archivedDevice = createTestDevice(isArchived = true)
        coEvery { deviceDao.getArchivedDevices() } returns flowOf(listOf(archivedDevice))

        repository.getArchivedDevices().test {
            val devices = awaitItem()
            assertTrue(devices.isNotEmpty()) // Should NOT be empty
            assertEquals(archivedDevice.phoneNumber, devices[0].phoneNumber)
        }
    }
}
```

### Pattern 3: Handler Logic Test with Mocked Dependencies

**What:** Test handler/business logic that notification actions trigger, not the notification itself
**When to use:** NOTF-01 (notification action bug)
**Example:**
```kotlin
// NEW pattern based on PairingRequestsViewModelTest.kt approach
class PairingApprovalHandlerTest {
    @MockK
    private lateinit var deviceRepository: PairedDeviceRepository
    @MockK
    private lateinit var smsSender: SmsSender

    @Test
    fun `notification approve action triggers pairing approval flow`() = runTest {
        // NOTF-01: Currently approve action just opens UI, doesn't approve
        val phoneNumber = "+1234567890"

        // Setup handler to receive approve action
        val handler = PairingApprovalHandler(deviceRepository, smsSender)

        // Simulate notification approve action
        handler.handleApproveAction(phoneNumber)

        // Should call repository to approve device and send PAIR_APPROVED SMS
        coVerify { deviceRepository.approvePairing(phoneNumber) }
        verify { smsSender.sendPairApproved(phoneNumber) }
        // ^^ This should FAIL because current code just opens MainActivity
    }
}
```

### Pattern 4: SMS Sending Verification Test

**What:** Verify SmsSender methods are called when sessions stop
**When to use:** SESS-03 (missing SMS notification)
**Example:**
```kotlin
// Source: Adapted from DeviceHistoryViewModelTest pattern
@Test
fun `stopForwarding sends STOP_FORWARD SMS to other device`() = runTest {
    val device = createTestDevice(phoneNumber = "+1234567890")
    coEvery { sessionRepository.endSessionForDevice(any(), any()) } just runs

    viewModel.stopForwarding(device.phoneNumber)

    // SESS-03: Should send STOP_FORWARD SMS
    verify { smsSender.sendStopForward(device.phoneNumber) }
    // ^^ This should FAIL because current code doesn't send SMS on stop
}
```

### Test Tagging Pattern

**What:** Use JUnit @Tag to group UAT tests for filtered execution
**When to use:** All UAT tests
**Example:**
```kotlin
import org.junit.jupiter.api.Tag

@Tag("uat-v0.0.65")
@Tag("HOME-01")
@Test
fun `TARGET device shows Forwarding label for active sessions`() = runTest {
    // ... test body
}
```

**Run tagged tests:**
```bash
# Run all UAT tests
./gradlew test --tests "*" --tests "*.uat-v0.0.65"

# Run specific issue tests
./gradlew test --tests "*" --tests "*.HOME-01"
```

### Anti-Patterns to Avoid

- **Using @Ignore on failing tests:** Defeats the purpose of TDD's Red phase. Let tests fail actively to provide clear feedback on what's broken.
- **Testing DAO directly for DEVH-01/02:** User decision specifies testing through repository layer, not DAO.
- **Testing notification dispatch for NOTF-01:** User decision specifies testing handler logic, not notification creation.
- **Modifying tests in fix phases:** Tests must be written so they pass after production fixes without test changes.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Flow testing | Custom Flow collectors with delays | Turbine library | Already in codebase, provides `.test { awaitItem() }` for clean assertions |
| Test data creation | Inline constructor calls with all params | TestFixtures helper | Already exists, provides sensible defaults |
| Coroutine test dispatchers | Manual dispatcher management | MainCoroutineRule | Already implemented, handles Dispatchers.Main setup/teardown |
| SMS verification | Custom SMS capture logic | MockK verify blocks | Built-in verification with `verify { smsSender.sendXxx() }` |

**Key insight:** The codebase already has all the infrastructure needed. Don't create new test helpers or patterns—use the existing ones consistently.

## Common Pitfalls

### Pitfall 1: Writing Tests That Pass Immediately

**What goes wrong:** Writing assertions that test current (buggy) behavior instead of correct behavior
**Why it happens:** Forgetting this is TDD Red phase—tests MUST fail to prove bugs exist
**How to avoid:** After writing each test, run it immediately and verify it FAILS with the expected failure message
**Warning signs:** Test passes on first run, no assertion failures

### Pitfall 2: Testing Wrong Layer

**What goes wrong:** Testing DAO when user wants repository testing, or testing notification creation when user wants handler testing
**Why it happens:** Not following user decisions in CONTEXT.md
**How to avoid:** Re-read CONTEXT.md "Test level" section before writing each test
**Warning signs:** Test involves database Room testing or NotificationManager mocking

### Pitfall 3: Forgetting suspend Function Mocking

**What goes wrong:** Test crashes with "no answer found" for repository suspend functions
**Why it happens:** Using `every` instead of `coEvery` for suspend functions
**How to avoid:** Always use `coEvery` for suspend functions, `coVerify` for verification
**Warning signs:** Test crash: "no answer found for: suspend fun methodName()"

### Pitfall 4: Over-Complicated SESS-03 Test

**What goes wrong:** Attempting to capture actual SMS content or verify Android SmsManager
**Why it happens:** Thinking too low-level about SMS verification
**How to avoid:** Just verify `smsSender.sendStopForward()` was called—that's sufficient proof
**Warning signs:** Test involves SmsManager mocking or PendingIntent verification

### Pitfall 5: @Ignore Instead of Active Failures

**What goes wrong:** Using @Ignore annotation hides the failures from test runs
**Why it happens:** Discomfort with failing tests in the codebase
**How to avoid:** Embrace TDD Red phase—failing tests are the point. Use tags for filtering, not @Ignore
**Warning signs:** Test suite shows all green but bugs still exist

## Code Examples

### Example 1: HOME-01 Test (ViewModel + Flow)

```kotlin
// File: app/src/test/java/dev/notyouraverage/smscourier/viewmodels/HomeViewModelTest.kt
// Source: Existing test patterns in HomeViewModelTest.kt

import org.junit.jupiter.api.Tag

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @MockK
    private lateinit var deviceRepository: PairedDeviceRepository
    @MockK
    private lateinit var sessionRepository: ForwardingSessionRepository

    private val approvedDevicesFlow = MutableStateFlow(emptyList<PairedDevice>())
    private val activeSessionsFlow = MutableStateFlow(emptyList<ForwardingSession>())

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        every { deviceRepository.getApprovedDevices() } returns approvedDevicesFlow
        every { sessionRepository.getActiveSessions() } returns activeSessionsFlow

        viewModel = HomeViewModel(deviceRepository, sessionRepository)
    }

    @Tag("uat-v0.0.65")
    @Tag("HOME-01")
    @Test
    fun `TARGET device with active session shows RECEIVING_FROM direction not FORWARDING_TO`() = runTest {
        // Setup: TARGET device with active forwarding session
        val targetDevice = createTestDevice(
            phoneNumber = "+1234567890",
            role = DeviceRole.TARGET,
            status = PairingStatus.APPROVED
        )
        val activeSession = createTestSession(devicePhoneNumber = "+1234567890")

        viewModel.homeState.test {
            // Initial state
            awaitItem()

            // Emit device and session
            approvedDevicesFlow.value = listOf(targetDevice)
            activeSessionsFlow.value = listOf(activeSession)

            val state = awaitItem()

            // HOME-01: TARGET device should show "Forwarding to" direction
            // But currently shows "Receiving" (which is wrong)
            assertEquals(1, state.directionalStatus.receivingFromCount)
            assertTrue(state.directionalStatus.activeSessions.any {
                it.direction == Direction.RECEIVING_FROM
            })

            // This test FAILS because:
            // - receivingFromCount should be 0
            // - There should be a FORWARDING_TO session instead
            // After fix: TARGET role → RECEIVING_FROM direction (forwarding messages out)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

### Example 2: SESS-03 Test (SMS Notification on Stop)

```kotlin
// File: app/src/test/java/dev/notyouraverage/smscourier/viewmodels/ForwardingControlViewModelTest.kt
// Source: Adapted from existing ForwardingControlViewModelTest patterns

@Tag("uat-v0.0.65")
@Tag("SESS-03")
@Test
fun `stopForwarding sends STOP_FORWARD SMS notification to other device`() = runTest {
    val devicePhoneNumber = "+1234567890"
    coEvery { sessionRepository.endSessionForDevice(any(), any()) } just runs
    coEvery { deviceRepository.updateEncryptionKey(any(), any(), any()) } just runs

    viewModel.stopForwarding(devicePhoneNumber)

    // SESS-03: Should send STOP_FORWARD SMS to notify other device
    verify { smsSender.sendStopForward(devicePhoneNumber) }

    // This test FAILS because:
    // - Current code only calls endSessionForDevice() and updateEncryptionKey()
    // - No SMS notification is sent
    // After fix: stopForwarding will call smsSender.sendStopForward()
}
```

### Example 3: DEVH-01 Test (Repository Layer)

```kotlin
// File: app/src/test/java/dev/notyouraverage/smscourier/repository/ForwardingSessionRepositoryTest.kt
// Source: New test file following ForwardedMessageRepositoryTest pattern

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ForwardingSessionRepositoryTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @MockK
    private lateinit var sessionDao: ForwardingSessionDao

    private lateinit var repository: ForwardingSessionRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        repository = ForwardingSessionRepository(sessionDao)
    }

    @Tag("uat-v0.0.65")
    @Tag("DEVH-01")
    @Test
    fun `getSessionsForDevice returns sessions for archived device with history data`() = runTest {
        val archivedDevicePhone = "+1234567890"
        val sessions = listOf(
            createTestSession(id = 1, devicePhoneNumber = archivedDevicePhone, isActive = false),
            createTestSession(id = 2, devicePhoneNumber = archivedDevicePhone, isActive = false)
        )

        every { sessionDao.getSessionsForDevice(archivedDevicePhone) } returns flowOf(sessions)

        repository.getSessionsForDevice(archivedDevicePhone).test {
            val result = awaitItem()

            // DEVH-01: Should return session history for archived devices
            assertFalse(result.isEmpty())
            assertEquals(2, result.size)

            // This test FAILS if:
            // - Query filters out archived device sessions
            // - isArchived flag blocks history queries
            // After fix: Archived devices retain full session history access

            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

### Example 4: NOTF-01 Test (Handler Logic)

```kotlin
// File: app/src/test/java/dev/notyouraverage/smscourier/handlers/PairingApprovalHandlerTest.kt
// Source: New test file following ViewModel test patterns

@OptIn(ExperimentalCoroutinesApi::class)
class PairingApprovalHandlerTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @MockK
    private lateinit var deviceRepository: PairedDeviceRepository
    @MockK
    private lateinit var smsSender: SmsSender

    private lateinit var handler: PairingApprovalHandler

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        // Handler may need to be extracted from PairingActionReceiver
        // or tested through a ViewModel method
    }

    @Tag("uat-v0.0.65")
    @Tag("NOTF-01")
    @Test
    fun `approve notification action triggers actual pairing approval`() = runTest {
        val phoneNumber = "+1234567890"
        val device = createTestDevice(
            phoneNumber = phoneNumber,
            status = PairingStatus.PENDING_RECEIVED
        )

        coEvery { deviceRepository.getByPhoneNumberAndRole(any(), any()) } returns device
        coEvery { deviceRepository.update(any()) } just runs

        // Simulate notification approve action
        // Currently: PairingActionReceiver just launches MainActivity
        // Should: Actually approve the pairing
        handler.handleApproveAction(phoneNumber)

        // NOTF-01: Should update device status and send PAIR_APPROVED SMS
        coVerify {
            deviceRepository.update(match {
                it.phoneNumber == phoneNumber &&
                it.status == PairingStatus.APPROVED
            })
        }
        verify { smsSender.sendPairApproved(phoneNumber) }

        // This test FAILS because:
        // - Current code just opens MainActivity with navigation intent
        // - No approval logic is triggered by notification action
        // After fix: Notification action directly approves pairing
    }
}
```

### Example 5: DEVH-02 Test (Archive Action)

```kotlin
// File: app/src/test/java/dev/notyouraverage/smscourier/viewmodels/DeviceHistoryViewModelTest.kt
// Source: Existing test in DeviceHistoryViewModelTest.kt (verify pattern exists)

@Tag("uat-v0.0.65")
@Tag("DEVH-02")
@Test
fun `archiveDevice moves active device to archived state`() = runTest {
    val device = createTestDevice(
        phoneNumber = "+1234567890",
        role = DeviceRole.TARGET,
        status = PairingStatus.APPROVED,
        isArchived = false
    )

    coEvery { deviceRepository.archiveDevice(any(), any(), any()) } just runs

    viewModel.archiveDevice(device)

    // DEVH-02: Should call repository archiveDevice method
    coVerify {
        deviceRepository.archiveDevice(
            phoneNumber = "+1234567890",
            role = DeviceRole.TARGET,
            initiatedBy = "USER"
        )
    }

    // This test FAILS if:
    // - No archiveDevice method exists in ViewModel
    // - Archive action doesn't exist in UI
    // After fix: User can explicitly archive without unpairing
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| @Ignore failing tests | Active failing tests with tags | Current (2024-2025) | Failing tests provide immediate feedback on what's broken |
| Manual Flow testing with delays | Turbine library `.test { awaitItem() }` | Already adopted | Clean, reliable Flow assertions without timing issues |
| Mockito for Kotlin | MockK native library | Already adopted | Better Kotlin support, coEvery/coVerify for suspend functions |
| JUnit 3 | JUnit 4 | Already adopted | @Test annotation, @Before/@After, Rules support |
| Manual dispatcher management | JUnit Rule pattern | Already adopted | MainCoroutineRule handles setup/teardown |

**Current practices (2024-2026):**
- Test-driven development with Red-Green-Refactor cycle
- Repository-layer testing (not DAO) per Clean Architecture
- MockK with relaxed mocks for dependencies
- Turbine for StateFlow testing in ViewModels
- Active failing tests during Red phase (not @Ignore)

## Open Questions

1. **Handler extraction for NOTF-01**
   - What we know: PairingActionReceiver currently just launches MainActivity with Intent
   - What's unclear: Whether to extract handler logic or test through ViewModel
   - Recommendation: Check if PairingRequestsViewModel has approval method we can test. If not, may need to extract handler logic to make it testable.

2. **SESS-01/02 test structure**
   - What we know: Both SOURCE and TARGET should see/stop sessions
   - What's unclear: Whether to write separate tests or combined scenario test
   - Recommendation: Write separate tests—clearer failure messages, easier to debug individual issues

3. **DEVH-01 actual bug location**
   - What we know: Archived devices should show history
   - What's unclear: Whether bug is in repository query, DAO query, or UI filtering
   - Recommendation: Start with repository-level test as specified in CONTEXT.md, may reveal actual bug location

## Sources

### Primary (HIGH confidence)
- Context7: /websites/junit_current - JUnit tagging and test organization
- Context7: /mockk/mockk - MockK mocking patterns for Kotlin
- Codebase: app/src/test/java/dev/notyouraverage/smscourier/ - Existing test patterns
- Codebase: TestFixtures.kt, MainCoroutineRule.kt - Test infrastructure
- CONTEXT.md: Phase 24 user decisions (test level, failing test strategy)

### Secondary (MEDIUM confidence)
- [Kotlin Unit Testing Guide for Android Developers](https://bugfender.com/blog/kotlin-unit-testing/) - Best practices for Kotlin testing
- [Test-Driven Development in Android with Kotlin](https://medium.com/@ramadan123sayed/test-driven-development-in-android-with-kotlin-e8f706d30e80) - TDD Red-Green-Refactor cycle
- [Unit testing in Kotlin projects with MockK](https://blog.logrocket.com/unit-testing-kotlin-projects-with-mockk-vs-mockito/) - MockK patterns and coEvery/coVerify
- [Test Your Android App With MockK](https://medium.com/getir/test-your-android-app-unit-test-with-mockk-28c1c465bafc) - MockK repository testing patterns

### Tertiary (LOW confidence)
- None required—primary sources (Context7 + codebase) provide full coverage

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All libraries already in use, verified in build.gradle.kts and existing tests
- Architecture: HIGH - Patterns extracted directly from existing test files in codebase
- Pitfalls: HIGH - Based on common TDD mistakes and Kotlin testing gotchas documented in official sources

**Research date:** 2026-02-16
**Valid until:** 60 days (stable tooling, established patterns)

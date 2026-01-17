# Testing Patterns

**Analysis Date:** 2026-01-17

## Test Framework

**Runner:**
- JUnit 4.13.2 - Primary test framework
- Robolectric 4.11.1 - Android component testing (SDK 34)
- Config: Test classes annotated with `@RunWith(RobolectricTestRunner::class)` when Android framework needed

**Assertion Library:**
- JUnit built-in assertions: `assertTrue`, `assertFalse`, `assertEquals`, `assertNull`
- Turbine for Flow assertions: `awaitItem()`, `cancelAndIgnoreRemainingEvents()`

**Mocking:**
- MockK 1.13.8 - Kotlin-first mocking
- Annotations: `@MockK`, `@RelaxedMockK`
- Setup: `MockKAnnotations.init(this, relaxed = true)`

**Run Commands:**
```bash
./gradlew test                              # Run all unit tests
./gradlew test --tests "*SecurityManager*"  # Run specific test class
./gradlew connectedAndroidTest              # Run instrumented tests (requires device)
```

## Test File Organization

**Location:**
- `app/src/test/java/dev/notyouraverage/smscourier/` - Unit tests
- Organized by feature, mirroring main source structure

**Naming:**
- Pattern: `*Test.kt` suffix
- Examples: `CommandParserTest.kt`, `SecurityManagerTest.kt`, `HomeViewModelTest.kt`

**Structure:**
```
app/src/test/java/dev/notyouraverage/smscourier/
├── MainCoroutineRule.kt              # Test setup rule for coroutines
├── TestFixtures.kt                   # Shared test data builders
├── commands/
│   ├── CommandParserTest.kt
│   └── CommandPatternsTest.kt
├── handlers/
│   └── SmsCommandHandlerTest.kt
├── security/
│   ├── MessageEncryptionTest.kt
│   ├── SecurityManagerStaticTest.kt
│   └── SecurityManagerTest.kt
├── receivers/
│   └── ServiceNotificationReceiverTest.kt
├── services/
│   └── MasterServiceTest.kt
└── viewmodels/
    ├── AddDeviceViewModelTest.kt
    ├── ForwardingControlViewModelTest.kt
    ├── HomeViewModelTest.kt
    ├── PairedDevicesViewModelTest.kt
    └── PairingRequestsViewModelTest.kt
```

## Test Structure

**Suite Organization:**
```kotlin
@ExperimentalCoroutinesApi
class HomeViewModelTest {
    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @MockK
    private lateinit var deviceRepository: PairedDeviceRepository

    @MockK
    private lateinit var sessionRepository: ForwardingSessionRepository

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        // Setup mock behaviors
        every { deviceRepository.getApprovedDevicesCount() } returns flowOf(0)
        viewModel = HomeViewModel(deviceRepository, sessionRepository)
    }

    @Test
    fun `homeState updates when approved devices change`() = runTest {
        // Test implementation
    }
}
```

**Patterns:**
- `@Before` for per-test setup
- `@After` for cleanup (if needed)
- `MockKAnnotations.init(this, relaxed = true)` for mock initialization
- `MainCoroutineRule` for coroutine test dispatcher

## Mocking

**Framework:**
- MockK for all mocking needs
- `@MockK` annotation for mock declarations
- `relaxed = true` for flexible mocking (returns sensible defaults)

**Patterns:**
```kotlin
// Mock declaration
@MockK
private lateinit var repository: PairedDeviceRepository

// Setup behavior
every { repository.getDevice(any()) } returns flowOf(device)
coEvery { repository.updateDevice(any()) } just Runs

// Verification
verify { repository.getDevice(phoneNumber) }
coVerify { repository.updateDevice(match { it.phoneNumber == phoneNumber }) }
```

**What to Mock:**
- Repositories (database access)
- SmsSender (SMS sending)
- External dependencies

**What NOT to Mock:**
- Pure functions (CommandParser, SecurityManager static methods)
- Simple utilities
- Data classes

## Fixtures and Factories

**Test Data:**
```kotlin
// TestFixtures.kt
object TestFixtures {
    fun createTestDevice(
        phoneNumber: String = "+1234567890",
        role: DeviceRole = DeviceRole.TARGET,
        status: PairingStatus = PairingStatus.APPROVED,
        passwordHash: String? = null,
        authKey: String? = null,
        failedAttempts: Int = 0,
        lockoutUntil: Long? = null
    ) = PairedDevice(
        phoneNumber = phoneNumber,
        role = role,
        status = status,
        // ... remaining fields
    )

    fun createTestSession(
        phoneNumber: String = "+1234567890",
        isActive: Boolean = true,
        durationMinutes: Int = 60
    ) = ForwardingSession(
        // ... fields
    )
}
```

**Location:**
- `app/src/test/java/dev/notyouraverage/smscourier/TestFixtures.kt`

## Coverage

**Requirements:**
- No enforced coverage target
- Focus on critical paths: security, command parsing, state management

**Configuration:**
- Jacoco not explicitly configured
- Manual coverage tracking via test presence

**Key Areas Tested:**
- `security/SecurityManager` - Password verification, lockout, challenge-response (40+ tests)
- `commands/CommandParser` - All SMSC command types
- `viewmodels/*` - State management with Turbine
- `handlers/SmsCommandHandler` - Command processing logic

## Test Types

**Unit Tests:**
- Scope: Single class/function in isolation
- Mocking: All dependencies mocked
- Speed: Fast (<100ms per test)
- Examples: `CommandParserTest.kt`, `MessageEncryptionTest.kt`

**ViewModel Tests:**
- Scope: ViewModel state management
- Mocking: Repositories mocked, flows simulated
- Tools: Turbine for flow testing, MainCoroutineRule for dispatcher
- Examples: `HomeViewModelTest.kt`, `ForwardingControlViewModelTest.kt`

**Robolectric Tests:**
- Scope: Tests requiring Android framework
- Annotation: `@RunWith(RobolectricTestRunner::class)` with `@Config(sdk = [34])`
- Examples: `SecurityManagerTest.kt`, `SmsCommandHandlerTest.kt`

**Integration Tests:**
- Scope: Multiple components together
- Current: Limited - mostly mocked integration
- Missing: Full SMS flow integration tests

**Instrumented Tests:**
- Location: `app/src/androidTest/`
- Status: Basic setup (AndroidJUnitRunner), minimal tests
- Run: `./gradlew connectedAndroidTest`

## Common Patterns

**Async Testing:**
```kotlin
@Test
fun `async operation completes successfully`() = runTest {
    val result = suspendFunction()
    assertEquals("expected", result)
}
```

**Flow Testing with Turbine:**
```kotlin
@Test
fun `state updates on data change`() = runTest {
    viewModel.state.test {
        awaitItem()  // Initial state
        dataFlow.value = newData
        val state = awaitItem()
        assertEquals(expected, state.value)
        cancelAndIgnoreRemainingEvents()
    }
}
```

**Error Testing:**
```kotlin
@Test
fun `throws on invalid input`() {
    assertThrows(IllegalArgumentException::class.java) {
        functionCall(invalidInput)
    }
}

@Test
fun `returns null on failure`() {
    val result = functionThatCanFail()
    assertNull(result)
}
```

**Test Naming:**
- Backtick-enclosed descriptive names
- Pattern: `` `action produces expected result`() ``
- Examples:
  - `` `parse returns null for non-SMSC message`() ``
  - `` `verifyPassword returns true for correct password`() ``
  - `` `recordFailedAttempt locks device after max attempts`() ``

## Test Utilities

**MainCoroutineRule:**
```kotlin
// MainCoroutineRule.kt
@ExperimentalCoroutinesApi
class MainCoroutineRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

**Usage:**
```kotlin
@get:Rule
val mainCoroutineRule = MainCoroutineRule()
```

## Coverage Gaps

**Not Tested:**
- `SmsReceiver` - Critical component, no dedicated tests
- Database migrations - No migration tests
- Full SMS flow integration
- Error boundary behavior

**Recommended Additions:**
- BroadcastReceiver tests with Robolectric
- Database migration tests
- Integration tests for pairing/forwarding flows

---

*Testing analysis: 2026-01-17*
*Update when test patterns change*

package dev.notyouraverage.smscourier.integration

import android.content.Context
import androidx.room.Room
import dev.notyouraverage.smscourier.MainCoroutineRule
import dev.notyouraverage.smscourier.data.SmsCourierDatabase
import dev.notyouraverage.smscourier.handlers.SmsCommandHandler
import dev.notyouraverage.smscourier.notifications.PairingNotificationManager
import dev.notyouraverage.smscourier.repository.ForwardingSessionRepository
import dev.notyouraverage.smscourier.repository.PairedDeviceRepository
import dev.notyouraverage.smscourier.security.SecurityManager
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Abstract base class for integration tests.
 *
 * Provides common setup:
 * - Robolectric Android environment
 * - In-memory Room database
 * - Real repositories (PairedDeviceRepository, ForwardingSessionRepository)
 * - CapturingSmsSender for verifying outgoing SMS
 * - Real SecurityManager with bcrypt validation
 * - SmsCommandHandler wired with all dependencies
 *
 * Subclasses can override setup() and tearDown() but must call super.
 */
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
    protected lateinit var notificationManager: PairingNotificationManager
    protected lateinit var commandHandler: SmsCommandHandler

    @Before
    open fun setup() {
        context = RuntimeEnvironment.getApplication()

        // Create in-memory database for isolated tests
        database = Room.inMemoryDatabaseBuilder(
            context,
            SmsCourierDatabase::class.java,
        ).allowMainThreadQueries().build()

        // Initialize repositories with in-memory database
        deviceRepository = PairedDeviceRepository(database.pairedDeviceDao())
        sessionRepository = ForwardingSessionRepository(database.forwardingSessionDao())

        // Initialize capturing SMS sender (captures outgoing messages for verification)
        capturingSmsSender = CapturingSmsSender()

        // Initialize security manager with real bcrypt validation
        securityManager = SecurityManager(deviceRepository)

        // Mock notification manager (relaxed - don't need to verify notifications)
        notificationManager = mockk(relaxed = true)

        // Wire up command handler with all dependencies
        commandHandler = SmsCommandHandler(
            deviceRepository = deviceRepository,
            sessionRepository = sessionRepository,
            smsSender = capturingSmsSender,
            notificationManager = notificationManager,
            securityManager = securityManager,
            onForwardingStateChanged = { /* no-op for tests */ },
        )
    }

    @After
    open fun tearDown() {
        // Close database to release resources
        database.close()

        // Clear captured messages for next test
        capturingSmsSender.clear()
    }

    /**
     * Waits for async coroutines to complete.
     *
     * Use when testing code that launches coroutines on Dispatchers.IO.
     * The 500ms timeout handles typical async operations.
     */
    protected fun waitForAsync() {
        val latch = CountDownLatch(1)
        latch.await(500, TimeUnit.MILLISECONDS)
    }
}

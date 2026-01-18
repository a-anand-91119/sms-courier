package dev.notyouraverage.smscourier.receivers

import android.content.Context
import android.content.Intent
import android.provider.Telephony
import dev.notyouraverage.smscourier.commands.ParsedCommand
import dev.notyouraverage.smscourier.data.SmsCourierDatabase
import dev.notyouraverage.smscourier.data.entities.DeviceRole
import dev.notyouraverage.smscourier.services.foreground.MasterService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SmsReceiverTest {

    companion object {
        private const val TAG = "SMSC:SmsReceiverTest"
        private const val TEST_SENDER = "+1234567890"
    }

    private lateinit var receiver: SmsReceiver
    private lateinit var context: Context
    private lateinit var database: SmsCourierDatabase

    @Before
    fun setup() {
        receiver = SmsReceiver()
        context = RuntimeEnvironment.getApplication()
        // Clear any previously started services
        shadowOf(RuntimeEnvironment.getApplication()).clearStartedServices()

        // Create in-memory database for forwarding tests
        // Note: SmsCourierDatabase.getDatabase() uses a singleton pattern,
        // so the database will be shared across tests. We use in-memory
        // database which Robolectric handles appropriately.
        database = SmsCourierDatabase.getDatabase(context)
    }

    @After
    fun tearDown() = runBlocking {
        // Clean up database between tests
        database.forwardingSessionDao().let { dao ->
            dao.getActiveSessionsList().forEach { session ->
                dao.endSession(session.id, "TEST_CLEANUP")
            }
        }
        database.pairedDeviceDao().let { dao ->
            // Clean up test devices
            listOf("+1234567890", "+1111111111", "+2222222222", "+9876543210").forEach { phone ->
                dao.deleteByPhoneNumber(phone)
            }
        }
    }

    // ==================== Validation Tests ====================

    @Test
    fun `onReceive does nothing with null context`() {
        // Given an SMS received intent
        val intent = Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)

        // When receiver handles the intent with null context
        try {
            receiver.onReceive(null, intent)
            // Then it should not crash - early return expected
            assertTrue("Should not crash with null context", true)
        } catch (e: Exception) {
            fail("Should not throw exception with null context: ${e.message}")
        }

        // Verify no service was started
        val shadowApplication = shadowOf(RuntimeEnvironment.getApplication())
        assertNull(
            "No service should be started with null context",
            shadowApplication.peekNextStartedService(),
        )
    }

    @Test
    fun `onReceive does nothing with null intent`() {
        // When receiver handles null intent
        try {
            receiver.onReceive(context, null)
            // Then it should not crash - early return expected
            assertTrue("Should not crash with null intent", true)
        } catch (e: Exception) {
            fail("Should not throw exception with null intent: ${e.message}")
        }

        // Verify no service was started
        val shadowApplication = shadowOf(RuntimeEnvironment.getApplication())
        assertNull(
            "No service should be started with null intent",
            shadowApplication.peekNextStartedService(),
        )
    }

    @Test
    fun `onReceive does nothing with wrong action`() {
        // Given an intent with wrong action
        val intent = Intent("WRONG_ACTION")

        // When receiver handles the intent
        receiver.onReceive(context, intent)

        // Then no service should be started
        val shadowApplication = shadowOf(RuntimeEnvironment.getApplication())
        assertNull(
            "No service should be started for wrong action",
            shadowApplication.peekNextStartedService(),
        )
    }

    @Test
    fun `onReceive does nothing with empty messages`() {
        // Given an SMS received intent without PDUs (empty messages array)
        val intent = Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        // Note: Without PDUs, getMessagesFromIntent returns empty array

        // When receiver handles the intent
        receiver.onReceive(context, intent)

        // Then no service should be started (empty messages trigger early return)
        val shadowApplication = shadowOf(RuntimeEnvironment.getApplication())
        assertNull(
            "No service should be started for empty messages",
            shadowApplication.peekNextStartedService(),
        )
    }

    // ==================== Command Routing Tests ====================
    // These tests verify that handleCommand routes commands correctly to MasterService.
    // We use reflection to call handleCommand directly since constructing valid SMS PDUs
    // in unit tests is complex. Integration tests (Phase 10) will test the full flow.

    @Test
    fun `routes PAIR_REQUEST command to MasterService`() {
        // Given a PAIR_REQUEST command
        val command = ParsedCommand.PairRequest(TEST_SENDER)

        // When handleCommand is called
        invokeHandleCommand(command, TEST_SENDER, "SMSC PAIR_REQUEST")

        // Then MasterService should be started with correct extras
        val startedService = getNextStartedService()
        assertNotNull("Service should be started", startedService)
        assertEquals(MasterService.PROCESS_COMMAND, startedService.action)
        assertEquals(TEST_SENDER, startedService.getStringExtra(MasterService.EXTRA_SENDER))
        assertEquals(
            "PAIR_REQUEST",
            startedService.getStringExtra(MasterService.EXTRA_COMMAND_TYPE),
        )
    }

    @Test
    fun `routes PAIR_APPROVED command to MasterService`() {
        // Given a PAIR_APPROVED command
        val command = ParsedCommand.PairApproved(TEST_SENDER)

        // When handleCommand is called
        invokeHandleCommand(command, TEST_SENDER, "SMSC PAIR_APPROVED")

        // Then MasterService should be started with correct extras
        val startedService = getNextStartedService()
        assertNotNull("Service should be started", startedService)
        assertEquals(MasterService.PROCESS_COMMAND, startedService.action)
        assertEquals(TEST_SENDER, startedService.getStringExtra(MasterService.EXTRA_SENDER))
        assertEquals(
            "PAIR_APPROVED",
            startedService.getStringExtra(MasterService.EXTRA_COMMAND_TYPE),
        )
    }

    @Test
    fun `routes PAIR_REJECTED command to MasterService`() {
        // Given a PAIR_REJECTED command
        val command = ParsedCommand.PairRejected(TEST_SENDER)

        // When handleCommand is called
        invokeHandleCommand(command, TEST_SENDER, "SMSC PAIR_REJECTED")

        // Then MasterService should be started with correct extras
        val startedService = getNextStartedService()
        assertNotNull("Service should be started", startedService)
        assertEquals(MasterService.PROCESS_COMMAND, startedService.action)
        assertEquals(TEST_SENDER, startedService.getStringExtra(MasterService.EXTRA_SENDER))
        assertEquals(
            "PAIR_REJECTED",
            startedService.getStringExtra(MasterService.EXTRA_COMMAND_TYPE),
        )
    }

    @Test
    fun `routes UNPAIR command to MasterService`() {
        // Given an UNPAIR command without role
        val command = ParsedCommand.Unpair(TEST_SENDER, roleToDelete = null)

        // When handleCommand is called
        invokeHandleCommand(command, TEST_SENDER, "SMSC UNPAIR")

        // Then MasterService should be started with correct extras
        val startedService = getNextStartedService()
        assertNotNull("Service should be started", startedService)
        assertEquals(MasterService.PROCESS_COMMAND, startedService.action)
        assertEquals(TEST_SENDER, startedService.getStringExtra(MasterService.EXTRA_SENDER))
        assertEquals("UNPAIR", startedService.getStringExtra(MasterService.EXTRA_COMMAND_TYPE))
        // No role extra should be present
        assertNull(startedService.getStringExtra(MasterService.EXTRA_UNPAIR_ROLE))
    }

    @Test
    fun `routes UNPAIR with role to MasterService`() {
        // Given an UNPAIR command with SOURCE role
        val command = ParsedCommand.Unpair(TEST_SENDER, roleToDelete = DeviceRole.SOURCE)

        // When handleCommand is called
        invokeHandleCommand(command, TEST_SENDER, "SMSC UNPAIR SOURCE")

        // Then MasterService should be started with correct extras including role
        val startedService = getNextStartedService()
        assertNotNull("Service should be started", startedService)
        assertEquals(MasterService.PROCESS_COMMAND, startedService.action)
        assertEquals(TEST_SENDER, startedService.getStringExtra(MasterService.EXTRA_SENDER))
        assertEquals("UNPAIR", startedService.getStringExtra(MasterService.EXTRA_COMMAND_TYPE))
        assertEquals("SOURCE", startedService.getStringExtra(MasterService.EXTRA_UNPAIR_ROLE))
    }

    @Test
    fun `routes AUTH_REQUEST command to MasterService`() {
        // Given an AUTH_REQUEST command
        val command = ParsedCommand.AuthRequest(TEST_SENDER)

        // When handleCommand is called
        invokeHandleCommand(command, TEST_SENDER, "SMSC AUTH_REQUEST")

        // Then MasterService should be started with correct extras
        val startedService = getNextStartedService()
        assertNotNull("Service should be started", startedService)
        assertEquals(MasterService.PROCESS_COMMAND, startedService.action)
        assertEquals(TEST_SENDER, startedService.getStringExtra(MasterService.EXTRA_SENDER))
        assertEquals(
            "AUTH_REQUEST",
            startedService.getStringExtra(MasterService.EXTRA_COMMAND_TYPE),
        )
    }

    @Test
    fun `routes AUTH_CHALLENGE command with nonce to MasterService`() {
        // Given an AUTH_CHALLENGE command with nonce
        val nonce = "abc123def456"
        val command = ParsedCommand.AuthChallenge(TEST_SENDER, nonce)

        // When handleCommand is called
        invokeHandleCommand(command, TEST_SENDER, "SMSC AUTH_CHALLENGE $nonce")

        // Then MasterService should be started with correct extras including nonce
        val startedService = getNextStartedService()
        assertNotNull("Service should be started", startedService)
        assertEquals(MasterService.PROCESS_COMMAND, startedService.action)
        assertEquals(TEST_SENDER, startedService.getStringExtra(MasterService.EXTRA_SENDER))
        assertEquals(
            "AUTH_CHALLENGE",
            startedService.getStringExtra(MasterService.EXTRA_COMMAND_TYPE),
        )
        assertEquals(nonce, startedService.getStringExtra(MasterService.EXTRA_NONCE))
    }

    @Test
    fun `routes START_FORWARD command with password to MasterService`() {
        // Given a START_FORWARD command with password only
        val password = "secret123"
        val command = ParsedCommand.StartForward(TEST_SENDER, password, durationMinutes = null)

        // When handleCommand is called
        invokeHandleCommand(command, TEST_SENDER, "SMSC START_FORWARD $password")

        // Then MasterService should be started with correct extras
        val startedService = getNextStartedService()
        assertNotNull("Service should be started", startedService)
        assertEquals(MasterService.PROCESS_COMMAND, startedService.action)
        assertEquals(TEST_SENDER, startedService.getStringExtra(MasterService.EXTRA_SENDER))
        assertEquals(
            "START_FORWARD",
            startedService.getStringExtra(MasterService.EXTRA_COMMAND_TYPE),
        )
        assertEquals(password, startedService.getStringExtra(MasterService.EXTRA_PASSWORD))
        // Duration should not be set (default 0)
        assertEquals(0, startedService.getIntExtra(MasterService.EXTRA_DURATION, 0))
    }

    @Test
    fun `routes START_FORWARD with duration to MasterService`() {
        // Given a START_FORWARD command with password and duration
        val password = "secret123"
        val duration = 60
        val command = ParsedCommand.StartForward(TEST_SENDER, password, durationMinutes = duration)

        // When handleCommand is called
        invokeHandleCommand(command, TEST_SENDER, "SMSC START_FORWARD $password $duration")

        // Then MasterService should be started with correct extras including duration
        val startedService = getNextStartedService()
        assertNotNull("Service should be started", startedService)
        assertEquals(MasterService.PROCESS_COMMAND, startedService.action)
        assertEquals(TEST_SENDER, startedService.getStringExtra(MasterService.EXTRA_SENDER))
        assertEquals(
            "START_FORWARD",
            startedService.getStringExtra(MasterService.EXTRA_COMMAND_TYPE),
        )
        assertEquals(password, startedService.getStringExtra(MasterService.EXTRA_PASSWORD))
        assertEquals(duration, startedService.getIntExtra(MasterService.EXTRA_DURATION, 0))
    }

    @Test
    fun `routes STOP_FORWARD command to MasterService`() {
        // Given a STOP_FORWARD command
        val command = ParsedCommand.StopForward(TEST_SENDER)

        // When handleCommand is called
        invokeHandleCommand(command, TEST_SENDER, "SMSC STOP_FORWARD")

        // Then MasterService should be started with correct extras
        val startedService = getNextStartedService()
        assertNotNull("Service should be started", startedService)
        assertEquals(MasterService.PROCESS_COMMAND, startedService.action)
        assertEquals(TEST_SENDER, startedService.getStringExtra(MasterService.EXTRA_SENDER))
        assertEquals(
            "STOP_FORWARD",
            startedService.getStringExtra(MasterService.EXTRA_COMMAND_TYPE),
        )
    }

    @Test
    fun `routes FWD command with original sender and message to MasterService`() {
        // Given a FWD command (forwarded data)
        val originalSender = "+0987654321"
        val message = "Hello from original sender"
        val command = ParsedCommand.ForwardedData(TEST_SENDER, originalSender, message)

        // When handleCommand is called
        invokeHandleCommand(command, TEST_SENDER, "SMSC FWD $originalSender $message")

        // Then MasterService should be started with correct extras
        val startedService = getNextStartedService()
        assertNotNull("Service should be started", startedService)
        assertEquals(MasterService.PROCESS_COMMAND, startedService.action)
        assertEquals(TEST_SENDER, startedService.getStringExtra(MasterService.EXTRA_SENDER))
        assertEquals("FWD", startedService.getStringExtra(MasterService.EXTRA_COMMAND_TYPE))
        assertEquals(
            originalSender,
            startedService.getStringExtra(MasterService.EXTRA_ORIGINAL_SENDER),
        )
        assertEquals(message, startedService.getStringExtra(MasterService.EXTRA_FORWARDED_CONTENT))
    }

    @Test
    fun `routes FWDE encrypted command to MasterService`() {
        // Given an FWDE command (encrypted forwarded data)
        val encryptedContent = "base64encryptedcontent=="
        val command = ParsedCommand.ForwardedDataEncrypted(TEST_SENDER, encryptedContent)

        // When handleCommand is called
        invokeHandleCommand(command, TEST_SENDER, "SMSC FWDE $encryptedContent")

        // Then MasterService should be started with correct extras
        val startedService = getNextStartedService()
        assertNotNull("Service should be started", startedService)
        assertEquals(MasterService.PROCESS_COMMAND, startedService.action)
        assertEquals(TEST_SENDER, startedService.getStringExtra(MasterService.EXTRA_SENDER))
        assertEquals("FWDE", startedService.getStringExtra(MasterService.EXTRA_COMMAND_TYPE))
        assertEquals(
            encryptedContent,
            startedService.getStringExtra(MasterService.EXTRA_FORWARDED_CONTENT),
        )
    }

    @Test
    fun `ignores unknown SMSC command`() {
        // Given an Unknown command (parsed but not recognized)
        val command = ParsedCommand.Unknown(TEST_SENDER, "SMSC UNKNOWN_COMMAND")

        // When handleCommand is called
        invokeHandleCommand(command, TEST_SENDER, "SMSC UNKNOWN_COMMAND")

        // Then no service should be started (early return in handleCommand)
        val shadowApplication = shadowOf(RuntimeEnvironment.getApplication())
        assertNull(
            "No service should be started for unknown command",
            shadowApplication.peekNextStartedService(),
        )
    }

    @Test
    fun `does not route non-SMSC messages`() {
        // Note: This is tested implicitly by the onReceive flow.
        // Non-SMSC messages don't get parsed as commands and go to handleRegularSms.
        // Since we're testing handleCommand directly via reflection, this test verifies
        // that CommandParser.parse returns null for non-SMSC messages.

        // Given a regular SMS (not SMSC command) - CommandParser returns null
        // The receiver would call handleRegularSms instead of handleCommand

        // Verify command parser returns null for non-SMSC messages
        val result = dev.notyouraverage.smscourier.commands.CommandParser.parse(
            TEST_SENDER,
            "Regular SMS message",
        )
        assertNull("Non-SMSC message should not be parsed as command", result)
    }

    // ==================== Multipart SMS Tests ====================
    // Note: The multipart SMS concatenation logic (groupBy sender, joinToString for parts)
    // cannot be easily unit tested because constructing valid SMS PDUs in tests is complex.
    // Robolectric's ShadowSmsManager has limited PDU support.
    //
    // The logic being tested is:
    //   val messagesBySender = messages.groupBy { it.originatingAddress ?: "Unknown" }
    //   val fullMessage = parts.joinToString("") { it.messageBody ?: "" }
    //
    // This is standard Kotlin standard library code that is well-tested.
    // Full end-to-end multipart SMS testing should be done in Phase 10/11 integration tests
    // with actual device or Android emulator SMS broadcasts.
    //
    // Tracking: Phase 10 integration test candidate

    // ==================== Legacy Command Tests ====================

    @Test
    fun `routes LEGACY_START command to MasterService`() {
        // Given a legacy start command
        val password = "legacypass"
        val command = ParsedCommand.LegacyStart(TEST_SENDER, password)

        // When handleCommand is called
        invokeHandleCommand(command, TEST_SENDER, "SMSC start $password")

        // Then MasterService should be started with LEGACY_START type
        val startedService = getNextStartedService()
        assertNotNull("Service should be started", startedService)
        assertEquals(MasterService.PROCESS_COMMAND, startedService.action)
        assertEquals(TEST_SENDER, startedService.getStringExtra(MasterService.EXTRA_SENDER))
        assertEquals(
            "LEGACY_START",
            startedService.getStringExtra(MasterService.EXTRA_COMMAND_TYPE),
        )
        assertEquals(password, startedService.getStringExtra(MasterService.EXTRA_PASSWORD))
    }

    @Test
    fun `routes LEGACY_STOP command to MasterService`() {
        // Given a legacy stop command
        val password = "legacypass"
        val command = ParsedCommand.LegacyStop(TEST_SENDER, password)

        // When handleCommand is called
        invokeHandleCommand(command, TEST_SENDER, "SMSC stop $password")

        // Then MasterService should be started with LEGACY_STOP type
        val startedService = getNextStartedService()
        assertNotNull("Service should be started", startedService)
        assertEquals(MasterService.PROCESS_COMMAND, startedService.action)
        assertEquals(TEST_SENDER, startedService.getStringExtra(MasterService.EXTRA_SENDER))
        assertEquals(
            "LEGACY_STOP",
            startedService.getStringExtra(MasterService.EXTRA_COMMAND_TYPE),
        )
        assertEquals(password, startedService.getStringExtra(MasterService.EXTRA_PASSWORD))
    }

    // ==================== Helper Methods ====================

    /**
     * Invokes the private handleCommand method via reflection.
     * This allows testing command routing without constructing SMS PDUs.
     */
    private fun invokeHandleCommand(command: ParsedCommand, sender: String, rawMessage: String) {
        val method = SmsReceiver::class.java.getDeclaredMethod(
            "handleCommand",
            Context::class.java,
            ParsedCommand::class.java,
            String::class.java,
            String::class.java,
        )
        method.isAccessible = true
        method.invoke(receiver, context, command, sender, rawMessage)
    }

    /**
     * Gets the next started service from the shadow application.
     */
    private fun getNextStartedService(): Intent {
        val shadowApplication = shadowOf(RuntimeEnvironment.getApplication())
        return shadowApplication.nextStartedService
    }
}

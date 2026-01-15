package dev.notyouraverage.smscourier.receivers

import android.content.Context
import android.content.Intent
import dev.notyouraverage.smscourier.services.foreground.MasterService
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ServiceNotificationReceiverTest {

    private lateinit var receiver: ServiceNotificationReceiver
    private lateinit var context: Context

    @Before
    fun setup() {
        receiver = ServiceNotificationReceiver()
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun `onReceive starts MasterService with RECREATE_NOTIFICATION action`() {
        // Given a notification dismissed intent
        val intent = Intent(ServiceNotificationReceiver.ACTION_NOTIFICATION_DISMISSED)

        // When receiver handles the intent
        receiver.onReceive(context, intent)

        // Then MasterService should be started with RECREATE_NOTIFICATION action
        val shadowApplication = shadowOf(RuntimeEnvironment.getApplication())
        val nextStartedService = shadowApplication.nextStartedService

        assertNotNull("Service should be started", nextStartedService)
        assertEquals(
            "Should start MasterService",
            MasterService::class.java.name,
            nextStartedService.component?.className,
        )
        assertEquals(
            "Should have RECREATE_NOTIFICATION action",
            MasterService.RECREATE_NOTIFICATION,
            nextStartedService.action,
        )
    }

    @Test
    fun `onReceive does nothing with null context`() {
        // Given a notification dismissed intent
        val intent = Intent(ServiceNotificationReceiver.ACTION_NOTIFICATION_DISMISSED)

        // When receiver handles the intent with null context
        // Then it should not crash
        try {
            receiver.onReceive(null, intent)
            // If we get here, the test passes (no crash)
            assertTrue(true)
        } catch (e: Exception) {
            fail("Should not throw exception with null context: ${e.message}")
        }
    }

    @Test
    fun `onReceive does nothing with null intent`() {
        // When receiver handles null intent
        // Then it should not crash
        try {
            receiver.onReceive(context, null)
            // If we get here, the test passes (no crash)
            assertTrue(true)
        } catch (e: Exception) {
            fail("Should not throw exception with null intent: ${e.message}")
        }
    }

    @Test
    fun `onReceive does nothing with wrong action`() {
        // Given an intent with wrong action
        val intent = Intent("SOME_OTHER_ACTION")

        // When receiver handles the intent
        receiver.onReceive(context, intent)

        // Then no service should be started
        val shadowApplication = shadowOf(RuntimeEnvironment.getApplication())
        val nextStartedService = shadowApplication.peekNextStartedService()

        assertNull("No service should be started for wrong action", nextStartedService)
    }

    @Test
    fun `receiver handles correct action constant`() {
        // Verify the action constant is correct
        assertEquals(
            "dev.notyouraverage.smscourier.NOTIFICATION_DISMISSED",
            ServiceNotificationReceiver.ACTION_NOTIFICATION_DISMISSED,
        )
    }

    @Test
    fun `onReceive with empty action does nothing`() {
        // Given an intent with empty action
        val intent = Intent("")

        // When receiver handles the intent
        receiver.onReceive(context, intent)

        // Then no service should be started
        val shadowApplication = shadowOf(RuntimeEnvironment.getApplication())
        val nextStartedService = shadowApplication.peekNextStartedService()

        assertNull("No service should be started for empty action", nextStartedService)
    }
}

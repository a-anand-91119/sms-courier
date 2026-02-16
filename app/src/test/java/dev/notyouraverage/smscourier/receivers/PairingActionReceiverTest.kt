package dev.notyouraverage.smscourier.receivers

import android.content.Context
import android.content.Intent
import dev.notyouraverage.smscourier.UATTest
import dev.notyouraverage.smscourier.notifications.PairingNotificationManager
import dev.notyouraverage.smscourier.services.foreground.MasterService
import org.junit.Assert.*
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PairingActionReceiverTest {

    @Category(UATTest::class)
    @Test
    fun `UAT NOTF-01 - approve action sends pairing approval to MasterService`() {
        val context: Context = RuntimeEnvironment.getApplication()
        val receiver = PairingActionReceiver()

        val intent = Intent().apply {
            action = PairingNotificationManager.ACTION_APPROVE
            putExtra(PairingNotificationManager.EXTRA_PHONE_NUMBER, "+5551234567")
        }

        receiver.onReceive(context, intent)

        // NOTF-01: Approve action should send to MasterService for processing
        // (like reject does), not just launch MainActivity
        // Currently: starts MainActivity with navigation intent
        // After fix: starts MasterService with PAIRING_APPROVE_REQUESTED action

        val shadowApplication = shadowOf(RuntimeEnvironment.getApplication())
        val startedService = shadowApplication.nextStartedService

        // Verify a service was started (not an activity)
        assertNotNull("Approve action should start MasterService, not just open activity", startedService)
        assertEquals(MasterService.PAIRING_APPROVE_REQUESTED, startedService.action)
        assertEquals("+5551234567", startedService.getStringExtra(MasterService.EXTRA_PHONE_NUMBER))
    }
}

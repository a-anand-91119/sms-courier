package dev.notyouraverage.smscourier.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.notyouraverage.smscourier.activities.MainActivity
import dev.notyouraverage.smscourier.notifications.PairingNotificationManager
import dev.notyouraverage.smscourier.services.foreground.MasterService

class PairingActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SMSC:PairingReceiver"

        // Intent extras for deep linking
        const val EXTRA_NAVIGATE_TO = "navigate_to"
        const val EXTRA_HIGHLIGHT_PHONE = "highlight_phone"
        const val NAVIGATE_TO_PAIRING_REQUESTS = "pairing_requests"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val phoneNumber = intent.getStringExtra(PairingNotificationManager.EXTRA_PHONE_NUMBER)
        if (phoneNumber.isNullOrBlank()) {
            Log.e(TAG, "PairingActionReceiver: Missing phone number")
            return
        }

        when (intent.action) {
            PairingNotificationManager.ACTION_APPROVE -> {
                Log.i(TAG, "PairingActionReceiver: Approve pairing for $phoneNumber")
                // Cancel the notification
                PairingNotificationManager(context).cancelPairingRequestNotification(phoneNumber)
                // Launch MainActivity and navigate to PairingRequestsScreen
                // The actual password creation will happen in the UI
                Intent(context, MainActivity::class.java).also {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    it.putExtra(EXTRA_NAVIGATE_TO, NAVIGATE_TO_PAIRING_REQUESTS)
                    it.putExtra(EXTRA_HIGHLIGHT_PHONE, phoneNumber)
                    context.startActivity(it)
                }
            }

            PairingNotificationManager.ACTION_REJECT -> {
                Log.i(TAG, "PairingActionReceiver: Reject pairing for $phoneNumber")
                // Cancel the notification
                PairingNotificationManager(context).cancelPairingRequestNotification(phoneNumber)
                // Send to MasterService to handle rejection
                Intent(context, MasterService::class.java).also {
                    it.action = MasterService.PAIRING_REJECT_REQUESTED
                    it.putExtra(MasterService.EXTRA_PHONE_NUMBER, phoneNumber)
                    context.startService(it)
                }
            }

            else -> {
                Log.w(TAG, "PairingActionReceiver: Unknown action ${intent.action}")
            }
        }
    }
}

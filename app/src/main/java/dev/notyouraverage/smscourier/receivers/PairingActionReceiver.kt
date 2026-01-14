package dev.notyouraverage.smscourier.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.notyouraverage.smscourier.notifications.PairingNotificationManager
import dev.notyouraverage.smscourier.services.foreground.MasterService

class PairingActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SMSC:PairingReceiver"
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
                // Send to MasterService to handle approval
                // The actual password creation will happen in the UI
                Intent(context, MasterService::class.java).also {
                    it.action = MasterService.PAIRING_APPROVE_REQUESTED
                    it.putExtra(MasterService.EXTRA_PHONE_NUMBER, phoneNumber)
                    context.startService(it)
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

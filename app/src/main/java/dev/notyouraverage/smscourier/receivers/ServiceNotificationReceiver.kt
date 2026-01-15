package dev.notyouraverage.smscourier.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.notyouraverage.smscourier.services.foreground.MasterService

/**
 * Handles notification dismissal for the foreground service.
 * On Android 14+, even ongoing notifications can be dismissed by users.
 * This receiver detects dismissal and recreates the notification immediately.
 */
class ServiceNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_NOTIFICATION_DISMISSED = "dev.notyouraverage.smscourier.NOTIFICATION_DISMISSED"
        private const val TAG = "SMSC:ServiceNotificationReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == ACTION_NOTIFICATION_DISMISSED) {
            Log.i(TAG, "Foreground notification dismissed, recreating...")

            // Tell the service to recreate the notification
            Intent(context, MasterService::class.java).apply {
                action = MasterService.RECREATE_NOTIFICATION
                context.startService(this)
            }
        }
    }
}

package dev.notyouraverage.smscourier.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.notyouraverage.smscourier.R
import dev.notyouraverage.smscourier.receivers.PairingActionReceiver

class PairingNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_PAIRING = "pairing_requests"
        const val CHANNEL_FORWARDING = "forwarding_status"
        const val CHANNEL_MESSAGES = "forwarded_messages"

        const val ACTION_APPROVE = "dev.notyouraverage.smscourier.ACTION_APPROVE_PAIRING"
        const val ACTION_REJECT = "dev.notyouraverage.smscourier.ACTION_REJECT_PAIRING"

        const val EXTRA_PHONE_NUMBER = "phone_number"

        private const val NOTIFICATION_ID_PAIRING_BASE = 1001
        private const val NOTIFICATION_ID_FORWARDING = 2001
        private const val NOTIFICATION_ID_MESSAGE_BASE = 3001
        private const val NOTIFICATION_ID_SESSION_STOPPED_BASE = 4001
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    private fun notifySafely(notificationId: Int, notification: Notification) {
        if (hasNotificationPermission()) {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }

    fun createNotificationChannels() {
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        // Pairing requests channel
        val pairingChannel = NotificationChannel(
            CHANNEL_PAIRING,
            "Pairing Requests",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notifications for incoming pairing requests"
        }
        notificationManager.createNotificationChannel(pairingChannel)

        // Forwarding status channel
        val forwardingChannel = NotificationChannel(
            CHANNEL_FORWARDING,
            "Forwarding Status",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Notifications about SMS forwarding status"
        }
        notificationManager.createNotificationChannel(forwardingChannel)

        // Forwarded messages channel
        val messagesChannel = NotificationChannel(
            CHANNEL_MESSAGES,
            "Forwarded Messages",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notifications for forwarded SMS messages"
        }
        notificationManager.createNotificationChannel(messagesChannel)
    }

    fun showPairingRequestNotification(phoneNumber: String) {
        val approveIntent = Intent(context, PairingActionReceiver::class.java).apply {
            action = ACTION_APPROVE
            putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
        }
        val approvePendingIntent = PendingIntent.getBroadcast(
            context,
            phoneNumber.hashCode(),
            approveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val rejectIntent = Intent(context, PairingActionReceiver::class.java).apply {
            action = ACTION_REJECT
            putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
        }
        val rejectPendingIntent = PendingIntent.getBroadcast(
            context,
            phoneNumber.hashCode() + 1,
            rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_PAIRING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Pairing Request")
            .setContentText("$phoneNumber wants to receive your SMS")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$phoneNumber wants to receive your SMS. Approve to allow this device to request SMS forwarding from you."),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                android.R.drawable.ic_menu_add,
                "Approve",
                approvePendingIntent,
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Reject",
                rejectPendingIntent,
            )
            .setAutoCancel(true)
            .build()

        notifySafely(getNotificationIdForPhone(phoneNumber), notification)
    }

    fun cancelPairingRequestNotification(phoneNumber: String) {
        NotificationManagerCompat.from(context)
            .cancel(getNotificationIdForPhone(phoneNumber))
    }

    fun showForwardingActiveNotification(targetPhoneNumber: String, durationMinutes: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_FORWARDING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("SMS Forwarding Active")
            .setContentText("Forwarding SMS to $targetPhoneNumber for $durationMinutes minutes")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        notifySafely(NOTIFICATION_ID_FORWARDING, notification)
    }

    fun cancelForwardingNotification() {
        NotificationManagerCompat.from(context)
            .cancel(NOTIFICATION_ID_FORWARDING)
    }

    fun showForwardedMessageNotification(originalSender: String, content: String, forwarderPhone: String) {
        val notificationId = NOTIFICATION_ID_MESSAGE_BASE + System.currentTimeMillis().toInt().and(0xFFFF)

        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("SMS from $originalSender")
            .setContentText(content)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(content)
                    .setSummaryText("Forwarded via $forwarderPhone"),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notifySafely(notificationId, notification)
    }

    fun showPairingResponseNotification(phoneNumber: String, approved: Boolean) {
        val title = if (approved) "Pairing Approved" else "Pairing Rejected"
        val message = if (approved) {
            "$phoneNumber approved your pairing request. You can now request SMS forwarding."
        } else {
            "$phoneNumber rejected your pairing request."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_PAIRING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notifySafely(getNotificationIdForPhone(phoneNumber), notification)
    }

    fun showSessionStoppedNotification(phoneNumber: String) {
        val intent = Intent(context, dev.notyouraverage.smscourier.activities.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        // Offset request code to avoid collision with pairing notifications
        val pendingIntent = PendingIntent.getActivity(
            context,
            phoneNumber.hashCode() + 100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_FORWARDING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Forwarding Stopped")
            .setContentText("$phoneNumber ended the forwarding session")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notifySafely(NOTIFICATION_ID_SESSION_STOPPED_BASE + phoneNumber.hashCode().and(0xFFFF), notification)
    }

    private fun getNotificationIdForPhone(phoneNumber: String): Int {
        return NOTIFICATION_ID_PAIRING_BASE + phoneNumber.hashCode().and(0xFFFF)
    }
}

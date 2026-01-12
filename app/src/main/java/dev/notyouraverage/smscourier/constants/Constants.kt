package dev.notyouraverage.smscourier.constants

import android.Manifest

object Constants {
    val SMS_PERMISSIONS = listOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,
    )
    val PHONE_PERMISSIONS =
        listOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.CALL_PHONE)
    val SERVICE_STATE_PERMISSIONS =
        listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_PHONE_STATE)

    const val NOTIFICATION_CHANNEL_GENERAL = "ListeningNotificationChannel"
    const val CODE_FOREGROUND_SERVICE = 1
}

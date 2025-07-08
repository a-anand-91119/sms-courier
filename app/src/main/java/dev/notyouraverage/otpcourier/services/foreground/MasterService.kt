package dev.notyouraverage.otpcourier.services.foreground

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Telephony
import android.telephony.ims.ImsRcsManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import dev.notyouraverage.otpcourier.R
import dev.notyouraverage.otpcourier.constants.Constants
import dev.notyouraverage.otpcourier.constants.Constants.CODE_FOREGROUND_SERVICE
import dev.notyouraverage.otpcourier.constants.Constants.NOTIFICATION_CHANNEL_GENERAL
import dev.notyouraverage.otpcourier.managers.DataStoreManager
import dev.notyouraverage.otpcourier.managers.smsCourierPreferenceDataStore
import dev.notyouraverage.otpcourier.models.SmsMessageData
import dev.notyouraverage.otpcourier.receivers.SmsReceiver
import dev.notyouraverage.otpcourier.services.background.SmsService
import dev.notyouraverage.otpcourier.utils.loadSavedPreferences


class MasterService : Service() {

    private lateinit var smsReceiver: SmsReceiver
    private var backgroundServiceRunning = false

    private val stopDelay: Long = 5 * 60 * 1000
    private val handler: Handler = Handler(Looper.getMainLooper())
    private var secretPassword = ""
    private var whiteListedPhoneNumber = ""

    companion object {
        private val TAG by lazy { MasterService::class.java.simpleName }
        const val SMS_DATA = "SMS_DATA"
        const val START_SELF = "START_SELF"
        const val STOP_SELF = "STOP_SELF"
        const val START_BACKGROUND = "START_BACKGROUND"
        const val STOP_BACKGROUND = "STOP_BACKGROUND"
        const val SEND_DATA = "SEND_DATA"
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "MasterService::onCreate")
//        if (this::smsReceiver.isInitialized) return
    }

    override fun onDestroy() {
        Toast.makeText(this, "Killing Foreground Service", Toast.LENGTH_SHORT).show()
        Log.i(TAG, "MasterService::onDestroy")
        if (this::smsReceiver != null && this::smsReceiver.isInitialized)
            unregisterReceiver(smsReceiver)
        stopBackgroundService()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "MasterService::onStartCommand")
        when (intent?.action) {
            START_SELF -> startSelf(intent.extras)
            STOP_SELF -> stopSelf()
            START_BACKGROUND -> startBackgroundService()
            STOP_BACKGROUND -> stopBackgroundService()
            SEND_DATA -> sendToBackgroundService(
                intent.getParcelableExtra(
                    SMS_DATA,
                    SmsMessageData::class.java
                )
            )
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun sendToBackgroundService(smsMessageData: SmsMessageData?) {
        if (!backgroundServiceRunning || smsMessageData == null) return
        Log.i(TAG, "Sending SMS to background service")
        Intent(this, SmsService::class.java).also {
            it.action = SmsService.SEND_SMS
            it.putExtra(SmsService.SMS_DATA, smsMessageData)
            startService(it)
        }
    }

    private fun stopBackgroundService() {
        if (!backgroundServiceRunning) return
        Log.i(TAG, "Stopping background service")
        Intent(this, SmsService::class.java).also {
            baseContext.stopService(it)
            backgroundServiceRunning = false
            handler.removeCallbacksAndMessages(null)
        }
    }

    private fun startBackgroundService() {
        if (backgroundServiceRunning) return
        Log.i(TAG, "Starting background service")
        Intent(this, SmsService::class.java).also {
            it.putExtra(Constants.TARGET_SMS_CONTACT_NUMBER, whiteListedPhoneNumber)
            it.action = SmsService.START_SERVICE

            baseContext.startService(it)
            backgroundServiceRunning = true

            handler.postDelayed({
                stopBackgroundService()
            }, stopDelay)
        }
    }

    private fun startSelf(extras: Bundle?) {
        secretPassword = extras?.getString(Constants.SECRET_PASSWORD) ?: ""
        whiteListedPhoneNumber = extras?.getString(Constants.WHITE_LISTED_CONTACT_NUMBER) ?: ""
        Log.i(TAG, "Setting secret password as $secretPassword")
        Log.i(TAG, "Setting whitelisted number as $whiteListedPhoneNumber")
        if (!this::smsReceiver.isInitialized) {
            smsReceiver = SmsReceiver(secretPassword, whiteListedPhoneNumber)
            baseContext.registerReceiver(
                smsReceiver,
                IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
            )
        }

        Log.i(TAG, "MasterService::startingForegroundService")
        Toast.makeText(this, "Starting Foreground Service", Toast.LENGTH_SHORT).show()
        with(NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_GENERAL)) {
            setTicker(null)
            setContentTitle("OTP Courier")
            setContentText("OTP Courier is running")
            setAutoCancel(false)
            setOngoing(true)
            setWhen(System.currentTimeMillis())
            setSmallIcon(R.drawable.ic_launcher_foreground)
            priority = NotificationManager.IMPORTANCE_HIGH
            startForeground(CODE_FOREGROUND_SERVICE, build())
        }
    }

}
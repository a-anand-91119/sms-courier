package dev.notyouraverage.otpcourier.services.background

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dev.notyouraverage.otpcourier.constants.Constants
import dev.notyouraverage.otpcourier.models.SmsMessageData
import dev.notyouraverage.otpcourier.services.foreground.MasterService
import dev.notyouraverage.otpcourier.services.foreground.MasterService.Companion
import dev.notyouraverage.otpcourier.workers.SmsWorker

class SmsService : Service() {

    private lateinit var workManager: WorkManager
    private var targetPhoneNumber = ""

    companion object {
        private val TAG by lazy { SmsService::class.java.simpleName }
        const val SMS_DATA = "SMS_DATA"
        const val SEND_SMS = "SEND_SMS"
        const val START_SERVICE = "START_SERVICE"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "SmsService::onStartCommand")

        when (intent?.action) {
            SEND_SMS -> sendSms(intent)
            START_SERVICE -> initializeService(intent)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun initializeService(intent: Intent) {
        targetPhoneNumber = intent.extras?.getString(Constants.TARGET_SMS_CONTACT_NUMBER) ?: ""
        Log.i(TAG, "Initialized SMS Service with target phone number as: $targetPhoneNumber")
    }

    private fun sendSms(intent: Intent) {
        val smsMessageData = intent.getParcelableExtra(
            SMS_DATA,
            SmsMessageData::class.java
        )
        val workRequest = OneTimeWorkRequestBuilder<SmsWorker>().setInputData(
            workDataOf(
                SmsWorker.PHONE_NUMBER to targetPhoneNumber,
                SmsWorker.SMS_CONTENT to smsMessageData?.rawMessage
            )
        ).build()
        Log.i(TAG, "Created a work request to forward sms...")
        workManager.enqueue(workRequest)
    }

    override fun onCreate() {
        super.onCreate()
        workManager = WorkManager.getInstance(baseContext)
        Log.i(TAG, "SmsService::onCreate")
    }

    override fun onDestroy() {
        Log.i(TAG, "SmsService::onDestroy")
        super.onDestroy()
    }

}
package dev.notyouraverage.otpcourier.services.background

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dev.notyouraverage.otpcourier.models.SmsMessageData
import dev.notyouraverage.otpcourier.workers.SmsWorker

class SmsService : Service() {

    private lateinit var workManager: WorkManager

    companion object {
        private const val TAG = "OTPC:SmsService"
        const val SMS_DATA = "SMS_DATA"
        const val SEND_SMS = "SEND_SMS"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "SmsService::onStartCommand")
        if (intent?.action != SEND_SMS) return super.onStartCommand(intent, flags, startId)

        val smsMessageData = intent.getParcelableExtra(
            SMS_DATA,
            SmsMessageData::class.java,
        )
        val workRequest = OneTimeWorkRequestBuilder<SmsWorker>().setInputData(
            workDataOf(
                SmsWorker.PHONE_NUMBER to smsMessageData?.sender,
                SmsWorker.SMS_CONTENT to smsMessageData?.rawMessage,
            ),
        ).build()
        workManager.enqueue(workRequest)
        return super.onStartCommand(intent, flags, startId)
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

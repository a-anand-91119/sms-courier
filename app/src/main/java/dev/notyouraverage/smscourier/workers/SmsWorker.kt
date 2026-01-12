package dev.notyouraverage.smscourier.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SmsWorker(
    private val context: Context,
    private val params: WorkerParameters,
) : CoroutineWorker(context, params) {

    companion object {
        const val PHONE_NUMBER = "PHONE_NUMBER"
        const val SMS_CONTENT = "SMS_CONTENT"
        const val SMS_SENT = "SMS_SENT"
    }

    private fun sendSms(phoneNumber: String, message: String) {
        val sentPI: PendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(SMS_SENT),
            PendingIntent.FLAG_IMMUTABLE,
        )
        context.getSystemService(SmsManager::class.java)
            .sendTextMessage(phoneNumber, null, message, sentPI, null)
    }

    override suspend fun doWork(): Result {
        val phoneNumber = params.inputData.getString(PHONE_NUMBER)
        val message = params.inputData.getString(SMS_CONTENT)
        if (message == null || phoneNumber == null) return Result.failure()
        sendSms(phoneNumber, message)
        return Result.success()
    }
}

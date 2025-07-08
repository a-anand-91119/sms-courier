package dev.notyouraverage.otpcourier.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Toast
import dev.notyouraverage.otpcourier.enums.SmsCommand
import dev.notyouraverage.otpcourier.models.SmsMessageData
import dev.notyouraverage.otpcourier.services.foreground.MasterService
import dev.notyouraverage.otpcourier.services.foreground.MasterService.Companion

class SmsReceiver(
    private val secretPassword: String,
    whiteListedContact: String,
) : BroadcastReceiver() {

    companion object {
        private val TAG by lazy { SmsReceiver::class.java.simpleName }
    }

    private val targetContacts = listOf(whiteListedContact)
    private val commandPattern = Regex("OTPC\\s+(start|stop)\\s+(\\S+)")

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i(TAG, "Using whitelisted numbers as $targetContacts")
        Log.i(TAG, "Received Action ${intent?.action}")

        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val extractMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        Log.i(
            TAG,
            "Received sms from ${processOriginatingAddress(extractMessages[0].originatingAddress)}"
        )
        Toast.makeText(
            context,
            "Received sms from ${processOriginatingAddress(extractMessages[0].originatingAddress)}",
            Toast.LENGTH_SHORT
        ).show()
        val commandMessage =
            extractMessages.filter { message ->
                targetContacts.contains(
                    processOriginatingAddress(
                        message.originatingAddress
                    )
                )
            }
                .firstOrNull { message -> matchesCommandPattern(message.messageBody.trim()) }

        if (commandMessage != null) {
            val smsCommand =
                extractCommandAndPassword(
                    commandMessage.originatingAddress,
                    commandMessage.messageBody
                )
            if (smsCommand?.secretPassword != secretPassword) return

            when (smsCommand.command?.uppercase()) {
                SmsCommand.START.toString() -> {
                    sendToMasterService(
                        context,
                        MasterService.START_BACKGROUND,
                        null
                    )
                }

                SmsCommand.STOP.toString() -> {
                    sendToMasterService(
                        context,
                        MasterService.STOP_BACKGROUND,
                        null
                    )
                }
            }
            Log.v(TAG, smsCommand.toString())
        } else {
            extractMessages.map(this::getSmsMessageData)
                .forEach { smsMessageData ->
                    sendToMasterService(
                        context,
                        MasterService.SEND_DATA,
                        smsMessageData
                    )
                }
        }
    }

    private fun processOriginatingAddress(originatingAddress: String?): String? {
        return originatingAddress?.trim()?.replace(" ", "")?.takeLast(10)
    }

    private fun getSmsMessageData(smsMessage: SmsMessage?): SmsMessageData {
        return SmsMessageData(
            sender = smsMessage?.originatingAddress ?: "Unknown",
            command = SmsCommand.SEND_TO_WORKER.toString(),
            rawMessage = smsMessage?.messageBody ?: "",
            secretPassword = "null"
        )
    }

    private fun sendToMasterService(
        context: Context?,
        action: String,
        smsMessageData: SmsMessageData?
    ) {
        Log.i(TAG, smsMessageData.toString())
        Intent(context, MasterService::class.java).also {
            it.setAction(action)
            it.putExtra(MasterService.SMS_DATA, smsMessageData)
            context?.startService(it)
        }
    }

    private fun extractCommandAndPassword(
        originatingAddress: String?,
        messageBody: String?
    ): SmsMessageData? {
        val matchResult = messageBody?.trim()?.let { commandPattern.find(it) }

        return if (matchResult != null && matchResult.groupValues.size == 3) {
            SmsMessageData(
                sender = processOriginatingAddress(originatingAddress) ?: "Unknown",
                command = matchResult.groupValues[1],
                rawMessage = messageBody,
                secretPassword = matchResult.groupValues[2]
            )
        } else null

    }

    private fun matchesCommandPattern(message: String): Boolean {
        return message.matches(commandPattern)
    }
}

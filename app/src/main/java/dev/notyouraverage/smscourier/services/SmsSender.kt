package dev.notyouraverage.smscourier.services

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import dev.notyouraverage.smscourier.data.entities.DeviceRole

class SmsSender(private val context: Context) {

    companion object {
        private const val TAG = "SMSC:SmsSender"
        const val SMS_SENT_ACTION = "dev.notyouraverage.smscourier.SMS_SENT"
        const val EXTRA_PHONE_NUMBER = "phone_number"
    }

    fun send(phoneNumber: String, message: String) {
        try {
            val smsManager = context.getSystemService(SmsManager::class.java)

            if (message.length <= 160) {
                val sentIntent = Intent(SMS_SENT_ACTION).apply {
                    putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
                }
                val sentPI = PendingIntent.getBroadcast(
                    context,
                    phoneNumber.hashCode(),
                    sentIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                smsManager.sendTextMessage(phoneNumber, null, message, sentPI, null)
                Log.i(TAG, "SMS queued (single) to $phoneNumber: ${message.take(50)}...")
            } else {
                val parts = smsManager.divideMessage(message)
                Log.i(TAG, "SMS is multipart (${parts.size} parts) to $phoneNumber")

                // Create sent intents for each part
                val sentIntents = ArrayList<PendingIntent>()
                for (i in parts.indices) {
                    val sentIntent = Intent(SMS_SENT_ACTION).apply {
                        putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
                        putExtra("part", i)
                    }
                    val sentPI = PendingIntent.getBroadcast(
                        context,
                        phoneNumber.hashCode() + i,
                        sentIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                    sentIntents.add(sentPI)
                }

                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, null)
                Log.i(TAG, "SMS queued (multipart) to $phoneNumber: ${message.take(50)}...")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS to $phoneNumber: ${e.message}", e)
        }
    }

    // Command-specific SMS methods
    fun sendPairRequest(targetPhoneNumber: String) {
        send(targetPhoneNumber, "SMSC PAIR_REQUEST")
    }

    fun sendPairApproved(sourcePhoneNumber: String) {
        send(sourcePhoneNumber, "SMSC PAIR_APPROVED")
    }

    fun sendPairRejected(sourcePhoneNumber: String) {
        send(sourcePhoneNumber, "SMSC PAIR_REJECTED")
    }

    fun sendUnpair(phoneNumber: String, roleToDelete: DeviceRole) {
        send(phoneNumber, "SMSC UNPAIR ${roleToDelete.name}")
    }

    // Authentication commands (challenge-response)
    fun sendAuthRequest(targetPhoneNumber: String) {
        send(targetPhoneNumber, "SMSC AUTH_REQUEST")
    }

    fun sendAuthChallenge(sourcePhoneNumber: String, nonce: String) {
        send(sourcePhoneNumber, "SMSC AUTH_CHALLENGE $nonce")
    }

    fun sendStartForwardWithResponse(targetPhoneNumber: String, response: String, durationMinutes: Int? = null) {
        val message = if (durationMinutes != null) {
            "SMSC START_FORWARD $response $durationMinutes"
        } else {
            "SMSC START_FORWARD $response"
        }
        send(targetPhoneNumber, message)
    }

    // Legacy: send START_FORWARD with plain password (backward compatibility)
    fun sendStartForward(targetPhoneNumber: String, password: String, durationMinutes: Int? = null) {
        val message = if (durationMinutes != null) {
            "SMSC START_FORWARD $password $durationMinutes"
        } else {
            "SMSC START_FORWARD $password"
        }
        send(targetPhoneNumber, message)
    }

    fun sendStopForward(targetPhoneNumber: String) {
        send(targetPhoneNumber, "SMSC STOP_FORWARD")
    }

    fun sendForwardedSms(sourcePhoneNumber: String, originalSender: String, message: String, encryptionKey: String? = null) {
        val forwardMessage = if (encryptionKey != null) {
            // Encrypt the content (sender + message together)
            val plainContent = "$originalSender|$message"
            val encrypted = dev.notyouraverage.smscourier.security.MessageEncryption.encrypt(plainContent, encryptionKey)
            if (encrypted != null) {
                Log.i(TAG, "Encrypted forwarded message successfully")
                "SMSC FWDE $encrypted" // FWDE = Forward Encrypted
            } else {
                Log.w(TAG, "Encryption failed, sending unencrypted")
                "SMSC FWD <$originalSender> $message"
            }
        } else {
            "SMSC FWD <$originalSender> $message"
        }
        send(sourcePhoneNumber, forwardMessage)
    }
}

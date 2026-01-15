package dev.notyouraverage.smscourier.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import dev.notyouraverage.smscourier.commands.CommandParser
import dev.notyouraverage.smscourier.commands.ParsedCommand
import dev.notyouraverage.smscourier.data.SmsCourierDatabase
import dev.notyouraverage.smscourier.data.entities.DeviceRole
import dev.notyouraverage.smscourier.data.entities.PairingStatus
import dev.notyouraverage.smscourier.enums.SmsCommand
import dev.notyouraverage.smscourier.models.SmsMessageData
import dev.notyouraverage.smscourier.services.foreground.MasterService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * SmsReceiver processes ALL incoming SMS messages.
 *
 * - Checks for SMSC commands from any sender
 * - Routes commands to MasterService for processing via SmsCommandHandler
 * - Forwards regular SMS only if there's an active forwarding session for that sender
 */
class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SMSC:SmsReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return

        // Group message parts by sender (for multipart SMS)
        val messagesBySender = messages.groupBy { it.originatingAddress ?: "Unknown" }

        messagesBySender.forEach { (sender, parts) ->
            // Combine multipart SMS
            val fullMessage = parts.joinToString("") { it.messageBody ?: "" }

            Log.d(TAG, "Received SMS from $sender: ${fullMessage.take(50)}...")

            // Try to parse as SMSC command
            val command = CommandParser.parse(sender, fullMessage)

            if (command != null) {
                Log.i(TAG, "Parsed SMSC command: ${command::class.simpleName} from $sender")
                handleCommand(context, command, sender, fullMessage)
            } else {
                // Not a command - check if we should forward this regular SMS
                handleRegularSms(context, sender, fullMessage)
            }
        }
    }

    private fun handleCommand(context: Context, command: ParsedCommand, sender: String, rawMessage: String) {
        val intent = Intent(context, MasterService::class.java).apply {
            action = MasterService.PROCESS_COMMAND
            putExtra(MasterService.EXTRA_SENDER, sender)
            putExtra(MasterService.EXTRA_RAW_MESSAGE, rawMessage)

            // Include command-specific data
            when (command) {
                is ParsedCommand.PairRequest -> {
                    putExtra(MasterService.EXTRA_COMMAND_TYPE, "PAIR_REQUEST")
                }
                is ParsedCommand.PairApproved -> {
                    putExtra(MasterService.EXTRA_COMMAND_TYPE, "PAIR_APPROVED")
                }
                is ParsedCommand.PairRejected -> {
                    putExtra(MasterService.EXTRA_COMMAND_TYPE, "PAIR_REJECTED")
                }
                is ParsedCommand.Unpair -> {
                    putExtra(MasterService.EXTRA_COMMAND_TYPE, "UNPAIR")
                    command.roleToDelete?.let { role ->
                        putExtra(MasterService.EXTRA_UNPAIR_ROLE, role.name)
                    }
                }
                is ParsedCommand.AuthRequest -> {
                    putExtra(MasterService.EXTRA_COMMAND_TYPE, "AUTH_REQUEST")
                }
                is ParsedCommand.AuthChallenge -> {
                    putExtra(MasterService.EXTRA_COMMAND_TYPE, "AUTH_CHALLENGE")
                    putExtra(MasterService.EXTRA_NONCE, command.nonce)
                }
                is ParsedCommand.StartForward -> {
                    putExtra(MasterService.EXTRA_COMMAND_TYPE, "START_FORWARD")
                    putExtra(MasterService.EXTRA_PASSWORD, command.password)
                    command.durationMinutes?.let {
                        putExtra(MasterService.EXTRA_DURATION, it)
                    }
                }
                is ParsedCommand.StopForward -> {
                    putExtra(MasterService.EXTRA_COMMAND_TYPE, "STOP_FORWARD")
                }
                is ParsedCommand.ForwardedData -> {
                    putExtra(MasterService.EXTRA_COMMAND_TYPE, "FWD")
                    putExtra(MasterService.EXTRA_ORIGINAL_SENDER, command.originalSender)
                    putExtra(MasterService.EXTRA_FORWARDED_CONTENT, command.message)
                }
                is ParsedCommand.ForwardedDataEncrypted -> {
                    putExtra(MasterService.EXTRA_COMMAND_TYPE, "FWDE")
                    putExtra(MasterService.EXTRA_FORWARDED_CONTENT, command.encryptedContent)
                }
                is ParsedCommand.LegacyStart -> {
                    putExtra(MasterService.EXTRA_COMMAND_TYPE, "LEGACY_START")
                    putExtra(MasterService.EXTRA_PASSWORD, command.password)
                }
                is ParsedCommand.LegacyStop -> {
                    putExtra(MasterService.EXTRA_COMMAND_TYPE, "LEGACY_STOP")
                    putExtra(MasterService.EXTRA_PASSWORD, command.password)
                }
                is ParsedCommand.Unknown -> {
                    // Don't send unknown commands to the service
                    Log.w(TAG, "Unknown command from $sender, ignoring")
                    return
                }
            }
        }
        context.startService(intent)
    }

    private fun handleRegularSms(context: Context, sender: String, message: String) {
        // Check if there's an active forwarding session for this sender
        // This runs async but we don't block the receiver
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = SmsCourierDatabase.getDatabase(context)
                val sessionDao = database.forwardingSessionDao()
                val deviceDao = database.pairedDeviceDao()

                // Check for active sessions where we are the TARGET (sending SMS to this device)
                val activeSessions = sessionDao.getActiveSessionsList()

                if (activeSessions.isNotEmpty()) {
                    // We have active forwarding sessions - forward this SMS to all active sources
                    activeSessions.forEach { session ->
                        // Get the source device that requested forwarding (we are TARGET, they are SOURCE)
                        val sourceDevice = deviceDao.getDeviceByPhoneNumberAndRole(
                            session.devicePhoneNumber,
                            DeviceRole.SOURCE
                        )

                        if (sourceDevice != null && sourceDevice.status == PairingStatus.APPROVED) {
                            Log.i(TAG, "Forwarding SMS from $sender to ${session.devicePhoneNumber} (encrypted=${session.encryptionKey != null})")

                            // Create forwarded message with encryption key
                            val smsData = SmsMessageData(
                                sender = sender,
                                command = SmsCommand.FORWARD_SMS.toString(),
                                rawMessage = message,
                                secretPassword = "",
                                targetPhoneNumber = session.devicePhoneNumber,
                                encryptionKey = session.encryptionKey,
                            )

                            Intent(context, MasterService::class.java).also {
                                it.action = MasterService.FORWARD_SMS
                                it.putExtra(MasterService.SMS_DATA, smsData)
                                context.startService(it)
                            }

                            // Increment forwarded message count
                            sessionDao.incrementMessagesForwarded(session.id)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking forwarding sessions", e)
            }
        }
    }
}

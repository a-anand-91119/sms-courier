package dev.notyouraverage.otpcourier.handlers

import android.util.Log
import dev.notyouraverage.otpcourier.commands.ParsedCommand
import dev.notyouraverage.otpcourier.data.entities.DeviceRole
import dev.notyouraverage.otpcourier.data.entities.PairedDevice
import dev.notyouraverage.otpcourier.data.entities.PairingStatus
import dev.notyouraverage.otpcourier.notifications.PairingNotificationManager
import dev.notyouraverage.otpcourier.repository.ForwardingSessionRepository
import dev.notyouraverage.otpcourier.repository.PairedDeviceRepository
import dev.notyouraverage.otpcourier.security.MessageEncryption
import dev.notyouraverage.otpcourier.security.SecurityManager
import dev.notyouraverage.otpcourier.services.SmsSender

class SmsCommandHandler(
    private val deviceRepository: PairedDeviceRepository,
    private val sessionRepository: ForwardingSessionRepository,
    private val smsSender: SmsSender,
    private val notificationManager: PairingNotificationManager,
    private val securityManager: SecurityManager,
    private val onForwardingStateChanged: (ForwardingState) -> Unit,
) {
    companion object {
        private const val TAG = "OTPC:CommandHandler"
        private const val DEFAULT_FORWARD_DURATION_MINUTES = 30
    }

    sealed class ForwardingState {
        data class Started(val phoneNumber: String, val durationMinutes: Int, val sessionId: Long) :
            ForwardingState()

        data class Stopped(val phoneNumber: String, val reason: String) : ForwardingState()
    }

    suspend fun handleCommand(command: ParsedCommand) {
        Log.d(TAG, "Handling command: $command")

        when (command) {
            is ParsedCommand.PairRequest -> handlePairRequest(command.senderPhoneNumber)
            is ParsedCommand.PairApproved -> handlePairApproved(command.senderPhoneNumber)
            is ParsedCommand.PairRejected -> handlePairRejected(command.senderPhoneNumber)
            is ParsedCommand.Unpair -> handleUnpair(command.senderPhoneNumber)
            is ParsedCommand.AuthRequest -> handleAuthRequest(command.senderPhoneNumber)
            is ParsedCommand.AuthChallenge -> {
                // AUTH_CHALLENGE is handled directly by MasterService on SOURCE side
                // This shouldn't normally reach here, but log it
                Log.d(TAG, "AuthChallenge received in handleCommand - should be handled by MasterService")
            }
            is ParsedCommand.StartForward -> handleStartForward(
                command.senderPhoneNumber,
                command.password,
                command.durationMinutes,
            )
            is ParsedCommand.StopForward -> handleStopForward(command.senderPhoneNumber)
            is ParsedCommand.ForwardedData -> handleForwardedData(
                command.senderPhoneNumber,
                command.originalSender,
                command.message,
            )
            is ParsedCommand.ForwardedDataEncrypted -> handleForwardedDataEncrypted(
                command.senderPhoneNumber,
                command.encryptedContent,
            )
            is ParsedCommand.LegacyStart -> handleLegacyStart(
                command.senderPhoneNumber,
                command.password,
            )
            is ParsedCommand.LegacyStop -> handleLegacyStop(
                command.senderPhoneNumber,
                command.password,
            )
            is ParsedCommand.Unknown -> {
                Log.w(TAG, "Unknown command from ${command.senderPhoneNumber}: ${command.rawMessage}")
            }
        }
    }

    suspend fun handlePairRequest(senderPhone: String) {
        Log.i(TAG, "Pair request from: $senderPhone")

        val existing = deviceRepository.getByPhoneNumber(senderPhone)

        when (existing?.status) {
            PairingStatus.APPROVED -> {
                Log.d(TAG, "Already approved device: $senderPhone - ignoring duplicate request")
                return
            }
            PairingStatus.PENDING_SENT, PairingStatus.PENDING_RECEIVED -> {
                Log.d(TAG, "Pending device: $senderPhone - refreshing notification")
                notificationManager.showPairingRequestNotification(senderPhone)
                return
            }
            PairingStatus.REJECTED -> {
                Log.d(TAG, "Previously rejected device: $senderPhone - allowing re-request")
                deviceRepository.updatePairingStatus(senderPhone, PairingStatus.PENDING_RECEIVED)
                notificationManager.showPairingRequestNotification(senderPhone)
                return
            }
            null -> {
                // New request (they are source, we are target)
                deviceRepository.insert(
                    PairedDevice(
                        phoneNumber = senderPhone,
                        role = DeviceRole.TARGET,
                        status = PairingStatus.PENDING_RECEIVED,
                        createdAt = System.currentTimeMillis(),
                        lastActivityAt = System.currentTimeMillis(),
                    ),
                )
                notificationManager.showPairingRequestNotification(senderPhone)
            }
        }
    }

    suspend fun handlePairApproved(senderPhone: String) {
        Log.i(TAG, "Pair approved from: $senderPhone")

        val device = deviceRepository.getByPhoneNumber(senderPhone)
        if (device == null) {
            Log.w(TAG, "Received PAIR_APPROVED from unknown device: $senderPhone")
            return
        }

        if (device.status == PairingStatus.PENDING_SENT) {
            deviceRepository.updatePairingStatus(senderPhone, PairingStatus.APPROVED)
            deviceRepository.updateLastActivity(senderPhone)
            Log.i(TAG, "Device $senderPhone is now approved")
            notificationManager.showPairingResponseNotification(senderPhone, approved = true)
        }
    }

    suspend fun handlePairRejected(senderPhone: String) {
        Log.i(TAG, "Pair rejected from: $senderPhone")

        val device = deviceRepository.getByPhoneNumber(senderPhone)
        if (device == null) {
            Log.w(TAG, "Received PAIR_REJECTED from unknown device: $senderPhone")
            return
        }

        if (device.status == PairingStatus.PENDING_SENT) {
            deviceRepository.updatePairingStatus(senderPhone, PairingStatus.REJECTED)
            deviceRepository.updateLastActivity(senderPhone)
            Log.i(TAG, "Device $senderPhone rejected pairing")
            notificationManager.showPairingResponseNotification(senderPhone, approved = false)
        }
    }

    suspend fun handleUnpair(senderPhone: String) {
        Log.i(TAG, "Unpair from: $senderPhone")

        val device = deviceRepository.getByPhoneNumber(senderPhone)
        if (device != null) {
            sessionRepository.endSessionForDevice(senderPhone, "UNPAIR")
            deviceRepository.deleteByPhoneNumber(senderPhone)
            Log.i(TAG, "Device $senderPhone unpaired")

            // Send UNPAIR back so the other device also removes us
            // (If they already deleted us, they'll ignore this)
            smsSender.sendUnpair(senderPhone)
            Log.i(TAG, "Sent UNPAIR confirmation to $senderPhone")
        }
    }

    /**
     * Handle AUTH_REQUEST from SOURCE device.
     * Generates a challenge and sends AUTH_CHALLENGE back.
     */
    suspend fun handleAuthRequest(senderPhone: String) {
        Log.i(TAG, "Auth request from: $senderPhone")

        val device = deviceRepository.getByPhoneNumber(senderPhone)
        if (device == null || device.status != dev.notyouraverage.otpcourier.data.entities.PairingStatus.APPROVED) {
            Log.w(TAG, "AUTH_REQUEST from unknown/unapproved device: $senderPhone")
            return
        }

        if (securityManager.isDeviceLocked(device)) {
            Log.w(TAG, "AUTH_REQUEST from locked device: $senderPhone")
            return
        }

        val nonce = securityManager.generateChallenge(senderPhone)
        smsSender.sendAuthChallenge(senderPhone, nonce)
        Log.i(TAG, "Sent auth challenge to $senderPhone")
    }

    suspend fun handleStartForward(
        senderPhone: String,
        response: String,
        requestedDuration: Int?,
    ) {
        Log.i(TAG, "Start forward request from: $senderPhone")

        // Challenge-response auth is required - no password fallback
        if (!securityManager.hasPendingChallenge(senderPhone)) {
            Log.w(TAG, "START_FORWARD without pending challenge from $senderPhone - must send AUTH_REQUEST first")
            return
        }

        val authResult = securityManager.validateChallengeAndAuthenticate(senderPhone, response)

        when (authResult) {
            is SecurityManager.AuthenticationResult.Success -> {
                val device = authResult.device
                val duration = requestedDuration?.coerceAtMost(device.maxForwardDurationMinutes)
                    ?: device.maxForwardDurationMinutes

                val encryptionKey = device.authKey
                val existingSession = sessionRepository.getActiveSessionForDevice(senderPhone)
                if (existingSession != null) {
                    sessionRepository.updateSessionDuration(existingSession.id, duration)
                    Log.i(TAG, "Extended forwarding session for $senderPhone to $duration minutes")
                    onForwardingStateChanged(
                        ForwardingState.Started(
                            senderPhone,
                            duration,
                            existingSession.id,
                        ),
                    )
                } else {
                    val sessionId = sessionRepository.startSession(senderPhone, duration, encryptionKey)
                    Log.i(TAG, "Started forwarding session for $senderPhone for $duration minutes (encrypted)")
                    onForwardingStateChanged(ForwardingState.Started(senderPhone, duration, sessionId))
                }
            }

            is SecurityManager.AuthenticationResult.DeviceNotFound -> {
                Log.w(TAG, "START_FORWARD from unknown/unapproved device: $senderPhone")
            }

            is SecurityManager.AuthenticationResult.DeviceLocked -> {
                Log.w(
                    TAG,
                    "START_FORWARD from locked device: $senderPhone (${authResult.remainingMs}ms remaining)",
                )
            }

            is SecurityManager.AuthenticationResult.InvalidResponse -> {
                Log.w(
                    TAG,
                    "START_FORWARD with invalid response from $senderPhone (${authResult.attemptsRemaining} attempts remaining)",
                )
            }

            is SecurityManager.AuthenticationResult.AuthKeyMissing -> {
                Log.e(
                    TAG,
                    "START_FORWARD failed: device $senderPhone has no auth key - needs to be re-paired",
                )
            }

            is SecurityManager.AuthenticationResult.NoPendingChallenge -> {
                Log.w(TAG, "START_FORWARD without valid pending challenge from $senderPhone")
            }
        }
    }

    suspend fun handleStopForward(senderPhone: String) {
        Log.i(TAG, "Stop forward from: $senderPhone")

        val activeSession = sessionRepository.getActiveSessionForDevice(senderPhone)
        if (activeSession != null) {
            sessionRepository.endSession(activeSession.id, "REMOTE")
            onForwardingStateChanged(ForwardingState.Stopped(senderPhone, "REMOTE"))
            Log.i(TAG, "Stopped forwarding session for $senderPhone by remote request")
        }
    }

    private suspend fun handleForwardedData(
        senderPhone: String,
        originalSender: String,
        message: String,
    ) {
        Log.i(TAG, "Received forwarded data from $senderPhone: original sender=$originalSender")
        notificationManager.showForwardedMessageNotification(originalSender, message, senderPhone)
    }

    suspend fun handleForwardedDataEncrypted(senderPhone: String, encryptedContent: String) {
        Log.i(TAG, "Received encrypted forwarded data from $senderPhone")

        val device = deviceRepository.getByPhoneNumber(senderPhone)
        if (device == null) {
            Log.w(TAG, "Received encrypted data from unknown device: $senderPhone")
            return
        }

        val encryptionKey = device.activeEncryptionKey
        if (encryptionKey.isNullOrBlank()) {
            Log.w(TAG, "No encryption key stored for device: $senderPhone")
            // Try to show as-is (won't make sense but at least visible)
            notificationManager.showForwardedMessageNotification("Unknown", "[Encrypted message - no key]", senderPhone)
            return
        }

        val decrypted = MessageEncryption.decrypt(encryptedContent, encryptionKey)
        if (decrypted == null) {
            Log.e(TAG, "Failed to decrypt forwarded message from $senderPhone")
            notificationManager.showForwardedMessageNotification("Unknown", "[Decryption failed]", senderPhone)
            return
        }

        // Parse the decrypted content (format: originalSender|message)
        val separatorIndex = decrypted.indexOf('|')
        if (separatorIndex == -1) {
            Log.e(TAG, "Invalid decrypted message format from $senderPhone")
            notificationManager.showForwardedMessageNotification("Unknown", decrypted, senderPhone)
            return
        }

        val originalSender = decrypted.take(separatorIndex)
        val message = decrypted.substring(separatorIndex + 1)

        Log.i(TAG, "Decrypted forwarded SMS from $senderPhone: original sender=$originalSender")
        notificationManager.showForwardedMessageNotification(originalSender, message, senderPhone)
    }

    private suspend fun handleLegacyStart(senderPhone: String, password: String) {
        Log.w(TAG, "Legacy start from $senderPhone - password auth no longer supported, use AUTH_REQUEST flow")
        // Legacy password auth is no longer supported - device must use challenge-response
    }

    private suspend fun handleLegacyStop(senderPhone: String, password: String) {
        Log.i(TAG, "Legacy stop from: $senderPhone")
        // Legacy stop doesn't require password validation since it's stopping
        handleStopForward(senderPhone)
    }

    suspend fun handleIncomingSms(originalSender: String, messageBody: String) {
        val activeSessions = sessionRepository.getActiveSessionsList()

        if (activeSessions.isEmpty()) {
            Log.d(TAG, "No active forwarding sessions - ignoring incoming SMS")
            return
        }

        for (session in activeSessions) {
            smsSender.sendForwardedSms(session.devicePhoneNumber, originalSender, messageBody)
            sessionRepository.recordForwardedMessage(session.id)
            Log.i(TAG, "Forwarded SMS to ${session.devicePhoneNumber}")
        }
    }

    suspend fun initiatePairing(targetPhoneNumber: String) {
        val existing = deviceRepository.getByPhoneNumber(targetPhoneNumber)
        if (existing != null) {
            Log.w(TAG, "Device $targetPhoneNumber already exists with status ${existing.status}")
            return
        }

        // We are source, they are target
        deviceRepository.insert(
            PairedDevice(
                phoneNumber = targetPhoneNumber,
                role = DeviceRole.SOURCE,
                status = PairingStatus.PENDING_SENT,
                createdAt = System.currentTimeMillis(),
                lastActivityAt = System.currentTimeMillis(),
            ),
        )

        smsSender.sendPairRequest(targetPhoneNumber)
        Log.i(TAG, "Sent pairing request to $targetPhoneNumber")
    }

    suspend fun approvePairing(sourcePhoneNumber: String, password: String) {
        val passwordHash = SecurityManager.hashPassword(password)
        val authKey = SecurityManager.deriveAuthKey(password)
        deviceRepository.updatePassword(
            sourcePhoneNumber,
            passwordHash.hash,
            passwordHash.salt,
        )
        deviceRepository.updateAuthKey(sourcePhoneNumber, authKey)
        deviceRepository.updatePairingStatus(sourcePhoneNumber, PairingStatus.APPROVED)
        smsSender.sendPairApproved(sourcePhoneNumber)
        Log.i(TAG, "Approved pairing for $sourcePhoneNumber (with auth key)")
    }

    suspend fun rejectPairing(sourcePhoneNumber: String) {
        deviceRepository.updatePairingStatus(sourcePhoneNumber, PairingStatus.REJECTED)
        smsSender.sendPairRejected(sourcePhoneNumber)
        Log.i(TAG, "Rejected pairing for $sourcePhoneNumber")
    }
}

package dev.notyouraverage.smscourier.commands

import dev.notyouraverage.smscourier.data.entities.DeviceRole

sealed class ParsedCommand {
    abstract val senderPhoneNumber: String

    // Pairing commands
    data class PairRequest(
        override val senderPhoneNumber: String,
    ) : ParsedCommand()

    data class PairApproved(
        override val senderPhoneNumber: String,
    ) : ParsedCommand()

    data class PairRejected(
        override val senderPhoneNumber: String,
    ) : ParsedCommand()

    data class Unpair(
        override val senderPhoneNumber: String,
        val roleToDelete: DeviceRole? = null, // null for backward compatibility
    ) : ParsedCommand()

    // Authentication commands (challenge-response)
    data class AuthRequest(
        override val senderPhoneNumber: String,
    ) : ParsedCommand()

    data class AuthChallenge(
        override val senderPhoneNumber: String,
        val nonce: String,
    ) : ParsedCommand()

    // Forwarding commands
    data class StartForward(
        override val senderPhoneNumber: String,
        val password: String,
        // null means use default
        val durationMinutes: Int? = null,
    ) : ParsedCommand()

    data class StopForward(
        override val senderPhoneNumber: String,
    ) : ParsedCommand()

    // Forwarded data (received by Source phone)
    data class ForwardedData(
        override val senderPhoneNumber: String,
        val originalSender: String,
        val message: String,
    ) : ParsedCommand()

    // Encrypted forwarded data (received by Source phone)
    data class ForwardedDataEncrypted(
        override val senderPhoneNumber: String,
        val encryptedContent: String,
    ) : ParsedCommand()

    // Legacy commands (backward compatibility)
    data class LegacyStart(
        override val senderPhoneNumber: String,
        val password: String,
    ) : ParsedCommand()

    data class LegacyStop(
        override val senderPhoneNumber: String,
        val password: String,
    ) : ParsedCommand()

    // Unknown command
    data class Unknown(
        override val senderPhoneNumber: String,
        val rawMessage: String,
    ) : ParsedCommand()
}

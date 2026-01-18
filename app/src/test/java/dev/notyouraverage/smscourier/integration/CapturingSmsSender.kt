package dev.notyouraverage.smscourier.integration

import dev.notyouraverage.smscourier.services.SmsSender
import io.mockk.mockk
import org.junit.Assert.assertTrue

/**
 * Mock SmsSender that captures all outgoing messages for test verification.
 *
 * Extends SmsSender with a relaxed mock context to avoid actual SMS transmission.
 * All calls to send() are captured and can be verified using assertion helpers.
 *
 * Usage:
 * ```
 * // Verify message was sent
 * capturingSmsSender.assertSent("+1234567890", Regex("SMSC PAIR_APPROVED"))
 *
 * // Verify no messages sent
 * capturingSmsSender.assertNothingSent()
 *
 * // Filter by prefix
 * val pairingMessages = capturingSmsSender.findByPrefix("SMSC PAIR_")
 * ```
 */
class CapturingSmsSender : SmsSender(mockk(relaxed = true)) {

    /**
     * Represents a captured outgoing SMS message.
     */
    data class SentMessage(
        val phoneNumber: String,
        val message: String,
        val timestamp: Long = System.currentTimeMillis(),
    )

    private val _sentMessages = mutableListOf<SentMessage>()

    /**
     * Returns a snapshot of all captured messages.
     */
    val sentMessages: List<SentMessage> get() = _sentMessages.toList()

    /**
     * Captures the message instead of actually sending SMS.
     */
    override fun send(phoneNumber: String, message: String) {
        _sentMessages.add(SentMessage(phoneNumber, message))
    }

    /**
     * Clears all captured messages.
     * Call this between tests or when resetting state.
     */
    fun clear() {
        _sentMessages.clear()
    }

    /**
     * Finds messages that start with the given prefix.
     *
     * @param prefix The message prefix to filter by (e.g., "SMSC PAIR_")
     * @return List of matching messages
     */
    fun findByPrefix(prefix: String): List<SentMessage> =
        sentMessages.filter { it.message.startsWith(prefix) }

    /**
     * Finds messages sent to a specific phone number.
     *
     * @param phoneNumber The recipient phone number to filter by
     * @return List of messages sent to that number
     */
    fun findByPhoneNumber(phoneNumber: String): List<SentMessage> =
        sentMessages.filter { it.phoneNumber == phoneNumber }

    // ==================== Assertion Helpers ====================

    /**
     * Asserts that a message matching the pattern was sent to the phone number.
     *
     * @param phoneNumber Expected recipient
     * @param messagePattern Regex pattern the message body should match
     * @throws AssertionError if no matching message was found
     */
    fun assertSent(phoneNumber: String, messagePattern: Regex) {
        val found = sentMessages.any {
            it.phoneNumber == phoneNumber && messagePattern.containsMatchIn(it.message)
        }
        assertTrue(
            "Expected message matching $messagePattern to $phoneNumber.\n" +
                "Actual messages: $sentMessages",
            found,
        )
    }

    /**
     * Asserts that no messages were sent.
     *
     * @throws AssertionError if any messages were captured
     */
    fun assertNothingSent() {
        assertTrue(
            "Expected no messages sent, but got: $sentMessages",
            sentMessages.isEmpty(),
        )
    }

    /**
     * Asserts that exactly the expected number of messages were sent.
     *
     * @param expected Expected message count
     * @throws AssertionError if count doesn't match
     */
    fun assertMessageCount(expected: Int) {
        assertTrue(
            "Expected $expected messages, but got ${sentMessages.size}: $sentMessages",
            sentMessages.size == expected,
        )
    }
}

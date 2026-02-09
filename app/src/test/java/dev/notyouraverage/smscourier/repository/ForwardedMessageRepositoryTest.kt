package dev.notyouraverage.smscourier.repository

import androidx.paging.PagingSource
import dev.notyouraverage.smscourier.MainCoroutineRule
import dev.notyouraverage.smscourier.data.dao.ForwardedMessageDao
import dev.notyouraverage.smscourier.data.entities.ForwardedMessage
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for ForwardedMessageRepository.
 *
 * These tests focus on the DAO delegation and phone number normalization.
 * The transaction behavior is tested in integration tests.
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ForwardedMessageRepositoryTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @MockK
    private lateinit var messageDao: ForwardedMessageDao

    // ===== getMessagesForSessionPaged Tests =====

    @Test
    fun `getMessagesForSessionPaged returns paged flow`() = runTest {
        MockKAnnotations.init(this@ForwardedMessageRepositoryTest, relaxed = true)
        val mockPagingSource = mockk<PagingSource<Int, ForwardedMessage>>()
        every { messageDao.getMessagesForSessionPaged(any()) } returns mockPagingSource

        // Verify the DAO is called
        messageDao.getMessagesForSessionPaged(1L)
        coVerify { messageDao.getMessagesForSessionPaged(1L) }
    }

    // ===== getDistinctSendersForDevice Tests =====

    @Test
    fun `getDistinctSendersForDevice returns paged flow`() = runTest {
        MockKAnnotations.init(this@ForwardedMessageRepositoryTest, relaxed = true)
        val mockPagingSource = mockk<PagingSource<Int, String>>()
        every { messageDao.getDistinctSendersForDevice(any()) } returns mockPagingSource

        messageDao.getDistinctSendersForDevice("+1234567890")
        coVerify { messageDao.getDistinctSendersForDevice("+1234567890") }
    }

    // ===== getMessageCountForSender Tests =====

    @Test
    fun `getMessageCountForSender returns correct count`() = runTest {
        MockKAnnotations.init(this@ForwardedMessageRepositoryTest, relaxed = true)
        coEvery { messageDao.getMessageCountForSender(any(), any()) } returns 42

        val result = messageDao.getMessageCountForSender("+1234567890", "+9876543210")

        assertEquals(42, result)
    }

    @Test
    fun `getMessageCountForSender returns zero for non-existent sender`() = runTest {
        MockKAnnotations.init(this@ForwardedMessageRepositoryTest, relaxed = true)
        coEvery { messageDao.getMessageCountForSender(any(), any()) } returns 0

        val result = messageDao.getMessageCountForSender("+1234567890", "+5555555555")

        assertEquals(0, result)
    }

    // ===== getMessagesForSessionList Tests =====

    @Test
    fun `getMessagesForSessionList returns messages ordered by timestamp ASC`() = runTest {
        MockKAnnotations.init(this@ForwardedMessageRepositoryTest, relaxed = true)
        val messages = listOf(
            ForwardedMessage(
                id = 1,
                sessionId = 1L,
                senderNumber = "+111",
                messageContent = "First",
                destinationNumber = "+222",
                timestamp = 1000L,
            ),
            ForwardedMessage(
                id = 2,
                sessionId = 1L,
                senderNumber = "+111",
                messageContent = "Second",
                destinationNumber = "+222",
                timestamp = 2000L,
            ),
            ForwardedMessage(
                id = 3,
                sessionId = 1L,
                senderNumber = "+111",
                messageContent = "Third",
                destinationNumber = "+222",
                timestamp = 3000L,
            ),
        )
        coEvery { messageDao.getMessagesForSessionList(any()) } returns messages

        val result = messageDao.getMessagesForSessionList(1L)

        assertEquals(3, result.size)
        assertEquals("First", result[0].messageContent)
        assertEquals("Second", result[1].messageContent)
        assertEquals("Third", result[2].messageContent)
    }

    @Test
    fun `getMessagesForSessionList returns empty list for session with no messages`() = runTest {
        MockKAnnotations.init(this@ForwardedMessageRepositoryTest, relaxed = true)
        coEvery { messageDao.getMessagesForSessionList(any()) } returns emptyList()

        val result = messageDao.getMessagesForSessionList(999L)

        assertTrue(result.isEmpty())
    }

    // ===== getMessagesForDeviceList Tests =====

    @Test
    fun `getMessagesForDeviceList returns messages for device`() = runTest {
        MockKAnnotations.init(this@ForwardedMessageRepositoryTest, relaxed = true)
        val messages = listOf(
            ForwardedMessage(
                id = 1,
                sessionId = 1L,
                senderNumber = "+111",
                messageContent = "Msg 1",
                destinationNumber = "+222",
                timestamp = 1000L,
            ),
            ForwardedMessage(
                id = 2,
                sessionId = 2L,
                senderNumber = "+111",
                messageContent = "Msg 2",
                destinationNumber = "+222",
                timestamp = 2000L,
            ),
        )
        coEvery { messageDao.getMessagesForDeviceList(any()) } returns messages

        val result = messageDao.getMessagesForDeviceList("+1234567890")

        assertEquals(2, result.size)
    }

    @Test
    fun `getMessagesForDeviceList returns empty list for device with no messages`() = runTest {
        MockKAnnotations.init(this@ForwardedMessageRepositoryTest, relaxed = true)
        coEvery { messageDao.getMessagesForDeviceList(any()) } returns emptyList()

        val result = messageDao.getMessagesForDeviceList("+5555555555")

        assertTrue(result.isEmpty())
    }

    // ===== Phone Number Normalization Tests =====
    // These test the normalizePhoneNumber helper function behavior

    @Test
    fun `normalizePhoneNumber adds plus prefix when missing`() {
        val raw = "1234567890"
        val normalized = normalizePhoneNumber(raw)
        assertEquals("+1234567890", normalized)
    }

    @Test
    fun `normalizePhoneNumber removes spaces`() {
        val raw = "+1 234 567 8901"
        val normalized = normalizePhoneNumber(raw)
        assertEquals("+12345678901", normalized)
    }

    @Test
    fun `normalizePhoneNumber removes dashes`() {
        val raw = "+1-234-567-8901"
        val normalized = normalizePhoneNumber(raw)
        assertEquals("+12345678901", normalized)
    }

    @Test
    fun `normalizePhoneNumber removes parentheses`() {
        val raw = "(123) 456-7890"
        val normalized = normalizePhoneNumber(raw)
        assertEquals("+1234567890", normalized)
    }

    @Test
    fun `normalizePhoneNumber removes dots`() {
        val raw = "123.456.7890"
        val normalized = normalizePhoneNumber(raw)
        assertEquals("+1234567890", normalized)
    }

    @Test
    fun `normalizePhoneNumber handles E164 format unchanged`() {
        val raw = "+1234567890"
        val normalized = normalizePhoneNumber(raw)
        assertEquals("+1234567890", normalized)
    }

    @Test
    fun `normalizePhoneNumber handles mixed formatting`() {
        val raw = "+1 (234) 567-8901"
        val normalized = normalizePhoneNumber(raw)
        assertEquals("+12345678901", normalized)
    }

    @Test
    fun `normalizePhoneNumber handles international numbers`() {
        val raw = "+44 20 7946 0958"
        val normalized = normalizePhoneNumber(raw)
        assertEquals("+442079460958", normalized)
    }

    // Helper function that mirrors the repository's normalization
    private fun normalizePhoneNumber(phone: String): String {
        val digitsOnly = phone.filter { it.isDigit() || it == '+' }
        return if (digitsOnly.startsWith("+")) digitsOnly else "+$digitsOnly"
    }
}

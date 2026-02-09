package dev.notyouraverage.smscourier.repository

import android.database.SQLException
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.withTransaction
import dev.notyouraverage.smscourier.data.SmsCourierDatabase
import dev.notyouraverage.smscourier.data.dao.ForwardedMessageDao
import dev.notyouraverage.smscourier.data.dao.ForwardingSessionDao
import dev.notyouraverage.smscourier.data.dao.PairedDeviceDao
import dev.notyouraverage.smscourier.data.entities.DeviceRole
import dev.notyouraverage.smscourier.data.entities.ForwardedMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ForwardedMessageRepository(
    private val database: SmsCourierDatabase,
    private val messageDao: ForwardedMessageDao,
    private val sessionDao: ForwardingSessionDao,
    private val deviceDao: PairedDeviceDao,
) {
    companion object {
        private const val TAG = "SMSC:MessageRepository"
    }

    /**
     * Normalizes phone numbers to match PairedDeviceRepository normalization.
     * Must match PairedDeviceRepository.normalizePhoneNumber() to ensure lookups work.
     */
    private fun normalizePhoneNumber(phoneNumber: String): String {
        return phoneNumber
            .replace(Regex("[\\s\\-().]"), "")
            .let { if (it.startsWith("+")) it else "+$it" }
    }

    /**
     * Stores a forwarded message and updates all counters atomically.
     * Called by TARGET device when forwarding SMS to SOURCE.
     *
     * @param sessionId The active forwarding session ID
     * @param senderNumber Original SMS sender phone number
     * @param messageContent The SMS message body
     * @param destinationNumber The SOURCE device that will receive this message
     * @param devicePhone The SOURCE device phone number for statistics
     * @param deviceRole Should be DeviceRole.TARGET (we are TARGET forwarding to SOURCE)
     *
     * @return Result.success with message ID on success, Result.failure on error
     */
    suspend fun storeMessageWithCounters(
        sessionId: Long,
        senderNumber: String,
        messageContent: String,
        destinationNumber: String,
        devicePhone: String,
        deviceRole: DeviceRole,
    ): Result<Long> = withContext(Dispatchers.IO) {
        val normalizedDevicePhone = normalizePhoneNumber(devicePhone)
        try {
            database.withTransaction {
                // Insert message
                val messageId = messageDao.insertMessage(
                    ForwardedMessage(
                        sessionId = sessionId,
                        senderNumber = senderNumber,
                        messageContent = messageContent,
                        destinationNumber = destinationNumber,
                    ),
                )

                // Update session counter
                sessionDao.incrementMessageCount(sessionId)

                // Update device statistics (use normalized phone to match device records)
                deviceDao.incrementTotalMessagesForwarded(normalizedDevicePhone, deviceRole)

                messageId
            }.let { Result.success(it) }
        } catch (e: SQLiteConstraintException) {
            // Foreign key violation - session was deleted while we tried to insert
            Log.w(TAG, "Session $sessionId not found (FK violation), dropping message")
            Result.failure(e)
        } catch (e: SQLException) {
            // Generic database error - transient, could retry
            Log.e(TAG, "Database error storing message: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error storing message: ${e.message}")
            Result.failure(e)
        }
    }

    fun getMessagesForSessionPaged(sessionId: Long): Flow<PagingData<ForwardedMessage>> {
        return Pager(
            config = PagingConfig(
                pageSize = 30,
                enablePlaceholders = true,
                prefetchDistance = 10,
            ),
            pagingSourceFactory = { messageDao.getMessagesForSessionPaged(sessionId) },
        ).flow
    }

    fun getDistinctSendersForDevice(phoneNumber: String): Flow<PagingData<String>> {
        val normalized = normalizePhoneNumber(phoneNumber)
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = true,
            ),
            pagingSourceFactory = { messageDao.getDistinctSendersForDevice(normalized) },
        ).flow
    }

    suspend fun getMessageCountForSender(phoneNumber: String, senderNumber: String): Int =
        withContext(Dispatchers.IO) {
            messageDao.getMessageCountForSender(normalizePhoneNumber(phoneNumber), senderNumber)
        }

    /**
     * Get all messages for a session as a list (for export).
     */
    suspend fun getMessagesForSessionList(sessionId: Long): List<ForwardedMessage> =
        withContext(Dispatchers.IO) {
            messageDao.getMessagesForSessionList(sessionId)
        }

    /**
     * Get all messages for a device across all sessions (for bulk export).
     */
    suspend fun getMessagesForDeviceList(phoneNumber: String): List<ForwardedMessage> =
        withContext(Dispatchers.IO) {
            messageDao.getMessagesForDeviceList(normalizePhoneNumber(phoneNumber))
        }
}

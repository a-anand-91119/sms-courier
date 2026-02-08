package dev.notyouraverage.smscourier.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.notyouraverage.smscourier.data.entities.ForwardedMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ForwardedMessageDao {

    @Insert
    suspend fun insertMessage(message: ForwardedMessage): Long

    @Query("SELECT * FROM forwarded_messages WHERE session_id = :sessionId ORDER BY timestamp DESC")
    fun getMessagesForSession(sessionId: Long): Flow<List<ForwardedMessage>>

    @Query("SELECT COUNT(*) FROM forwarded_messages WHERE session_id = :sessionId")
    suspend fun getMessageCountForSession(sessionId: Long): Int

    @Query("DELETE FROM forwarded_messages WHERE session_id = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: Long)

    @Query("DELETE FROM forwarded_messages WHERE timestamp < :threshold")
    suspend fun deleteMessagesOlderThan(threshold: Long)

    @Query("SELECT * FROM forwarded_messages WHERE session_id = :sessionId ORDER BY timestamp DESC")
    fun getMessagesForSessionPaged(sessionId: Long): PagingSource<Int, ForwardedMessage>

    @Query("SELECT DISTINCT sender_number FROM forwarded_messages fm INNER JOIN forwarding_sessions fs ON fm.session_id = fs.id WHERE fs.device_phone_number = :phoneNumber ORDER BY sender_number ASC")
    fun getDistinctSendersForDevice(phoneNumber: String): PagingSource<Int, String>

    @Query("SELECT COUNT(*) FROM forwarded_messages fm INNER JOIN forwarding_sessions fs ON fm.session_id = fs.id WHERE fs.device_phone_number = :phoneNumber AND fm.sender_number = :senderNumber")
    suspend fun getMessageCountForSender(phoneNumber: String, senderNumber: String): Int

    /**
     * Get all messages for a session as a list (for export).
     * Returns complete message list without pagination.
     */
    @Query("SELECT * FROM forwarded_messages WHERE session_id = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSessionList(sessionId: Long): List<ForwardedMessage>

    /**
     * Get all messages for a device across all sessions (for bulk export).
     * Used when exporting all data for a device.
     */
    @Query(
        """
        SELECT m.* FROM forwarded_messages m
        INNER JOIN forwarding_sessions s ON m.session_id = s.id
        WHERE s.device_phone_number = :phoneNumber
        ORDER BY m.session_id, m.timestamp ASC
        """,
    )
    suspend fun getMessagesForDeviceList(phoneNumber: String): List<ForwardedMessage>
}

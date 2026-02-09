package dev.notyouraverage.smscourier.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import dev.notyouraverage.smscourier.data.entities.ForwardingSession
import kotlinx.coroutines.flow.Flow

@Dao
interface ForwardingSessionDao {

    @Query("SELECT * FROM forwarding_sessions WHERE is_active = 1")
    fun getActiveSessions(): Flow<List<ForwardingSession>>

    @Query("SELECT * FROM forwarding_sessions WHERE is_active = 1")
    suspend fun getActiveSessionsList(): List<ForwardingSession>

    @Query("SELECT * FROM forwarding_sessions WHERE device_phone_number = :phoneNumber AND is_active = 1")
    suspend fun getActiveSessionForDevice(phoneNumber: String): ForwardingSession?

    @Query("SELECT * FROM forwarding_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): ForwardingSession?

    @Query("SELECT * FROM forwarding_sessions ORDER BY started_at DESC")
    fun getAllSessions(): Flow<List<ForwardingSession>>

    @Query("SELECT * FROM forwarding_sessions WHERE device_phone_number = :phoneNumber ORDER BY started_at DESC")
    fun getSessionsForDevice(phoneNumber: String): Flow<List<ForwardingSession>>

    @Query("SELECT * FROM forwarding_sessions WHERE device_phone_number = :phoneNumber ORDER BY started_at DESC")
    fun getSessionsForDevicePaged(phoneNumber: String): PagingSource<Int, ForwardingSession>

    @Insert
    suspend fun insertSession(session: ForwardingSession): Long

    @Update
    suspend fun updateSession(session: ForwardingSession)

    @Query(
        """
        UPDATE forwarding_sessions
        SET is_active = 0, stopped_by = :stoppedBy
        WHERE id = :sessionId
        """,
    )
    suspend fun endSession(sessionId: Long, stoppedBy: String)

    @Query(
        """
        UPDATE forwarding_sessions
        SET is_active = 0, stopped_by = :stoppedBy
        WHERE device_phone_number = :phoneNumber AND is_active = 1
        """,
    )
    suspend fun endSessionForDevice(phoneNumber: String, stoppedBy: String)

    @Query("UPDATE forwarding_sessions SET is_active = 0, stopped_by = 'TIMEOUT' WHERE is_active = 1 AND expires_at < :now")
    suspend fun expireSessions(now: Long = System.currentTimeMillis())

    @Query("UPDATE forwarding_sessions SET messages_forwarded = messages_forwarded + 1 WHERE id = :sessionId")
    suspend fun incrementMessagesForwarded(sessionId: Long)

    @Query("UPDATE forwarding_sessions SET message_count = message_count + 1 WHERE id = :sessionId")
    suspend fun incrementMessageCount(sessionId: Long)

    @Query(
        """
        UPDATE forwarding_sessions
        SET duration_minutes = :durationMinutes, expires_at = :expiresAt
        WHERE id = :sessionId
        """,
    )
    suspend fun updateSessionDuration(sessionId: Long, durationMinutes: Int, expiresAt: Long)

    @Query("SELECT COUNT(*) FROM forwarding_sessions WHERE device_phone_number = :phoneNumber")
    suspend fun getSessionCountForDevice(phoneNumber: String): Int

    /**
     * Get all sessions for a device as a list (for export).
     * Returns complete session list without pagination.
     */
    @Query("SELECT * FROM forwarding_sessions WHERE device_phone_number = :phoneNumber ORDER BY started_at DESC")
    suspend fun getSessionsForDeviceList(phoneNumber: String): List<ForwardingSession>

    /**
     * Count sessions older than threshold that are NOT active.
     * Used to show preview before cleanup.
     */
    @Query("SELECT COUNT(*) FROM forwarding_sessions WHERE started_at < :thresholdMs AND is_active = 0")
    suspend fun countSessionsOlderThan(thresholdMs: Long): Int

    /**
     * Get inactive sessions older than threshold.
     * Used to calculate message count before deletion.
     */
    @Query("SELECT * FROM forwarding_sessions WHERE started_at < :thresholdMs AND is_active = 0")
    suspend fun getSessionsOlderThan(thresholdMs: Long): List<ForwardingSession>

    /**
     * Delete inactive sessions older than threshold.
     * Messages are CASCADE deleted via foreign key.
     */
    @Query("DELETE FROM forwarding_sessions WHERE started_at < :thresholdMs AND is_active = 0")
    suspend fun deleteSessionsOlderThan(thresholdMs: Long): Int

    /**
     * Delete all sessions for a device.
     * Messages are CASCADE deleted via foreign key.
     */
    @Query("DELETE FROM forwarding_sessions WHERE device_phone_number = :phoneNumber")
    suspend fun deleteAllForDevice(phoneNumber: String)
}

package dev.notyouraverage.smscourier.data.dao

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
}

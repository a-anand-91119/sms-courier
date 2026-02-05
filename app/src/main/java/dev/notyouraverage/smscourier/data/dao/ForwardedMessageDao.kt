package dev.notyouraverage.smscourier.data.dao

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
}

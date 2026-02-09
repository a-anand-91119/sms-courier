package dev.notyouraverage.smscourier.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import dev.notyouraverage.smscourier.data.dao.ForwardingSessionDao
import dev.notyouraverage.smscourier.data.entities.ForwardingSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Result of a cleanup operation.
 */
data class CleanupResult(
    val sessionCount: Int,
    val messageCount: Int,
)

class ForwardingSessionRepository(
    private val forwardingSessionDao: ForwardingSessionDao,
) {
    private val ioDispatcher = Dispatchers.IO

    /**
     * Normalizes phone numbers to match PairedDeviceRepository normalization.
     * Must match PairedDeviceRepository.normalizePhoneNumber() to ensure lookups work.
     */
    private fun normalizePhoneNumber(phoneNumber: String): String {
        return phoneNumber
            .replace(Regex("[\\s\\-().]"), "")
            .let { if (it.startsWith("+")) it else "+$it" }
    }

    fun getActiveSessions(): Flow<List<ForwardingSession>> =
        forwardingSessionDao.getActiveSessions()

    suspend fun getActiveSessionsList(): List<ForwardingSession> = withContext(ioDispatcher) {
        forwardingSessionDao.getActiveSessionsList()
    }

    suspend fun getActiveSessionForDevice(phoneNumber: String): ForwardingSession? =
        withContext(ioDispatcher) {
            forwardingSessionDao.getActiveSessionForDevice(normalizePhoneNumber(phoneNumber))
        }

    suspend fun getSessionById(sessionId: Long): ForwardingSession? = withContext(ioDispatcher) {
        forwardingSessionDao.getSessionById(sessionId)
    }

    fun getAllSessions(): Flow<List<ForwardingSession>> =
        forwardingSessionDao.getAllSessions()

    fun getSessionsForDevice(phoneNumber: String): Flow<List<ForwardingSession>> =
        forwardingSessionDao.getSessionsForDevice(normalizePhoneNumber(phoneNumber))

    fun getSessionsForDevicePaged(phoneNumber: String): Flow<PagingData<ForwardingSession>> {
        val normalized = normalizePhoneNumber(phoneNumber)
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = true,
                prefetchDistance = 5,
            ),
            pagingSourceFactory = { forwardingSessionDao.getSessionsForDevicePaged(normalized) },
        ).flow
    }

    suspend fun startSession(devicePhoneNumber: String, durationMinutes: Int, encryptionKey: String? = null): Long =
        withContext(ioDispatcher) {
            val now = System.currentTimeMillis()
            val expiresAt = now + (durationMinutes * 60 * 1000L)
            val session = ForwardingSession(
                devicePhoneNumber = normalizePhoneNumber(devicePhoneNumber),
                startedAt = now,
                durationMinutes = durationMinutes,
                expiresAt = expiresAt,
                isActive = true,
                encryptionKey = encryptionKey,
            )
            forwardingSessionDao.insertSession(session)
        }

    suspend fun endSession(sessionId: Long, stoppedBy: String) = withContext(ioDispatcher) {
        forwardingSessionDao.endSession(sessionId, stoppedBy)
    }

    suspend fun endSessionForDevice(phoneNumber: String, stoppedBy: String) = withContext(ioDispatcher) {
        forwardingSessionDao.endSessionForDevice(normalizePhoneNumber(phoneNumber), stoppedBy)
    }

    suspend fun expireSessions() = withContext(ioDispatcher) {
        forwardingSessionDao.expireSessions()
    }

    suspend fun recordForwardedMessage(sessionId: Long) = withContext(ioDispatcher) {
        forwardingSessionDao.incrementMessagesForwarded(sessionId)
    }

    suspend fun updateSessionDuration(sessionId: Long, durationMinutes: Int) = withContext(ioDispatcher) {
        val session = forwardingSessionDao.getSessionById(sessionId)
        if (session != null) {
            val newExpiresAt = session.startedAt + (durationMinutes * 60 * 1000L)
            forwardingSessionDao.updateSessionDuration(sessionId, durationMinutes, newExpiresAt)
        }
    }

    suspend fun getSessionCountForDevice(phoneNumber: String): Int = withContext(ioDispatcher) {
        forwardingSessionDao.getSessionCountForDevice(normalizePhoneNumber(phoneNumber))
    }

    /**
     * Get all sessions for a device as a list (for export).
     */
    suspend fun getSessionsForDeviceList(phoneNumber: String): List<ForwardingSession> =
        withContext(ioDispatcher) {
            forwardingSessionDao.getSessionsForDeviceList(normalizePhoneNumber(phoneNumber))
        }

    /**
     * Clean up old sessions and messages based on retention setting.
     * @param retentionDays Number of days to retain (0 = forever, skip cleanup)
     * @return CleanupResult with counts of deleted sessions and messages
     */
    suspend fun cleanupOldSessions(retentionDays: Int): CleanupResult = withContext(ioDispatcher) {
        // Forever setting: nothing to clean
        if (retentionDays == 0) {
            return@withContext CleanupResult(0, 0)
        }

        val thresholdMs = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)

        // Get sessions to calculate message count before deletion
        val sessionsToDelete = forwardingSessionDao.getSessionsOlderThan(thresholdMs)
        val sessionCount = sessionsToDelete.size
        val messageCount = sessionsToDelete.sumOf { it.messageCount }

        // Delete sessions (messages CASCADE deleted via foreign key)
        forwardingSessionDao.deleteSessionsOlderThan(thresholdMs)

        CleanupResult(sessionCount, messageCount)
    }

    /**
     * Delete all sessions for a device.
     * Messages are CASCADE deleted via foreign key.
     */
    suspend fun deleteAllForDevice(phoneNumber: String) = withContext(ioDispatcher) {
        forwardingSessionDao.deleteAllForDevice(normalizePhoneNumber(phoneNumber))
    }
}

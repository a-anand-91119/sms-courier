package dev.notyouraverage.smscourier.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import dev.notyouraverage.smscourier.data.dao.ForwardingSessionDao
import dev.notyouraverage.smscourier.data.entities.ForwardingSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ForwardingSessionRepository(
    private val forwardingSessionDao: ForwardingSessionDao,
) {
    private val ioDispatcher = Dispatchers.IO

    fun getActiveSessions(): Flow<List<ForwardingSession>> =
        forwardingSessionDao.getActiveSessions()

    suspend fun getActiveSessionsList(): List<ForwardingSession> = withContext(ioDispatcher) {
        forwardingSessionDao.getActiveSessionsList()
    }

    suspend fun getActiveSessionForDevice(phoneNumber: String): ForwardingSession? =
        withContext(ioDispatcher) {
            forwardingSessionDao.getActiveSessionForDevice(phoneNumber)
        }

    suspend fun getSessionById(sessionId: Long): ForwardingSession? = withContext(ioDispatcher) {
        forwardingSessionDao.getSessionById(sessionId)
    }

    fun getAllSessions(): Flow<List<ForwardingSession>> =
        forwardingSessionDao.getAllSessions()

    fun getSessionsForDevice(phoneNumber: String): Flow<List<ForwardingSession>> =
        forwardingSessionDao.getSessionsForDevice(phoneNumber)

    fun getSessionsForDevicePaged(phoneNumber: String): Flow<PagingData<ForwardingSession>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = true,
                prefetchDistance = 5
            ),
            pagingSourceFactory = { forwardingSessionDao.getSessionsForDevicePaged(phoneNumber) }
        ).flow
    }

    suspend fun startSession(devicePhoneNumber: String, durationMinutes: Int, encryptionKey: String? = null): Long =
        withContext(ioDispatcher) {
            val now = System.currentTimeMillis()
            val expiresAt = now + (durationMinutes * 60 * 1000L)
            val session = ForwardingSession(
                devicePhoneNumber = devicePhoneNumber,
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
        forwardingSessionDao.endSessionForDevice(phoneNumber, stoppedBy)
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
        forwardingSessionDao.getSessionCountForDevice(phoneNumber)
    }

    /**
     * Get all sessions for a device as a list (for export).
     */
    suspend fun getSessionsForDeviceList(phoneNumber: String): List<ForwardingSession> =
        withContext(ioDispatcher) {
            forwardingSessionDao.getSessionsForDeviceList(phoneNumber)
        }
}

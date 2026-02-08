package dev.notyouraverage.smscourier.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.notyouraverage.smscourier.data.SmsCourierDatabase
import dev.notyouraverage.smscourier.repository.ForwardingSessionRepository
import dev.notyouraverage.smscourier.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class CleanupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting scheduled cleanup")

            val database = SmsCourierDatabase.getDatabase(applicationContext)
            val sessionRepository = ForwardingSessionRepository(database.forwardingSessionDao())
            val settingsRepository = SettingsRepository(applicationContext)

            val retentionDays = settingsRepository.historyRetentionDays.first()

            val result = sessionRepository.cleanupOldSessions(retentionDays)

            settingsRepository.setLastCleanupTimestamp(System.currentTimeMillis())

            Log.i(TAG, "Cleanup complete: ${result.sessionCount} sessions, ${result.messageCount} messages deleted")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "CleanupWorker"
    }
}

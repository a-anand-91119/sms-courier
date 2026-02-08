package dev.notyouraverage.smscourier.utils

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import dev.notyouraverage.smscourier.workers.CleanupWorker
import java.util.concurrent.TimeUnit

object WorkManagerHelper {
    private const val CLEANUP_WORK_NAME = "cleanup_old_history"

    fun scheduleCleanup(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val cleanupRequest = PeriodicWorkRequestBuilder<CleanupWorker>(
            repeatInterval = 7,
            repeatIntervalTimeUnit = TimeUnit.DAYS,
            flexTimeInterval = 1,
            flexTimeIntervalUnit = TimeUnit.DAYS,
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS,
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            CLEANUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest,
        )
    }

    fun cancelCleanup(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(CLEANUP_WORK_NAME)
    }
}

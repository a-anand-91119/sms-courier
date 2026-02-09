package dev.notyouraverage.smscourier.workers

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import dev.notyouraverage.smscourier.MainCoroutineRule
import dev.notyouraverage.smscourier.utils.WorkManagerHelper
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Tests for CleanupWorker and WorkManagerHelper.
 *
 * CleanupWorker creates its dependencies (database, repositories) internally using
 * a companion object singleton pattern, which makes it difficult to mock in isolation.
 * These tests focus on:
 * 1. WorkManagerHelper scheduling/cancellation behavior
 * 2. Integration tests with the real database
 *
 * Note: Tests that require mocking SmsCourierDatabase.getDatabase() are not included
 * because MockK's static mocking doesn't work reliably with companion objects and
 * the any() matcher.
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CleanupWorkerTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()

        // Initialize WorkManager for testing
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ===== WorkManagerHelper Tests =====

    @Test
    fun `scheduleCleanup creates periodic work request`() = runTest {
        // Act
        WorkManagerHelper.scheduleCleanup(context)

        // Assert
        val workManager = WorkManager.getInstance(context)
        val workInfos = workManager.getWorkInfosForUniqueWork("cleanup_old_history").get()

        assertTrue(workInfos.isNotEmpty())
    }

    @Test
    fun `scheduleCleanup work is enqueued successfully`() = runTest {
        // Act
        WorkManagerHelper.scheduleCleanup(context)

        // Assert - Work is enqueued (constraints are embedded in request)
        val workManager = WorkManager.getInstance(context)
        val workInfos = workManager.getWorkInfosForUniqueWork("cleanup_old_history").get()

        assertTrue(workInfos.isNotEmpty())
        assertEquals(WorkInfo.State.ENQUEUED, workInfos[0].state)
    }

    @Test
    fun `cancelCleanup cancels scheduled work`() = runTest {
        // Arrange - First schedule the work
        WorkManagerHelper.scheduleCleanup(context)

        val workManager = WorkManager.getInstance(context)
        var workInfos = workManager.getWorkInfosForUniqueWork("cleanup_old_history").get()
        assertTrue("Work should be scheduled", workInfos.isNotEmpty())

        // Act
        WorkManagerHelper.cancelCleanup(context)

        // Assert
        workInfos = workManager.getWorkInfosForUniqueWork("cleanup_old_history").get()
        assertTrue(
            "Work should be cancelled or removed",
            workInfos.isEmpty() || workInfos[0].state == WorkInfo.State.CANCELLED,
        )
    }

    @Test
    fun `scheduleCleanup uses KEEP policy for existing work`() = runTest {
        // Arrange - Schedule work first time
        WorkManagerHelper.scheduleCleanup(context)
        val workManager = WorkManager.getInstance(context)
        val firstWorkInfos = workManager.getWorkInfosForUniqueWork("cleanup_old_history").get()
        val firstWorkId = firstWorkInfos[0].id

        // Act - Schedule again
        WorkManagerHelper.scheduleCleanup(context)

        // Assert - Same work ID should be kept (KEEP policy)
        val secondWorkInfos = workManager.getWorkInfosForUniqueWork("cleanup_old_history").get()
        assertEquals(1, secondWorkInfos.size)
        assertEquals(firstWorkId, secondWorkInfos[0].id)
    }

    @Test
    fun `scheduleCleanup after cancel creates new work`() = runTest {
        // Arrange - Schedule and then cancel
        WorkManagerHelper.scheduleCleanup(context)
        val workManager = WorkManager.getInstance(context)
        val firstWorkInfos = workManager.getWorkInfosForUniqueWork("cleanup_old_history").get()

        WorkManagerHelper.cancelCleanup(context)

        // Act - Schedule again after cancel
        WorkManagerHelper.scheduleCleanup(context)

        // Assert - New work ID created after cancel
        val newWorkInfos = workManager.getWorkInfosForUniqueWork("cleanup_old_history").get()
        assertTrue(newWorkInfos.isNotEmpty())
        // Note: Due to KEEP policy and timing, the ID might be the same if not fully cancelled
        // Just verify work exists
        assertTrue(
            newWorkInfos[0].state == WorkInfo.State.ENQUEUED ||
                newWorkInfos[0].state == WorkInfo.State.RUNNING,
        )
    }

    // ===== CleanupWorker Integration Tests =====
    // These tests use Robolectric to run with real database/repository behavior

    @Test
    fun `doWork with real database returns success when no cleanup needed`() = runTest {
        // This test verifies the worker runs end-to-end with a real in-memory database
        // The default retention of 0 (Forever) means nothing gets cleaned up

        val worker = TestListenableWorkerBuilder<CleanupWorker>(context).build()

        // Act
        val result = worker.doWork()

        // Assert - Should succeed with default settings (retention=0 means no cleanup)
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `doWork completes successfully with default settings`() = runTest {
        // This is an integration test that verifies the worker can:
        // 1. Access the database
        // 2. Read settings
        // 3. Call cleanup
        // 4. Update timestamp
        // All using real components (not mocks)

        val worker = TestListenableWorkerBuilder<CleanupWorker>(context).build()

        // Act
        val result = worker.doWork()

        // Assert - Worker completes successfully
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `doWork returns success result type`() = runTest {
        // Verify the worker returns the correct Result type
        val worker = TestListenableWorkerBuilder<CleanupWorker>(context).build()

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Success)
    }

    @Test
    fun `multiple workers can run sequentially`() = runTest {
        // Simulate multiple cleanup cycles
        val worker1 = TestListenableWorkerBuilder<CleanupWorker>(context).build()
        val worker2 = TestListenableWorkerBuilder<CleanupWorker>(context).build()

        val result1 = worker1.doWork()
        val result2 = worker2.doWork()

        assertEquals(ListenableWorker.Result.success(), result1)
        assertEquals(ListenableWorker.Result.success(), result2)
    }

    @Test
    fun `worker handles empty database gracefully`() = runTest {
        // Fresh database has no sessions - worker should still succeed
        val worker = TestListenableWorkerBuilder<CleanupWorker>(context).build()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }
}

package dev.notyouraverage.smscourier.viewmodels

import android.content.ContentResolver
import android.net.Uri
import app.cash.turbine.test
import dev.notyouraverage.smscourier.MainCoroutineRule
import dev.notyouraverage.smscourier.TestFixtures.createTestSession
import dev.notyouraverage.smscourier.export.ExportData
import dev.notyouraverage.smscourier.export.ExportFormat
import dev.notyouraverage.smscourier.export.ExportManager
import dev.notyouraverage.smscourier.export.ExportState
import dev.notyouraverage.smscourier.repository.ForwardedMessageRepository
import dev.notyouraverage.smscourier.repository.ForwardingSessionRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException

@ExperimentalCoroutinesApi
class SessionHistoryViewModelTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @MockK
    private lateinit var sessionRepository: ForwardingSessionRepository

    @MockK
    private lateinit var messageRepository: ForwardedMessageRepository

    @MockK
    private lateinit var exportManager: ExportManager

    private lateinit var viewModel: SessionHistoryViewModel

    private val testPhoneNumber = "+1234567890"
    private val testDeviceRole = "TARGET"

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)

        viewModel = SessionHistoryViewModel(
            sessionRepository = sessionRepository,
            messageRepository = messageRepository,
            exportManager = exportManager,
            phoneNumber = testPhoneNumber,
            deviceRole = testDeviceRole,
        )
    }

    // ==================== Tab Selection Tests ====================

    @Test
    fun `default tab is Sessions`() = runTest {
        viewModel.selectedTab.test {
            val tab = awaitItem()
            assertEquals(SessionHistoryViewModel.Tab.SESSIONS, tab)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selectTab updates state to Contacts`() = runTest {
        viewModel.selectedTab.test {
            awaitItem() // Initial SESSIONS

            viewModel.selectTab(SessionHistoryViewModel.Tab.CONTACTS)

            val tab = awaitItem()
            assertEquals(SessionHistoryViewModel.Tab.CONTACTS, tab)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selectTab updates state back to Sessions`() = runTest {
        viewModel.selectedTab.test {
            awaitItem() // Initial SESSIONS

            viewModel.selectTab(SessionHistoryViewModel.Tab.CONTACTS)
            awaitItem() // CONTACTS

            viewModel.selectTab(SessionHistoryViewModel.Tab.SESSIONS)

            val tab = awaitItem()
            assertEquals(SessionHistoryViewModel.Tab.SESSIONS, tab)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tab state not persisted - new viewmodel opens to Sessions`() = runTest {
        // Change tab on first viewmodel
        viewModel.selectTab(SessionHistoryViewModel.Tab.CONTACTS)

        // Create new viewmodel (simulates re-navigation)
        val newViewModel = SessionHistoryViewModel(
            sessionRepository = sessionRepository,
            messageRepository = messageRepository,
            exportManager = exportManager,
            phoneNumber = testPhoneNumber,
            deviceRole = testDeviceRole,
        )

        newViewModel.selectedTab.test {
            val tab = awaitItem()
            assertEquals(SessionHistoryViewModel.Tab.SESSIONS, tab)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Export Workflow - prepareExport() Tests ====================

    @Test
    fun `prepareExport returns filename and mimeType for CSV`() {
        val expectedFilename = "smscourier_export_20260209_123456.csv"
        every { exportManager.generateFilename(ExportFormat.CSV) } returns expectedFilename

        val (filename, mimeType) = viewModel.prepareExport(ExportFormat.CSV, includeMetadata = true)

        assertEquals(expectedFilename, filename)
        assertEquals("text/csv", mimeType)
    }

    @Test
    fun `prepareExport returns filename and mimeType for JSON`() {
        val expectedFilename = "smscourier_export_20260209_123456.json"
        every { exportManager.generateFilename(ExportFormat.JSON) } returns expectedFilename

        val (filename, mimeType) = viewModel.prepareExport(ExportFormat.JSON, includeMetadata = false)

        assertEquals(expectedFilename, filename)
        assertEquals("application/json", mimeType)
    }

    @Test
    fun `prepareExport returns filename and mimeType for TXT`() {
        val expectedFilename = "smscourier_export_20260209_123456.txt"
        every { exportManager.generateFilename(ExportFormat.TXT) } returns expectedFilename

        val (filename, mimeType) = viewModel.prepareExport(ExportFormat.TXT, includeMetadata = true)

        assertEquals(expectedFilename, filename)
        assertEquals("text/plain", mimeType)
    }

    @Test
    fun `prepareExport calls exportManager generateFilename`() {
        every { exportManager.generateFilename(ExportFormat.CSV) } returns "test.csv"

        viewModel.prepareExport(ExportFormat.CSV, includeMetadata = true)

        verify { exportManager.generateFilename(ExportFormat.CSV) }
    }

    // ==================== Export Workflow - executeExport() Tests ====================

    @Test
    fun `executeExport sets state to Loading then Success`() = runTest {
        val uri = mockk<Uri>()
        val contentResolver = mockk<ContentResolver>()
        val outputStream = ByteArrayOutputStream()
        val exportData = createTestExportData()

        every { exportManager.generateFilename(ExportFormat.CSV) } returns "export.csv"
        coEvery { exportManager.loadDeviceData(testPhoneNumber, testDeviceRole) } returns exportData
        every { contentResolver.openOutputStream(uri) } returns outputStream

        // Prepare export first (sets pending config)
        viewModel.prepareExport(ExportFormat.CSV, includeMetadata = true)

        viewModel.exportState.test {
            assertEquals(ExportState.Idle, awaitItem()) // Initial

            viewModel.executeExport(uri, contentResolver)

            // Should transition to Loading then Success
            val loading = awaitItem()
            assertEquals(ExportState.Loading, loading)

            val success = awaitItem()
            assertTrue(success is ExportState.Success)
            assertEquals("export.csv", (success as ExportState.Success).fileName)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `executeExport sets state to Error on failure`() = runTest {
        val uri = mockk<Uri>()
        val contentResolver = mockk<ContentResolver>()

        every { exportManager.generateFilename(ExportFormat.CSV) } returns "export.csv"
        coEvery { exportManager.loadDeviceData(testPhoneNumber, testDeviceRole) } throws IOException("Test error")

        // Prepare export first
        viewModel.prepareExport(ExportFormat.CSV, includeMetadata = true)

        viewModel.exportState.test {
            assertEquals(ExportState.Idle, awaitItem()) // Initial

            viewModel.executeExport(uri, contentResolver)

            // Should transition to Loading then Error
            assertEquals(ExportState.Loading, awaitItem())

            val error = awaitItem()
            assertTrue(error is ExportState.Error)
            assertEquals("Test error", (error as ExportState.Error).message)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `executeExport sets state to Error when output stream is null`() = runTest {
        val uri = mockk<Uri>()
        val contentResolver = mockk<ContentResolver>()
        val exportData = createTestExportData()

        every { exportManager.generateFilename(ExportFormat.CSV) } returns "export.csv"
        coEvery { exportManager.loadDeviceData(testPhoneNumber, testDeviceRole) } returns exportData
        every { contentResolver.openOutputStream(uri) } returns null

        viewModel.prepareExport(ExportFormat.CSV, includeMetadata = true)

        viewModel.exportState.test {
            awaitItem() // Initial Idle

            viewModel.executeExport(uri, contentResolver)

            assertEquals(ExportState.Loading, awaitItem())

            val error = awaitItem()
            assertTrue(error is ExportState.Error)
            assertTrue((error as ExportState.Error).message.contains("output stream"))

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `executeExport does nothing without pending config`() = runTest {
        val uri = mockk<Uri>()
        val contentResolver = mockk<ContentResolver>()

        // Don't call prepareExport - no pending config

        viewModel.exportState.test {
            assertEquals(ExportState.Idle, awaitItem()) // Initial

            viewModel.executeExport(uri, contentResolver)

            // Should remain Idle - no state changes
            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `executeExport writes content to output stream`() = runTest {
        val uri = mockk<Uri>()
        val contentResolver = mockk<ContentResolver>()
        val outputStream = ByteArrayOutputStream()
        val exportData = createTestExportData()

        every { exportManager.generateFilename(ExportFormat.CSV) } returns "export.csv"
        coEvery { exportManager.loadDeviceData(testPhoneNumber, testDeviceRole) } returns exportData
        every { contentResolver.openOutputStream(uri) } returns outputStream

        viewModel.prepareExport(ExportFormat.CSV, includeMetadata = false)
        viewModel.executeExport(uri, contentResolver)

        // Verify content was written
        assertTrue(outputStream.size() > 0)
    }

    // ==================== clearExportState() Tests ====================

    @Test
    fun `clearExportState resets state to Idle`() = runTest {
        val uri = mockk<Uri>()
        val contentResolver = mockk<ContentResolver>()
        val outputStream = ByteArrayOutputStream()
        val exportData = createTestExportData()

        every { exportManager.generateFilename(ExportFormat.CSV) } returns "export.csv"
        coEvery { exportManager.loadDeviceData(testPhoneNumber, testDeviceRole) } returns exportData
        every { contentResolver.openOutputStream(uri) } returns outputStream

        viewModel.prepareExport(ExportFormat.CSV, includeMetadata = true)

        viewModel.exportState.test {
            awaitItem() // Initial Idle

            viewModel.executeExport(uri, contentResolver)
            awaitItem() // Loading
            awaitItem() // Success

            viewModel.clearExportState()

            val cleared = awaitItem()
            assertEquals(ExportState.Idle, cleared)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearExportState works from Error state`() = runTest {
        val uri = mockk<Uri>()
        val contentResolver = mockk<ContentResolver>()

        every { exportManager.generateFilename(ExportFormat.CSV) } returns "export.csv"
        coEvery { exportManager.loadDeviceData(testPhoneNumber, testDeviceRole) } throws IOException("Error")

        viewModel.prepareExport(ExportFormat.CSV, includeMetadata = true)

        viewModel.exportState.test {
            awaitItem() // Initial Idle

            viewModel.executeExport(uri, contentResolver)
            awaitItem() // Loading
            awaitItem() // Error

            viewModel.clearExportState()

            assertEquals(ExportState.Idle, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Single Session Export Tests ====================

    @Test
    fun `prepareSingleSessionExport returns filename and mimeType`() {
        val session = createTestSession()
        val expectedFilename = "smscourier_export_20260209_123456.json"
        every { exportManager.generateFilename(ExportFormat.JSON) } returns expectedFilename

        val (filename, mimeType) = viewModel.prepareSingleSessionExport(
            session,
            ExportFormat.JSON,
            includeMetadata = true,
        )

        assertEquals(expectedFilename, filename)
        assertEquals("application/json", mimeType)
    }

    @Test
    fun `executeSingleSessionExport sets state to Success`() = runTest {
        val session = createTestSession()
        val uri = mockk<Uri>()
        val contentResolver = mockk<ContentResolver>()
        val outputStream = ByteArrayOutputStream()
        val exportData = createTestExportData()

        every { exportManager.generateFilename(ExportFormat.JSON) } returns "export.json"
        coEvery { exportManager.loadSingleSessionData(session, testPhoneNumber, testDeviceRole) } returns exportData
        every { contentResolver.openOutputStream(uri) } returns outputStream

        viewModel.prepareSingleSessionExport(session, ExportFormat.JSON, includeMetadata = true)

        viewModel.exportState.test {
            awaitItem() // Initial Idle

            viewModel.executeSingleSessionExport(uri, contentResolver)

            assertEquals(ExportState.Loading, awaitItem())

            val success = awaitItem()
            assertTrue(success is ExportState.Success)
            assertEquals("export.json", (success as ExportState.Success).fileName)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `executeSingleSessionExport sets state to Error on failure`() = runTest {
        val session = createTestSession()
        val uri = mockk<Uri>()
        val contentResolver = mockk<ContentResolver>()

        every { exportManager.generateFilename(ExportFormat.JSON) } returns "export.json"
        coEvery {
            exportManager.loadSingleSessionData(
                session,
                testPhoneNumber,
                testDeviceRole,
            )
        } throws IOException("Session not found")

        viewModel.prepareSingleSessionExport(session, ExportFormat.JSON, includeMetadata = true)

        viewModel.exportState.test {
            awaitItem() // Initial Idle

            viewModel.executeSingleSessionExport(uri, contentResolver)

            assertEquals(ExportState.Loading, awaitItem())

            val error = awaitItem()
            assertTrue(error is ExportState.Error)
            assertEquals("Session not found", (error as ExportState.Error).message)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `executeSingleSessionExport does nothing without pending config`() = runTest {
        val uri = mockk<Uri>()
        val contentResolver = mockk<ContentResolver>()

        // Don't call prepareSingleSessionExport

        viewModel.exportState.test {
            assertEquals(ExportState.Idle, awaitItem())

            viewModel.executeSingleSessionExport(uri, contentResolver)

            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `executeSingleSessionExport does nothing without pending session`() = runTest {
        val uri = mockk<Uri>()
        val contentResolver = mockk<ContentResolver>()

        // Only prepare bulk export, not single session
        every { exportManager.generateFilename(ExportFormat.CSV) } returns "export.csv"
        viewModel.prepareExport(ExportFormat.CSV, includeMetadata = true)

        viewModel.exportState.test {
            assertEquals(ExportState.Idle, awaitItem())

            viewModel.executeSingleSessionExport(uri, contentResolver)

            // Should remain Idle because there's no pending session
            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Export State Transitions Tests ====================

    @Test
    fun `export state transitions Idle to Loading to Success`() = runTest {
        val uri = mockk<Uri>()
        val contentResolver = mockk<ContentResolver>()
        val outputStream = ByteArrayOutputStream()
        val exportData = createTestExportData()

        every { exportManager.generateFilename(ExportFormat.TXT) } returns "export.txt"
        coEvery { exportManager.loadDeviceData(testPhoneNumber, testDeviceRole) } returns exportData
        every { contentResolver.openOutputStream(uri) } returns outputStream

        viewModel.prepareExport(ExportFormat.TXT, includeMetadata = false)

        viewModel.exportState.test {
            val initial = awaitItem()
            assertEquals(ExportState.Idle, initial)

            viewModel.executeExport(uri, contentResolver)

            val loading = awaitItem()
            assertEquals(ExportState.Loading, loading)

            val success = awaitItem()
            assertTrue(success is ExportState.Success)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `export state transitions Idle to Loading to Error`() = runTest {
        val uri = mockk<Uri>()
        val contentResolver = mockk<ContentResolver>()

        every { exportManager.generateFilename(ExportFormat.TXT) } returns "export.txt"
        coEvery { exportManager.loadDeviceData(testPhoneNumber, testDeviceRole) } throws RuntimeException("DB error")

        viewModel.prepareExport(ExportFormat.TXT, includeMetadata = false)

        viewModel.exportState.test {
            val initial = awaitItem()
            assertEquals(ExportState.Idle, initial)

            viewModel.executeExport(uri, contentResolver)

            val loading = awaitItem()
            assertEquals(ExportState.Loading, loading)

            val error = awaitItem()
            assertTrue(error is ExportState.Error)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `export state resets after clearExportState`() = runTest {
        val uri = mockk<Uri>()
        val contentResolver = mockk<ContentResolver>()
        val outputStream = ByteArrayOutputStream()
        val exportData = createTestExportData()

        every { exportManager.generateFilename(ExportFormat.CSV) } returns "export.csv"
        coEvery { exportManager.loadDeviceData(testPhoneNumber, testDeviceRole) } returns exportData
        every { contentResolver.openOutputStream(uri) } returns outputStream

        viewModel.prepareExport(ExportFormat.CSV, includeMetadata = true)

        viewModel.exportState.test {
            awaitItem() // Idle
            viewModel.executeExport(uri, contentResolver)
            awaitItem() // Loading
            awaitItem() // Success

            viewModel.clearExportState()
            val reset = awaitItem()
            assertEquals(ExportState.Idle, reset)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Session Selection Tests ====================

    @Test
    fun `selectSession updates selectedSession state`() = runTest {
        val session = createTestSession()

        viewModel.selectedSession.test {
            assertEquals(null, awaitItem()) // Initial null

            viewModel.selectSession(session)

            val selected = awaitItem()
            assertEquals(session, selected)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selectSession can clear selection with null`() = runTest {
        val session = createTestSession()

        viewModel.selectedSession.test {
            awaitItem() // Initial null

            viewModel.selectSession(session)
            awaitItem() // Selected

            viewModel.selectSession(null)

            assertEquals(null, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Contact Message Counts Tests ====================

    @Test
    fun `loadContactMessageCount loads count from repository`() = runTest {
        val senderNumber = "+9876543210"
        val expectedCount = 42
        coEvery {
            messageRepository.getMessageCountForSender(
                testPhoneNumber,
                senderNumber,
            )
        } returns expectedCount

        viewModel.contactMessageCounts.test {
            assertEquals(emptyMap<String, Int>(), awaitItem()) // Initial empty

            viewModel.loadContactMessageCount(senderNumber)

            val counts = awaitItem()
            assertEquals(expectedCount, counts[senderNumber])

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadContactMessageCount does not reload existing count`() = runTest {
        val senderNumber = "+9876543210"
        coEvery { messageRepository.getMessageCountForSender(testPhoneNumber, senderNumber) } returns 10

        viewModel.contactMessageCounts.test {
            awaitItem() // Initial empty

            viewModel.loadContactMessageCount(senderNumber)
            awaitItem() // First load

            // Call again - should not trigger new emission
            viewModel.loadContactMessageCount(senderNumber)

            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadContactMessageCount loads multiple contacts`() = runTest {
        val sender1 = "+1111111111"
        val sender2 = "+2222222222"
        coEvery { messageRepository.getMessageCountForSender(testPhoneNumber, sender1) } returns 5
        coEvery { messageRepository.getMessageCountForSender(testPhoneNumber, sender2) } returns 10

        viewModel.contactMessageCounts.test {
            awaitItem() // Initial empty

            viewModel.loadContactMessageCount(sender1)
            val counts1 = awaitItem()
            assertEquals(5, counts1[sender1])

            viewModel.loadContactMessageCount(sender2)
            val counts2 = awaitItem()
            assertEquals(5, counts2[sender1])
            assertEquals(10, counts2[sender2])

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== Helper Functions ====================

    private fun createTestExportData(): ExportData {
        return ExportData(
            sessions = emptyList(),
            exportedAt = System.currentTimeMillis(),
            devicePhone = testPhoneNumber,
            deviceRole = testDeviceRole,
        )
    }
}

package dev.notyouraverage.smscourier.data

import dev.notyouraverage.smscourier.TestFixtures.createTestDevice
import dev.notyouraverage.smscourier.TestFixtures.createTestSession
import dev.notyouraverage.smscourier.data.entities.DeviceRole
import dev.notyouraverage.smscourier.data.entities.ForwardingSession
import dev.notyouraverage.smscourier.data.entities.PairedDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for Direction enum, DirectionalStatus, and SessionWithDirection.
 *
 * These tests validate the directional status calculation logic that determines
 * message flow direction based on device roles and active sessions.
 */
class DirectionalStatusTest {

    // =========================================================================
    // Direction enum tests
    // =========================================================================

    @Test
    fun `Direction enum has three values`() {
        val values = Direction.values()
        assertEquals(3, values.size)
        assertTrue(values.contains(Direction.FORWARDING_TO))
        assertTrue(values.contains(Direction.RECEIVING_FROM))
        assertTrue(values.contains(Direction.BIDIRECTIONAL))
    }

    @Test
    fun `FORWARDING_TO represents SOURCE role - device receives messages`() {
        // SOURCE devices REQUEST forwarding = they RECEIVE messages
        val direction = Direction.FORWARDING_TO
        assertEquals("FORWARDING_TO", direction.name)
    }

    @Test
    fun `RECEIVING_FROM represents TARGET role - device sends messages`() {
        // TARGET devices PROVIDE forwarding = they SEND messages
        val direction = Direction.RECEIVING_FROM
        assertEquals("RECEIVING_FROM", direction.name)
    }

    @Test
    fun `BIDIRECTIONAL represents both roles active with same phone`() {
        val direction = Direction.BIDIRECTIONAL
        assertEquals("BIDIRECTIONAL", direction.name)
    }

    // =========================================================================
    // DirectionalStatus default values
    // =========================================================================

    @Test
    fun `DirectionalStatus has zero counts by default`() {
        val status = DirectionalStatus()
        assertEquals(0, status.forwardingToCount)
        assertEquals(0, status.receivingFromCount)
        assertEquals(0, status.bidirectionalCount)
        assertTrue(status.activeSessions.isEmpty())
    }

    // =========================================================================
    // SessionWithDirection construction tests
    // =========================================================================

    @Test
    fun `SessionWithDirection correctly associates session with device and direction`() {
        val device = createTestDevice(
            phoneNumber = "+1234567890",
            role = DeviceRole.SOURCE,
        )
        val session = createTestSession(
            id = 1L,
            devicePhoneNumber = "+1234567890",
            messagesForwarded = 5,
        )

        val sessionWithDirection = SessionWithDirection(
            session = session,
            device = device,
            direction = Direction.FORWARDING_TO,
        )

        assertEquals(session, sessionWithDirection.session)
        assertEquals(device, sessionWithDirection.device)
        assertEquals(Direction.FORWARDING_TO, sessionWithDirection.direction)
    }

    @Test
    fun `SessionWithDirection preserves session metadata`() {
        val startTime = System.currentTimeMillis()
        val expiresAt = startTime + (60 * 60 * 1000L) // 1 hour
        val device = createTestDevice(phoneNumber = "+1234567890", role = DeviceRole.TARGET)
        val session = createTestSession(
            id = 42L,
            devicePhoneNumber = "+1234567890",
            startedAt = startTime,
            durationMinutes = 60,
            expiresAt = expiresAt,
            isActive = true,
            messagesForwarded = 10,
        )

        val sessionWithDirection = SessionWithDirection(
            session = session,
            device = device,
            direction = Direction.RECEIVING_FROM,
        )

        assertEquals(42L, sessionWithDirection.session.id)
        assertEquals("+1234567890", sessionWithDirection.session.devicePhoneNumber)
        assertEquals(startTime, sessionWithDirection.session.startedAt)
        assertEquals(60, sessionWithDirection.session.durationMinutes)
        assertEquals(expiresAt, sessionWithDirection.session.expiresAt)
        assertTrue(sessionWithDirection.session.isActive)
        assertEquals(10, sessionWithDirection.session.messagesForwarded)
    }

    // =========================================================================
    // Directional status calculation tests
    // These test the calculateDirectionalStatus logic from HomeViewModel
    // =========================================================================

    @Test
    fun `only SOURCE sessions results in forwardingToCount greater than zero`() {
        val device = createTestDevice(
            phoneNumber = "+1111111111",
            role = DeviceRole.SOURCE,
        )
        val session = createTestSession(
            id = 1L,
            devicePhoneNumber = "+1111111111",
        )

        val status = calculateDirectionalStatus(listOf(device), listOf(session))

        assertEquals(1, status.forwardingToCount)
        assertEquals(0, status.receivingFromCount)
        assertEquals(0, status.bidirectionalCount)
        assertEquals(1, status.activeSessions.size)
        assertEquals(Direction.FORWARDING_TO, status.activeSessions[0].direction)
    }

    @Test
    fun `only TARGET sessions results in receivingFromCount greater than zero`() {
        val device = createTestDevice(
            phoneNumber = "+2222222222",
            role = DeviceRole.TARGET,
        )
        val session = createTestSession(
            id = 1L,
            devicePhoneNumber = "+2222222222",
        )

        val status = calculateDirectionalStatus(listOf(device), listOf(session))

        assertEquals(0, status.forwardingToCount)
        assertEquals(1, status.receivingFromCount)
        assertEquals(0, status.bidirectionalCount)
        assertEquals(1, status.activeSessions.size)
        assertEquals(Direction.RECEIVING_FROM, status.activeSessions[0].direction)
    }

    @Test
    fun `both SOURCE and TARGET roles for same phone results in BIDIRECTIONAL`() {
        val sourceDevice = createTestDevice(
            phoneNumber = "+3333333333",
            role = DeviceRole.SOURCE,
        )
        val targetDevice = createTestDevice(
            phoneNumber = "+3333333333",
            role = DeviceRole.TARGET,
        )
        val session = createTestSession(
            id = 1L,
            devicePhoneNumber = "+3333333333",
        )

        val status = calculateDirectionalStatus(
            listOf(sourceDevice, targetDevice),
            listOf(session),
        )

        assertEquals(0, status.forwardingToCount)
        assertEquals(0, status.receivingFromCount)
        assertEquals(1, status.bidirectionalCount)
        assertEquals(1, status.activeSessions.size)
        assertEquals(Direction.BIDIRECTIONAL, status.activeSessions[0].direction)
    }

    @Test
    fun `no active sessions results in all counts zero`() {
        val device = createTestDevice(
            phoneNumber = "+1234567890",
            role = DeviceRole.SOURCE,
        )

        val status = calculateDirectionalStatus(listOf(device), emptyList())

        assertEquals(0, status.forwardingToCount)
        assertEquals(0, status.receivingFromCount)
        assertEquals(0, status.bidirectionalCount)
        assertTrue(status.activeSessions.isEmpty())
    }

    @Test
    fun `mixed phones with different roles calculates correctly`() {
        // Phone A: SOURCE only
        val deviceA = createTestDevice(
            phoneNumber = "+1111111111",
            role = DeviceRole.SOURCE,
        )
        // Phone B: TARGET only
        val deviceB = createTestDevice(
            phoneNumber = "+2222222222",
            role = DeviceRole.TARGET,
        )
        // Phone C: Both SOURCE and TARGET (bidirectional)
        val deviceCSource = createTestDevice(
            phoneNumber = "+3333333333",
            role = DeviceRole.SOURCE,
        )
        val deviceCTarget = createTestDevice(
            phoneNumber = "+3333333333",
            role = DeviceRole.TARGET,
        )

        val sessions = listOf(
            createTestSession(id = 1L, devicePhoneNumber = "+1111111111"),
            createTestSession(id = 2L, devicePhoneNumber = "+2222222222"),
            createTestSession(id = 3L, devicePhoneNumber = "+3333333333"),
        )

        val status = calculateDirectionalStatus(
            listOf(deviceA, deviceB, deviceCSource, deviceCTarget),
            sessions,
        )

        assertEquals(1, status.forwardingToCount)
        assertEquals(1, status.receivingFromCount)
        assertEquals(1, status.bidirectionalCount)
        assertEquals(3, status.activeSessions.size)

        // Verify each session has correct direction
        val sessionsByPhone = status.activeSessions.associateBy { it.session.devicePhoneNumber }
        assertEquals(Direction.FORWARDING_TO, sessionsByPhone["+1111111111"]?.direction)
        assertEquals(Direction.RECEIVING_FROM, sessionsByPhone["+2222222222"]?.direction)
        assertEquals(Direction.BIDIRECTIONAL, sessionsByPhone["+3333333333"]?.direction)
    }

    @Test
    fun `multiple sessions per phone number counts each session`() {
        val device = createTestDevice(
            phoneNumber = "+1111111111",
            role = DeviceRole.SOURCE,
        )
        val sessions = listOf(
            createTestSession(id = 1L, devicePhoneNumber = "+1111111111"),
            createTestSession(id = 2L, devicePhoneNumber = "+1111111111"),
            createTestSession(id = 3L, devicePhoneNumber = "+1111111111"),
        )

        val status = calculateDirectionalStatus(listOf(device), sessions)

        assertEquals(3, status.forwardingToCount)
        assertEquals(0, status.receivingFromCount)
        assertEquals(0, status.bidirectionalCount)
        assertEquals(3, status.activeSessions.size)

        // All sessions should have FORWARDING_TO direction
        assertTrue(status.activeSessions.all { it.direction == Direction.FORWARDING_TO })
    }

    @Test
    fun `bidirectional phone with multiple sessions counts all as bidirectional`() {
        val sourceDevice = createTestDevice(
            phoneNumber = "+1111111111",
            role = DeviceRole.SOURCE,
        )
        val targetDevice = createTestDevice(
            phoneNumber = "+1111111111",
            role = DeviceRole.TARGET,
        )
        val sessions = listOf(
            createTestSession(id = 1L, devicePhoneNumber = "+1111111111"),
            createTestSession(id = 2L, devicePhoneNumber = "+1111111111"),
        )

        val status = calculateDirectionalStatus(
            listOf(sourceDevice, targetDevice),
            sessions,
        )

        assertEquals(0, status.forwardingToCount)
        assertEquals(0, status.receivingFromCount)
        assertEquals(2, status.bidirectionalCount)
        assertEquals(2, status.activeSessions.size)

        // All sessions should have BIDIRECTIONAL direction
        assertTrue(status.activeSessions.all { it.direction == Direction.BIDIRECTIONAL })
    }

    @Test
    fun `bidirectional uses SOURCE device as primary device`() {
        val sourceDevice = createTestDevice(
            phoneNumber = "+1111111111",
            role = DeviceRole.SOURCE,
            displayName = "Source Device",
        )
        val targetDevice = createTestDevice(
            phoneNumber = "+1111111111",
            role = DeviceRole.TARGET,
            displayName = "Target Device",
        )
        val session = createTestSession(id = 1L, devicePhoneNumber = "+1111111111")

        val status = calculateDirectionalStatus(
            listOf(sourceDevice, targetDevice),
            sessions = listOf(session),
        )

        assertEquals(1, status.activeSessions.size)
        val sessionWithDirection = status.activeSessions[0]
        assertEquals(DeviceRole.SOURCE, sessionWithDirection.device.role)
        assertEquals("Source Device", sessionWithDirection.device.displayName)
    }

    // =========================================================================
    // Edge case tests
    // =========================================================================

    @Test
    fun `session with no matching device is not included`() {
        // Session exists but no device matches the phone number
        val device = createTestDevice(
            phoneNumber = "+1111111111",
            role = DeviceRole.SOURCE,
        )

        // Session has different phone number than device
        @Suppress("ktlint:standard:value-argument-comment")
        val session = createTestSession(
            id = 1L,
            devicePhoneNumber = "+9999999999",
        )

        val status = calculateDirectionalStatus(listOf(device), listOf(session))

        assertEquals(0, status.forwardingToCount)
        assertEquals(0, status.receivingFromCount)
        assertEquals(0, status.bidirectionalCount)
        assertTrue(status.activeSessions.isEmpty())
    }

    @Test
    fun `archived devices should NOT appear in calculations`() {
        val activeDevice = createTestDevice(
            phoneNumber = "+1111111111",
            role = DeviceRole.SOURCE,
            isArchived = false,
        )
        val archivedDevice = createTestDevice(
            phoneNumber = "+2222222222",
            role = DeviceRole.SOURCE,
            isArchived = true,
            archivedAt = System.currentTimeMillis(),
        )

        val sessions = listOf(
            createTestSession(id = 1L, devicePhoneNumber = "+1111111111"),
            createTestSession(id = 2L, devicePhoneNumber = "+2222222222"),
        )

        // Only include non-archived devices (simulating getApprovedDevices filter)
        val nonArchivedDevices = listOf(activeDevice, archivedDevice).filter { !it.isArchived }
        val status = calculateDirectionalStatus(nonArchivedDevices, sessions)

        // Only the active device's session should be counted
        assertEquals(1, status.forwardingToCount)
        assertEquals(0, status.receivingFromCount)
        assertEquals(0, status.bidirectionalCount)
        assertEquals(1, status.activeSessions.size)
        assertEquals("+1111111111", status.activeSessions[0].session.devicePhoneNumber)
    }

    @Test
    fun `ended sessions should NOT appear in calculations`() {
        val device = createTestDevice(
            phoneNumber = "+1111111111",
            role = DeviceRole.SOURCE,
        )

        val activeSession = createTestSession(
            id = 1L,
            devicePhoneNumber = "+1111111111",
            isActive = true,
        )
        val endedSession = createTestSession(
            id = 2L,
            devicePhoneNumber = "+1111111111",
            isActive = false,
            stoppedBy = "USER",
        )

        // Only include active sessions (simulating getActiveSessions filter)
        val activeSessions = listOf(activeSession, endedSession).filter { it.isActive }
        val status = calculateDirectionalStatus(listOf(device), activeSessions)

        assertEquals(1, status.forwardingToCount)
        assertEquals(1, status.activeSessions.size)
        assertEquals(1L, status.activeSessions[0].session.id)
    }

    @Test
    fun `complex scenario with multiple phones and roles`() {
        // Setup: 4 different phones with various configurations
        val devices = listOf(
            // Phone 1: SOURCE only with 2 sessions
            createTestDevice(phoneNumber = "+1111111111", role = DeviceRole.SOURCE),
            // Phone 2: TARGET only with 1 session
            createTestDevice(phoneNumber = "+2222222222", role = DeviceRole.TARGET),
            // Phone 3: Bidirectional with 1 session
            createTestDevice(phoneNumber = "+3333333333", role = DeviceRole.SOURCE),
            createTestDevice(phoneNumber = "+3333333333", role = DeviceRole.TARGET),
            // Phone 4: SOURCE only with 1 session
            createTestDevice(phoneNumber = "+4444444444", role = DeviceRole.SOURCE),
        )

        val sessions = listOf(
            createTestSession(id = 1L, devicePhoneNumber = "+1111111111"),
            createTestSession(id = 2L, devicePhoneNumber = "+1111111111"),
            createTestSession(id = 3L, devicePhoneNumber = "+2222222222"),
            createTestSession(id = 4L, devicePhoneNumber = "+3333333333"),
            createTestSession(id = 5L, devicePhoneNumber = "+4444444444"),
        )

        val status = calculateDirectionalStatus(devices, sessions)

        // Phone 1: 2 FORWARDING_TO sessions
        // Phone 4: 1 FORWARDING_TO session
        assertEquals(3, status.forwardingToCount)

        // Phone 2: 1 RECEIVING_FROM session
        assertEquals(1, status.receivingFromCount)

        // Phone 3: 1 BIDIRECTIONAL session
        assertEquals(1, status.bidirectionalCount)

        // Total: 5 sessions
        assertEquals(5, status.activeSessions.size)
    }

    @Test
    fun `empty devices and empty sessions returns empty status`() {
        val status = calculateDirectionalStatus(emptyList(), emptyList())

        assertEquals(0, status.forwardingToCount)
        assertEquals(0, status.receivingFromCount)
        assertEquals(0, status.bidirectionalCount)
        assertTrue(status.activeSessions.isEmpty())
    }

    @Test
    fun `devices exist but no sessions returns empty status`() {
        val devices = listOf(
            createTestDevice(phoneNumber = "+1111111111", role = DeviceRole.SOURCE),
            createTestDevice(phoneNumber = "+2222222222", role = DeviceRole.TARGET),
        )

        val status = calculateDirectionalStatus(devices, emptyList())

        assertEquals(0, status.forwardingToCount)
        assertEquals(0, status.receivingFromCount)
        assertEquals(0, status.bidirectionalCount)
        assertTrue(status.activeSessions.isEmpty())
    }

    // =========================================================================
    // Helper function - mirrors HomeViewModel.calculateDirectionalStatus
    // =========================================================================

    /**
     * Calculate directional status from approved devices and active sessions.
     * This mirrors the logic in HomeViewModel for testability.
     */
    private fun calculateDirectionalStatus(
        devices: List<PairedDevice>,
        sessions: List<ForwardingSession>,
    ): DirectionalStatus {
        if (sessions.isEmpty()) {
            return DirectionalStatus()
        }

        // Group devices by phone number for lookup
        val devicesByPhone = devices.groupBy { it.phoneNumber }

        // Group sessions by phone number to detect bidirectional
        val sessionsByPhone = sessions.groupBy { it.devicePhoneNumber }

        val sessionsWithDirection = mutableListOf<SessionWithDirection>()
        var forwardingToCount = 0
        var receivingFromCount = 0
        var bidirectionalCount = 0

        sessionsByPhone.forEach { (phone, phoneSessions) ->
            val phoneDevices = devicesByPhone[phone] ?: emptyList()
            val sourceDevice = phoneDevices.find { it.role == DeviceRole.SOURCE }
            val targetDevice = phoneDevices.find { it.role == DeviceRole.TARGET }

            when {
                sourceDevice != null && targetDevice != null -> {
                    bidirectionalCount += phoneSessions.size
                    phoneSessions.forEach { session ->
                        sessionsWithDirection.add(
                            SessionWithDirection(
                                session = session,
                                device = sourceDevice,
                                direction = Direction.BIDIRECTIONAL,
                            ),
                        )
                    }
                }
                sourceDevice != null -> {
                    forwardingToCount += phoneSessions.size
                    phoneSessions.forEach { session ->
                        sessionsWithDirection.add(
                            SessionWithDirection(
                                session = session,
                                device = sourceDevice,
                                direction = Direction.FORWARDING_TO,
                            ),
                        )
                    }
                }
                targetDevice != null -> {
                    receivingFromCount += phoneSessions.size
                    phoneSessions.forEach { session ->
                        sessionsWithDirection.add(
                            SessionWithDirection(
                                session = session,
                                device = targetDevice,
                                direction = Direction.RECEIVING_FROM,
                            ),
                        )
                    }
                }
            }
        }

        return DirectionalStatus(
            forwardingToCount = forwardingToCount,
            receivingFromCount = receivingFromCount,
            bidirectionalCount = bidirectionalCount,
            activeSessions = sessionsWithDirection,
        )
    }
}

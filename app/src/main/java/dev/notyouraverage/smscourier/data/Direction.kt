package dev.notyouraverage.smscourier.data

import dev.notyouraverage.smscourier.data.entities.ForwardingSession
import dev.notyouraverage.smscourier.data.entities.PairedDevice

/**
 * Represents the direction of message flow from THIS device's perspective.
 *
 * CRITICAL SEMANTIC NOTE:
 * - SOURCE devices REQUEST forwarding = they RECEIVE messages from TARGET devices (arrow down)
 * - TARGET devices PROVIDE forwarding = they SEND messages to SOURCE devices (arrow up)
 *
 * Mental model:
 * - FORWARDING_TO: This device forwards messages TO others (TARGET role, sending, arrow up)
 * - RECEIVING_FROM: This device receives messages FROM others (SOURCE role, receiving, arrow down)
 * - BIDIRECTIONAL: Both directions active with same phone number
 */
enum class Direction {
    /**
     * This device is TARGET role - forwarding messages TO source device(s).
     * Messages flow FROM this device (arrow up).
     */
    FORWARDING_TO,

    /**
     * This device is SOURCE role - receiving messages FROM target device(s).
     * Messages flow TO this device (arrow down).
     */
    RECEIVING_FROM,

    /**
     * Both directions active with same phone number.
     * This device has both SOURCE and TARGET roles with active sessions.
     */
    BIDIRECTIONAL,
}

/**
 * Associates a forwarding session with its paired device and direction.
 */
data class SessionWithDirection(
    val session: ForwardingSession,
    val device: PairedDevice,
    val direction: Direction,
)

/**
 * Aggregated directional status for active forwarding sessions.
 *
 * Counts represent the number of active sessions in each direction:
 * - forwardingToCount: Sessions where this device FORWARDS messages (TARGET role)
 * - receivingFromCount: Sessions where this device RECEIVES messages (SOURCE role)
 * - bidirectionalCount: Sessions where both directions are active with same phone
 */
data class DirectionalStatus(
    val forwardingToCount: Int = 0,
    val receivingFromCount: Int = 0,
    val bidirectionalCount: Int = 0,
    val activeSessions: List<SessionWithDirection> = emptyList(),
)

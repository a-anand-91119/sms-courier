package dev.notyouraverage.smscourier.composables.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.notyouraverage.smscourier.data.Direction
import dev.notyouraverage.smscourier.data.SessionWithDirection
import dev.notyouraverage.smscourier.data.entities.ForwardingSession

/**
 * Bottom sheet displaying active sessions grouped by direction.
 * Shows Receiving From, Forwarding To, and Bidirectional sections
 * with stop buttons for each session.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionBreakdownBottomSheet(
    sessions: List<SessionWithDirection>,
    onDismiss: () -> Unit,
    onStopSession: (ForwardingSession) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Active Sessions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            if (sessions.isEmpty()) {
                EmptySessionState()
            } else {
                // Group by direction
                val forwardingTo = sessions.filter { it.direction == Direction.FORWARDING_TO }
                val receivingFrom = sessions.filter { it.direction == Direction.RECEIVING_FROM }
                val bidirectional = sessions.filter { it.direction == Direction.BIDIRECTIONAL }

                // "Receiving From" = FORWARDING_TO direction (messages coming TO this device)
                if (forwardingTo.isNotEmpty()) {
                    SessionGroup(
                        title = "Receiving From (${forwardingTo.size})",
                        sessions = forwardingTo,
                        color = MaterialTheme.colorScheme.primary,
                        onStopSession = onStopSession,
                    )
                }

                // "Forwarding To" = RECEIVING_FROM direction (messages going FROM this device)
                if (receivingFrom.isNotEmpty()) {
                    SessionGroup(
                        title = "Forwarding To (${receivingFrom.size})",
                        sessions = receivingFrom,
                        color = MaterialTheme.colorScheme.tertiary,
                        onStopSession = onStopSession,
                    )
                }

                if (bidirectional.isNotEmpty()) {
                    SessionGroup(
                        title = "Bidirectional (${bidirectional.size})",
                        sessions = bidirectional,
                        color = MaterialTheme.colorScheme.secondary,
                        onStopSession = onStopSession,
                    )
                }
            }
        }
    }
}

/**
 * Section displaying sessions of a specific direction type.
 */
@Composable
fun SessionGroup(
    title: String,
    sessions: List<SessionWithDirection>,
    color: Color,
    onStopSession: (ForwardingSession) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = color,
        )

        sessions.forEach { sessionWithDir ->
            SessionRow(
                phoneNumber = sessionWithDir.session.devicePhoneNumber,
                messageCount = sessionWithDir.session.messageCount,
                onStop = { onStopSession(sessionWithDir.session) },
            )
        }
    }
}

/**
 * Individual session row showing phone number, message count, and stop button.
 */
@Composable
fun SessionRow(
    phoneNumber: String,
    messageCount: Int,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "$messageCount messages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            TextButton(onClick = onStop) {
                Text("Stop")
            }
        }
    }
}

/**
 * Empty state shown when no active forwarding sessions exist.
 */
@Composable
fun EmptySessionState(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "No active forwarding sessions",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Start forwarding from a paired device to see sessions here",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
    }
}

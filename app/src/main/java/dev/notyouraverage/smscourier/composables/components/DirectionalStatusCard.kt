package dev.notyouraverage.smscourier.composables.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Card displaying directional forwarding status indicators.
 *
 * Shows at-a-glance whether this device is:
 * - Receiving from (arrow down): RECEIVING_FROM direction = SOURCE role, messages flow TO this device
 * - Forwarding to (arrow up): FORWARDING_TO direction = TARGET role, messages flow FROM this device
 *
 * Bidirectional sessions are merged into both counts. On large screens (>=400dp), shows both
 * direction indicators (grayed out when zero). On small screens (<400dp), shows compact text-only
 * list, hiding zero-count directions.
 *
 * Tapping the card triggers [onCardClick] to open the session breakdown sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectionalStatusCard(
    forwardingToCount: Int,
    receivingFromCount: Int,
    bidirectionalCount: Int,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Merge bidirectional count into both directions
    val effectiveForwardingTo = forwardingToCount + bidirectionalCount
    val effectiveReceivingFrom = receivingFromCount + bidirectionalCount

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        onClick = onCardClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Active Forwarding",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth >= 400.dp) {
                    // Large screen: horizontal layout with icons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Receiving from (arrow down) - this device is SOURCE, receiving messages
                        // Uses primary color (green-ish in Material You)
                        DirectionalIndicator(
                            icon = Icons.Default.KeyboardArrowDown,
                            count = effectiveReceivingFrom,
                            label = "Receiving from",
                            color = MaterialTheme.colorScheme.primary,
                            isActive = effectiveReceivingFrom > 0,
                            modifier = Modifier.weight(1f),
                        )

                        // Forwarding to (arrow up) - this device is TARGET, sending messages
                        // Uses tertiary color (blue-ish in Material You)
                        DirectionalIndicator(
                            icon = Icons.Default.KeyboardArrowUp,
                            count = effectiveForwardingTo,
                            label = "Forwarding to",
                            color = MaterialTheme.colorScheme.tertiary,
                            isActive = effectiveForwardingTo > 0,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    // Small screen: compact vertical layout, text-only, hide zero counts
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (effectiveForwardingTo > 0) {
                            Text(
                                text = "Forwarding to: $effectiveForwardingTo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                        if (effectiveReceivingFrom > 0) {
                            Text(
                                text = "Receiving from: $effectiveReceivingFrom",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (effectiveForwardingTo == 0 && effectiveReceivingFrom == 0) {
                            Text(
                                text = "No active sessions",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual directional indicator with icon, badge, and label.
 *
 * Uses [BadgedBox] to display count badge when active (count > 0).
 * Inactive indicators are grayed out using alpha transparency.
 */
@Composable
fun DirectionalIndicator(
    icon: ImageVector,
    count: Int,
    label: String,
    color: Color,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BadgedBox(
            badge = {
                if (count > 0) {
                    Badge(
                        containerColor = color,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Text(formatBadgeCount(count))
                    }
                }
            },
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(32.dp),
                tint = if (isActive) {
                    color
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                },
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) {
                color
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            },
        )
    }
}

/**
 * Formats count for badge display, abbreviating large numbers.
 *
 * - 0-99: Shows exact count
 * - 100-999: Shows "99+"
 * - 1000+: Shows "Xk+" format (e.g., "1k+", "2k+")
 */
private fun formatBadgeCount(count: Int): String = when {
    count <= 99 -> count.toString()
    count <= 999 -> "99+"
    else -> "${count / 1000}k+"
}

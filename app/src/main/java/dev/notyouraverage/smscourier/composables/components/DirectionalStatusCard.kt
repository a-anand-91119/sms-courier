package dev.notyouraverage.smscourier.composables.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
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
 * - Receiving (arrow down): forwardingToCount sessions where this device receives messages
 * - Forwarding (arrow up): receivingFromCount sessions where this device sends messages
 * - Bidirectional (swap vert): bidirectionalCount sessions with both directions active
 *
 * Inactive directions (zero count) are grayed out but always visible.
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Receiving (arrow down) - this device is SOURCE, receiving messages
                // Uses primary color (green-ish in Material You)
                DirectionalIndicator(
                    icon = Icons.Default.KeyboardArrowDown,
                    count = forwardingToCount,
                    label = "Receiving",
                    color = MaterialTheme.colorScheme.primary,
                    isActive = forwardingToCount > 0,
                    modifier = Modifier.weight(1f),
                )

                // Forwarding (arrow up) - this device is TARGET, sending messages
                // Uses tertiary color (blue-ish in Material You)
                DirectionalIndicator(
                    icon = Icons.Default.KeyboardArrowUp,
                    count = receivingFromCount,
                    label = "Forwarding",
                    color = MaterialTheme.colorScheme.tertiary,
                    isActive = receivingFromCount > 0,
                    modifier = Modifier.weight(1f),
                )

                // Bidirectional (refresh) - both directions active
                // Uses secondary color (purple-ish in Material You)
                // IMPORTANT: Label is "Bidirectional" per CONTEXT.md locked decision
                DirectionalIndicator(
                    icon = Icons.Default.Refresh,
                    count = bidirectionalCount,
                    label = "Bidirectional",
                    color = MaterialTheme.colorScheme.secondary,
                    isActive = bidirectionalCount > 0,
                    modifier = Modifier.weight(1f),
                )
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

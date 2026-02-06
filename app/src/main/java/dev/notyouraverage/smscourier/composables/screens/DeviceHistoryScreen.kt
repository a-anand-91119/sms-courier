package dev.notyouraverage.smscourier.composables.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.notyouraverage.smscourier.composables.components.CollapsibleSectionHeader
import dev.notyouraverage.smscourier.composables.components.DeviceHistoryCard
import dev.notyouraverage.smscourier.composables.components.SkeletonDeviceCard
import dev.notyouraverage.smscourier.data.entities.PairedDevice
import dev.notyouraverage.smscourier.viewmodels.DeviceHistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceHistoryScreen(
    viewModel: DeviceHistoryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSessionHistory: (phoneNumber: String, role: String) -> Unit,
    onNavigateToArchiveManagement: (phoneNumber: String, role: String) -> Unit,
) {
    val activeDevices by viewModel.activeDevices.collectAsState()
    val removedDevices by viewModel.removedDevices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showRemovedSection by remember { mutableStateOf(false) }
    var selectedActiveDevice by remember { mutableStateOf<PairedDevice?>(null) }
    var selectedRemovedDevice by remember { mutableStateOf<PairedDevice?>(null) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Device History",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { paddingValues ->
        when {
            isLoading -> {
                // Loading state with skeleton cards
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(3) {
                        SkeletonDeviceCard()
                    }
                }
            }
            activeDevices.isEmpty() && removedDevices.isEmpty() -> {
                // Empty state - no devices at all
                EmptyDeviceHistoryState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }
            else -> {
                // Device list with sections
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Active Devices Section Header
                    if (activeDevices.isNotEmpty()) {
                        item {
                            Text(
                                text = "Active Devices (${activeDevices.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }

                        items(
                            activeDevices,
                            key = { "${it.device.phoneNumber}_${it.device.role}" },
                        ) { deviceWithSession ->
                            DeviceHistoryCard(
                                device = deviceWithSession.device,
                                hasActiveSession = deviceWithSession.hasActiveSession,
                                onClick = { selectedActiveDevice = deviceWithSession.device },
                            )
                        }
                    }

                    // Removed Devices Section (collapsible, per CONTEXT.md)
                    // Only show if there are removed devices (Claude's discretion: hide header when empty)
                    if (removedDevices.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            CollapsibleSectionHeader(
                                title = "Removed Devices",
                                count = removedDevices.size,
                                isExpanded = showRemovedSection,
                                onToggle = { showRemovedSection = !showRemovedSection },
                            )
                        }

                        if (showRemovedSection) {
                            items(
                                removedDevices,
                                key = { "${it.phoneNumber}_${it.role}" },
                            ) { device ->
                                DeviceHistoryCard(
                                    device = device,
                                    hasActiveSession = false, // Removed devices can't have active sessions
                                    onClick = { selectedRemovedDevice = device },
                                    isMuted = true, // Per CONTEXT.md: lower opacity
                                )
                            }
                        }
                    }

                    // Bottom padding for scroll
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }

    // Bottom sheet for active device details
    selectedActiveDevice?.let { device ->
        DeviceDetailBottomSheet(
            device = device,
            onDismiss = { selectedActiveDevice = null },
            onViewSessions = {
                selectedActiveDevice = null
                onNavigateToSessionHistory(device.phoneNumber, device.role.name)
            },
            onUnpair = {
                // TODO: Phase 18 will implement unpair
                selectedActiveDevice = null
            },
        )
    }

    // Dialog for removed device options
    selectedRemovedDevice?.let { device ->
        RemovedDeviceDialog(
            device = device,
            onDismiss = { selectedRemovedDevice = null },
            onViewHistory = {
                selectedRemovedDevice = null
                onNavigateToArchiveManagement(device.phoneNumber, device.role.name)
            },
            onRestore = {
                // TODO: Implement restore
                selectedRemovedDevice = null
            },
            onDelete = {
                // TODO: Implement delete with confirmation
                selectedRemovedDevice = null
            },
        )
    }
}

/**
 * Bottom sheet showing active device details with statistics and actions.
 * Per CONTEXT.md: "Bottom sheet shows: aggregate stats, 'View Sessions' link, unpair button"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceDetailBottomSheet(
    device: PairedDevice,
    onDismiss: () -> Unit,
    onViewSessions: () -> Unit,
    onUnpair: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header with device name/number
            Text(
                text = device.displayName ?: device.phoneNumber,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            if (device.displayName != null) {
                Text(
                    text = device.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider()

            // Statistics section
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Total Sessions", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(device.totalSessions.toString(), fontWeight = FontWeight.Medium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Messages Forwarded", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(device.totalMessagesForwarded.toString(), fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action buttons
            FilledTonalButton(
                onClick = onViewSessions,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Sessions")
            }

            TextButton(
                onClick = onUnpair,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Unpair Device")
            }

            Spacer(modifier = Modifier.height(16.dp)) // Bottom padding
        }
    }
}

/**
 * Dialog for removed device options.
 * Per CONTEXT.md: "Tapping removed device shows confirmation dialog: View history, Restore, Delete"
 */
@Composable
private fun RemovedDeviceDialog(
    device: PairedDevice,
    onDismiss: () -> Unit,
    onViewHistory: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(device.displayName ?: device.phoneNumber) },
        text = { Text("This device was removed. What would you like to do?") },
        confirmButton = {
            TextButton(onClick = onViewHistory) {
                Text("View History")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onRestore) {
                    Text("Restore")
                }
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Delete")
                }
            }
        },
    )
}

/**
 * Empty state when no devices have been paired.
 * Per CONTEXT.md: "No devices: illustration + 'No devices paired yet' text"
 */
@Composable
private fun EmptyDeviceHistoryState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "No devices paired yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Pair a device to start seeing your forwarding history here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

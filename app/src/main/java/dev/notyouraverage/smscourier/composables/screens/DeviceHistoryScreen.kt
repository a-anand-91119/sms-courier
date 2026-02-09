package dev.notyouraverage.smscourier.composables.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.notyouraverage.smscourier.composables.components.CollapsibleSectionHeader
import dev.notyouraverage.smscourier.composables.components.DeviceHistoryCard
import dev.notyouraverage.smscourier.composables.components.ExportFormatBottomSheet
import dev.notyouraverage.smscourier.composables.components.SkeletonDeviceCard
import dev.notyouraverage.smscourier.data.entities.PairedDevice
import dev.notyouraverage.smscourier.export.ExportState
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
    val exportState by viewModel.exportState.collectAsState()
    var showRemovedSection by remember { mutableStateOf(false) }
    var selectedActiveDevice by remember { mutableStateOf<PairedDevice?>(null) }
    var selectedRemovedDevice by remember { mutableStateOf<PairedDevice?>(null) }
    var showExportSheet by remember { mutableStateOf(false) }
    var deviceToExport by remember { mutableStateOf<PairedDevice?>(null) }
    var deviceToUnpair by remember { mutableStateOf<PairedDevice?>(null) }
    var deviceToUnpairHasActiveSession by remember { mutableStateOf(false) }
    var deviceToDelete by remember { mutableStateOf<PairedDevice?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current

    // SAF launcher for export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*"),
    ) { uri ->
        uri?.let { viewModel.executeExport(it, context.contentResolver) }
    }

    // Handle export state changes
    LaunchedEffect(exportState) {
        when (val state = exportState) {
            is ExportState.Success -> {
                snackbarHostState.showSnackbar("Exported to ${state.fileName}")
                viewModel.clearExportState()
            }
            is ExportState.Error -> {
                snackbarHostState.showSnackbar("Export failed: ${state.message}")
                viewModel.clearExportState()
            }
            else -> {}
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                                // Removed devices: no active sessions, lower opacity per CONTEXT.md
                                DeviceHistoryCard(
                                    device = device,
                                    hasActiveSession = false,
                                    onClick = { selectedRemovedDevice = device },
                                    isMuted = true,
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
            isExporting = exportState is ExportState.Loading,
            onDismiss = { selectedActiveDevice = null },
            onViewSessions = {
                selectedActiveDevice = null
                onNavigateToSessionHistory(device.phoneNumber, device.role.name)
            },
            onExport = {
                deviceToExport = device
                selectedActiveDevice = null
                showExportSheet = true
            },
            onUnpair = {
                deviceToUnpair = selectedActiveDevice
                // Check if device has active session
                deviceToUnpairHasActiveSession = activeDevices.any {
                    it.device.phoneNumber == selectedActiveDevice?.phoneNumber &&
                        it.device.role == selectedActiveDevice?.role &&
                        it.hasActiveSession
                }
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
            onDelete = {
                deviceToDelete = device
                selectedRemovedDevice = null
            },
        )
    }

    // Unpair confirmation dialog
    deviceToUnpair?.let { device ->
        AlertDialog(
            onDismissRequest = { deviceToUnpair = null },
            title = { Text("Unpair Device") },
            text = {
                if (deviceToUnpairHasActiveSession) {
                    Text("This device has an active forwarding session. Unpairing will end the session and remove ${device.displayName ?: device.phoneNumber} from your paired devices.")
                } else {
                    Text("Remove ${device.displayName ?: device.phoneNumber} from your paired devices? You can re-pair later.")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.unpairDevice(device)
                        deviceToUnpair = null
                        deviceToUnpairHasActiveSession = false
                        selectedActiveDevice = null
                    },
                ) {
                    Text("Unpair", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    deviceToUnpair = null
                    deviceToUnpairHasActiveSession = false
                }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Permanent delete confirmation dialog
    deviceToDelete?.let { device ->
        AlertDialog(
            onDismissRequest = { deviceToDelete = null },
            title = { Text("Delete Device Permanently") },
            text = {
                Text("This will permanently delete ${device.displayName ?: device.phoneNumber} and all its message history. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.permanentlyDeleteDevice(device)
                        deviceToDelete = null
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deviceToDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Export format picker
    if (showExportSheet) {
        ExportFormatBottomSheet(
            onDismiss = {
                showExportSheet = false
                deviceToExport = null
            },
            onExport = { format, includeMetadata ->
                showExportSheet = false
                deviceToExport?.let { device ->
                    val (filename, _) = viewModel.prepareExport(device, format, includeMetadata)
                    exportLauncher.launch(filename)
                }
                deviceToExport = null
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
    isExporting: Boolean,
    onDismiss: () -> Unit,
    onViewSessions: () -> Unit,
    onExport: () -> Unit,
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
            OutlinedButton(
                onClick = onExport,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isExporting,
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exporting...")
                } else {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export History")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
 * Actions: View history, Delete (permanently removes device and all history)
 */
@Composable
private fun RemovedDeviceDialog(
    device: PairedDevice,
    onDismiss: () -> Unit,
    onViewHistory: () -> Unit,
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
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Delete")
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

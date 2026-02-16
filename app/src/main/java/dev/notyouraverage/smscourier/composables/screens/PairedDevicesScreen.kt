package dev.notyouraverage.smscourier.composables.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.notyouraverage.smscourier.data.Direction
import dev.notyouraverage.smscourier.data.entities.DeviceRole
import dev.notyouraverage.smscourier.data.entities.ForwardingSession
import dev.notyouraverage.smscourier.data.entities.PairedDevice
import dev.notyouraverage.smscourier.data.entities.PairingStatus
import dev.notyouraverage.smscourier.viewmodels.PairedDevicesViewModel
import dev.notyouraverage.smscourier.viewmodels.ResendStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairedDevicesScreen(
    viewModel: PairedDevicesViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddDevice: () -> Unit,
    onDeviceClick: (PairedDevice) -> Unit,
) {
    val sourceDevices by viewModel.sourceDevices.collectAsState()
    val targetDevices by viewModel.targetDevices.collectAsState()
    val activeSessionsMap by viewModel.activeSessionsMap.collectAsState()
    val resendingDevice by viewModel.resendingDevice.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var deviceToDelete by remember { mutableStateOf<PairedDevice?>(null) }
    var showMenuForDevice by remember { mutableStateOf<PairedDevice?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // Confirmation dialog for device removal
    deviceToDelete?.let { device ->
        AlertDialog(
            onDismissRequest = { deviceToDelete = null },
            title = { Text("Remove Device") },
            text = {
                Text("Remove ${device.displayName ?: device.phoneNumber}? This will notify the other device.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDevice(device)
                        deviceToDelete = null
                    },
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deviceToDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Paired Devices",
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddDevice,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Device")
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Segmented Button Tabs
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                ) {
                    Text(
                        text = "Forward To Me (${sourceDevices.size})",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                ) {
                    Text(
                        text = "I Forward To (${targetDevices.size})",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            when (selectedTab) {
                0 -> DeviceList(
                    devices = sourceDevices,
                    activeSessionsMap = activeSessionsMap,
                    emptyTitle = "No devices yet",
                    emptyMessage = "Add a device to receive forwarded SMS from them.",
                    onDeviceClick = onDeviceClick,
                    onLongPressDevice = { showMenuForDevice = it },
                    showMenuForDevice = showMenuForDevice,
                    onDismissMenu = { showMenuForDevice = null },
                    onDeleteDevice = { deviceToDelete = it },
                    viewModel = viewModel,
                    resendingDevice = resendingDevice,
                )
                1 -> DeviceList(
                    devices = targetDevices,
                    activeSessionsMap = activeSessionsMap,
                    emptyTitle = "No devices yet",
                    emptyMessage = "Other devices can request pairing with you.",
                    onDeviceClick = onDeviceClick,
                    onLongPressDevice = { showMenuForDevice = it },
                    showMenuForDevice = showMenuForDevice,
                    onDismissMenu = { showMenuForDevice = null },
                    onDeleteDevice = { deviceToDelete = it },
                    viewModel = viewModel,
                    resendingDevice = resendingDevice,
                )
            }
        }
    }
}

@Composable
fun DeviceList(
    devices: List<PairedDevice>,
    activeSessionsMap: Map<String, ForwardingSession>,
    emptyTitle: String,
    emptyMessage: String,
    onDeviceClick: (PairedDevice) -> Unit,
    onLongPressDevice: (PairedDevice) -> Unit,
    showMenuForDevice: PairedDevice?,
    onDismissMenu: () -> Unit,
    onDeleteDevice: (PairedDevice) -> Unit,
    viewModel: PairedDevicesViewModel,
    resendingDevice: String?,
) {
    if (devices.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
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
                    text = emptyTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(devices, key = { "${it.phoneNumber}_${it.role}" }) { device ->
                Box {
                    DeviceCard(
                        device = device,
                        activeSession = activeSessionsMap[device.phoneNumber],
                        onClick = { onDeviceClick(device) },
                        onLongClick = { onLongPressDevice(device) },
                        isResending = resendingDevice == device.phoneNumber,
                    )
                    DropdownMenu(
                        expanded = showMenuForDevice == device,
                        onDismissRequest = onDismissMenu,
                    ) {
                        // Show "Resend Request" only for PENDING_SENT status
                        if (device.status == PairingStatus.PENDING_SENT) {
                            val resendStatus = viewModel.canResendPairingRequest(device)
                            DropdownMenuItem(
                                text = {
                                    when (resendStatus) {
                                        is ResendStatus.CanResend -> Text("Resend Request")
                                        is ResendStatus.Cooldown -> Text(
                                            "Resend (${resendStatus.remainingMs / 1000}s)",
                                        )
                                        is ResendStatus.MaxAttemptsReached -> Text(
                                            "Max Attempts Reached",
                                        )
                                        else -> Text("Resend Request")
                                    }
                                },
                                onClick = {
                                    if (resendStatus is ResendStatus.CanResend) {
                                        viewModel.resendPairingRequest(device)
                                        onDismissMenu()
                                    }
                                },
                                enabled = resendStatus is ResendStatus.CanResend,
                            )
                        }

                        // Always show delete option
                        DropdownMenuItem(
                            text = { Text("Remove Device") },
                            onClick = {
                                onDismissMenu()
                                onDeleteDevice(device)
                            },
                        )
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(80.dp)) // FAB space
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeviceCard(
    device: PairedDevice,
    activeSession: ForwardingSession?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isResending: Boolean = false,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = (
                        device.displayName?.firstOrNull()
                            ?: device.phoneNumber.lastOrNull()
                            ?: '?'
                        ).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.displayName ?: device.phoneNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (device.displayName != null) {
                    Text(
                        text = device.phoneNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Directional subtitle
                val direction = if (activeSession != null) {
                    when (device.role) {
                        DeviceRole.SOURCE -> Direction.FORWARDING_TO
                        DeviceRole.TARGET -> Direction.RECEIVING_FROM
                    }
                } else {
                    null
                }
                DirectionalSubtitle(
                    direction = direction,
                    lastActivityAt = device.lastActivityAt,
                )
            }

            // Loading indicator or Status Badge
            if (isResending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                StatusBadge(status = device.status)
            }
        }
    }
}

@Composable
fun StatusBadge(status: PairingStatus) {
    val (backgroundColor, textColor, text) = when (status) {
        PairingStatus.APPROVED -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "Paired",
        )
        PairingStatus.PENDING_SENT -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "Pending",
        )
        PairingStatus.PENDING_RECEIVED -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "Waiting",
        )
        PairingStatus.REJECTED -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "Rejected",
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = textColor,
        )
    }
}

/**
 * Displays directional status subtitle for a device.
 *
 * Shows:
 * - "Forwarding to - Active now" (tertiary color) for FORWARDING_TO direction (TARGET role)
 * - "Receiving from - Active now" (primary color) for RECEIVING_FROM direction (SOURCE role)
 * - "Forwarding & Receiving - Active now" (secondary color) for bidirectional sessions
 * - "Idle - Last active: X ago" (gray) for inactive devices
 */
@Composable
fun DirectionalSubtitle(
    direction: Direction?,
    lastActivityAt: Long?,
    modifier: Modifier = Modifier,
) {
    val (text, color) = when (direction) {
        // FORWARDING_TO: this device forwards messages TO others (TARGET role)
        Direction.FORWARDING_TO -> "Forwarding to - Active now" to MaterialTheme.colorScheme.tertiary
        // RECEIVING_FROM: this device receives messages FROM others (SOURCE role)
        Direction.RECEIVING_FROM -> "Receiving from - Active now" to MaterialTheme.colorScheme.primary
        // Both directions active
        Direction.BIDIRECTIONAL -> "Forwarding & Receiving - Active now" to MaterialTheme.colorScheme.secondary
        // No active session
        null -> {
            val timeText = lastActivityAt?.let { formatLastActive(it) } ?: "Never"
            "Idle - Last active: $timeText" to MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        modifier = modifier,
    )
}

/**
 * Formats a timestamp into human-readable relative time.
 */
private fun formatLastActive(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffMs = now - timestamp
    val diffMinutes = diffMs / (60 * 1000)
    return when {
        diffMinutes < 1 -> "Just now"
        diffMinutes < 60 -> "$diffMinutes min ago"
        diffMinutes < 1440 -> "${diffMinutes / 60} hours ago"
        else -> "${diffMinutes / 1440} days ago"
    }
}

package dev.notyouraverage.smscourier.composables.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
    onActiveDeviceClick: (PairedDevice) -> Unit,
    onRemovedDeviceClick: (PairedDevice) -> Unit,
) {
    val activeDevices by viewModel.activeDevices.collectAsState()
    val removedDevices by viewModel.removedDevices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showRemovedSection by remember { mutableStateOf(false) }
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
                                onClick = { onActiveDeviceClick(deviceWithSession.device) },
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
                                    onClick = { onRemovedDeviceClick(device) },
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

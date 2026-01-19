package dev.notyouraverage.smscourier.composables.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.notyouraverage.smscourier.data.settings.AppTheme
import dev.notyouraverage.smscourier.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Collect settings state
    val notificationPersistence by viewModel.notificationPersistence.collectAsState()
    val defaultDuration by viewModel.defaultForwardingDurationMinutes.collectAsState()
    val theme by viewModel.theme.collectAsState()

    // Duration dropdown state
    var durationExpanded by remember { mutableStateOf(false) }
    val durationOptions = listOf(15, 30, 60)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Settings",
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
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                SectionHeader(title = "Preferences")
            }

            // Notification persistence toggle
            item {
                SettingsSwitchItem(
                    title = "Persistent notification",
                    subtitle = if (notificationPersistence) {
                        "Service notification recreates when dismissed"
                    } else {
                        "Service notification stays dismissed"
                    },
                    checked = notificationPersistence,
                    onCheckedChange = { viewModel.setNotificationPersistence(it) },
                )
            }

            // Default forwarding duration selector
            item {
                Box {
                    SettingsSelectionItem(
                        title = "Default forwarding duration",
                        selectedValue = formatDuration(defaultDuration),
                        onClick = { durationExpanded = true },
                    )
                    DropdownMenu(
                        expanded = durationExpanded,
                        onDismissRequest = { durationExpanded = false },
                    ) {
                        durationOptions.forEach { minutes ->
                            DropdownMenuItem(
                                text = { Text(formatDuration(minutes)) },
                                onClick = {
                                    viewModel.setDefaultForwardingDuration(minutes)
                                    durationExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            // Theme selection
            item {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        AppTheme.entries.forEachIndexed { index, appTheme ->
                            SegmentedButton(
                                selected = theme == appTheme,
                                onClick = { viewModel.setTheme(appTheme) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = AppTheme.entries.size,
                                ),
                            ) {
                                Text(
                                    when (appTheme) {
                                        AppTheme.LIGHT -> "Light"
                                        AppTheme.DARK -> "Dark"
                                        AppTheme.SYSTEM -> "System"
                                    },
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "Information")
            }

            // Placeholder for information items (will be added in Plan 13-03)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
    )
}

@Composable
private fun SettingsSelectionItem(
    title: String,
    selectedValue: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(selectedValue) },
        trailingContent = {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

private fun formatDuration(minutes: Int): String = when (minutes) {
    15 -> "15 minutes"
    30 -> "30 minutes"
    60 -> "1 hour"
    else -> "$minutes minutes"
}

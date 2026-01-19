package dev.notyouraverage.smscourier.composables.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.notyouraverage.smscourier.BuildConfig
import dev.notyouraverage.smscourier.constants.AboutLinks
import dev.notyouraverage.smscourier.data.settings.AppTheme
import dev.notyouraverage.smscourier.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Collect settings state
    val notificationPersistence by viewModel.notificationPersistence.collectAsState()
    val defaultDuration by viewModel.defaultForwardingDurationMinutes.collectAsState()
    val theme by viewModel.theme.collectAsState()

    // Duration dropdown state
    var durationExpanded by remember { mutableStateOf(false) }
    val durationOptions = listOf(15, 30, 60)

    // Permission states that refresh when screen resumes
    var smsReceiveGranted by remember { mutableStateOf(false) }
    var smsSendGranted by remember { mutableStateOf(false) }
    var notificationGranted by remember { mutableStateOf(false) }

    // Refresh permissions when lifecycle resumes (user returns from settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                smsReceiveGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECEIVE_SMS,
                ) == PackageManager.PERMISSION_GRANTED
                smsSendGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.SEND_SMS,
                ) == PackageManager.PERMISSION_GRANTED
                notificationGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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

            // Permissions sub-section
            item {
                Text(
                    text = "Permissions",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
                )
            }

            item {
                PermissionStatusItem(
                    label = "Receive SMS",
                    isGranted = smsReceiveGranted,
                    onFixClick = { openAppSettings(context) },
                )
            }

            item {
                PermissionStatusItem(
                    label = "Send SMS",
                    isGranted = smsSendGranted,
                    onFixClick = { openAppSettings(context) },
                )
            }

            item {
                PermissionStatusItem(
                    label = "Notifications",
                    isGranted = notificationGranted,
                    onFixClick = { openAppSettings(context) },
                )
            }

            // Divider and About sub-section
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
                )
            }

            item {
                AboutItem(
                    title = "Version",
                    subtitle = BuildConfig.VERSION_NAME,
                )
            }

            item {
                AboutItem(
                    title = "Privacy Policy",
                    onClick = { openUrl(context, AboutLinks.PRIVACY_POLICY_URL) },
                )
            }

            item {
                AboutItem(
                    title = "Support",
                    subtitle = "Report issues on GitLab",
                    onClick = { openUrl(context, AboutLinks.SUPPORT_URL) },
                )
            }
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

@Composable
private fun PermissionStatusItem(
    label: String,
    isGranted: Boolean,
    onFixClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = {
            Text(
                text = if (isGranted) "Granted" else "Not granted",
                color = if (isGranted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        },
        leadingContent = {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGranted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        },
        trailingContent = {
            if (!isGranted) {
                TextButton(onClick = onFixClick) {
                    Text("Fix")
                }
            }
        },
    )
}

@Composable
private fun AboutItem(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = onClick?.let {
            {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    )
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}

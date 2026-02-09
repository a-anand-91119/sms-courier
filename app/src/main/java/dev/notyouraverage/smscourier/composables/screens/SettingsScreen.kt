package dev.notyouraverage.smscourier.composables.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.notyouraverage.smscourier.BuildConfig
import dev.notyouraverage.smscourier.constants.AboutLinks
import dev.notyouraverage.smscourier.data.settings.AppTheme
import dev.notyouraverage.smscourier.viewmodels.CleanupState
import dev.notyouraverage.smscourier.viewmodels.SettingsViewModel
import kotlin.math.roundToInt

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

    // Collect advanced/security settings
    val lockoutDuration by viewModel.lockoutDurationMinutes.collectAsState()
    val maxFailedAttempts by viewModel.maxFailedAttempts.collectAsState()
    val challengeExpiry by viewModel.challengeExpiryMinutes.collectAsState()
    val maxPairingResend by viewModel.maxPairingResendAttempts.collectAsState()
    val pairingResendCooldown by viewModel.pairingResendCooldownMinutes.collectAsState()
    val authRequestTimeout by viewModel.authRequestTimeoutMinutes.collectAsState()
    val settingError by viewModel.settingError.collectAsState()

    // Data & Storage settings
    val historyRetentionDays by viewModel.historyRetentionDays.collectAsState()
    val cleanupState by viewModel.cleanupState.collectAsState()
    val autoCleanupEnabled by viewModel.autoCleanupEnabled.collectAsState()
    val lastCleanupTimestamp by viewModel.lastCleanupTimestamp.collectAsState()

    // Advanced section expand/collapse state
    var advancedExpanded by remember { mutableStateOf(false) }

    // Data & Storage UI state
    var retentionExpanded by remember { mutableStateOf(false) }
    val retentionOptions = listOf(7, 30, 90, 0) // 0 = Forever
    var showCleanupConfirmDialog by remember { mutableStateOf(false) }

    // Snackbar state for error messages
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar when there's an error
    LaunchedEffect(settingError) {
        settingError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearSettingError()
        }
    }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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

            // Default forwarding duration slider
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Default forwarding duration",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "$defaultDuration minutes",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Slider(
                        value = defaultDuration.toFloat(),
                        onValueChange = { viewModel.setDefaultForwardingDuration(it.roundToInt()) },
                        valueRange = 5f..30f,
                        steps = 4,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "5 min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "30 min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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

            // Data & Storage Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(title = "Data & Storage")
            }

            // History retention dropdown
            item {
                Box {
                    SettingsSelectionItem(
                        title = "History retention",
                        selectedValue = formatRetention(historyRetentionDays),
                        onClick = { retentionExpanded = true },
                    )
                    DropdownMenu(
                        expanded = retentionExpanded,
                        onDismissRequest = { retentionExpanded = false },
                    ) {
                        retentionOptions.forEach { days ->
                            DropdownMenuItem(
                                text = { Text(formatRetention(days)) },
                                onClick = {
                                    viewModel.setHistoryRetentionDays(days)
                                    retentionExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            // Clean up now button
            item {
                CleanupButtonItem(
                    isLoading = cleanupState is CleanupState.Loading,
                    onClick = { showCleanupConfirmDialog = true },
                )
            }

            // Auto-cleanup toggle (hidden when retention = Forever)
            if (historyRetentionDays != 0) {
                item {
                    SettingsSwitchItem(
                        title = "Auto-cleanup",
                        subtitle = if (autoCleanupEnabled) {
                            "Last cleaned: ${formatLastCleanup(lastCleanupTimestamp)}"
                        } else {
                            "Clean up old data automatically every 7 days"
                        },
                        checked = autoCleanupEnabled,
                        onCheckedChange = { viewModel.setAutoCleanupEnabled(it) },
                    )
                }
            }

            // Advanced section (collapsible)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = LocalIndication.current,
                            onClick = { advancedExpanded = !advancedExpanded },
                        )
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Advanced",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        imageVector = if (advancedExpanded) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = if (advancedExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            item {
                AnimatedVisibility(visible = advancedExpanded) {
                    Column {
                        // Warning header
                        ListItem(
                            headlineContent = {
                                Text(
                                    "These settings affect security. Change with care.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                        )

                        // Security sub-section
                        Text(
                            text = "Security",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
                        )

                        SettingsNumberInputItem(
                            title = "Lockout duration",
                            value = lockoutDuration,
                            onValueChange = { viewModel.setLockoutDuration(it) },
                            minValue = 1,
                            maxValue = 60,
                            unit = "minutes",
                            helperText = "How long devices are locked after failed auth",
                        )

                        SettingsNumberInputItem(
                            title = "Max failed attempts",
                            value = maxFailedAttempts,
                            onValueChange = { viewModel.setMaxFailedAttempts(it) },
                            minValue = 1,
                            maxValue = 10,
                            unit = "attempts",
                            helperText = "Failed attempts before lockout",
                        )

                        SettingsNumberInputItem(
                            title = "Challenge expiry",
                            value = challengeExpiry,
                            onValueChange = { viewModel.setChallengeExpiry(it) },
                            minValue = 1,
                            maxValue = 10,
                            unit = "minutes",
                            helperText = "Time before auth challenge expires",
                        )

                        // Pairing sub-section
                        Text(
                            text = "Pairing",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                        )

                        SettingsNumberInputItem(
                            title = "Max pairing resend",
                            value = maxPairingResend,
                            onValueChange = { viewModel.setMaxPairingResendAttempts(it) },
                            minValue = 1,
                            maxValue = 10,
                            unit = "attempts",
                            helperText = "Maximum resend attempts",
                        )

                        SettingsNumberInputItem(
                            title = "Pairing resend cooldown",
                            value = pairingResendCooldown,
                            onValueChange = { viewModel.setPairingResendCooldown(it) },
                            minValue = 1,
                            maxValue = 10,
                            unit = "minutes",
                            helperText = "Wait time between resends",
                        )

                        SettingsNumberInputItem(
                            title = "Auth request timeout",
                            value = authRequestTimeout,
                            onValueChange = { viewModel.setAuthRequestTimeout(it) },
                            minValue = 1,
                            maxValue = 30,
                            unit = "minutes",
                            helperText = "Time before auth request expires",
                        )
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

        // Cleanup confirmation dialog
        if (showCleanupConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showCleanupConfirmDialog = false },
                title = { Text("Clean up history?") },
                text = {
                    Text(
                        if (historyRetentionDays == 0) {
                            "Retention is set to Forever. No data will be deleted."
                        } else {
                            "This will delete sessions and messages older than ${formatRetention(historyRetentionDays)}."
                        },
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showCleanupConfirmDialog = false
                            viewModel.cleanupOldHistory()
                        },
                    ) {
                        Text(if (historyRetentionDays == 0) "OK" else "Clean up")
                    }
                },
                dismissButton = {
                    if (historyRetentionDays != 0) {
                        TextButton(onClick = { showCleanupConfirmDialog = false }) {
                            Text("Cancel")
                        }
                    }
                },
            )
        }

        // Cleanup result dialog
        val successState = cleanupState as? CleanupState.Success
        if (successState != null) {
            AlertDialog(
                onDismissRequest = { viewModel.resetCleanupState() },
                title = { Text("Cleanup complete") },
                text = {
                    Text(
                        if (successState.sessionsDeleted == 0 && successState.messagesDeleted == 0) {
                            "No old data to clean up."
                        } else {
                            "Deleted ${successState.sessionsDeleted} session(s) and ${successState.messagesDeleted} message(s)."
                        },
                    )
                },
                confirmButton = {
                    Button(onClick = { viewModel.resetCleanupState() }) {
                        Text("OK")
                    }
                },
            )
        }

        // Cleanup error dialog
        val errorState = cleanupState as? CleanupState.Error
        if (errorState != null) {
            AlertDialog(
                onDismissRequest = { viewModel.resetCleanupState() },
                title = { Text("Cleanup failed") },
                text = { Text(errorState.message) },
                confirmButton = {
                    Button(onClick = { viewModel.resetCleanupState() }) {
                        Text("OK")
                    }
                },
            )
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
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = LocalIndication.current,
            onClick = onClick,
        ),
    )
}

@Composable
private fun SettingsNumberInputItem(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    minValue: Int,
    maxValue: Int,
    unit: String,
    helperText: String? = null,
) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Column {
                Text("$minValue-$maxValue $unit")
                helperText?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        trailingContent = {
            OutlinedTextField(
                value = textValue,
                onValueChange = { newValue ->
                    textValue = newValue
                    newValue.toIntOrNull()?.let { intValue ->
                        onValueChange(intValue)
                    }
                },
                modifier = Modifier.width(80.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
    )
}

private fun formatDuration(minutes: Int): String = when (minutes) {
    15 -> "15 minutes"
    30 -> "30 minutes"
    60 -> "1 hour"
    else -> "$minutes minutes"
}

private fun formatRetention(days: Int): String = when (days) {
    0 -> "Forever"
    7 -> "7 days"
    30 -> "30 days"
    90 -> "90 days"
    else -> "$days days"
}

private fun formatLastCleanup(timestampMs: Long): String {
    if (timestampMs == 0L) {
        return "Never"
    }
    return DateUtils.getRelativeTimeSpanString(
        timestampMs,
        System.currentTimeMillis(),
        DateUtils.DAY_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
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
        modifier = if (onClick != null) {
            Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = onClick,
            )
        } else {
            Modifier
        },
    )
}

@Composable
private fun CleanupButtonItem(
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text("Clean up now") },
        supportingContent = { Text("Delete old sessions and messages") },
        trailingContent = {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.width(24.dp).height(24.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                OutlinedButton(onClick = onClick) {
                    Text("Clean up")
                }
            }
        },
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

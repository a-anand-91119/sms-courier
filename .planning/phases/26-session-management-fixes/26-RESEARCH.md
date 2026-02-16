# Phase 26: Session Management Fixes - Research

**Researched:** 2026-02-16
**Domain:** Android session management with cross-device SMS notification
**Confidence:** HIGH

## Summary

This phase fixes three interconnected session management bugs: visibility (SESS-01), role-based key clearing (SESS-02), and remote notification (SESS-03). The existing architecture already has most infrastructure in place—Room DAO queries, SMS sender methods, and notification channels. The fixes involve query adjustments, dynamic role detection, and SMS notification flow.

The standard Android approach uses:
- Room DAO queries with multiple WHERE conditions for bidirectional session handling
- StateFlow in ViewModels for managing loading/success/error states during async SMS operations
- AlertDialog with confirmButton/dismissButton for confirmation UX
- NotificationCompat with setAutoCancel(true) for one-time dismissable notifications
- try-catch in viewModelScope for SMS failure handling with local state updates

**Primary recommendation:** Leverage existing infrastructure (endSessionForDevice DAO method, sendStopForward SMS method, PairingNotificationManager pattern) and add minimal new code—query both roles, detect role dynamically, send SMS with error handling, show notification on receive.

## Standard Stack

The project already uses the required libraries and patterns for this phase:

### Core (Already in Project)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Room | (androidx.room) | Session persistence with DAO queries | Android's official local database, supports complex WHERE conditions |
| Kotlin Coroutines | (kotlinx.coroutines) | Async operations in ViewModels | Standard for async Android development, integrates with ViewModel lifecycle |
| Jetpack Compose | (androidx.compose) | UI with AlertDialog | Modern Android UI toolkit, declarative dialog patterns |
| NotificationCompat | (androidx.core) | Cross-device notifications | Backward-compatible notification API |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| StateFlow | (kotlinx.coroutines.flow) | UI state management | Already used in ForwardingControlViewModel for loading/error states |
| viewModelScope | (androidx.lifecycle) | Coroutine scope tied to ViewModel | Already used for lifecycle-aware async operations |
| MockK | (io.mockk) | Test mocking | Already used in Phase 24 tests |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Room @Query | Raw SQL queries | Room provides compile-time verification and type safety |
| StateFlow | LiveData | StateFlow is newer, better for coroutines, already used in codebase |
| NotificationCompat | Notification.Builder | NotificationCompat provides backward compatibility |

**Installation:**
No new dependencies required—all libraries already in project.

## Architecture Patterns

### Recommended Project Structure
The project already follows this structure:
```
app/src/main/java/dev/notyouraverage/smscourier/
├── viewmodels/               # ForwardingControlViewModel (add confirmation state)
├── data/dao/                 # ForwardingSessionDao (queries already exist)
├── repository/               # ForwardingSessionRepository (add method for all active)
├── services/                 # SmsSender (sendStopForward already exists)
├── notifications/            # PairingNotificationManager (add session stop notification)
├── handlers/                 # SmsCommandHandler (handleStopForward needs ALL sessions logic)
└── composables/screens/      # ForwardingControlScreen (add confirmation dialog)
```

### Pattern 1: Bidirectional Session Termination
**What:** Single SMS command ends ALL active sessions between two devices
**When to use:** When stopping forwarding in bidirectional pairing scenarios
**Example:**
```kotlin
// In ForwardingSessionRepository
suspend fun endAllSessionsForDevice(phoneNumber: String, stoppedBy: String) =
    withContext(ioDispatcher) {
        // This DAO method already ends ALL active sessions for device
        forwardingSessionDao.endSessionForDevice(normalizePhoneNumber(phoneNumber), stoppedBy)
    }

// In SmsCommandHandler.handleStopForward()
suspend fun handleStopForward(senderPhone: String) {
    // Change from: getActiveSessionForDevice (singular)
    // To: endSessionForDevice (already handles ALL sessions via WHERE clause)
    sessionRepository.endSessionForDevice(senderPhone, "REMOTE")

    // Show notification (NEW)
    notificationManager.showSessionStoppedNotification(senderPhone)
}
```
**Source:** Existing DAO at ForwardingSessionDao.kt line 50-57 already uses `WHERE device_phone_number = :phoneNumber AND is_active = 1` which affects ALL active sessions.

### Pattern 2: Dynamic Role Detection for Encryption Key
**What:** Determine device role at runtime based on active session, not hardcoded
**When to use:** When clearing encryption keys after stopping forwarding
**Example:**
```kotlin
// In ForwardingControlViewModel.stopForwarding()
fun stopForwarding(devicePhoneNumber: String) {
    viewModelScope.launch {
        try {
            // Option A: Query the active session to get encryptionKey presence
            val session = sessionRepository.getActiveSessionForDevice(devicePhoneNumber)
            val role = if (session?.encryptionKey != null) {
                DeviceRole.SOURCE  // We have encryption key = we are SOURCE
            } else {
                DeviceRole.TARGET  // No encryption key = we are TARGET
            }

            // Option B: Try both roles (simpler, no extra query)
            deviceRepository.updateEncryptionKey(devicePhoneNumber, DeviceRole.SOURCE, null)
            deviceRepository.updateEncryptionKey(devicePhoneNumber, DeviceRole.TARGET, null)

            // End session and notify
            sessionRepository.endSessionForDevice(devicePhoneNumber, "USER")
            smsSender.sendStopForward(devicePhoneNumber)  // NEW - SESS-03 fix

            _uiState.value = _uiState.value.copy(isLoading = false)
        } catch (e: Exception) {
            // Handle SMS failure but still end local session
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Session ended locally but notification failed: ${e.message}"
            )
        }
    }
}
```
**Source:** Existing pattern in ForwardingControlViewModel.kt line 99-116, needs SMS call and role fix.

### Pattern 3: Confirmation Dialog State Management
**What:** Use Boolean state with AlertDialog that resets on dismiss/confirm
**When to use:** Any destructive action requiring user confirmation
**Example:**
```kotlin
// In ForwardingControlViewModel
data class ForwardingUiState(
    // ... existing fields
    val confirmStopPhoneNumber: String? = null  // null = no dialog, non-null = show dialog
)

fun requestStopConfirmation(phoneNumber: String) {
    _uiState.value = _uiState.value.copy(confirmStopPhoneNumber = phoneNumber)
}

fun cancelStopConfirmation() {
    _uiState.value = _uiState.value.copy(confirmStopPhoneNumber = null)
}

// In ForwardingControlScreen composable
val uiState by viewModel.uiState.collectAsState()

uiState.confirmStopPhoneNumber?.let { phoneNumber ->
    AlertDialog(
        onDismissRequest = { viewModel.cancelStopConfirmation() },
        title = { Text("Stop forwarding?") },
        text = {
            Text("Stop forwarding to $phoneNumber? The other device will be notified.")
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.stopForwarding(phoneNumber)
                viewModel.cancelStopConfirmation()
            }) {
                Text("Stop")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.cancelStopConfirmation() }) {
                Text("Cancel")
            }
        }
    )
}
```
**Source:** Android Developers - Dialog documentation (https://developer.android.com/develop/ui/compose/components/dialog)

### Pattern 4: Session Stopped Notification
**What:** One-time dismissable notification when remote device stops session
**When to use:** When handling STOP_FORWARD command from remote device
**Example:**
```kotlin
// In PairingNotificationManager
fun showSessionStoppedNotification(phoneNumber: String) {
    val intent = Intent(context, MainActivity::class.java)  // Open home screen
    val pendingIntent = PendingIntent.getActivity(
        context,
        phoneNumber.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_FORWARDING)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Forwarding Stopped")
        .setContentText("$phoneNumber ended the forwarding session")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)  // Dismisses when tapped
        .build()

    notifySafely(getNotificationIdForPhone(phoneNumber), notification)
}
```
**Source:** Android Developers - Notifications (https://developer.android.com/develop/ui/views/notifications), existing pattern in PairingNotificationManager.kt

### Pattern 5: SMS Failure Graceful Degradation
**What:** End local session even if SMS notification fails, warn user
**When to use:** Any SMS send operation that could fail due to network/permissions
**Example:**
```kotlin
fun stopForwarding(devicePhoneNumber: String) {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        try {
            // Try to send SMS first
            smsSender.sendStopForward(devicePhoneNumber)

            // If SMS succeeds, end session
            sessionRepository.endSessionForDevice(devicePhoneNumber, "USER")
            deviceRepository.updateEncryptionKey(devicePhoneNumber, DeviceRole.SOURCE, null)

            _uiState.value = _uiState.value.copy(isLoading = false)
        } catch (e: Exception) {
            // SMS failed - end local session anyway, warn user
            sessionRepository.endSessionForDevice(devicePhoneNumber, "USER")
            deviceRepository.updateEncryptionKey(devicePhoneNumber, DeviceRole.SOURCE, null)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Session stopped but remote device wasn't notified"
            )
        }
    }
}
```
**Source:** Best practices from Android Developers - Coroutines (https://developer.android.com/kotlin/coroutines/coroutines-best-practices)

### Anti-Patterns to Avoid
- **Hardcoded DeviceRole**: SESS-02 bug was caused by hardcoded DeviceRole.SOURCE in stopForwarding—must determine role dynamically or clear both
- **Single-session queries for bidirectional**: Don't use getActiveSessionForDevice (singular) when you need to affect ALL sessions—use endSessionForDevice which handles all via WHERE clause
- **Missing SMS notification**: Don't end sessions without notifying the other device—creates desync
- **Blocking on SMS failure**: Don't prevent local session cleanup if SMS fails—end locally and warn user

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| End all sessions for device | Loop through active sessions calling endSession per ID | forwardingSessionDao.endSessionForDevice() | Existing DAO method already handles ALL sessions atomically with WHERE clause |
| Determine device role | Add session.deviceRole field | Query session.encryptionKey presence OR update both roles | encryption key presence indicates SOURCE role, or clearing both is safe |
| SMS command format | Build string manually | smsSender.sendStopForward() | Existing method with correct protocol format ("SMSC STOP_FORWARD") |
| Notification channels | Create new channel | Use existing CHANNEL_FORWARDING | Already configured in PairingNotificationManager.createNotificationChannels() |
| Dialog state | Complex state machine | String? field (null = no dialog) | Simple, idiomatic Compose pattern |

**Key insight:** The existing codebase has 90% of infrastructure needed—ForwardingSessionDao.endSessionForDevice already ends ALL sessions, SmsSender.sendStopForward exists, notification channels are configured. Only need to wire these together and add confirmation dialog.

## Common Pitfalls

### Pitfall 1: Not Querying Both Roles (SESS-01)
**What goes wrong:** ForwardingControlViewModel only queries SOURCE devices, so TARGET devices with active sessions are invisible in the UI
**Why it happens:** Initial implementation assumed only SOURCE devices would show sessions in the forwarding control screen
**How to avoid:** Query both DeviceRole.SOURCE and DeviceRole.TARGET with PairingStatus.APPROVED, combine results
**Warning signs:** Phase 24 test SESS-01 fails with "verify { getDevicesByRoleAndStatus(TARGET, ...) }" not called
**Code location:** ForwardingControlViewModel.kt line 34-36 (approvedSourceDevices initialization)

### Pitfall 2: Hardcoding DeviceRole in Encryption Key Clear (SESS-02)
**What goes wrong:** Using DeviceRole.SOURCE hardcoded means TARGET devices fail to clear their encryption key
**Why it happens:** Encryption key field exists on PairedDevice for both roles, but was implemented with SOURCE-only assumption
**How to avoid:** Either query the active session to determine role OR clear both SOURCE and TARGET roles (no harm, one will be no-op)
**Warning signs:** Phase 24 test SESS-02 fails expecting DeviceRole.TARGET but got DeviceRole.SOURCE
**Code location:** ForwardingControlViewModel.kt line 107 (updateEncryptionKey call)

### Pitfall 3: Forgetting to Send SMS Notification (SESS-03)
**What goes wrong:** Local device ends session but remote device never knows—session stays active remotely
**Why it happens:** Focus on local state management without considering distributed system implications
**How to avoid:** Always send STOP_FORWARD SMS before or after ending local session; if SMS fails, log error and continue
**Warning signs:** Phase 24 test SESS-03 fails with "verify { smsSender.sendStopForward(...) }" not called
**Code location:** ForwardingControlViewModel.kt line 99-116 (stopForwarding method missing SMS call)

### Pitfall 4: Dialog Confirmation State Leaks
**What goes wrong:** Dialog state persists after dismiss, causing dialog to reappear on navigation or configuration change
**Why it happens:** Forgot to reset dialog state in onDismissRequest or onConfirmation callbacks
**How to avoid:** ALWAYS reset dialog state (set to null) in both dismiss and confirm actions
**Warning signs:** Dialog appears unexpectedly after rotation or navigation back
**Example:**
```kotlin
// BAD - state not cleared on dismiss
AlertDialog(
    onDismissRequest = { /* nothing */ },  // BUG: state persists
    // ...
)

// GOOD - state cleared on all exit paths
AlertDialog(
    onDismissRequest = { viewModel.cancelStopConfirmation() },  // Resets state
    confirmButton = {
        TextButton(onClick = {
            viewModel.stopForwarding(phoneNumber)
            viewModel.cancelStopConfirmation()  // Also resets state
        })
    }
)
```

### Pitfall 5: Wrong Notification Channel Importance
**What goes wrong:** Session stopped notification uses high-priority channel, causing intrusive notifications
**Why it happens:** Reusing CHANNEL_MESSAGES (high priority) instead of CHANNEL_FORWARDING (low priority)
**How to avoid:** Use CHANNEL_FORWARDING for session lifecycle notifications (start/stop), reserve CHANNEL_MESSAGES for actual forwarded SMS content
**Warning signs:** User complaints about disruptive notifications for non-urgent events
**Code location:** Use PairingNotificationManager.CHANNEL_FORWARDING (already configured with IMPORTANCE_LOW)

### Pitfall 6: SMS Failure Blocks Session Cleanup
**What goes wrong:** If sendStopForward throws exception, local session never ends and user is stuck
**Why it happens:** No try-catch around SMS operation, or catch block doesn't continue with cleanup
**How to avoid:** Wrap SMS send in try-catch, always end local session in finally or after catch, surface warning to user
**Warning signs:** User taps stop button multiple times, session remains active locally even though user intended to stop
**Example:**
```kotlin
// BAD - exception stops cleanup
fun stopForwarding(phone: String) {
    smsSender.sendStopForward(phone)  // If this throws, session never ends
    sessionRepository.endSessionForDevice(phone, "USER")
}

// GOOD - cleanup happens regardless
fun stopForwarding(phone: String) {
    var smsFailed = false
    try {
        smsSender.sendStopForward(phone)
    } catch (e: Exception) {
        smsFailed = true
    }
    // Always execute cleanup
    sessionRepository.endSessionForDevice(phone, "USER")
    if (smsFailed) {
        _uiState.value = _uiState.value.copy(
            error = "Session stopped but remote device wasn't notified"
        )
    }
}
```

## Code Examples

Verified patterns from official sources and existing codebase:

### Query Both Roles for Active Sessions
```kotlin
// In ForwardingControlViewModel - fix for SESS-01
class ForwardingControlViewModel(...) : ViewModel() {
    // OLD: Only queried SOURCE devices
    val approvedSourceDevices: StateFlow<List<PairedDevice>> = deviceRepository
        .getDevicesByRoleAndStatus(DeviceRole.SOURCE, PairingStatus.APPROVED)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // NEW: Query both roles and combine
    val approvedDevices: StateFlow<List<PairedDevice>> = combine(
        deviceRepository.getDevicesByRoleAndStatus(DeviceRole.SOURCE, PairingStatus.APPROVED),
        deviceRepository.getDevicesByRoleAndStatus(DeviceRole.TARGET, PairingStatus.APPROVED)
    ) { sourceDevices, targetDevices ->
        sourceDevices + targetDevices
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
```
**Source:** Existing pattern in codebase, StateFlow + combine from Kotlin Coroutines

### Stop Forwarding with SMS Notification and Error Handling
```kotlin
// In ForwardingControlViewModel - fixes for SESS-02 and SESS-03
fun stopForwarding(devicePhoneNumber: String) {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        var smsNotificationFailed = false
        try {
            // SESS-03 FIX: Send SMS notification to remote device
            smsSender.sendStopForward(devicePhoneNumber)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send STOP_FORWARD SMS", e)
            smsNotificationFailed = true
        }

        // Always end local session regardless of SMS result
        sessionRepository.endSessionForDevice(devicePhoneNumber, "USER")

        // SESS-02 FIX: Clear encryption key for correct role
        // Option A: Clear both roles (simple, safe)
        deviceRepository.updateEncryptionKey(devicePhoneNumber, DeviceRole.SOURCE, null)
        deviceRepository.updateEncryptionKey(devicePhoneNumber, DeviceRole.TARGET, null)

        // Option B: Determine role from session (more precise)
        // val session = sessionRepository.getActiveSessionForDevice(devicePhoneNumber)
        // val role = if (session?.encryptionKey != null) DeviceRole.SOURCE else DeviceRole.TARGET
        // deviceRepository.updateEncryptionKey(devicePhoneNumber, role, null)

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = if (smsNotificationFailed) {
                "Session stopped but remote device wasn't notified"
            } else null
        )
    }
}
```
**Source:** Existing stopForwarding method at ForwardingControlViewModel.kt line 99-116, with fixes applied

### Confirmation Dialog with Loading State
```kotlin
// In ForwardingControlViewModel - add confirmation state
data class ForwardingUiState(
    val password: String = "",
    val durationMinutes: Int = SettingsDefaults.DEFAULT_FORWARDING_DURATION,
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val confirmStopPhoneNumber: String? = null,  // NEW
)

fun requestStopConfirmation(phoneNumber: String) {
    _uiState.value = _uiState.value.copy(confirmStopPhoneNumber = phoneNumber)
}

fun cancelStopConfirmation() {
    _uiState.value = _uiState.value.copy(confirmStopPhoneNumber = null)
}

// In ForwardingControlScreen composable
@Composable
fun ForwardingControlScreen(viewModel: ForwardingControlViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    // Show confirmation dialog when state has phone number
    uiState.confirmStopPhoneNumber?.let { phoneNumber ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelStopConfirmation() },
            title = { Text("Stop forwarding?") },
            text = {
                Text("Stop forwarding to $phoneNumber? The other device will be notified.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.stopForwarding(phoneNumber)
                        viewModel.cancelStopConfirmation()
                    },
                    enabled = !uiState.isLoading  // Disable during SMS send
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Stop")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.cancelStopConfirmation() },
                    enabled = !uiState.isLoading
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Button to trigger stop (opens dialog)
    Button(onClick = { viewModel.requestStopConfirmation(devicePhone) }) {
        Text("Stop Forwarding")
    }
}
```
**Source:** Android Developers - Dialog documentation (https://developer.android.com/develop/ui/compose/components/dialog)

### Session Stopped Notification
```kotlin
// In PairingNotificationManager - add new method
fun showSessionStoppedNotification(phoneNumber: String) {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        phoneNumber.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_FORWARDING)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Forwarding Stopped")
        .setContentText("$phoneNumber ended the forwarding session")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)  // Dismisses when tapped or swiped
        .build()

    notifySafely(getNotificationIdForPhone(phoneNumber), notification)
}

// In SmsCommandHandler.handleStopForward() - add notification
suspend fun handleStopForward(senderPhone: String) {
    Log.i(TAG, "Stop forward from: $senderPhone")

    // End ALL active sessions for this device (already correct via DAO)
    sessionRepository.endSessionForDevice(senderPhone, "REMOTE")

    // NEW: Show notification that remote device stopped the session
    notificationManager.showSessionStoppedNotification(senderPhone)

    onForwardingStateChanged(ForwardingState.Stopped(senderPhone, "REMOTE"))
    Log.i(TAG, "Stopped forwarding session for $senderPhone by remote request")
}
```
**Source:** Existing notification pattern in PairingNotificationManager.kt line 177-195, Android notification docs

### Bidirectional Session Check for Dialog Message
```kotlin
// In ForwardingControlViewModel - add helper to detect bidirectional
suspend fun isBidirectionalSession(phoneNumber: String): Boolean {
    val sourceDevice = deviceRepository.getByPhoneNumberAndRole(phoneNumber, DeviceRole.SOURCE)
    val targetDevice = deviceRepository.getByPhoneNumberAndRole(phoneNumber, DeviceRole.TARGET)

    val sourceHasSession = sourceDevice != null &&
        sessionRepository.getActiveSessionForDevice(phoneNumber) != null
    val targetHasSession = targetDevice != null &&
        sessionRepository.getActiveSessionForDevice(phoneNumber) != null

    return sourceHasSession && targetHasSession
}

// In ForwardingControlScreen - adjust dialog text for bidirectional
uiState.confirmStopPhoneNumber?.let { phoneNumber ->
    val isBidirectional = remember(phoneNumber) {
        // In real implementation, expose from ViewModel as StateFlow
        false  // placeholder - determine from ViewModel state
    }

    AlertDialog(
        title = { Text("Stop forwarding?") },
        text = {
            Text(
                if (isBidirectional) {
                    "Stop all forwarding with $phoneNumber? This will stop forwarding in both directions. The other device will be notified."
                } else {
                    "Stop forwarding to $phoneNumber? The other device will be notified."
                }
            )
        },
        // ... rest of dialog
    )
}
```
**Source:** User requirements from CONTEXT.md, bidirectional detection pattern from existing codebase

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Manual session tracking | Room DAO with Flow | Initial implementation | Reactive UI updates when sessions change |
| Password-based auth | Challenge-response auth | Phase 23 | More secure, HMAC-based verification |
| Single-role queries | Multi-role queries needed | Phase 26 (this phase) | Both SOURCE and TARGET devices visible |
| Silent session stops | SMS notification | Phase 26 (this phase) | Remote device knows session ended |
| Hardcoded role for key clear | Dynamic role detection | Phase 26 (this phase) | Correct key cleared for both roles |

**Deprecated/outdated:**
- Manual session list management: Room Flow + StateFlow handles reactivity automatically
- LiveData for UI state: StateFlow is preferred for coroutines (already migrated in codebase)
- Foreground service notifications only: One-time notifications needed for events like session stops

## Open Questions

Things that couldn't be fully resolved:

1. **Bidirectional session detection performance**
   - What we know: Can query both roles and check for active sessions
   - What's unclear: Whether to cache bidirectional status or query on-demand for dialog
   - Recommendation: Query on-demand when showing dialog (rare operation), don't optimize prematurely

2. **Encryption key clearing for bidirectional pairings**
   - What we know: Device can have both SOURCE and TARGET roles with same phone number
   - What's unclear: Whether clearing both roles is safe or if we need precise role detection
   - Recommendation: Clear both roles—it's a no-op for the role without a key, simpler than querying

3. **SMS notification failure retry strategy**
   - What we know: SMS can fail due to network/permissions
   - What's unclear: Whether to implement retry logic or just show warning
   - Recommendation: Show warning only (no retry)—user can manually restart session if needed, keeps implementation simple

## Sources

### Primary (HIGH confidence)
- Android Developers - Notifications: https://developer.android.com/develop/ui/views/notifications
- Android Developers - Dialog (Jetpack Compose): https://developer.android.com/develop/ui/compose/components/dialog
- Android Developers - Coroutines Best Practices: https://developer.android.com/kotlin/coroutines/coroutines-best-practices
- Android Developers - Room DAO: https://developer.android.com/training/data-storage/room/accessing-data
- Existing codebase files:
  - ForwardingSessionDao.kt (DAO queries, line 50-57 for endSessionForDevice)
  - ForwardingControlViewModel.kt (current stopForwarding implementation)
  - SmsCommandHandler.kt (handleStopForward method)
  - SmsSender.kt (sendStopForward method line 107-109)
  - PairingNotificationManager.kt (notification patterns)

### Secondary (MEDIUM confidence)
- nek12.dev - How to load data in Kotlin with MVVM: https://nek12.dev/blog/en/how-to-load-data-in-kotlin-with-mvvm-mvi-flow-coroutines-complete-guide/
- Jetpack Compose Playground - AlertDialog: https://foso.github.io/Jetpack-Compose-Playground/material/alertdialog/
- CodePath Android - Room Guide: https://guides.codepath.com/android/Room-Guide

### Tertiary (LOW confidence)
None—all findings verified with official docs or existing codebase.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All libraries already in project, verified in build files
- Architecture: HIGH - Existing patterns in codebase, official Android docs
- Pitfalls: HIGH - Identified from Phase 24 failing tests with specific line numbers

**Research date:** 2026-02-16
**Valid until:** 90 days (stable Android APIs, not fast-moving domain)

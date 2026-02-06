# Phase 17: Device History UI - Research

**Researched:** 2026-02-06
**Domain:** Jetpack Compose Device List UI / Material3 Bottom Sheets
**Confidence:** HIGH

## Summary

Phase 17 builds a Device History screen showing all paired devices (active and removed) with aggregate statistics. The app already has established Material3 Compose patterns (LargeTopAppBar, Card-based layouts, LazyColumn lists) that should be followed for consistency. The PairedDevice entity already includes soft delete columns (isArchived, archivedAt, archivalInitiatedBy) and aggregate statistics (totalSessions, totalMessagesForwarded) from Phase 15-01, making this a pure UI phase with no database changes needed.

The primary approach is extending existing patterns: use the two-line device card design from PairedDevicesScreen.kt, add collapsible sections using remember/mutableStateOf, implement Material3 ModalBottomSheet for device details, and use standard Android DateUtils for "last active" formatting. Entry point is a new Quick Actions menu item on HomeScreen.

**Primary recommendation:** Build on existing device card patterns with collapsible sections and bottom sheet details, leveraging established ViewModel/Repository architecture.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Jetpack Compose | BOM 2024.08.00 | Declarative UI | App standard, Material3 ready |
| Material3 Compose | Via BOM | UI components | ModalBottomSheet, ListItem, Badge |
| Room | 2.6.1 | Database queries | Existing DAO methods available |
| Navigation Compose | 2.7.7 | Navigation | Existing app pattern |
| Lifecycle ViewModel Compose | 2.7.0 | State management | Existing app pattern |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Android DateUtils | Platform | Relative time formatting | "2 minutes ago" formatting |
| Kotlin Flow | stdlib | Reactive data | Already used throughout app |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Custom shimmer | valentinilk/compose-shimmer | Library adds dependency; simple custom implementation sufficient |
| Custom date formatter | kotlinx-datetime | Overkill for simple relative time; DateUtils built-in |
| Custom bottom sheet | Morfly/advanced-bottomsheet-compose | Material3 ModalBottomSheet is sufficient |

**Installation:**
No new dependencies required. All features available in existing stack.

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/dev/notyouraverage/smscourier/
├── composables/screens/
│   └── DeviceHistoryScreen.kt        # Main screen with active/removed sections
├── viewmodels/
│   └── DeviceHistoryViewModel.kt     # State management for device list
├── navigation/
│   └── Screen.kt                      # Add DeviceHistory route
└── data/dao/
    └── PairedDeviceDao.kt             # Query methods already exist
```

### Pattern 1: Device List with Active/Removed Sections
**What:** Two-section list with collapsible "Removed" section
**When to use:** Displaying categorized items where one category is secondary

**Example:**
```kotlin
// From existing app patterns
@Composable
fun DeviceHistoryScreen(viewModel: DeviceHistoryViewModel) {
    val activeDevices by viewModel.activeDevices.collectAsState()
    val removedDevices by viewModel.removedDevices.collectAsState()
    var showRemovedSection by remember { mutableStateOf(false) }

    LazyColumn {
        // Active devices section (always visible)
        item { SectionHeader("Active Devices") }
        items(activeDevices, key = { "${it.phoneNumber}_${it.role}" }) { device ->
            DeviceCard(device = device, onClick = { /* show bottom sheet */ })
        }

        // Removed devices section (collapsible)
        if (removedDevices.isNotEmpty()) {
            item {
                CollapsibleSectionHeader(
                    title = "Removed Devices",
                    count = removedDevices.size,
                    isExpanded = showRemovedSection,
                    onToggle = { showRemovedSection = !showRemovedSection }
                )
            }
            if (showRemovedSection) {
                items(removedDevices, key = { "${it.phoneNumber}_${it.role}" }) { device ->
                    RemovedDeviceCard(device = device, onClick = { /* show dialog */ })
                }
            }
        }
    }
}
```

### Pattern 2: Material3 ModalBottomSheet for Device Details
**What:** Bottom sheet showing device statistics with dismiss handling
**When to use:** Showing detailed information without navigation

**Example:**
```kotlin
// Source: https://developer.android.com/develop/ui/compose
@Composable
fun DeviceDetailsBottomSheet(
    device: PairedDevice,
    onDismiss: () -> Unit,
    onViewSessions: () -> Unit,
    onUnpair: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = device.displayName ?: device.phoneNumber,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Statistics
            StatRow(label = "Total Sessions", value = device.totalSessions.toString())
            StatRow(label = "Messages Forwarded", value = device.totalMessagesForwarded.toString())

            // Actions
            TextButton(onClick = onViewSessions) { Text("View Sessions") }
            TextButton(onClick = onUnpair, colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )) { Text("Unpair Device") }
        }
    }
}
```

### Pattern 3: Collapsible Section Header
**What:** Header with expand/collapse toggle indicator
**When to use:** Sections that can be hidden to reduce clutter

**Example:**
```kotlin
// Based on existing app patterns
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsibleSectionHeader(
    title: String,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggle,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$title ($count)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                              else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand"
            )
        }
    }
}
```

### Pattern 4: Two-Line Device Card with Statistics
**What:** Card showing phone number + role on top line, stats on bottom line
**When to use:** Compact display of device info with metrics

**Example:**
```kotlin
// Extending existing DeviceCard pattern from PairedDevicesScreen.kt
@Composable
fun DeviceHistoryCard(
    device: PairedDevice,
    hasActiveSession: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Top line: phone number + role badge + active indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = device.displayName ?: device.phoneNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    RoleBadge(role = device.role)
                    if (hasActiveSession) {
                        ActiveBadge()
                    }
                }

                // Bottom line: statistics
                Text(
                    text = "Sessions: ${device.totalSessions}  •  Messages: ${device.totalMessagesForwarded}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

### Pattern 5: Relative Time Formatting
**What:** Format timestamps as "just now", "2 minutes ago", "3 days ago"
**When to use:** Showing last activity or session times

**Example:**
```kotlin
import android.text.format.DateUtils

fun formatRelativeTime(timestamp: Long): String {
    return DateUtils.getRelativeTimeSpanString(
        timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
}

// Usage in composable
Text(
    text = "Last active: ${formatRelativeTime(device.lastActivityAt)}",
    style = MaterialTheme.typography.bodySmall
)
```

### Pattern 6: Skeleton Loading State
**What:** Placeholder UI while data loads
**When to use:** Initial screen load or data refresh

**Example:**
```kotlin
// Simple shimmer effect without external library
@Composable
fun SkeletonDeviceCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
                )
            }
        }
    }
}
```

### Anti-Patterns to Avoid
- **Fetching full ForwardingSession list per device in list view:** Only fetch aggregate counts; detailed sessions belong in a separate screen
- **Not handling empty removed section:** Hide section header entirely when no removed devices exist
- **Using state hoisting without proper key:** LazyColumn items must have stable keys for animations
- **Blocking UI on database queries:** Always use Flow/StateFlow, never blocking calls in composables

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Relative time formatting | Custom date formatter | Android DateUtils.getRelativeTimeSpanString | Handles localization, edge cases |
| Shimmer loading effect | Complex custom animation | Simple rememberInfiniteTransition | Sufficient for skeleton cards |
| Bottom sheet state | Custom state management | rememberModalBottomSheetState | Material3 handles dismiss, drag |
| Device query with archive filter | Custom WHERE clauses | Add DAO methods: getActiveDevices(), getArchivedDevices() | Type-safe, cached |
| Active session detection | Loop through sessions in UI | Join query or separate Flow | Keep UI logic simple |

**Key insight:** The database schema already supports everything needed (isArchived, totalSessions, totalMessagesForwarded). Don't add new columns or tables—just add query methods if needed.

## Common Pitfalls

### Pitfall 1: Loading Full Session History Per Device
**What goes wrong:** UI becomes extremely slow with many devices/sessions
**Why it happens:** Temptation to show "last session date" by loading all sessions
**How to avoid:** Use aggregate columns (totalSessions already exists), or add lastSessionAt column if needed
**Warning signs:** LazyColumn stutters, long load times on screen open

### Pitfall 2: Not Filtering Removed Devices Properly
**What goes wrong:** Removed devices appear in active section or vice versa
**Why it happens:** isArchived column not checked in DAO query
**How to avoid:** Modify existing DAO queries to filter by isArchived = false for active, true for removed
**Warning signs:** Unpaired devices still showing in "Active" section

### Pitfall 3: Bottom Sheet Dismiss Fighting with Click Handler
**What goes wrong:** Clicking outside sheet triggers both dismiss and background navigation
**Why it happens:** Event propagation not stopped
**How to avoid:** ModalBottomSheet handles this automatically; don't add extra click handlers to scrim
**Warning signs:** Navigation jumps after dismissing sheet

### Pitfall 4: Forgetting Role Context in Actions
**What goes wrong:** "Unpair" action doesn't work because phone number alone isn't unique
**Why it happens:** PairedDevice has composite key (phoneNumber + role)
**How to avoid:** Always pass both phoneNumber and role to repository methods
**Warning signs:** Database query returns null or wrong device

### Pitfall 5: Not Styling Removed Device Cards Differently
**What goes wrong:** Removed devices look identical to active devices
**Why it happens:** Forgot to apply muted styling per CONTEXT.md decision
**How to avoid:** Use lower alpha on removed cards (e.g., .copy(alpha = 0.6f))
**Warning signs:** User confusion about which devices are active

### Pitfall 6: Empty State Without Illustration
**What goes wrong:** Empty screen shows only text, looks unpolished
**Why it happens:** Illustration step skipped
**How to avoid:** Use Icon in circular background (pattern from PairedDevicesScreen.kt empty state)
**Warning signs:** Empty state looks plain compared to other screens

## Code Examples

Verified patterns from official sources and existing codebase:

### Material3 Badge for Active Session Indicator
```kotlin
// Source: https://m3.material.io/components/badges
@Composable
fun ActiveBadge() {
    Badge(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Text(
            text = "Active",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}
```

### Role Badge (Existing Pattern)
```kotlin
// Adapted from existing StatusBadge in PairedDevicesScreen.kt
@Composable
fun RoleBadge(role: DeviceRole) {
    val (backgroundColor, textColor, text) = when (role) {
        DeviceRole.SOURCE -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "SOURCE"
        )
        DeviceRole.TARGET -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "TARGET"
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}
```

### ViewModel Pattern with Device History
```kotlin
// Following existing HomeViewModel.kt pattern
class DeviceHistoryViewModel(
    private val deviceRepository: PairedDeviceRepository,
    private val sessionRepository: ForwardingSessionRepository
) : ViewModel() {

    // Active devices (not archived)
    val activeDevices: StateFlow<List<DeviceWithSession>> = combine(
        deviceRepository.getActiveDevices(), // Need to add this DAO method
        sessionRepository.getActiveSessions()
    ) { devices, sessions ->
        devices.map { device ->
            val hasActiveSession = sessions.any { it.devicePhoneNumber == device.phoneNumber }
            DeviceWithSession(device, hasActiveSession)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Removed devices (archived)
    val removedDevices: StateFlow<List<PairedDevice>> =
        deviceRepository.getArchivedDevices() // Need to add this DAO method
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    data class DeviceWithSession(
        val device: PairedDevice,
        val hasActiveSession: Boolean
    )

    class Factory(
        private val deviceRepository: PairedDeviceRepository,
        private val sessionRepository: ForwardingSessionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DeviceHistoryViewModel(deviceRepository, sessionRepository) as T
        }
    }
}
```

### Required DAO Methods
```kotlin
// Add to PairedDeviceDao.kt
@Query("SELECT * FROM paired_devices WHERE is_archived = 0 AND pairing_status = 'APPROVED'")
fun getActiveDevices(): Flow<List<PairedDevice>>

@Query("SELECT * FROM paired_devices WHERE is_archived = 1 ORDER BY archived_at DESC")
fun getArchivedDevices(): Flow<List<PairedDevice>>
```

### Navigation Integration
```kotlin
// Add to Screen.kt
sealed class Screen(val route: String) {
    // ... existing screens
    data object DeviceHistory : Screen("device_history")
}

// Add to NavGraph.kt
composable(Screen.DeviceHistory.route) {
    val viewModel = viewModel<DeviceHistoryViewModel>(
        factory = DeviceHistoryViewModel.Factory(
            deviceRepository = deviceRepository,
            sessionRepository = sessionRepository
        )
    )
    DeviceHistoryScreen(
        viewModel = viewModel,
        onNavigateBack = { navController.navigateUp() },
        onNavigateToSessionHistory = { phoneNumber ->
            navController.navigate(Screen.SessionHistory.createRoute(phoneNumber))
        }
    )
}

// Add to HomeScreen.kt Quick Actions
QuickActionCard(
    icon = Icons.Default.History, // or similar icon
    title = "Device History",
    subtitle = "View all devices and their activity",
    onClick = onNavigateToDeviceHistory
)
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Manual timestamp formatting | DateUtils.getRelativeTimeSpanString | Android 1.6+ (2009) | Automatic localization |
| BottomSheetDialogFragment | Material3 ModalBottomSheet | Material3 (2022) | Compose-native, simpler state |
| External shimmer libraries | Built-in rememberInfiniteTransition | Compose 1.0 (2021) | No dependency needed |
| Badge via custom composable | Material3 Badge component | Material3 (2022) | Consistent styling |
| Custom collapsible sections | Simple remember + AnimatedVisibility | Compose 1.0 (2021) | Declarative, less code |

**Deprecated/outdated:**
- BottomSheetBehavior (View system) - Replaced by ModalBottomSheet in Compose
- Manual shimmer with Canvas - rememberInfiniteTransition + Modifier.alpha simpler
- Third-party relative date libraries - DateUtils handles most cases

## Open Questions

1. **What icon should be used for Device History in Quick Actions?**
   - What we know: Need an icon that represents "history" or "devices"
   - What's unclear: Material Icons doesn't have perfect match
   - Recommendation: Use `Icons.Default.History` or `Icons.Default.Devices` - planner decides

2. **Should we show "last session date" on device cards?**
   - What we know: Cards show "Sessions: X • Messages: Y"
   - What's unclear: CONTEXT.md doesn't specify showing last session date
   - Recommendation: Skip for Phase 17, add in Phase 18 (Session History) if needed

3. **Should empty removed section hide header completely?**
   - What we know: CONTEXT.md says "Claude's discretion"
   - Recommendation: Hide header entirely when 0 removed devices (cleaner UI)

## Sources

### Primary (HIGH confidence)
- **Codebase analysis:**
  - `/app/src/main/java/dev/notyouraverage/smscourier/composables/screens/HomeScreen.kt` - Quick Actions pattern
  - `/app/src/main/java/dev/notyouraverage/smscourier/composables/screens/PairedDevicesScreen.kt` - Device card, empty state patterns
  - `/app/src/main/java/dev/notyouraverage/smscourier/data/entities/PairedDevice.kt` - isArchived, aggregate columns
  - `/app/src/main/java/dev/notyouraverage/smscourier/data/dao/PairedDeviceDao.kt` - Existing query methods
  - `/app/src/main/java/dev/notyouraverage/smscourier/viewmodels/HomeViewModel.kt` - ViewModel factory pattern

- [**Jetpack Compose ModalBottomSheet Documentation**](https://developer.android.com/develop/ui/compose) - Official Material3 bottom sheet implementation
- [**Android DateUtils API Reference**](https://developer.android.com/reference/android/text/format/DateUtils) - Built-in relative time formatting
- [**Material Design 3 Badge Guidelines**](https://m3.material.io/components/badges) - Official badge component design

### Secondary (MEDIUM confidence)
- [**Modal Bottom Sheets in Android: Practical Patterns (2026)**](https://thelinuxcode.com/modal-bottom-sheets-in-android-practical-patterns-and-runnable-examples-2026/) - Best practices for bottom sheet state management
- [**Seamless shimmer integration with existing Compose code**](https://proandroiddev.com/seamless-shimmer-integration-with-existing-compose-code-b95cc3bbcd17) - ProAndroidDev article on simple shimmer without libraries
- [**Easiest Way to Create a Shimmer Effect in Jetpack Compose**](https://www.droidcon.com/2025/07/22/easiest-way-to-create-a-shimmer-effect-in-jetpack-compose/) - droidcon 2025 article

### Tertiary (LOW confidence)
- [**valentinilk/compose-shimmer**](https://github.com/valentinilk/compose-shimmer) - External shimmer library (not recommended - adds dependency)
- [**BoltUIX/Empty-State-Mobile-UIX-Jetpack-Compose**](https://github.com/BoltUIX/Empty-State-Mobile-UIX-Jetpack-Compose) - Empty state examples (reference only)

## Metadata

**Confidence breakdown:**
- Standard Stack: HIGH - All dependencies already in project
- Device Card Patterns: HIGH - Existing PairedDevicesScreen.kt demonstrates pattern
- Bottom Sheet Implementation: HIGH - Material3 ModalBottomSheet well-documented
- Database Queries: HIGH - Schema already supports all features, just need new DAO methods
- Collapsible Sections: HIGH - Simple remember/mutableStateOf pattern
- Shimmer Loading: MEDIUM - Multiple approaches work; chose simplest
- Empty State Design: MEDIUM - Pattern exists but needs icon decision

**Research date:** 2026-02-06
**Valid until:** 2026-03-06 (30 days - stable patterns)

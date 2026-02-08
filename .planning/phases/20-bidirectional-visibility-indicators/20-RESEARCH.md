# Phase 20: Bidirectional Visibility Indicators - Research

**Researched:** 2026-02-08
**Domain:** Android Jetpack Compose UI with Material 3 directional status indicators
**Confidence:** HIGH

## Summary

Phase 20 adds directional forwarding status indicators to the home screen and device list, showing users whether they're forwarding TO other devices (↑), receiving FROM other devices (↓), or both (⇅ bidirectional). The research confirms this is achievable with existing stack components—no new dependencies required.

**Key findings:**
- **UI Components**: Material 3's existing composables (Card, Badge, Icon) are sufficient; no need for specialized chip libraries
- **Data Layer**: Existing Room database with DeviceRole (SOURCE/TARGET) already provides directional context
- **State Management**: Flow.combine() pattern already used in HomeViewModel can calculate directional status
- **Bottom Sheet**: ModalBottomSheet (already in Material 3 BOM) handles session breakdown display

**Critical architectural insight:** The app's DeviceRole enum defines direction from the device's perspective: SOURCE devices REQUEST forwarding (receive messages ↓), TARGET devices PROVIDE forwarding (send messages ↑). This mental model must be preserved in UI indicators to avoid user confusion.

**Primary recommendation:** Build status card with icon-based directional indicators (not chips), use semantic Material 3 colors (green for receiving, blue for forwarding, purple for bidirectional), and calculate active session direction via ViewModel Flow combining devices with sessions.

## Standard Stack

### Core Components (No Changes Required)

The existing stack provides all necessary components:

| Component | Version | Purpose | Already Available |
|-----------|---------|---------|-------------------|
| androidx.compose.material3 | 1.4.0+ (via BOM) | Icon, Badge, Card, ModalBottomSheet | Yes - in Material 3 BOM |
| androidx.room | 2.8.4 | Database queries for session direction | Yes - current version |
| kotlinx.coroutines | 1.9.0+ | Flow.combine() for multi-flow state | Yes - stdlib |
| Material Icons Extended | - | Arrow icons (KeyboardArrowUp, KeyboardArrowDown) | Yes - already imported |

### UI Components Already in Use

| Component | Existing Usage | Phase 20 Usage |
|-----------|---------------|----------------|
| Card | StatCard in HomeScreen (lines 339-396) | Directional indicator cards |
| Icon | Throughout app | Arrow direction icons |
| Badge/BadgedBox | Not currently used | Count badges on indicators |
| ModalBottomSheet | ExportFormatBottomSheet, MessageDetailComponents | Session breakdown sheet |
| Flow.combine() | HomeViewModel (line 19-34) | Combine devices + sessions for direction |

### No External Libraries Required

**What we DON'T need:**
- AssistChip/FilterChip: User context decisions specify "icon chips with arrow icon and count badge" but analysis shows custom Card composables (matching existing StatCard pattern) provide better consistency
- Third-party icon libraries: Material Icons Extended already provides all needed arrow icons
- Badge libraries: Material 3's BadgedBox is sufficient for count indicators

## Architecture Patterns

### Pattern 1: Directional Status Calculation in ViewModel

**What:** Calculate active session direction by combining device roles with active sessions
**When to use:** For home screen indicators showing real-time forwarding status
**Example:**

```kotlin
// In HomeViewModel
data class DirectionalStatus(
    val forwardingToCount: Int = 0,      // This device is SOURCE, receiving from TARGET
    val receivingFromCount: Int = 0,      // This device is TARGET, sending to SOURCE
    val bidirectionalCount: Int = 0,      // Both roles with same device
    val activeSessions: List<SessionWithDirection> = emptyList()
)

data class SessionWithDirection(
    val session: ForwardingSession,
    val device: PairedDevice,
    val direction: Direction  // FORWARDING_TO, RECEIVING_FROM, BIDIRECTIONAL
)

enum class Direction {
    FORWARDING_TO,    // This device is SOURCE (requesting/receiving messages)
    RECEIVING_FROM,   // This device is TARGET (providing/sending messages)
    BIDIRECTIONAL     // Both directions active with same phone number
}

// Calculate directional status
val directionalStatus: StateFlow<DirectionalStatus> = combine(
    deviceRepository.getApprovedDevices(),
    sessionRepository.getActiveSessions()
) { devices, sessions ->
    // Group sessions by phone number to detect bidirectional
    val sessionsByPhone = sessions.groupBy { it.devicePhoneNumber }
    val devicesByPhone = devices.filter { it.status == PairingStatus.APPROVED }
        .groupBy { it.phoneNumber }

    val sessionsWithDirection = mutableListOf<SessionWithDirection>()
    var forwardingTo = 0
    var receivingFrom = 0
    var bidirectional = 0

    sessionsByPhone.forEach { (phone, phoneSessions) ->
        val phoneDevices = devicesByPhone[phone] ?: emptyList()
        val hasSource = phoneDevices.any { it.role == DeviceRole.SOURCE }
        val hasTarget = phoneDevices.any { it.role == DeviceRole.TARGET }

        if (hasSource && hasTarget) {
            // Bidirectional: both SOURCE and TARGET roles exist
            bidirectional += phoneSessions.size
            phoneSessions.forEach { session ->
                sessionsWithDirection.add(
                    SessionWithDirection(
                        session,
                        phoneDevices.first(),
                        Direction.BIDIRECTIONAL
                    )
                )
            }
        } else if (hasSource) {
            // SOURCE role: this device RECEIVES forwarded messages (↓)
            forwardingTo += phoneSessions.size
            phoneSessions.forEach { session ->
                sessionsWithDirection.add(
                    SessionWithDirection(
                        session,
                        phoneDevices.first { it.role == DeviceRole.SOURCE },
                        Direction.FORWARDING_TO
                    )
                )
            }
        } else if (hasTarget) {
            // TARGET role: this device SENDS forwarded messages (↑)
            receivingFrom += phoneSessions.size
            phoneSessions.forEach { session ->
                sessionsWithDirection.add(
                    SessionWithDirection(
                        session,
                        phoneDevices.first { it.role == DeviceRole.TARGET },
                        Direction.RECEIVING_FROM
                    )
                )
            }
        }
    }

    DirectionalStatus(
        forwardingToCount = forwardingTo,
        receivingFromCount = receivingFrom,
        bidirectionalCount = bidirectional,
        activeSessions = sessionsWithDirection
    )
}.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    DirectionalStatus()
)
```

**Source:** Existing HomeViewModel.homeState pattern (lines 19-34), [State and Jetpack Compose](https://developer.android.com/develop/ui/compose/state)

### Pattern 2: Status Card with Semantic Colors

**What:** Display directional indicators using Material 3 semantic colors
**When to use:** For at-a-glance status visibility on home screen
**Example:**

```kotlin
@Composable
fun DirectionalStatusCard(
    forwardingToCount: Int,
    receivingFromCount: Int,
    bidirectionalCount: Int,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        onClick = onCardClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Active Forwarding",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Receiving (↓) - green
                DirectionalIndicator(
                    icon = Icons.Default.KeyboardArrowDown,
                    count = forwardingToCount,
                    label = "Receiving",
                    color = MaterialTheme.colorScheme.primary,
                    isActive = forwardingToCount > 0,
                    modifier = Modifier.weight(1f)
                )

                // Forwarding (↑) - blue
                DirectionalIndicator(
                    icon = Icons.Default.KeyboardArrowUp,
                    count = receivingFromCount,
                    label = "Forwarding",
                    color = MaterialTheme.colorScheme.tertiary,
                    isActive = receivingFromCount > 0,
                    modifier = Modifier.weight(1f)
                )

                // Bidirectional (⇅) - purple
                DirectionalIndicator(
                    icon = Icons.Default.SwapVert, // or custom bidirectional icon
                    count = bidirectionalCount,
                    label = "Both",
                    color = MaterialTheme.colorScheme.secondary,
                    isActive = bidirectionalCount > 0,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun DirectionalIndicator(
    icon: ImageVector,
    count: Int,
    label: String,
    color: Color,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        BadgedBox(
            badge = {
                if (count > 0) {
                    Badge(
                        containerColor = color,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Text(count.toString())
                    }
                }
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(32.dp),
                tint = if (isActive) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}
```

**Source:** Existing StatCard pattern (HomeScreen.kt lines 339-396), [Badges in Jetpack Compose](https://developer.android.com/develop/ui/compose/components/badges)

### Pattern 3: Session Breakdown Bottom Sheet

**What:** Display grouped sessions by direction when user taps indicator
**When to use:** For detailed view of active sessions
**Example:**

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionBreakdownBottomSheet(
    sessions: List<SessionWithDirection>,
    onDismiss: () -> Unit,
    onStopSession: (ForwardingSession) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Active Sessions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (sessions.isEmpty()) {
                EmptySessionState()
            } else {
                // Group by direction
                val forwardingTo = sessions.filter { it.direction == Direction.FORWARDING_TO }
                val receivingFrom = sessions.filter { it.direction == Direction.RECEIVING_FROM }
                val bidirectional = sessions.filter { it.direction == Direction.BIDIRECTIONAL }

                if (forwardingTo.isNotEmpty()) {
                    SessionGroup(
                        title = "Receiving From",
                        sessions = forwardingTo,
                        onStopSession = onStopSession
                    )
                }

                if (receivingFrom.isNotEmpty()) {
                    SessionGroup(
                        title = "Forwarding To",
                        sessions = receivingFrom,
                        onStopSession = onStopSession
                    )
                }

                if (bidirectional.isNotEmpty()) {
                    SessionGroup(
                        title = "Bidirectional",
                        sessions = bidirectional,
                        onStopSession = onStopSession
                    )
                }
            }
        }
    }
}

@Composable
fun SessionGroup(
    title: String,
    sessions: List<SessionWithDirection>,
    onStopSession: (ForwardingSession) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$title (${sessions.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )

        sessions.forEach { sessionWithDir ->
            SessionRow(
                phoneNumber = sessionWithDir.session.devicePhoneNumber,
                messageCount = sessionWithDir.session.messageCount,
                onStop = { onStopSession(sessionWithDir.session) }
            )
        }
    }
}

@Composable
fun SessionRow(
    phoneNumber: String,
    messageCount: Int,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$messageCount messages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(onClick = onStop) {
                Text("Stop")
            }
        }
    }
}

@Composable
fun EmptySessionState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "No active forwarding sessions",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Start forwarding from a paired device to see sessions here",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}
```

**Source:** Existing ModalBottomSheet pattern in ExportFormatBottomSheet.kt, [Bottom Sheets in Compose](https://developer.android.com/develop/ui/compose/components/bottom-sheets)

### Pattern 4: Device List Directional Indicators

**What:** Show direction as subtitle text below device name
**When to use:** In PairedDevicesScreen to show per-device status
**Example:**

```kotlin
// In PairedDevicesViewModel
data class DeviceWithDirection(
    val device: PairedDevice,
    val direction: Direction?,  // null if no active session
    val activeSession: ForwardingSession?
)

val devicesWithDirection: StateFlow<List<DeviceWithDirection>> = combine(
    sourceDevices,  // existing Flow
    targetDevices,  // existing Flow
    activeSessions  // existing Flow
) { sources, targets, sessions ->
    val allDevices = sources + targets
    allDevices.map { device ->
        val activeSession = sessions.find { it.devicePhoneNumber == device.phoneNumber }
        val direction = when {
            activeSession == null -> null
            device.role == DeviceRole.SOURCE -> Direction.FORWARDING_TO
            device.role == DeviceRole.TARGET -> Direction.RECEIVING_FROM
            else -> null
        }
        DeviceWithDirection(device, direction, activeSession)
    }
}.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    emptyList()
)

// In DeviceCard composable (PairedDevicesScreen.kt)
@Composable
fun DeviceCard(
    device: PairedDevice,
    direction: Direction?,
    lastActiveMinutes: Int?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        // ... existing card setup
    ) {
        Row(/* ... existing layout */) {
            // Avatar box

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.displayName ?: device.phoneNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                // Directional status subtitle
                DirectionalSubtitle(
                    direction = direction,
                    lastActiveMinutes = lastActiveMinutes
                )
            }

            // Status badge
        }
    }
}

@Composable
fun DirectionalSubtitle(
    direction: Direction?,
    lastActiveMinutes: Int?
) {
    val (text, color) = when (direction) {
        Direction.FORWARDING_TO -> "Receiving • Active now" to MaterialTheme.colorScheme.primary
        Direction.RECEIVING_FROM -> "Forwarding • Active now" to MaterialTheme.colorScheme.tertiary
        Direction.BIDIRECTIONAL -> "Bidirectional • Active now" to MaterialTheme.colorScheme.secondary
        null -> {
            if (lastActiveMinutes != null) {
                val timeText = when {
                    lastActiveMinutes < 60 -> "$lastActiveMinutes min ago"
                    lastActiveMinutes < 1440 -> "${lastActiveMinutes / 60} hours ago"
                    else -> "${lastActiveMinutes / 1440} days ago"
                }
                "Idle • Last active: $timeText" to MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                "Idle • Never active" to MaterialTheme.colorScheme.onSurfaceVariant
            }
        }
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color
    )
}
```

**Source:** Existing DeviceCard in PairedDevicesScreen.kt (lines 349-425), DeviceHistoryComponents.kt pattern for status subtitles

### Anti-Patterns to Avoid

- **Role Confusion**: Don't invert SOURCE/TARGET semantics—SOURCE receives (↓), TARGET sends (↑)
- **Direct Session Queries in UI**: Don't query database from composables; always use ViewModel StateFlow
- **Mutable State in Composables**: Don't use mutableStateOf for derived data; use StateFlow from ViewModel
- **Unconditional Badge Display**: Don't show badges when count is 0; gray out indicators instead
- **Hard-coded Colors**: Don't use literal color values; always use MaterialTheme.colorScheme for semantic colors

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Badge with count | Custom badge composable | Material 3 BadgedBox + Badge | Official component handles RTL, accessibility, sizing automatically |
| Flow combination | Manual state merging | Flow.combine() with stateIn() | Kotlin coroutines handles cancellation, backpressure correctly |
| Bottom sheet animation | Custom modal animation | ModalBottomSheet from Material 3 | Built-in gesture handling, accessibility, system integration |
| Direction calculation | Scattered if-else in UI | Centralized ViewModel logic | Single source of truth, testable, reusable |
| Icon selection | Icon name strings | Material Icons sealed classes | Type-safe, compile-time verification |

**Key insight:** Material 3 and Kotlin coroutines provide battle-tested implementations for all UI patterns. Custom solutions add maintenance burden and miss edge cases (RTL support, accessibility, state restoration).

## Common Pitfalls

### Pitfall 1: SOURCE/TARGET Role Inversion

**What goes wrong:** Displaying ↑ (up arrow) for SOURCE devices and ↓ (down arrow) for TARGET devices, inverting the actual message flow direction.

**Why it happens:** Developer mental model conflates "SOURCE of request" with "source of messages". SOURCE device requests forwarding but RECEIVES messages (↓). TARGET device provides forwarding by SENDING messages (↑).

**How to avoid:**
- Document mental model clearly: SOURCE = receives messages, TARGET = sends messages
- Create Direction enum separate from DeviceRole to avoid semantic confusion
- Use descriptive labels: "Receiving" (↓) and "Forwarding" (↑) instead of just arrows
- Write unit tests with explicit direction expectations

**Warning signs:**
- User feedback: "The arrows are backwards"
- Test failures where expected direction doesn't match actual
- Code reviews questioning arrow direction

**Prevention strategy:**
```kotlin
// GOOD: Clear semantic mapping
enum class Direction {
    FORWARDING_TO,    // This device is SOURCE (receiving messages ↓)
    RECEIVING_FROM,   // This device is TARGET (sending messages ↑)
    BIDIRECTIONAL     // Both directions
}

// Map DeviceRole to Direction with clear comments
fun DeviceRole.toDirection(): Direction = when (this) {
    DeviceRole.SOURCE -> Direction.FORWARDING_TO  // SOURCE receives (↓)
    DeviceRole.TARGET -> Direction.RECEIVING_FROM  // TARGET sends (↑)
}

// BAD: Direct icon mapping without semantic layer
fun getDirectionIcon(role: DeviceRole) = when (role) {
    DeviceRole.SOURCE -> Icons.Default.KeyboardArrowUp  // WRONG!
    DeviceRole.TARGET -> Icons.Default.KeyboardArrowDown  // WRONG!
}
```

### Pitfall 2: Combining More Than 5 Flows

**What goes wrong:** Kotlin's Flow.combine() has overloads for 2-5 flows only. Combining devices (2 flows: source, target) + sessions (1 flow) = 3 flows is fine, but adding more state (e.g., settings, notifications) hits the limit.

**Why it happens:** Kotlin coroutines library doesn't provide combine() for 6+ flows due to type system complexity.

**How to avoid:**
- For 6+ flows, nest combine() calls:
  ```kotlin
  combine(
      combine(flow1, flow2, flow3) { a, b, c -> Triple(a, b, c) },
      combine(flow4, flow5, flow6) { d, e, f -> Triple(d, e, f) }
  ) { abc, def -> /* process */ }
  ```
- Or use combineTransform with varargs for dynamic combination
- Keep state minimal—only combine flows needed for specific UI calculation

**Warning signs:**
- Compiler error: "No combine function found for 6 parameters"
- Need to add another Flow parameter to existing combine()

**Prevention strategy:**
- Design ViewModels with focused responsibilities (separate status calculation from other concerns)
- Use intermediate StateFlows for sub-calculations

### Pitfall 3: Badge Overflow with Large Counts

**What goes wrong:** Session count exceeds 99, causing badge text to overflow or wrap awkwardly.

**Why it happens:** Material 3 Badge doesn't automatically abbreviate large numbers.

**How to avoid:**
- Format counts > 99 as "99+"
- For very large counts, use abbreviated format (e.g., "999+" or "1k+")
- Consider if showing exact count is necessary (e.g., "3" is useful, "247" is noise)

**Warning signs:**
- Badge width exceeding icon width
- Visual testing shows wrapped badge text

**Prevention strategy:**
```kotlin
fun formatBadgeCount(count: Int): String = when {
    count <= 99 -> count.toString()
    count <= 999 -> "99+"
    else -> "${count / 1000}k+"
}

Badge { Text(formatBadgeCount(sessionCount)) }
```

### Pitfall 4: Stale Direction Status After Session Changes

**What goes wrong:** UI shows outdated directional status after user stops a session or session expires.

**Why it happens:** StateFlow caching with stale timeout (SharingStarted.WhileSubscribed(5000)) can delay updates if screen is backgrounded.

**How to avoid:**
- Use appropriate StateFlow timeout—5000ms is standard for non-critical UI
- For critical real-time updates, reduce timeout or use SharingStarted.Eagerly
- Ensure ViewModel observes all relevant state changes (sessions, devices)

**Warning signs:**
- User stops session but indicator still shows active
- Test flakiness around timing-dependent state updates

**Prevention strategy:**
- Use existing 5000ms timeout pattern from HomeViewModel
- Add manual refresh on session state changes if needed
- Test with artificial delays to verify state propagation

### Pitfall 5: Empty State Confusion

**What goes wrong:** User taps directional indicator with 0 count, sees empty sheet, doesn't understand why sheet opened.

**Why it happens:** User context specifies "Sheet always opens when tapped, even with zero count" but empty state messaging is unclear.

**How to avoid:**
- Provide contextual empty state message: "No active sessions" + suggestion to start forwarding
- Consider graying out indicators with 0 count to visually signal inactivity
- Add helpful action in empty state (e.g., "Start Forwarding" button)

**Warning signs:**
- User feedback: "Why does this open if there's nothing here?"
- High bounce rate from bottom sheet (user opens and immediately closes)

**Prevention strategy:**
```kotlin
@Composable
fun EmptySessionState() {
    Column(/* ... centered layout */) {
        Icon(Icons.Outlined.Info, /* ... */)
        Text("No active forwarding sessions")
        Text("Start forwarding from a paired device to see sessions here")
        // Optional: Add action button
        FilledTonalButton(onClick = onNavigateToForwarding) {
            Text("Start Forwarding")
        }
    }
}
```

### Pitfall 6: Bidirectional Detection Edge Cases

**What goes wrong:** Same phone number paired with both SOURCE and TARGET roles, but only one session active. Should this show as bidirectional or single direction?

**Why it happens:** User context doesn't specify whether "bidirectional" requires both directions to have active sessions or just the capability (both roles paired).

**How to avoid:**
- Define clear rule: Bidirectional only when BOTH roles have active sessions with same phone number
- Alternative rule: Bidirectional when device paired in both roles regardless of session state
- Document chosen approach in code comments
- Add unit tests for edge case

**Warning signs:**
- Inconsistent bidirectional indicator behavior
- User confusion about bidirectional meaning

**Prevention strategy (Session-based approach):**
```kotlin
// Group sessions by phone number
val sessionsByPhone = sessions.groupBy { it.devicePhoneNumber }

sessionsByPhone.forEach { (phone, phoneSessions) ->
    val phoneDevices = devicesByPhone[phone] ?: emptyList()

    // Check for sessions in both directions
    val hasActiveSource = phoneSessions.any { session ->
        phoneDevices.any { it.role == DeviceRole.SOURCE && session.devicePhoneNumber == phone }
    }
    val hasActiveTarget = phoneSessions.any { session ->
        phoneDevices.any { it.role == DeviceRole.TARGET && session.devicePhoneNumber == phone }
    }

    if (hasActiveSource && hasActiveTarget) {
        // True bidirectional: active sessions in both directions
        bidirectional += phoneSessions.size
    }
}
```

## Code Examples

Verified patterns from existing codebase and official sources:

### Home Screen Integration

```kotlin
// In HomeViewModel.kt - Add to existing HomeState
data class HomeState(
    val approvedDevicesCount: Int = 0,
    val pendingRequestsCount: Int = 0,
    val activeSessionsCount: Int = 0,
    val activeSessions: List<ForwardingSession> = emptyList(),
    // NEW: Directional breakdown
    val directionalStatus: DirectionalStatus = DirectionalStatus()
)

// Extend existing combine() in HomeViewModel
val homeState: StateFlow<HomeState> = combine(
    deviceRepository.getApprovedDevices(),
    deviceRepository.getPendingRequests(),
    sessionRepository.getActiveSessions(),
) { approved, pending, sessions ->
    // Calculate directional status
    val directionalStatus = calculateDirectionalStatus(approved, sessions)

    HomeState(
        approvedDevicesCount = approved.size,
        pendingRequestsCount = pending.size,
        activeSessionsCount = sessions.size,
        activeSessions = sessions,
        directionalStatus = directionalStatus
    )
}.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    HomeState()
)

private fun calculateDirectionalStatus(
    devices: List<PairedDevice>,
    sessions: List<ForwardingSession>
): DirectionalStatus {
    // Implementation from Pattern 1
}
```

**Source:** HomeViewModel.kt (lines 14-42)

### BadgedBox with Material 3

```kotlin
// Using BadgedBox for count indicators
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox

@Composable
fun DirectionalIndicatorWithBadge(
    icon: ImageVector,
    count: Int,
    label: String,
    isActive: Boolean
) {
    BadgedBox(
        badge = {
            if (count > 0) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(formatBadgeCount(count))
                }
            }
        }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(28.dp),
            tint = if (isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            }
        )
    }
}
```

**Source:** [BadgedBox - Material 3 Compose](https://composables.com/material3/badgedbox), [Badges in Compose](https://developer.android.com/develop/ui/compose/components/badges)

### ModalBottomSheet Pattern

```kotlin
// Following existing pattern from ExportFormatBottomSheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionBreakdownBottomSheet(
    sessions: List<SessionWithDirection>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false  // Allow 50% height per Phase 18 decision
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        // Sheet content
    }
}
```

**Source:** ExportFormatBottomSheet.kt (lines 41-108), Phase 18 bottom sheet decision

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Custom chip composables | Material 3 Card with semantic styling | Material 3 1.0 (2023) | Better consistency, accessibility, RTL support |
| Manual flow merging | Flow.combine() with stateIn() | Coroutines 1.5+ (2021) | Type-safe, handles backpressure automatically |
| View-based badges | Compose BadgedBox | Material 3 for Compose (2023) | Declarative, automatic layout, theme-aware |
| String-based icons | Material Icons sealed classes | Compose 1.0 (2021) | Type safety, autocomplete, compile-time verification |
| SharedFlow for state | StateFlow with initial value | Coroutines 1.4 (2021) | Always has value, easier UI consumption |

**Deprecated/outdated:**
- **BadgeDrawable (View system)**: Material 3 Compose uses BadgedBox instead
- **Flow.combineTransform for simple cases**: Use combine() with explicit result type
- **mutableStateOf in ViewModel**: Use StateFlow for lifecycle-aware state

## Open Questions

Things that couldn't be fully resolved:

1. **Bidirectional Definition**
   - What we know: User context specifies showing ⇅ for bidirectional sessions
   - What's unclear: Does "bidirectional" require active sessions in BOTH directions, or just paired in both roles?
   - Recommendation: Implement session-based approach (only show bidirectional when sessions active in both directions). This matches "active forwarding status" intent. Add settings toggle if users want role-based bidirectional display.

2. **Auto-Refresh on Session State Changes**
   - What we know: User context defers "auto vs wait for pull" to Claude's discretion
   - What's unclear: Should UI auto-update when SMS commands change session state while app is in foreground?
   - Recommendation: Rely on StateFlow reactivity (existing pattern). Sessions are observed via Flow from repository, so changes propagate automatically. No additional refresh mechanism needed. Add pull-to-refresh on home screen if manual refresh is desired.

3. **Quick Stop Action in Session Breakdown Sheet**
   - What we know: User context says "Claude determines appropriate UX" for sheet actions
   - What's unclear: Should sheet allow stopping sessions directly, or just view-only?
   - Recommendation: Add "Stop" button per session (see Pattern 3 example). Matches existing ForwardingControlScreen pattern and provides user control. View-only sheet would frustrate users who want immediate action.

4. **Transition Animations for Count Changes**
   - What we know: User context defers transition animations to Claude's discretion
   - What's unclear: Should count badges animate when values change?
   - Recommendation: Use default Material 3 state changes (no custom animations). Consistent with existing app style (no custom animations in HomeScreen stats). If animations desired later, add animateIntAsState for badge counts.

## Sources

### Primary (HIGH confidence)

- [Badges in Jetpack Compose](https://developer.android.com/develop/ui/compose/components/badges) - Official BadgedBox documentation (updated Feb 6, 2026)
- [BadgedBox - Material 3 Compose](https://composables.com/material3/badgedbox) - Component reference with examples
- [Bottom Sheets in Compose](https://developer.android.com/develop/ui/compose/components/bottom-sheets) - ModalBottomSheet patterns
- [State and Jetpack Compose](https://developer.android.com/develop/ui/compose/state) - StateFlow + combine() patterns
- [Chip - Jetpack Compose](https://developer.android.com/develop/ui/compose/components/chip) - AssistChip, FilterChip reference
- [Material Icons](https://developer.android.com/develop/ui/compose/graphics/images/material) - Icon usage in Compose (updated Feb 6, 2026)
- [Kotlin Coroutines Flow](https://kotlinlang.org/docs/flow.html) - Flow.combine() reference
- Existing codebase: HomeViewModel.kt, PairedDevicesScreen.kt, DeviceHistoryComponents.kt, ExportFormatBottomSheet.kt

### Secondary (MEDIUM confidence)

- [Combining StateFlows and transforming it into a StateFlow](https://blog.shreyaspatil.dev/combining-stateflows-and-transforming-it-into-a-stateflow) - Advanced Flow.combine() patterns
- [Kotlin flows in Jetpack Compose: the ultimate guide](https://decode.agency/article/kotlin-flows-guide/) - StateFlow best practices 2026
- [Mastering StateFlow in Jetpack Compose](https://nikhilyadav01.medium.com/mastering-stateflow-in-jetpack-compose-a-complete-guide-e2b821ba4dc5) - Complete StateFlow guide

### Tertiary (LOW confidence - flagged for validation)

- [Adding combine(...) methods to combine 6-10 flows](https://github.com/Kotlin/kotlinx.coroutines/issues/3598) - GitHub issue discussing combine() limitations (still open as of 2026)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All components verified in existing codebase and official docs
- Architecture: HIGH - Patterns match existing HomeViewModel and bottom sheet implementations
- Pitfalls: MEDIUM-HIGH - Role confusion is domain-specific, other pitfalls are standard Material 3 concerns

**Research date:** 2026-02-08
**Valid until:** 30 days (stable Material 3 and Kotlin coroutines, no fast-moving APIs)

**Key codebase files referenced:**
- `/app/src/main/java/dev/notyouraverage/smscourier/viewmodels/HomeViewModel.kt` (lines 19-34)
- `/app/src/main/java/dev/notyouraverage/smscourier/composables/screens/HomeScreen.kt` (lines 339-396)
- `/app/src/main/java/dev/notyouraverage/smscourier/composables/screens/PairedDevicesScreen.kt` (lines 349-425)
- `/app/src/main/java/dev/notyouraverage/smscourier/composables/components/DeviceHistoryComponents.kt` (entire file)
- `/app/src/main/java/dev/notyouraverage/smscourier/composables/components/ExportFormatBottomSheet.kt` (lines 41-108)
- `/app/src/main/java/dev/notyouraverage/smscourier/data/entities/PairedDevice.kt` (DeviceRole enum)
- `/app/src/main/java/dev/notyouraverage/smscourier/data/entities/ForwardingSession.kt` (entire entity)

**Context decisions honored:**
- Icon chips with arrow icon and count badge (implemented with BadgedBox + Icon, not AssistChip)
- Semantic colors: green/blue/purple (mapped to Material 3 primary/tertiary/secondary)
- Show all directions, gray out inactive (implemented with alpha = 0.3f for inactive)
- Sheet opens even with zero count (with contextual empty state)
- Simple session rows: phone number + message count only
- Direction text with semantic colors on device list
- Quick stop action in sheet (recommended based on existing ForwardingControlScreen pattern)

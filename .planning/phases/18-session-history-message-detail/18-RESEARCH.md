# Phase 18: Session History & Message Detail - Research

**Researched:** 2026-02-06
**Domain:** Android Paging 3, DataStore Preferences, Material 3 ModalBottomSheet, Jetpack Compose UI
**Confidence:** HIGH

## Summary

Phase 18 implements session history browsing and message detail viewing for the SMS Courier app. The phase requires integrating Paging 3 for efficient lazy loading of large session and message lists, DataStore Preferences for persisting the session/contact view toggle, Material 3 ModalBottomSheet for message detail display, and custom formatting utilities for relative dates and durations.

The codebase already has established patterns: Room 2.6.1 database (v7 schema with ForwardingSession and ForwardedMessage tables), DataStore Preferences usage in SettingsRepository, ModalBottomSheet implementation in DeviceHistoryScreen, and Navigation Compose with URL-encoded phone number parameters. The standard stack for this phase leverages existing dependencies (paging-compose 3.4.0 available, currently not added) and follows established architectural patterns (ViewModel with StateFlow, Repository pattern, Compose screens with LargeTopAppBar).

Key technical decisions from CONTEXT.md: relative date formatting ("Today", "Yesterday", "3 days ago"), relative duration formatting ("2h 15m"), active session indicators with pulsing dot, inline message expansion (not separate screens), and bottom sheet opening at 50% height with drag-to-full-screen capability.

**Primary recommendation:** Add Paging 3 Compose dependency, create PagingSource implementations in DAOs for sessions and messages, use collectAsLazyPagingItems() in LazyColumn, persist view toggle preference in DataStore, implement ModalBottomSheet for message detail with rememberModalBottomSheetState(), and create utility functions for relative date/duration formatting using Android's built-in APIs.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| androidx.paging:paging-compose | 3.4.0 | Paging 3 Compose integration | Official AndroidX library for lazy loading large datasets in Compose LazyColumn, mandatory per STATE.md |
| androidx.room:room-paging | 2.6.1 | Room Paging integration | Provides PagingSource return type support in Room DAOs, version locked to match existing Room 2.6.1 |
| androidx.datastore:datastore-preferences | 1.1.1 | Settings persistence | Already in use (build.gradle), Flow-based reactive preferences storage |
| androidx.compose.material3:material3 | (via BOM 2024.08.00) | Material 3 UI components | Already in use, provides ModalBottomSheet composable |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| android.text.format.DateUtils | Platform API | Relative date formatting | Use for "Today", "Yesterday", "X days ago" formatting |
| android.icu.text.RelativeDateTimeFormatter | Platform API 24+ | ICU-based relative dates | Optional alternative to DateUtils for more localization control |
| java.time.Duration | Platform API 26+ | Duration calculations | Use for calculating "2h 15m" from milliseconds (minSdk 34, safe) |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Paging 3 | Manual pagination with offset/limit | Paging 3 is mandatory per STATE.md, manual approach doesn't handle configuration changes or placeholders |
| DataStore Preferences | SharedPreferences | DataStore required per PROJECT.md decisions, SharedPreferences synchronous and error-prone |
| ModalBottomSheet | Custom Dialog | ModalBottomSheet is established pattern (Phase 17), custom approach inconsistent |

**Installation:**
```gradle
// build.gradle.kts (app module)
dependencies {
    // Paging 3 Compose (NEW - add this)
    implementation("androidx.paging:paging-compose:3.4.0")

    // Already present in build.gradle
    implementation(libs.androidx.room.runtime)      // 2.6.1
    implementation(libs.androidx.room.ktx)          // 2.6.1
    implementation(libs.androidx.datastore.preferences) // 1.1.1
    implementation(libs.androidx.material3)         // via BOM
}
```

## Architecture Patterns

### Recommended Project Structure
```
app/src/main/java/dev/notyouraverage/smscourier/
├── composables/
│   ├── screens/
│   │   ├── SessionHistoryScreen.kt         # Main screen with session list
│   │   └── (DeviceHistoryScreen.kt)        # Existing, navigates here
│   └── components/
│       ├── SessionCard.kt                   # Individual session card
│       ├── MessageDetailBottomSheet.kt      # Bottom sheet for messages
│       └── MessageRow.kt                    # Expandable message row
├── viewmodels/
│   └── SessionHistoryViewModel.kt           # Manages session/message paging
├── repository/
│   ├── (ForwardingSessionRepository.kt)     # Existing, add paging methods
│   └── (ForwardedMessageRepository.kt)      # Existing, add paging methods
├── data/
│   └── dao/
│       ├── (ForwardingSessionDao.kt)        # Add PagingSource queries
│       └── (ForwardedMessageDao.kt)         # Add PagingSource queries
├── navigation/
│   └── (Screen.kt)                          # Add SessionHistory route
└── utils/
    └── DateTimeFormatters.kt                # Relative date/duration utilities
```

### Pattern 1: Paging 3 with Room and LazyColumn

**What:** Room DAO returns PagingSource, Repository wraps in Pager, ViewModel exposes Flow<PagingData>, Screen collects as LazyPagingItems

**When to use:** For any list with potentially 100+ items (sessions, messages)

**Example:**
```kotlin
// Source: Context7 - /websites/developer_android_jetpack_androidx
// DAO returns PagingSource (Room 2.3.0+)
@Dao
interface ForwardingSessionDao {
    @Query("SELECT * FROM forwarding_sessions WHERE device_phone_number = :phoneNumber ORDER BY started_at DESC")
    fun pagingSource(phoneNumber: String): PagingSource<Int, ForwardingSession>
}

// Repository wraps in Pager
class ForwardingSessionRepository(private val dao: ForwardingSessionDao) {
    fun getSessionsPaged(phoneNumber: String): Flow<PagingData<ForwardingSession>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = true,
                prefetchDistance = 5
            ),
            pagingSourceFactory = { dao.pagingSource(phoneNumber) }
        ).flow
    }
}

// ViewModel exposes paging flow
class SessionHistoryViewModel(
    private val repository: ForwardingSessionRepository,
    phoneNumber: String
) : ViewModel() {
    val sessions: Flow<PagingData<ForwardingSession>> =
        repository.getSessionsPaged(phoneNumber)
            .cachedIn(viewModelScope)
}

// Screen uses collectAsLazyPagingItems
@Composable
fun SessionHistoryScreen(viewModel: SessionHistoryViewModel) {
    val sessions = viewModel.sessions.collectAsLazyPagingItems()

    LazyColumn {
        items(
            count = sessions.itemCount,
            key = sessions.itemKey { it.id }
        ) { index ->
            val session = sessions[index]
            if (session != null) {
                SessionCard(session)
            } else {
                SessionCardSkeleton()
            }
        }
    }
}
```

### Pattern 2: DataStore Preferences for View Toggle

**What:** Store session/contact view preference as boolean in DataStore, expose as Flow in Repository, collect in ViewModel as StateFlow

**When to use:** For any user preference that persists across app restarts

**Example:**
```kotlin
// Source: Existing codebase - SettingsRepository.kt pattern
// PreferenceKeys definition
object PreferenceKeys {
    val SESSION_VIEW_MODE = booleanPreferencesKey("session_view_mode") // true = sessions, false = contacts
}

// Repository with Flow property
class SettingsRepository(context: Context) {
    private val dataStore = context.settingsDataStore

    val sessionViewMode: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferenceKeys.SESSION_VIEW_MODE] ?: true // Default: sessions view
        }

    suspend fun setSessionViewMode(isSessionView: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SESSION_VIEW_MODE] = isSessionView
        }
    }
}

// ViewModel exposes as StateFlow
class SessionHistoryViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val isSessionView: StateFlow<Boolean> = settingsRepository.sessionViewMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun toggleViewMode() {
        viewModelScope.launch {
            settingsRepository.setSessionViewMode(!isSessionView.value)
        }
    }
}
```

### Pattern 3: ModalBottomSheet with Draggable State

**What:** Use rememberModalBottomSheetState() with skipPartiallyExpanded = false for 50% initial height, show/hide with state management

**When to use:** For message detail display (per CONTEXT.md: bottom sheet at 50% height, draggable to full)

**Example:**
```kotlin
// Source: https://developer.android.com/develop/ui/compose/components/bottom-sheets
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryScreen() {
    var selectedSession by remember { mutableStateOf<ForwardingSession?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // Session list
    LazyColumn {
        items(sessions) { session ->
            SessionCard(
                session = session,
                onClick = { selectedSession = session }
            )
        }
    }

    // Message detail bottom sheet
    selectedSession?.let { session ->
        ModalBottomSheet(
            onDismissRequest = { selectedSession = null },
            sheetState = sheetState
        ) {
            MessageDetailContent(sessionId = session.id)
        }
    }
}
```

### Pattern 4: Inline Expandable List Items

**What:** Use animateContentSize() modifier with expanded state per item, show/hide content with AnimatedVisibility

**When to use:** For message row expansion (per CONTEXT.md: tap row to expand inline, show full content)

**Example:**
```kotlin
// Source: https://proandroiddev.com/expandable-lists-in-jetpack-compose-b0b78c767b4
@Composable
fun MessageRow(message: ForwardedMessage) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(
                animationSpec = tween(durationMillis = 300)
            )
            .padding(16.dp)
    ) {
        // Compact row (always visible)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = message.senderNumber,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatTimestamp(message.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = message.messageContent,
            style = MaterialTheme.typography.bodySmall,
            maxLines = if (expanded) Int.MAX_VALUE else 1,
            overflow = TextOverflow.Ellipsis
        )

        // Full content (when expanded)
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = message.messageContent,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
```

### Pattern 5: Relative Date/Duration Formatting

**What:** Use Android's DateUtils.getRelativeTimeSpanString() for dates, custom function for duration formatting

**When to use:** For session cards (per CONTEXT.md: "Today", "Yesterday", "3 days ago" for dates; "2h 15m" for durations)

**Example:**
```kotlin
// Source: https://developer.android.com/reference/android/text/format/DateUtils
object DateTimeFormatters {
    fun formatRelativeDate(timestampMillis: Long, context: Context): String {
        val now = System.currentTimeMillis()
        val dayStart = now - (now % DateUtils.DAY_IN_MILLIS)
        val daysDiff = ((dayStart - timestampMillis) / DateUtils.DAY_IN_MILLIS).toInt()

        return when {
            daysDiff == 0 -> "Today"
            daysDiff == 1 -> "Yesterday"
            daysDiff < 7 -> "$daysDiff days ago"
            else -> {
                // Use absolute date for older items
                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                dateFormat.format(Date(timestampMillis))
            }
        }
    }

    fun formatDuration(durationMinutes: Int): String {
        val hours = durationMinutes / 60
        val minutes = durationMinutes % 60

        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }

    // Alternative: Use platform API
    fun formatRelativeDatePlatform(timestampMillis: Long): CharSequence {
        return DateUtils.getRelativeTimeSpanString(
            timestampMillis,
            System.currentTimeMillis(),
            DateUtils.DAY_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        )
    }
}
```

### Anti-Patterns to Avoid

- **Don't use PagingSource directly in ViewModel:** Always wrap in Pager with cachedIn(viewModelScope) to survive configuration changes
- **Don't forget itemKey parameter:** Required for proper item identity in LazyColumn with Paging, prevents re-composition bugs
- **Don't block UI thread with date formatting:** DateUtils operations are fast enough, but avoid complex date parsing in composition
- **Don't use SharedPreferences:** DataStore is the standard per PROJECT.md, SharedPreferences is synchronous and error-prone
- **Don't create custom bottom sheet:** Use Material 3 ModalBottomSheet for consistency with existing DeviceHistoryScreen pattern

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Pagination logic | Custom offset/limit management | Paging 3 PagingSource | Handles configuration changes, placeholders, load states, error recovery automatically |
| Relative date strings | Manual date difference calculations | DateUtils.getRelativeTimeSpanString() | Handles localization, time zones, edge cases (DST, leap years), platform-tested |
| Bottom sheet drag behavior | Custom gesture detection | ModalBottomSheet with rememberModalBottomSheetState() | Handles accessibility, animation curves, dismiss gestures, state restoration |
| View preference persistence | Custom file I/O or SharedPreferences | DataStore Preferences | Type-safe, coroutine-based, handles write failures, atomic updates, migration from SharedPreferences |
| Expandable animation | Manual height animation | Modifier.animateContentSize() | Handles layout measurement, animates all size changes automatically, works with dynamic content |

**Key insight:** Paging 3 looks simple (just offset/limit queries) but handles dozens of edge cases: configuration changes during load, rapid scrolling, error retry, placeholder rendering, load state management, and memory optimization. Custom pagination inevitably leads to bugs when users scroll fast, rotate screen, or have slow network. Similarly, date formatting seems trivial until you hit time zone edge cases, localization requirements, and DST transitions.

## Common Pitfalls

### Pitfall 1: Not Using cachedIn(viewModelScope) with Paging Flow

**What goes wrong:** Paging flow recreates on every recomposition, losing loaded pages and scroll position on configuration changes (rotation)

**Why it happens:** Flow<PagingData> is cold by default, each collector gets a new stream

**How to avoid:**
```kotlin
// WRONG - Flow recreates on rotation
val sessions: Flow<PagingData<ForwardingSession>> =
    repository.getSessionsPaged(phoneNumber)

// CORRECT - Cached in ViewModel scope
val sessions: Flow<PagingData<ForwardingSession>> =
    repository.getSessionsPaged(phoneNumber)
        .cachedIn(viewModelScope)
```

**Warning signs:** Scroll position resets after rotation, "jumping" back to top of list, duplicate loads on configuration change

### Pitfall 2: Forgetting itemKey Parameter with Paging

**What goes wrong:** LazyColumn items re-compose unnecessarily, animations glitch, items appear to "jump" during scrolling

**Why it happens:** Without key, Compose can't track item identity across recompositions, treats all items as new

**How to avoid:**
```kotlin
// WRONG - No key specified
items(count = lazyPagingItems.itemCount) { index ->
    val item = lazyPagingItems[index]
    if (item != null) ItemCard(item)
}

// CORRECT - Use itemKey extension
items(
    count = lazyPagingItems.itemCount,
    key = lazyPagingItems.itemKey { it.id }
) { index ->
    val item = lazyPagingItems[index]
    if (item != null) ItemCard(item)
}
```

**Warning signs:** Items flicker during scroll, animations restart unexpectedly, performance degradation with large lists

### Pitfall 3: Mixing Load States with Data Display Logic

**What goes wrong:** Loading indicators show incorrectly, error states not handled, users see empty screen during load

**Why it happens:** LazyPagingItems provides separate load state properties (loadState.refresh, loadState.append, loadState.prepend) that need explicit handling

**How to avoid:**
```kotlin
// WRONG - Only checking itemCount
if (lazyPagingItems.itemCount == 0) {
    EmptyState()  // Shows during initial load!
}

// CORRECT - Check load state first
when (lazyPagingItems.loadState.refresh) {
    is LoadState.Loading -> LoadingIndicator()
    is LoadState.Error -> ErrorState()
    is LoadState.NotLoading -> {
        if (lazyPagingItems.itemCount == 0) {
            EmptyState()
        } else {
            LazyColumn { /* items */ }
        }
    }
}
```

**Warning signs:** Empty state flashes briefly on load, loading indicator never shows, errors swallowed silently

### Pitfall 4: Not Handling Null Items in LazyPagingItems

**What goes wrong:** NullPointerException crashes, blank spaces in list where items should be

**Why it happens:** Paging 3 uses placeholders (null items) while loading, accessing [index] can return null

**How to avoid:**
```kotlin
// WRONG - Assumes non-null
items(lazyPagingItems.itemCount) { index ->
    SessionCard(lazyPagingItems[index]) // NPE if placeholder!
}

// CORRECT - Handle null with placeholder UI
items(lazyPagingItems.itemCount) { index ->
    val session = lazyPagingItems[index]
    if (session != null) {
        SessionCard(session)
    } else {
        SessionCardSkeleton()  // Placeholder for loading
    }
}
```

**Warning signs:** Random crashes during fast scrolling, blank gaps in list, items "pop in" without shimmer

### Pitfall 5: Using ModalBottomSheet Without State Management

**What goes wrong:** Bottom sheet doesn't animate correctly, dismiss doesn't work, sheet doesn't survive rotation

**Why it happens:** ModalBottomSheet requires explicit state tracking with rememberModalBottomSheetState() and conditional composition

**How to avoid:**
```kotlin
// WRONG - Always composed, no state
ModalBottomSheet(onDismissRequest = { /* what to do? */ }) {
    MessageList()
}

// CORRECT - Conditional composition with state
var selectedSession by remember { mutableStateOf<ForwardingSession?>(null) }
val sheetState = rememberModalBottomSheetState()

selectedSession?.let { session ->
    ModalBottomSheet(
        onDismissRequest = { selectedSession = null },
        sheetState = sheetState
    ) {
        MessageDetailContent(session)
    }
}
```

**Warning signs:** Bottom sheet appears on screen load, can't dismiss sheet, sheet loses content on rotation

### Pitfall 6: Not Persisting View Toggle State in DataStore

**What goes wrong:** Session/contact view toggle resets to default on app restart, loses user preference

**Why it happens:** UI state (remember { mutableStateOf() }) is lost on process death, not persisted to disk

**How to avoid:**
```kotlin
// WRONG - UI state only
var isSessionView by remember { mutableStateOf(true) }

// CORRECT - Persisted in DataStore, collected as StateFlow
val isSessionView by viewModel.isSessionView.collectAsState()

// ViewModel
val isSessionView: StateFlow<Boolean> = settingsRepository.sessionViewMode
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
```

**Warning signs:** Toggle state lost after closing app, preference doesn't persist between sessions

## Code Examples

Verified patterns from official sources:

### Paging 3 PagingConfig Best Practices

```kotlin
// Source: https://developer.android.com/topic/libraries/architecture/paging/v3-overview
fun getSessionsPaged(phoneNumber: String): Flow<PagingData<ForwardingSession>> {
    return Pager(
        config = PagingConfig(
            pageSize = 20,              // Number of items per page
            enablePlaceholders = true,   // Show placeholders while loading
            prefetchDistance = 5,        // Start loading N items before end
            initialLoadSize = 40,        // Load 2x pageSize initially
            maxSize = 200                // Max items in memory (10 pages)
        ),
        pagingSourceFactory = { dao.pagingSource(phoneNumber) }
    ).flow.cachedIn(viewModelScope)
}
```

### Handling All Load States in UI

```kotlin
// Source: https://developer.android.com/develop/ui/compose/quick-guides/content/lazily-load-list
@Composable
fun SessionListWithLoadStates(sessions: LazyPagingItems<ForwardingSession>) {
    when (sessions.loadState.refresh) {
        is LoadState.Loading -> {
            // Initial load
            LoadingScreen()
        }
        is LoadState.Error -> {
            // Error loading first page
            ErrorScreen(
                error = (sessions.loadState.refresh as LoadState.Error).error,
                onRetry = { sessions.retry() }
            )
        }
        is LoadState.NotLoading -> {
            // Data loaded successfully
            LazyColumn {
                items(
                    count = sessions.itemCount,
                    key = sessions.itemKey { it.id }
                ) { index ->
                    sessions[index]?.let { SessionCard(it) }
                }

                // Handle append state (loading more at bottom)
                if (sessions.loadState.append is LoadState.Loading) {
                    item {
                        LoadingIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
```

### Active Session Badge with Pulsing Dot

```kotlin
// Source: Jetpack Compose animation best practices
@Composable
fun ActiveSessionBadge() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                    shape = CircleShape
                )
        )
        Text(
            text = "Active",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
```

### Navigation with URL-Encoded Phone Numbers

```kotlin
// Source: Existing codebase - Screen.kt and DeviceHistoryScreen.kt pattern
sealed class Screen(val route: String) {
    data object SessionHistory : Screen("session_history/{phoneNumber}/{role}") {
        fun createRoute(phoneNumber: String, role: String): String {
            val encodedPhone = java.net.URLEncoder.encode(phoneNumber, "UTF-8")
            return "session_history/$encodedPhone/$role"
        }
    }
}

// NavHost setup
composable(
    route = Screen.SessionHistory.route,
    arguments = listOf(
        navArgument("phoneNumber") { type = NavType.StringType },
        navArgument("role") { type = NavType.StringType }
    )
) { backStackEntry ->
    val phoneNumber = backStackEntry.arguments?.getString("phoneNumber")
        ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: return@composable
    val role = backStackEntry.arguments?.getString("role") ?: return@composable

    SessionHistoryScreen(
        viewModel = viewModel(
            factory = SessionHistoryViewModel.Factory(
                sessionRepository, messageRepository, phoneNumber, role
            )
        ),
        onNavigateBack = { navController.popBackStack() }
    )
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Manual pagination with offset/limit | Paging 3 with PagingSource | Paging 3.0 (2020) | Mandatory per STATE.md, handles configuration changes and load states |
| SharedPreferences | DataStore Preferences | DataStore 1.0 (2021) | Mandatory per PROJECT.md, coroutine-based and type-safe |
| BottomSheetScaffold | ModalBottomSheet | Material 3 (2022) | Used in Phase 17 DeviceHistoryScreen, consistent with existing UI |
| Manual expand/collapse animation | Modifier.animateContentSize() | Compose 1.0 (2021) | Automatic animation for size changes, no manual calculation |
| Custom relative date formatting | DateUtils.getRelativeTimeSpanString() | Platform API (since API 3) | Handles localization and edge cases, no need for custom |

**Deprecated/outdated:**
- **BottomSheetScaffold:** Replaced by ModalBottomSheet in Material 3, BottomSheetScaffold doesn't support modal (dim background) behavior
- **SharedPreferences:** Synchronous API, no error handling, race conditions possible. DataStore is the standard.
- **Manual offset/limit pagination:** Doesn't handle placeholders, configuration changes, or load state management
- **LiveData in ViewModels:** Project uses StateFlow/Flow exclusively (existing ViewModels confirm this pattern)

## Open Questions

Things that couldn't be fully resolved:

1. **Contact view grouping implementation**
   - What we know: CONTEXT.md specifies "Contact view grouping: Claude's discretion on best approach"
   - What's unclear: Should contacts be grouped by sender number with expandable sections, or flat list sorted by sender? Should it show all messages across sessions or just session metadata?
   - Recommendation: Use flat list sorted by sender number, showing aggregate message count per sender. Tapping navigates to filtered session list for that sender. Avoids complex grouping logic and maintains performance with Paging.

2. **Scroll position behavior when switching views**
   - What we know: CONTEXT.md says "Scroll position: Claude's discretion based on UX best practices"
   - What's unclear: Should scroll position persist when toggling between session/contact view, or reset to top?
   - Recommendation: Reset scroll to top when switching views. Persisting scroll position across different data structures (sessions vs contacts) is confusing and technically complex with Paging.

3. **Paging 3 with Room 2.6.1 compatibility**
   - What we know: Room 2.6.1 retained per STATE.md for Kotlin 1.9.0 compatibility, Paging 3.4.0 is latest
   - What's unclear: Any known compatibility issues between Room 2.6.1 and paging-compose 3.4.0?
   - Recommendation: Proceed with paging-compose 3.4.0. Room 2.6.1 supports PagingSource (added in Room 2.3.0). If issues arise, can downgrade to paging-compose 3.2.0 (contemporary with Room 2.6.1).

4. **Active session real-time message count update**
   - What we know: SESS-06 requires "Active sessions show real-time message count with [Active] badge"
   - What's unclear: Should this update while viewing the session list (expensive, requires Flow observation), or only on initial load/refresh?
   - Recommendation: Update on list refresh only (pull-to-refresh), not real-time. Real-time updates require observing all active sessions' message counts continuously, draining battery. Users can refresh to see updated counts.

## Sources

### Primary (HIGH confidence)
- [AndroidX Paging 3](https://developer.android.com/jetpack/androidx/releases/paging) - Official release notes, version 3.4.0 confirmed
- [Context7: AndroidX](https://developer.android.com/jetpack/androidx) - Official Room PagingSource integration documentation
- [Android ModalBottomSheet](https://developer.android.com/develop/ui/compose/components/bottom-sheets) - Official Material 3 bottom sheet guide (updated 2026-02-02)
- [Paging with LazyColumn](https://developer.android.com/develop/ui/compose/quick-guides/content/lazily-load-list) - Official Compose Paging integration guide
- [DataStore Preferences](https://developer.android.com/jetpack/androidx/releases/datastore) - Official DataStore API reference
- Existing codebase: build.gradle.kts, SettingsRepository.kt, DeviceHistoryScreen.kt, Screen.kt, DeviceHistoryViewModel.kt

### Secondary (MEDIUM confidence)
- [ModalBottomSheet Guide](https://medium.com/@fofito.1295/modalbottomsheet-in-jetpack-compose-material-3-your-complete-guide-to-an-impeccable-ui-6ee0bba07e12) - Community guide verified against official docs
- [Paging 3 Best Practices](https://medium.com/@android./a-full-guide-to-use-paging3-library-along-with-jetpack-composes-lazyrow-lazycolumn-and-lazygrid-7e6c6bf3812d) - Community best practices article
- [Expandable Lists in Compose](https://proandroiddev.com/expandable-lists-in-jetpack-compose-b0b78c767b4) - ProAndroidDev article on inline expansion patterns
- [DateUtils API](https://developer.android.com/reference/android/text/format/DateUtils) - Official Android reference for relative date formatting
- [RelativeDateTimeFormatter API](https://developer.android.com/reference/android/icu/text/RelativeDateTimeFormatter) - Official Android ICU API reference

### Tertiary (LOW confidence)
- [Duration Formatting Gist](https://gist.github.com/Jeehut/78534c27b24d78f14a3cbd3eebead861) - Community example for localized duration formatting
- [PrettyTime Library](https://www.ocpsoft.org/prettytime/) - Third-party alternative (not recommended, adds dependency)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All libraries verified in official docs, versions confirmed in AndroidX releases
- Architecture: HIGH - Patterns confirmed in official Android guides and existing codebase
- Pitfalls: HIGH - Based on official documentation warnings and verified community issues

**Research date:** 2026-02-06
**Valid until:** 2026-03-06 (30 days for stable APIs - Paging 3, DataStore, Material 3 are mature)

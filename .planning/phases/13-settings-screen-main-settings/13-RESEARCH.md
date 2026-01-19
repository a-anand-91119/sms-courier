# Phase 13 Research: Settings Screen & Main Settings

**Researched:** 2026-01-19
**Domain:** Jetpack Compose Settings UI / Android Permissions
**Confidence:** HIGH

## Executive Summary

Phase 13 builds a settings screen UI on top of the existing SettingsRepository infrastructure from Phase 12. The app already uses Material3 Compose with established patterns (LargeTopAppBar, Card, Switch, LazyColumn) that should be followed for consistency. Navigation to Settings is already wired via `onNavigateToSettings` callback from HomeScreen with a placeholder SettingsScreen in NavGraph.kt.

The primary approach is using custom Compose composables (not a preference library) since the app already demonstrates this pattern. Material3 ListItem is the standard component for settings rows. Theme changes require modifying MainActivity to observe the SettingsRepository theme Flow and pass darkTheme to smscourierTheme().

**Primary recommendation:** Build custom settings composables using existing app patterns (Card, ListItem, Switch) rather than adding a new preference library.

## Navigation Architecture

### Current Setup

Navigation is already configured for Settings:

1. **Screen.kt** defines `Screen.Settings` with route "settings"
2. **NavGraph.kt** has a placeholder SettingsScreen composable (lines 131-136, 140-170)
3. **HomeScreen.kt** has a gear icon in the top app bar that calls `onNavigateToSettings` (lines 101-108)

### What Needs to Change

Replace the placeholder SettingsScreen in NavGraph.kt with a proper implementation. The placeholder currently shows "Settings coming soon" text.

**Key files:**
- `/app/src/main/java/dev/notyouraverage/smscourier/navigation/NavGraph.kt` - Replace placeholder
- Create new `/app/src/main/java/dev/notyouraverage/smscourier/composables/screens/SettingsScreen.kt`
- Create new `/app/src/main/java/dev/notyouraverage/smscourier/viewmodels/SettingsViewModel.kt`

## Existing UI Patterns

The app uses consistent Material3 patterns that Settings should follow:

### TopAppBar Pattern
```kotlin
// From HomeScreen.kt, ForwardingControlScreen.kt, PairedDevicesScreen.kt
LargeTopAppBar(
    title = { Text("Title", fontWeight = FontWeight.Bold) },
    navigationIcon = {
        IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
    },
    scrollBehavior = scrollBehavior,
    colors = TopAppBarDefaults.largeTopAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
    ),
)
```

### Card Pattern
```kotlin
// From HomeScreen.kt - used for grouped content
Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
)
```

### Switch Pattern
```kotlin
// From HomeScreen.kt ServiceStatusCard
Switch(
    checked = isEnabled,
    onCheckedChange = onToggle,
    colors = SwitchDefaults.colors(
        checkedThumbColor = MaterialTheme.colorScheme.primary,
        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    ),
)
```

### Section Header Pattern
```kotlin
// From HomeScreen.kt
Text(
    text = "Quick Actions",
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.SemiBold,
    color = MaterialTheme.colorScheme.onSurface,
    modifier = Modifier.padding(top = 8.dp),
)
```

### ViewModel Factory Pattern
```kotlin
// From HomeViewModel.kt - standard pattern used throughout
class Factory(
    private val settingsRepository: SettingsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(settingsRepository) as T
    }
}
```

## SettingsRepository API

Phase 12 completed the SettingsRepository with these Flows relevant to Phase 13:

### Main Settings (Phase 13 scope)
| Flow | Type | Default | Setter |
|------|------|---------|--------|
| `notificationPersistence` | `Flow<Boolean>` | false | `setNotificationPersistence(Boolean)` |
| `defaultForwardingDurationMinutes` | `Flow<Int>` | 15 | `setDefaultForwardingDuration(Int)` (1-60) |
| `theme` | `Flow<AppTheme>` | SYSTEM | `setTheme(AppTheme)` |

### AppTheme Enum
```kotlin
enum class AppTheme {
    LIGHT,
    DARK,
    SYSTEM,
}
```

### Usage in ViewModel
```kotlin
val notificationPersistence: StateFlow<Boolean> = settingsRepository.notificationPersistence
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

fun setNotificationPersistence(enabled: Boolean) {
    viewModelScope.launch {
        settingsRepository.setNotificationPersistence(enabled)
    }
}
```

## Settings Screen Patterns

### Recommended: Custom Composables with Material3 ListItem

Material3 ListItem is the standard component for settings rows:

```kotlin
@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = icon?.let { { Icon(it, contentDescription = null) } },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
    )
}
```

### Duration Selection Pattern

For "Default forwarding duration" with predefined options (15min, 30min, 1hr, 2hr), use a clickable ListItem with dropdown or dialog:

```kotlin
// Option 1: DropdownMenu (inline)
var expanded by remember { mutableStateOf(false) }
ListItem(
    headlineContent = { Text("Default duration") },
    supportingContent = { Text("$currentDuration minutes") },
    trailingContent = {
        Box {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Select")
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listOf(15, 30, 60, 120).forEach { minutes ->
                    DropdownMenuItem(
                        text = { Text(formatDuration(minutes)) },
                        onClick = { onDurationSelected(minutes); expanded = false },
                    )
                }
            }
        }
    },
    modifier = Modifier.clickable { expanded = true },
)
```

### Theme Selection Pattern

For theme selection (Light, Dark, System), use SingleChoiceSegmentedButtonRow (already used in PairedDevicesScreen.kt) or a radio button list:

```kotlin
// Option 1: SegmentedButtonRow (consistent with existing code)
SingleChoiceSegmentedButtonRow {
    AppTheme.entries.forEachIndexed { index, theme ->
        SegmentedButton(
            selected = currentTheme == theme,
            onClick = { onThemeSelected(theme) },
            shape = SegmentedButtonDefaults.itemShape(index = index, count = AppTheme.entries.size),
        ) {
            Text(theme.displayName)
        }
    }
}
```

### Why Not Use a Preference Library

The app does NOT currently use any preference library (like compose-settings or preference-ktx). Adding one would:
- Introduce dependency inconsistency
- Require learning new patterns
- The existing patterns (Card, ListItem, Switch) are sufficient

## Permission Status

### Permissions to Check

From AndroidManifest.xml, the app requires:
- `RECEIVE_SMS` - Required for core functionality
- `SEND_SMS` - Required for core functionality
- `POST_NOTIFICATIONS` - Required for notifications

### Checking Permission Status

```kotlin
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

fun checkPermission(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) ==
        PackageManager.PERMISSION_GRANTED
}

// In ViewModel or Screen
val smsReceiveGranted = ContextCompat.checkSelfPermission(
    context, Manifest.permission.RECEIVE_SMS
) == PackageManager.PERMISSION_GRANTED
```

### Opening App Settings for Permission Fix

```kotlin
fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
```

### Permission Status UI Pattern

Display each permission with status indicator and fix action:

```kotlin
@Composable
fun PermissionStatusItem(
    permission: String,
    label: String,
    isGranted: Boolean,
    onFixClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = {
            Text(if (isGranted) "Granted" else "Not granted")
        },
        leadingContent = {
            Icon(
                if (isGranted) Icons.Default.Check else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGranted) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.error,
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
```

## Theme System

### Current Implementation

Located in `/app/src/main/java/dev/notyouraverage/smscourier/ui/theme/Theme.kt`:

```kotlin
@Composable
fun smscourierTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
)
```

### Required Changes for Theme Setting

MainActivity needs to observe the theme setting and pass the appropriate `darkTheme` value:

```kotlin
// In MainActivity.kt
setContent {
    val settingsRepository = remember { SettingsRepository(applicationContext) }
    val themeSetting by settingsRepository.theme.collectAsState(initial = AppTheme.SYSTEM)

    val darkTheme = when (themeSetting) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    smscourierTheme(darkTheme = darkTheme) {
        // ... rest of content
    }
}
```

### Theme Transition

Material3 handles theme transitions automatically when the color scheme changes. No additional animation code needed unless you want custom crossfade.

## About Screen Data

### App Version

The app uses environment variables for version in build.gradle.kts:
```kotlin
versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
versionName = System.getenv("VERSION_NAME") ?: "0.0.1-dev"
```

Access via BuildConfig (simplest) or PackageInfo:

```kotlin
// Option 1: BuildConfig (no context needed)
val versionName = BuildConfig.VERSION_NAME
val versionCode = BuildConfig.VERSION_CODE

// Option 2: PackageInfo (context needed)
val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
val versionName = packageInfo.versionName
```

### URLs Needed

Per CONTEXT.md decisions:
- **Privacy Policy**: URL TBD - needs to be defined (possibly as a constant or remote config)
- **Support Contact**: GitLab issues page URL

Recommend creating a Constants file or adding to existing Constants.kt:

```kotlin
object AboutLinks {
    const val PRIVACY_POLICY_URL = "https://..." // TBD
    const val SUPPORT_URL = "https://gitlab.com/notyouraverage/smscourier/-/issues"
}
```

### Opening URLs

```kotlin
fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}
```

## Key Files

### Files to Create
| File | Purpose |
|------|---------|
| `composables/screens/SettingsScreen.kt` | Main settings UI |
| `viewmodels/SettingsViewModel.kt` | Settings state management |

### Files to Modify
| File | Change |
|------|--------|
| `navigation/NavGraph.kt` | Replace placeholder with real SettingsScreen |
| `activities/MainActivity.kt` | Observe theme setting, pass to smscourierTheme |
| `constants/Constants.kt` | Add About URLs (privacy, support) |

### Files to Reference (patterns)
| File | Pattern |
|------|---------|
| `composables/screens/HomeScreen.kt` | Card, Switch, section headers |
| `composables/screens/PairedDevicesScreen.kt` | SegmentedButtonRow, ListItem |
| `viewmodels/HomeViewModel.kt` | ViewModel factory pattern |

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Settings persistence | Custom SharedPreferences wrapper | SettingsRepository (Phase 12) | Already done, tested |
| Theme switching | Custom theme state | smscourierTheme() darkTheme param | Already supports dark/light |
| Permission checking | Custom permission tracker | ContextCompat.checkSelfPermission | Standard Android API |
| Opening URLs | WebView or custom browser | Intent.ACTION_VIEW | Standard pattern |
| Settings rows | Custom layouts from scratch | Material3 ListItem | Consistent, accessible |

## Common Pitfalls

### Pitfall 1: Forgetting to Observe Theme at App Level
**What goes wrong:** Theme setting changes but app doesn't update
**Why it happens:** Theme must be observed in MainActivity, not just SettingsScreen
**How to avoid:** Hoist theme observation to MainActivity setContent block
**Warning signs:** Theme changes require app restart to take effect

### Pitfall 2: Permission Status Not Refreshing
**What goes wrong:** Permission granted in system settings but UI shows "not granted"
**Why it happens:** Permission check happens once on composition
**How to avoid:** Check permissions in LaunchedEffect with lifecycle awareness, or re-check on screen resume
**Warning signs:** User returns from system settings and status is stale

### Pitfall 3: Hardcoding Duration Options
**What goes wrong:** Requirements say 15min/30min/1hr/2hr but validation allows 1-60
**Why it happens:** Mismatch between UI options and SettingsRepository validation
**How to avoid:** UI should constrain to predefined options that fit validation
**Warning signs:** Runtime validation error when selecting valid UI option

### Pitfall 4: Not Handling Missing URLs
**What goes wrong:** App crashes when opening undefined privacy policy URL
**Why it happens:** URL constants not defined yet
**How to avoid:** Add placeholder URLs, handle gracefully if empty
**Warning signs:** Crash on tapping privacy policy link

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| PreferenceFragment | Compose custom UI | Compose 1.0 (2021) | Full control, no XML |
| SharedPreferences | DataStore Preferences | 2020 | Type safety, coroutine support |
| Theme recreation | Compose recomposition | Compose | No activity recreation needed |

## Open Questions

1. **Privacy Policy URL**: Where should this link to? Not defined in CONTEXT.md.
   - What we know: Requirements say include it (INFO-02)
   - What's unclear: The actual URL
   - Recommendation: Add a TODO or placeholder constant, implement with real URL later

2. **GitLab Issues URL**: Exact URL format?
   - What we know: Should link to GitLab issues page
   - Recommendation: Use `https://gitlab.com/notyouraverage/smscourier/-/issues` (inferred from package name)

## Sources

### Primary (HIGH confidence)
- Codebase analysis: NavGraph.kt, HomeScreen.kt, SettingsRepository.kt, MainActivity.kt
- Android official docs for permission checking and Settings intents

### Secondary (MEDIUM confidence)
- [Material Design 3 for Jetpack Compose](https://m3.material.io/develop/android/jetpack-compose)
- [ListItem composable documentation](https://composables.com/material3/listitem)
- [Android runtime permissions guide](https://developer.android.com/training/permissions/requesting)
- [Opening Android settings programmatically](https://developer.android.com/reference/android/provider/Settings)
- [Getting app version in Android](https://www.geeksforgeeks.org/kotlin/how-to-get-the-build-version-number-of-an-android-application-using-jetpack-compose/)

## Metadata

**Confidence breakdown:**
- Navigation: HIGH - Existing code review, clear pattern
- UI Patterns: HIGH - Codebase demonstrates patterns to follow
- SettingsRepository API: HIGH - Phase 12 completed with tests
- Theme integration: HIGH - Clear path, minimal changes
- Permission status: MEDIUM - Standard Android APIs, patterns verified
- About screen: MEDIUM - URLs need clarification

**Research date:** 2026-01-19
**Valid until:** 2026-02-19 (stable patterns, unlikely to change)

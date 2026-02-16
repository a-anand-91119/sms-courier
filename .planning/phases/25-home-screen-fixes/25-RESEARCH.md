# Phase 25: Home Screen Fixes - Research

**Researched:** 2026-02-16
**Domain:** Android Compose UI responsive layout and enum refactoring
**Confidence:** HIGH

## Summary

Phase 25 fixes two UI bugs on the Home screen: (1) direction labels showing inverted text for TARGET devices (HOME-01), and (2) layout overflow on small screens (HOME-02). The research confirms both issues are straightforward fixes using existing Compose patterns—no new dependencies required.

**Key findings:**
- **Direction mapping bug**: HomeViewModel's `calculateDirectionalStatus()` correctly assigns `Direction.RECEIVING_FROM` to TARGET devices, but the current naming is inverted. TARGET devices *forward* messages TO source devices, so they should display "Forwarding to" not "Receiving from". The fix requires renaming enum values to match their semantic meaning.
- **Small screen layout**: Compose's `BoxWithConstraints` provides a standard pattern for responsive layouts. A breakpoint around 400dp is conventional for switching from horizontal icon badges to vertical text-only lists.
- **Pluralization**: Jetpack Compose's `pluralStringResource()` API handles "1 device" vs "2 devices" grammar correctly when string resources are defined in `res/values/strings.xml`.

**Critical architectural insight:** The Direction enum was named from a *source device's perspective* ("forwarding to me", "receiving from others"), but users on TARGET devices experience it differently. Renaming the enum values to match the *action* (FORWARDING_TO = sending messages, RECEIVING_FROM = getting messages) eliminates confusion.

**Primary recommendation:** Rename Direction enum values to semantic names (e.g., `SENDING` and `RECEIVING` or keep current names but fix UI labels), add plural string resources for device counts, and wrap DirectionalStatusCard content in BoxWithConstraints to switch layout below 400dp width.

## Standard Stack

### Core Components (Already Available)

All required functionality exists in the current stack:

| Component | Version | Purpose | Already Available |
|-----------|---------|---------|-------------------|
| Jetpack Compose | 1.5.1 (Kotlin compiler) | UI framework | Yes - via Material 3 BOM |
| BoxWithConstraints | Compose Foundation | Responsive layout based on available width | Yes - androidx.compose.foundation.layout |
| LocalConfiguration | Compose UI | Access screen dimensions for breakpoints | Yes - androidx.compose.ui.platform |
| pluralStringResource | Compose Material 3 | Pluralization (1 device / 2 devices) | Yes - androidx.compose.material3 |
| Material 3 Icons | Extended | Arrow icons already imported | Yes - current imports |

### No New Dependencies Required

The phase uses existing Compose APIs:
- `BoxWithConstraints { maxWidth }` for responsive layout switching
- `LocalConfiguration.current.screenWidthDp` as alternative for custom breakpoints
- Standard `Column`, `Row`, `Text` composables for compact layout
- Existing `Direction` enum (needs renaming only)

## Architecture Patterns

### Pattern 1: Responsive Layout with BoxWithConstraints

**What:** Switch between horizontal badge row and vertical text list based on available width
**When to use:** When layout must adapt to different screen sizes without separate screen size classes
**Example:**

```kotlin
// Source: https://developer.android.com/develop/ui/compose/layouts/adaptive
@Composable
fun DirectionalStatusCard(
    forwardingToCount: Int,
    receivingFromCount: Int,
    bidirectionalCount: Int,
    onCardClick: () -> Unit,
) {
    Card(onClick = onCardClick) {
        BoxWithConstraints {
            if (maxWidth < 400.dp) {
                // COMPACT: Vertical text-only list
                Column {
                    if (forwardingToCount > 0) {
                        CompactDirectionRow("Forwarding to", forwardingToCount)
                    }
                    if (receivingFromCount > 0) {
                        CompactDirectionRow("Receiving from", receivingFromCount)
                    }
                    if (bidirectionalCount > 0) {
                        CompactDirectionRow("Bidirectional", bidirectionalCount)
                    }
                }
            } else {
                // STANDARD: Horizontal icon badges (existing layout)
                Row {
                    DirectionalIndicator(
                        icon = Icons.Default.KeyboardArrowDown,
                        count = forwardingToCount,
                        label = "Receiving from",
                        isActive = forwardingToCount > 0,
                    )
                    // ... other indicators
                }
            }
        }
    }
}

@Composable
fun CompactDirectionRow(label: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}
```

**Key insights:**
- 400dp breakpoint is Android convention (tablets typically ≥600dp, small phones <400dp)
- `BoxWithConstraints` provides `maxWidth` constraint reactively
- Hide inactive directions on small screens to conserve space
- Keep large screen layout unchanged (all three indicators visible)

### Pattern 2: Enum Renaming with Broad Impact

**What:** Rename Direction enum values while updating all references
**When to use:** When enum names cause confusion or don't match their semantic meaning
**Example:**

```kotlin
// CURRENT (confusing from TARGET device perspective):
enum class Direction {
    FORWARDING_TO,    // This device is SOURCE (receiving messages)
    RECEIVING_FROM,   // This device is TARGET (sending messages)
    BIDIRECTIONAL
}

// OPTION A: Semantic action-based names (RECOMMENDED)
enum class Direction {
    SENDING,          // This device sends (was RECEIVING_FROM)
    RECEIVING,        // This device receives (was FORWARDING_TO)
    BIDIRECTIONAL
}

// OPTION B: Keep names, fix UI labels only (less clear but safer)
// Keep enum as-is, change only display strings in composables
```

**Refactoring process:**
1. Search all files: `Direction.FORWARDING_TO`, `Direction.RECEIVING_FROM` (74 total occurrences across 16 files)
2. Update Direction.kt enum definition
3. Update HomeViewModel.calculateDirectionalStatus() logic (lines 54-130)
4. Update all UI composables: HomeScreen, DirectionalStatusCard, SessionBreakdownBottomSheet, PairedDevicesScreen
5. Update all test files: HomeViewModelTest, DirectionalStatusTest (20 occurrences in tests)
6. Run `./gradlew test` and `./gradlew uatTest` to verify no regressions

**Impact scope:**
- 7 source files (Direction.kt, HomeViewModel.kt, HomeScreen.kt, DirectionalStatusCard.kt, SessionBreakdownBottomSheet.kt, PairedDevicesScreen.kt, PairedDevicesViewModel.kt)
- 3 test files (HomeViewModelTest.kt, DirectionalStatusTest.kt)
- All changes are mechanical find-replace with updated semantic meaning

### Pattern 3: Pluralization with String Resources

**What:** Use Android plural string resources for grammatically correct device counts
**When to use:** Any time displaying counts that need singular/plural forms
**Example:**

```xml
<!-- res/values/strings.xml -->
<resources>
    <plurals name="forwarding_to_count">
        <item quantity="one">Forwarding to %d device</item>
        <item quantity="other">Forwarding to %d devices</item>
    </plurals>
    <plurals name="receiving_from_count">
        <item quantity="one">Receiving from %d device</item>
        <item quantity="other">Receiving from %d devices</item>
    </plurals>
</resources>
```

```kotlin
// In Composable:
// Source: https://developer.android.com/develop/ui/compose/resources
val forwardingText = pluralStringResource(
    R.plurals.forwarding_to_count,
    count,
    count  // Pass twice: once for plural selection, once for %d formatting
)
Text(text = forwardingText)
```

**Key insights:**
- Must pass count parameter twice when using `%d` format placeholder
- Android handles all locale-specific plural rules automatically
- Quantity values: `zero`, `one`, `two`, `few`, `many`, `other` (English uses `one` and `other`)

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Responsive breakpoints | Custom screen size detection with hardcoded if/else | BoxWithConstraints | Reactive to constraint changes, works with compose layout system |
| Pluralization | String templates with `if (count == 1) "device" else "devices"` | pluralStringResource | Handles all locales correctly (some languages have 6+ plural forms) |
| Direction semantics | Add boolean flags like `isSending` to Direction enum | Rename enum values to match semantic meaning | Enums should be self-documenting, not require parallel state |
| Custom breakpoint helper | Global singleton or ViewModel for screen size | LocalConfiguration.current.screenWidthDp | Compose-native, recomposes automatically |

**Key insight:** Compose provides first-class responsive layout APIs. Custom screen size detection is unnecessary and misses layout-specific constraints (e.g., split screen, foldables).

## Common Pitfalls

### Pitfall 1: Enum Renaming Without Exhaustive Find-Replace

**What goes wrong:** Renaming enum values but missing some references causes compile errors or incorrect behavior
**Why it happens:** Direction enum has 74 references across 16 files including tests and planning docs
**How to avoid:**
1. Use IDE refactor rename (Shift+F6 in Android Studio) instead of manual find-replace
2. After rename, search codebase for old enum names to catch missed references
3. Run full test suite (`./gradlew test && ./gradlew uatTest`) before committing
4. Check planning docs (.md files) for stale enum references in comments

**Warning signs:**
- Compile errors in test files after enum rename
- UAT tests fail with unexpected Direction values
- Comments reference old enum names

### Pitfall 2: BoxWithConstraints Inside LazyColumn

**What goes wrong:** BoxWithConstraints inside scrollable content causes performance issues
**Why it happens:** Each item remeasures on scroll, triggering recomposition
**How to avoid:** DirectionalStatusCard is *not* in a LazyColumn (it's in a regular Column with verticalScroll), so this is safe. But for future reference:
- Use BoxWithConstraints at screen level, not per list item
- Or use `WindowSizeClass` pattern instead for list items

**Warning signs:**
- Janky scrolling when card is visible
- Excessive recompositions in Layout Inspector

### Pitfall 3: Breakpoint Choice Without Testing on Small Devices

**What goes wrong:** Choosing arbitrary breakpoint (e.g., 320dp) that's too aggressive
**Why it happens:** Modern phones are typically 360dp+ width, but developer picks edge case
**How to avoid:**
- Use 400dp as breakpoint (standard Android convention)
- Test on 360dp emulator (common small phone size)
- Consider that 320dp is *extremely* small (older devices only)

**Warning signs:**
- Compact layout appears on normal-sized phones
- User confusion about "missing" visual elements (icons)

### Pitfall 4: Forgetting to Pass Count Twice in pluralStringResource

**What goes wrong:** Runtime crash or wrong plural form selected
**Why it happens:** `pluralStringResource(R.plurals.foo, count)` selects plural form but doesn't substitute `%d`
**How to avoid:** Always pass count twice when format placeholder exists: `pluralStringResource(R.plurals.foo, count, count)`

**Warning signs:**
- Crash: `java.util.UnknownFormatConversionException`
- Text shows "%d" literally instead of number

### Pitfall 5: Direction Enum Inversion Confusion

**What goes wrong:** After renaming enum, HomeViewModel logic assigns wrong direction to devices
**Why it happens:** calculateDirectionalStatus() logic is tightly coupled to current enum names
**How to avoid:**
- Review HomeViewModel.calculateDirectionalStatus() logic carefully during rename
- Understand semantic mapping:
  - DeviceRole.SOURCE (requests forwarding) → displays "Receiving from" → Direction.RECEIVING
  - DeviceRole.TARGET (provides forwarding) → displays "Forwarding to" → Direction.SENDING
- Run HOME-01 UAT test to verify correct direction assignment

**Warning signs:**
- HOME-01 test still fails after enum rename
- TARGET device still shows "Receiving" instead of "Forwarding"

## Code Examples

### Example 1: Current Direction Enum and Inverted Semantic Issue

```kotlin
// Current implementation (from Direction.kt)
// Source: /app/src/main/java/dev/notyouraverage/smscourier/data/Direction.kt

/**
 * CRITICAL: These names are INVERTED from user perspective on TARGET devices.
 * - FORWARDING_TO means "forwarding messages TO this device" (SOURCE device receiving)
 * - RECEIVING_FROM means "receiving messages FROM this device" (TARGET device sending)
 *
 * But TARGET users see "Receiving" when they're actually FORWARDING messages.
 * Phase 25 fixes this by either:
 * 1. Renaming enums to match actions (SENDING/RECEIVING)
 * 2. Or fixing UI labels while keeping confusing enum names
 */
enum class Direction {
    FORWARDING_TO,    // This device is SOURCE role (receiving messages)
    RECEIVING_FROM,   // This device is TARGET role (sending messages)
    BIDIRECTIONAL,
}
```

### Example 2: HomeViewModel Direction Assignment Logic

```kotlin
// Current implementation (from HomeViewModel.kt lines 95-120)
// This logic is CORRECT but enum names cause confusion

when {
    sourceDevice != null -> {
        // SOURCE role: this device RECEIVES messages from TARGET
        forwardingToCount += phoneSessions.size  // Count is correct
        sessionsWithDirection.add(
            SessionWithDirection(
                session = session,
                device = sourceDevice,
                direction = Direction.FORWARDING_TO,  // Name is confusing
            ),
        )
    }
    targetDevice != null -> {
        // TARGET role: this device SENDS messages to SOURCE
        receivingFromCount += phoneSessions.size  // Count is correct
        sessionsWithDirection.add(
            SessionWithDirection(
                session = session,
                device = targetDevice,
                direction = Direction.RECEIVING_FROM,  // Name is inverted
            ),
        )
    }
}
```

### Example 3: Responsive DirectionalStatusCard with BoxWithConstraints

```kotlin
// New implementation for Phase 25
@Composable
fun DirectionalStatusCard(
    forwardingToCount: Int,
    receivingFromCount: Int,
    bidirectionalCount: Int,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        onClick = onCardClick,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            if (maxWidth < 400.dp) {
                // COMPACT: Vertical text-only list for small screens
                CompactDirectionalStatus(
                    forwardingToCount = forwardingToCount,
                    receivingFromCount = receivingFromCount,
                    bidirectionalCount = bidirectionalCount,
                )
            } else {
                // STANDARD: Horizontal icon badges (existing layout)
                StandardDirectionalStatus(
                    forwardingToCount = forwardingToCount,
                    receivingFromCount = receivingFromCount,
                    bidirectionalCount = bidirectionalCount,
                )
            }
        }
    }
}

@Composable
fun CompactDirectionalStatus(
    forwardingToCount: Int,
    receivingFromCount: Int,
    bidirectionalCount: Int,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Active Forwarding",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        // Only show active directions (hide zero counts)
        if (forwardingToCount > 0) {
            CompactDirectionRow(
                label = pluralStringResource(
                    R.plurals.receiving_from_count,
                    forwardingToCount,
                    forwardingToCount
                ),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (receivingFromCount > 0) {
            CompactDirectionRow(
                label = pluralStringResource(
                    R.plurals.forwarding_to_count,
                    receivingFromCount,
                    receivingFromCount
                ),
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        if (bidirectionalCount > 0) {
            CompactDirectionRow(
                label = "Bidirectional ($bidirectionalCount)",
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}
```

### Example 4: Plural String Resources

```xml
<!-- res/values/strings.xml -->
<resources>
    <string name="app_name">SMS Courier</string>

    <!-- Phase 25: Direction labels with pluralization -->
    <plurals name="forwarding_to_count">
        <item quantity="one">Forwarding to %d device</item>
        <item quantity="other">Forwarding to %d devices</item>
    </plurals>

    <plurals name="receiving_from_count">
        <item quantity="one">Receiving from %d device</item>
        <item quantity="other">Receiving from %d devices</item>
    </plurals>

    <!-- For SessionBreakdownBottomSheet detail view -->
    <plurals name="device_count">
        <item quantity="one">%d device</item>
        <item quantity="other">%d devices</item>
    </plurals>
</resources>
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Hardcoded layout for all screens | BoxWithConstraints for responsive | Compose 1.0+ (2021) | Small screen devices now standard consideration |
| `if (count == 1) "device" else "devices"` | pluralStringResource | Compose 1.2+ (2022) | Proper localization support |
| String resource overload (separate strings per count) | Plural resources with quantity | Android 1.0 (always available) | Cleaner strings.xml |
| WindowSizeClass library (Material 3 Adaptive) | BoxWithConstraints for component-level responsiveness | Material 3 1.2+ (2024) | WindowSizeClass for app-level layout, BoxWithConstraints for components |

**Deprecated/outdated:**
- Manual screen size detection with DisplayMetrics: Use LocalConfiguration.current instead
- Separate layouts for different screen sizes: Use BoxWithConstraints for dynamic adaptation
- Hardcoded singular/plural strings: Use pluralStringResource

## Open Questions

1. **Direction enum naming strategy**
   - What we know: Current names (FORWARDING_TO, RECEIVING_FROM) are inverted from TARGET device perspective
   - What's unclear: Best semantic names (SENDING/RECEIVING vs OUTBOUND/INBOUND vs keep names but fix labels)
   - Recommendation: Use SENDING and RECEIVING for clarity, or keep current names and only fix UI labels (CONTEXT.md grants Claude discretion on naming)

2. **Exact compact layout breakpoint**
   - What we know: 400dp is Android convention, small phones are typically 360dp+
   - What's unclear: Should breakpoint be 380dp, 400dp, or 420dp?
   - Recommendation: Start with 400dp, test on 360dp emulator, adjust if needed

3. **Bidirectional handling in compact layout**
   - What we know: CONTEXT.md says remove merged "Bidirectional" and show as "Forwarding & Receiving"
   - What's unclear: Does this apply to compact layout too, or only to icon badge row?
   - Recommendation: Keep "Bidirectional" label in compact layout (space-efficient), implement "Forwarding & Receiving" separation only if explicitly requested in planning

## Sources

### Primary (HIGH confidence)
- /websites/developer_android_develop_ui_compose - Jetpack Compose responsive layouts documentation
- Codebase analysis: Direction.kt, HomeViewModel.kt, DirectionalStatusCard.kt, HomeScreen.kt
- Existing test patterns: DirectionalStatusTest.kt (561 lines validating direction logic)
- Phase 20 research: .planning/phases/20-bidirectional-visibility-indicators/20-RESEARCH.md (established direction patterns)

### Secondary (MEDIUM confidence)
- Android developer documentation: BoxWithConstraints usage patterns
- Material 3 guidelines: 400dp as conventional small screen breakpoint

### Tertiary (LOW confidence)
- None - all findings verified against official sources or codebase

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All components already in project, no new dependencies
- Architecture: HIGH - Patterns verified in official Compose docs and existing codebase
- Direction enum refactoring: HIGH - Scope fully understood (74 references across 16 files)
- Responsive layout: HIGH - BoxWithConstraints is standard Compose API with clear documentation
- Pluralization: HIGH - pluralStringResource verified in Compose docs with code examples
- Pitfalls: HIGH - Based on common Compose mistakes and enum refactoring risks

**Research date:** 2026-02-16
**Valid until:** 2026-03-16 (30 days - stable technologies, no fast-moving APIs)

**Files analyzed:**
- 7 source files (Direction.kt, HomeViewModel.kt, HomeScreen.kt, DirectionalStatusCard.kt, SessionBreakdownBottomSheet.kt, PairedDevicesScreen.kt, PairedDevicesViewModel.kt)
- 3 test files (HomeViewModelTest.kt, DirectionalStatusTest.kt)
- 1 prior phase research (Phase 20)
- 1 context file (25-CONTEXT.md)

**Test coverage:**
- HOME-01 UAT test exists and is failing (verified)
- HOME-02 has no unit test (manual verification required per STATE.md decisions)
- DirectionalStatusTest.kt has 20 test cases covering direction assignment logic

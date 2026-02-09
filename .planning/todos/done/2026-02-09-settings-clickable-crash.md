# UAT Issue: Settings Screen Crashes on Open

**Found:** 2026-02-09
**Severity:** Critical (blocking)
**Screen:** SettingsScreen

## Issue

App crashes when clicking the settings button. Same class of issue as 23-01 but different screen.

## Stack Trace

```
java.lang.IllegalArgumentException: clickable only supports IndicationNodeFactory instances provided to LocalIndication, but Indication was provided instead. Either migrate the Indication implementation to implement IndicationNodeFactory, or use the other clickable overload that takes an Indication parameter, and explicitly pass LocalIndication.current there. The Indication instance provided here was: androidx.compose.material.ripple.PlatformRipple@7f802409
    at androidx.compose.foundation.internal.InlineClassHelperKt.throwIllegalArgumentException(InlineClassHelper.kt:34)
    at androidx.compose.foundation.AbstractClickableNode.onObservedReadsChanged$lambda$0(Clickable.kt:1940)
    ...
```

## Root Cause

Same as 23-01 - `clickable` modifier (not `combinedClickable`) needs explicit `interactionSource` and `indication = LocalIndication.current` parameters.

## Fix Pattern

Find clickable modifiers in SettingsScreen.kt and apply same fix as 23-01:

```kotlin
// Before
.clickable { onClick() }

// After
.clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = LocalIndication.current,
    onClick = { onClick() }
)
```

## Files to Investigate

- `app/src/main/java/dev/notyouraverage/smscourier/composables/screens/SettingsScreen.kt`
- Any custom clickable components used in Settings
- Check all `clickable` and `combinedClickable` usages across codebase

## Notes

- 23-01 only fixed PairedDevicesScreen and SessionHistoryComponents
- Need comprehensive audit of all clickable/combinedClickable usages
- Consider creating a reusable modifier extension to avoid this pattern repeating

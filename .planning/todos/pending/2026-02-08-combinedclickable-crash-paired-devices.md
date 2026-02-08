# UAT Issue: combinedClickable Crashes (Multiple Locations)

**Created:** 2026-02-08
**Source:** Manual verification of v0.0.64
**Priority:** must (crash)
**Type:** Bug

## Problem

App crashes in multiple places due to deprecated `combinedClickable` Indication API.

## Steps to Reproduce

**Crash 1: Paired Devices Page**
1. Open app
2. Click on Paired Devices card on home screen
3. App crashes immediately

**Crash 2: Device in "I Forward To"**
1. Open app with at least one paired device
2. Navigate to Paired Devices screen (if it doesn't crash)
3. Click on a device in "I Forward To" section
4. App crashes

## Stack Trace

```
java.lang.IllegalArgumentException: clickable only supports IndicationNodeFactory instances
provided to LocalIndication, but Indication was provided instead. Either migrate the
Indication implementation to implement IndicationNodeFactory, or use the other clickable
overload that takes an Indication parameter, and explicitly pass LocalIndication.current there.
```

## Root Cause

`combinedClickable` modifier in `PairedDevicesScreen.kt:367` uses deprecated Indication API that's incompatible with newer Compose Foundation versions.

## Affected Files

- `app/src/main/java/dev/notyouraverage/smscourier/composables/screens/PairedDevicesScreen.kt` (line 367)
- `app/src/main/java/dev/notyouraverage/smscourier/composables/components/SessionHistoryComponents.kt` (line 66)

## Fix Options

1. Use `clickable` overload that explicitly passes `LocalIndication.current`
2. Remove `indication` parameter and use default behavior
3. Migrate to `IndicationNodeFactory` API

## Acceptance Criteria

- [ ] Paired Devices screen opens without crash
- [ ] Clicking device in "I Forward To" does not crash
- [ ] Long-press export functionality still works
- [ ] Session History screen works (also uses combinedClickable)
- [ ] All other clickable elements work correctly

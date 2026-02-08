# UAT Issue: Crash on "I Forward To" Click in Paired Devices

**Created:** 2026-02-08
**Source:** Manual verification of v0.0.64
**Priority:** must (crash)
**Type:** Bug

## Problem

App crashes when clicking on a device in the "I Forward To" section of the Paired Devices screen.

## Steps to Reproduce

1. Open app with at least one paired device
2. Click on Paired Devices card
3. See "I Forward To" section with one device
4. Click on the device
5. App crashes

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

- [ ] Clicking device in "I Forward To" does not crash
- [ ] Long-press export functionality still works
- [ ] All other clickable elements work correctly

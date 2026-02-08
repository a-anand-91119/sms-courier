# UAT Issue: Remove Device from Paired Devices Screen Does Nothing

**Found:** 2026-02-09
**Severity:** High
**Screen:** PairedDevicesScreen

## Issue

When trying to remove/unpair a device from the Paired Devices screen (via long-press menu), nothing happens. The device remains in the list.

However, unpair from Device History screen works correctly (fixed in 23-02) - device moves to Removed Devices section.

After unpairing from Device History, the device still appears in Paired Devices screen - indicating the screens are not in sync or using different data sources.

## Affected Flows

1. Long-press device in Paired Devices > "Remove Device" > Confirm > **Nothing happens**
2. Same issue for both paired and pending devices
3. Device History unpair works, but Paired Devices still shows the device

## Expected Behavior

- Remove Device should archive the device (same as Device History unpair)
- Device should disappear from Paired Devices screen
- Device should appear in Device History's Removed Devices section

## Technical Notes

- PairedDevicesScreen uses `viewModel.deleteDevice(device)`
- DeviceHistoryScreen uses `viewModel.unpairDevice(device)` which calls `archiveDevice()`
- May be using different underlying operations (hard delete vs soft delete/archive)
- Need to align both screens to use the same archive pattern

## Files to Investigate

- `app/src/main/java/dev/notyouraverage/smscourier/composables/screens/PairedDevicesScreen.kt`
- `app/src/main/java/dev/notyouraverage/smscourier/viewmodels/PairedDevicesViewModel.kt`
- Compare with DeviceHistoryViewModel.unpairDevice() pattern

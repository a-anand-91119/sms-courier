# UAT Issue: Re-paired Device Not Showing in Device History

**Found:** 2026-02-09
**Severity:** High
**Screen:** DeviceHistoryScreen

## Issue

When re-pairing a device that was previously removed/archived:
- Device appears in Paired Devices screen as "paired" ✓
- Device does NOT appear in Device History screen ✗

## Steps to Reproduce

1. Pair a device
2. Unpair/remove the device (via Device History)
3. Device moves to "Removed Devices" section
4. Re-pair the same device
5. Device shows in Paired Devices as paired
6. Device History does not show the device in Active Devices section

## Expected Behavior

Re-paired device should appear in Device History's "Active Devices" section with:
- Fresh statistics (or preserved history from previous pairing)
- Accessible session history

## Likely Root Cause

When device is archived (`isArchived = true`), the flag may not be reset when re-pairing. Device History query likely filters by `isArchived = false`, so archived devices don't appear even after re-pairing.

## Technical Notes

- Check if pairing flow resets `isArchived` flag to `false`
- Check `PairedDeviceDao` query for active devices
- May need to "unarchive" device when re-pairing
- Or create new device entry instead of reusing archived one

## Files to Investigate

- `app/src/main/java/dev/notyouraverage/smscourier/handlers/SmsCommandHandler.kt` (pairing logic)
- `app/src/main/java/dev/notyouraverage/smscourier/data/dao/PairedDeviceDao.kt` (queries)
- `app/src/main/java/dev/notyouraverage/smscourier/repository/PairedDeviceRepository.kt`

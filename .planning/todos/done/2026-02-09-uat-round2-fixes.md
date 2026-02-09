---
created: 2026-02-09T15:05
completed: 2026-02-09T15:05
title: UAT Bug Fixes Round 2
area: uat
files:
  - app/src/main/java/dev/notyouraverage/smscourier/viewmodels/ForwardingControlViewModel.kt
  - app/src/main/java/dev/notyouraverage/smscourier/viewmodels/DeviceHistoryViewModel.kt
  - app/src/main/java/dev/notyouraverage/smscourier/repository/ForwardedMessageRepository.kt
  - app/src/main/java/dev/notyouraverage/smscourier/repository/ForwardingSessionRepository.kt
  - app/src/main/java/dev/notyouraverage/smscourier/composables/screens/HomeScreen.kt
  - app/src/main/java/dev/notyouraverage/smscourier/composables/screens/DeviceHistoryScreen.kt
  - app/src/main/java/dev/notyouraverage/smscourier/data/settings/SettingsDefaults.kt
  - app/src/main/java/dev/notyouraverage/smscourier/composables/screens/ForwardingControlScreen.kt
  - app/src/main/java/dev/notyouraverage/smscourier/data/dao/ForwardingSessionDao.kt
---

## Issues Identified

### 1. Service Toggle Color Contrast (UI)
**Problem:** Service enable/disable toggle has same blue color theme for both states, making it difficult to distinguish enabled vs disabled.
**Status:** FIXED
**Solution:** Changed switch colors - checked uses white thumb on primary track, unchecked uses onSurfaceVariant thumb on outline track.

### 2. Message Counter Always Shows 0
**Problem:** After forwarding messages, counter shows 0 everywhere. Device stats, session history all show 0 messages.
**Root Cause:** Phone number normalization mismatch. Sessions stored phone numbers without normalization, but device records were normalized. The `incrementTotalMessagesForwarded` DAO call couldn't find matching records.
**Status:** FIXED
**Solution:** Added `normalizePhoneNumber()` to ForwardedMessageRepository and ForwardingSessionRepository. Applied normalization consistently to all phone number parameters.

### 3. Session History Shows Sessions But Messages Empty
**Problem:** Device history shows sessions correctly, but message count inside sessions is 0.
**Root Cause:** Same phone number normalization issue as #2.
**Status:** FIXED (same fix)

### 4. Exported File Session Details Empty
**Problem:** CSV export shows session count (e.g., 4) but session table is empty.
**Root Cause:** Same phone number normalization issue - `getSessionsForDeviceList` couldn't match.
**Status:** FIXED (same fix)

### 5. Active Session Persists After Unpair
**Problem:** With active session, unpairing device from device history removes device from other places, but session still shows in start forwarding screen.
**Root Cause:** `unpairDevice()` didn't end active sessions before archiving.
**Status:** FIXED
**Solution:** Modified `DeviceHistoryViewModel.unpairDevice()` to call `sessionRepository.endSessionForDevice()` before archiving.

### 6. Home Screen Inconsistency After Unpair
**Problem:** After unpair with active session: Paired=0, Pending=0, Active=1. Clicking Active shows "no active sessions".
**Root Cause:** Same as #5 - session not ended on unpair.
**Status:** FIXED (same fix as #5)

### 7. Block Removal If Session Active
**Problem:** User requested blocking device removal if session is active.
**Status:** IMPLEMENTED (alternative approach)
**Solution:** Instead of blocking, we now warn the user in the unpair confirmation dialog that "This device has an active forwarding session. Unpairing will end the session and remove [device]." Then we end the session when they confirm.

### 8. Default Forwarding Duration Wrong
**Problem:** Settings shows 5 min default, but forwarding sheet defaults to 30m.
**Root Cause:** `ForwardingControlViewModel.ForwardingUiState` had hardcoded `durationMinutes = 30`.
**Status:** FIXED
**Solution:** Changed to use `SettingsDefaults.DEFAULT_FORWARDING_DURATION` (which is 5).

### 9. Home Screen UI (Deferred)
**Problem:** User doesn't like current home screen layout.
**Status:** DEFERRED - User said no changes needed now.

## Files Modified

| File | Changes |
|------|---------|
| `SettingsDefaults.kt` | Added `MAX_FORWARDING_DURATION = 30` |
| `ForwardingControlScreen.kt` | Slider range 5-30, label "30 min" |
| `ForwardingControlViewModel.kt` | Default duration from SettingsDefaults |
| `DeviceHistoryScreen.kt` | Removed Restore button, added delete confirmation, active session warning |
| `DeviceHistoryViewModel.kt` | `unpairDevice()` ends sessions, added `hasActiveSession()`, added `permanentlyDeleteDevice()` |
| `ForwardingSessionDao.kt` | Added `deleteAllForDevice()` |
| `ForwardingSessionRepository.kt` | Added `normalizePhoneNumber()`, normalized all phone parameters, added `deleteAllForDevice()` |
| `ForwardedMessageRepository.kt` | Added `normalizePhoneNumber()`, normalized all phone parameters |
| `HomeScreen.kt` | Fixed switch colors for better contrast |
| `HomeViewModel.kt` | Added `approvedDevices` and `pendingDevices` lists to HomeState |
| `DateTimeFormatters.kt` | Fixed syntax error (stray backtick) |

## Test Updates

- `ForwardingControlViewModelTest.kt` - Updated to use `SettingsDefaults.DEFAULT_FORWARDING_DURATION`

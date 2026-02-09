---
created: 2026-02-09T19:00
title: "Settings default session duration not used when starting forwarding"
area: uat
priority: high
files:
  - app/src/main/java/dev/notyouraverage/smscourier/viewmodels/ForwardingControlViewModel.kt
  - app/src/main/java/dev/notyouraverage/smscourier/repository/SettingsRepository.kt
  - app/src/main/java/dev/notyouraverage/smscourier/composables/screens/ForwardingControlScreen.kt
---

## Problem

When starting a forwarding session, the duration slider does not use the default value saved in Settings.

Example:
- User sets default forwarding duration to 10 minutes in Settings
- User goes to start a forwarding session
- Duration slider shows 5 minutes (hardcoded default) instead of 10 minutes

The saved settings value is being ignored.

## Expected Behavior

The ForwardingControlScreen should:
1. Read the user's saved default duration from SettingsRepository
2. Initialize the duration slider to that value
3. User can still adjust, but starting point should respect their preference

## Files to Investigate

- `ForwardingControlViewModel.kt` - Check if it reads `defaultForwardingDurationMinutes` from settings
- `SettingsRepository.kt` - Verify the setting is being stored/retrieved correctly
- `ForwardingControlScreen.kt` - Check slider initialization

## Solution

Ensure ForwardingControlViewModel reads the default duration from SettingsRepository on init and uses it to initialize the UI state.

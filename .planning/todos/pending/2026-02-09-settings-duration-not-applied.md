---
created: 2026-02-09T18:32
title: "Settings default duration not applied when starting session"
area: uat
priority: medium
files:
  - app/src/main/java/dev/notyouraverage/smscourier/viewmodels/ForwardingControlViewModel.kt
  - app/src/main/java/dev/notyouraverage/smscourier/repository/SettingsRepository.kt
  - app/src/main/java/dev/notyouraverage/smscourier/composables/screens/ForwardingControlScreen.kt
---

## Problem

When starting a forwarding session:
- User has set default duration to 10 minutes in Settings
- ForwardingControlScreen shows 5 minutes as default instead of the saved 10

The settings value is not being read/applied when initializing the forwarding control UI.

## Solution

1. Check if ForwardingControlViewModel reads defaultForwardingDurationMinutes from SettingsRepository
2. Verify the value is used to initialize the duration state
3. Write test to verify settings are respected

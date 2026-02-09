---
created: 2026-02-09T16:09
title: Settings default duration should be slider not dropdown
area: uat
files:
  - app/src/main/java/dev/notyouraverage/smscourier/composables/screens/SettingsScreen.kt
---

## Problem

In the Settings page, the default forwarding duration setting is currently a dropdown menu. User expects it to be a slider (consistent with the duration slider in the ForwardingControlScreen when starting a session).

The ForwardingControlScreen already has a slider for duration (5-30 minutes range). The Settings page should match this UX pattern for consistency.

## Solution

Replace the dropdown in SettingsScreen with a Slider component:
- Range: 5 to MAX_FORWARDING_DURATION (30 minutes)
- Steps: Match ForwardingControlScreen (steps = 4 for 5, 10, 15, 20, 25, 30)
- Show current value label
- Use SettingsDefaults.DEFAULT_FORWARDING_DURATION as initial value

---
status: complete
phase: 13-settings-screen-main-settings
source: [13-01-SUMMARY.md, 13-02-SUMMARY.md, 13-03-SUMMARY.md]
started: 2026-01-19T05:55:00Z
updated: 2026-01-19T06:20:00Z
---

## Current Test

[testing complete]

## Tests

### 1. Navigate to Settings
expected: Tap the gear icon on the home screen. Settings screen opens with "Settings" title in large top app bar and back arrow for navigation.
result: pass

### 2. Back Navigation
expected: From Settings screen, tap back arrow or Android back gesture. Returns to home screen.
result: pass

### 3. Toggle Notification Persistence
expected: In Preferences section, find "Notification persistence" toggle. Tap to change. Subtitle updates to show current behavior ("recreates when dismissed" vs "stays dismissed").
result: pass

### 4. Select Forwarding Duration
expected: Tap "Default forwarding duration" row. Dropdown shows 15 min, 30 min, 1 hr options. Select one. Dropdown closes, selection shown in row.
result: pass
note: Dropdown appears left-aligned (cosmetic - known Material3 DropdownMenu behavior)

### 5. Select Theme
expected: Find theme selector with Light, Dark, System buttons. Tap each one. App theme changes immediately (no restart needed). Dark makes app dark, Light makes app light, System follows device setting.
result: pass

### 6. Permission Status Display
expected: In Information section, see permission status for SMS receive, SMS send, and Notifications. Each shows "Granted" (green) or "Not granted" (red).
result: pass

### 7. Fix Permissions
expected: If any permission shows "Not granted", tap Fix button. Opens Android app settings page where you can grant permission. Return to app - permission status should update.
result: pass

### 8. View App Version
expected: Version row shows app version number (e.g., "0.0.62" or similar).
result: pass
note: Shows "0.0.1-dev" for local builds (expected - CI sets real version via env var)

### 9. Privacy Policy Link
expected: Tap Privacy Policy row. Opens browser to privacy policy URL.
result: pass
note: Fixed URL to https://www.smscourier.app/privacy (commit 7eb3f9c)

### 10. Support Link
expected: Tap Support row. Opens browser to GitLab issues page.
result: pass
note: Fixed URL to correct GitLab instance (commit 65d1dad)

### 11. Settings Persist
expected: Change notification toggle, duration, and/or theme. Force close app completely. Reopen app. Go to Settings. All changes are still saved.
result: pass

## Summary

total: 11
passed: 11
issues: 0
pending: 0
skipped: 0

## Gaps

[none yet]

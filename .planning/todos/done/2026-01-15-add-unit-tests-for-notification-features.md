---
created: 2026-01-15T11:21
title: Add unit tests for notification features
area: testing
files:
  - app/src/main/java/dev/notyouraverage/smscourier/services/foreground/MasterService.kt:154-192
  - app/src/main/java/dev/notyouraverage/smscourier/receivers/ServiceNotificationReceiver.kt
---

## Problem

Recent commits added notification functionality (non-dismissible behavior, click to open app) but no unit tests were created to verify these features work correctly.

Commits without tests:
- db56b8d: feat: make foreground notification clickable to open app
- 789e20c: fix: make foreground notification truly non-dismissible on Android 14+
- 58ff914: feat: make SMS Courier foreground service notification non-dismissible

Key functionality that needs testing:
1. `buildForegroundNotification()` creates notification with correct properties (contentIntent, deleteIntent, ongoing, category)
2. ServiceNotificationReceiver handles NOTIFICATION_DISMISSED action and triggers service recreation
3. `recreateForegroundNotification()` properly recreates notification when dismissed
4. contentIntent launches MainActivity with correct flags

## Solution

Create unit tests in `app/src/test/java/` directory:
- MasterServiceTest.kt: Test notification building logic, intent creation, flags
- ServiceNotificationReceiverTest.kt: Test broadcast receiver handles dismissal correctly

Use Robolectric for Android framework dependencies (NotificationCompat, PendingIntent, etc.)
Mock NotificationManager to verify notification creation/recreation behavior

---
created: 2026-01-15T11:18
title: Make notification clickable to open app
area: ui
files:
  - app/src/main/java/dev/notyouraverage/smscourier/services/foreground/MasterService.kt:147-172
---

## Problem

The SMS Courier foreground service notification currently doesn't open the app when the user taps on it. Users expect to be able to tap the notification to return to the app.

Currently, the notification is created in MasterService.kt:147-172 in the `buildForegroundNotification()` method, but it doesn't have a contentIntent set.

## Solution

Add a PendingIntent that launches MainActivity when the notification is clicked. Use `setContentIntent()` on the NotificationCompat.Builder with a PendingIntent pointing to MainActivity.

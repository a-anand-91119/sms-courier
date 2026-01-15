---
created: 2026-01-15T11:08
title: Make SMS Courier notification non-dismissible
area: ui
files:
  - app/src/main/java/dev/notyouraverage/smscourier/services/foreground/MasterService.kt:468-478
---

## Problem

The SMS Courier foreground service notification can currently be dismissed by the user. This makes it hard to tell if the service is running. The notification should be non-dismissible so users always know when SMS Courier is active.

Currently at MasterService.kt:468-478, the notification is created with:
- `setAutoCancel(false)`
- `setOngoing(true)`

However, these settings alone may not prevent dismissal on newer Android versions. The notification needs to be truly non-dismissible to provide clear visibility of the service status.

## Solution

Review Android notification best practices for foreground services to ensure the notification cannot be dismissed. This may involve:
- Verifying the notification category is appropriate for a foreground service
- Ensuring the notification channel configuration prevents dismissal
- Testing on various Android versions to confirm non-dismissible behavior

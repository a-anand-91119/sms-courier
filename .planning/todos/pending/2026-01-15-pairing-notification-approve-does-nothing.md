---
created: 2026-01-15T17:38
title: Pairing notification approve button does nothing
area: ui
files:
  - app/src/main/java/dev/notyouraverage/smscourier/services/foreground/MasterService.kt:287-296
  - app/src/main/java/dev/notyouraverage/smscourier/receivers/PairingActionReceiver.kt:26-36
  - app/src/main/java/dev/notyouraverage/smscourier/notifications/PairingNotificationManager.kt:88-134
---

## Problem

When a pairing request notification appears and the user taps "Approve", nothing happens. The notification dismisses but no password creation dialog appears and the pairing is not approved.

The "Reject" button works as expected - it properly rejects the pairing request.

**Root cause:**
The approval flow is incomplete. The code path is:
1. User taps "Approve" → PairingActionReceiver receives ACTION_APPROVE
2. PairingActionReceiver cancels notification and sends PAIRING_APPROVE_REQUESTED to MasterService
3. MasterService.handlePairingApproveRequested() (line 287-296) only logs the request with a TODO comment: `// TODO: Launch password creation activity/dialog`
4. Nothing actually happens after that

In contrast, the reject flow is complete:
1. User taps "Reject" → PairingActionReceiver receives ACTION_REJECT
2. MasterService.handlePairingRejectRequested() calls `commandHandler.rejectPairing(phoneNumber)` which completes the rejection

**Expected behavior:**
Tapping "Approve" should:
1. Dismiss the notification
2. Launch an Activity or Dialog for password creation
3. Once password is set, send PAIR_APPROVED SMS to the requesting device
4. Store the pairing with passwordHash in the database

## Solution

**Option 1: Launch Activity for password creation**
- Create PairingApprovalActivity
- Launch it from MasterService.handlePairingApproveRequested()
- Activity shows password creation form
- On submit, calls commandHandler.approvePairing(phoneNumber, password)

**Option 2: Use existing PairingRequestsViewModel**
- Navigation to PairingRequestsScreen when notification is tapped
- Screen already has approve/reject logic with password dialog
- Simpler - reuses existing UI

**Option 3: Notification with direct text input**
- Use RemoteInput in the notification for password entry
- Handle password submission in PairingActionReceiver
- More seamless UX but less secure (password visible in notification shade)

**Recommended:** Option 2 - tap "Approve" navigates to the PairingRequestsScreen with that request pre-selected or highlighted. This reuses existing tested UI components.

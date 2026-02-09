---
created: 2026-02-09T16:09
title: Unpair from device history should send SMS to remote device
area: uat
files:
  - app/src/main/java/dev/notyouraverage/smscourier/viewmodels/DeviceHistoryViewModel.kt
  - app/src/main/java/dev/notyouraverage/smscourier/handlers/SmsCommandHandler.kt
  - app/src/main/java/dev/notyouraverage/smscourier/services/SmsSender.kt
---

## Problem

When unpairing a device from the Device History screen, the app only archives the device locally but does NOT send an UNPAIR SMS command to the remote device. This means:

1. The remote device still thinks it's paired
2. The remote device may continue trying to forward messages
3. Inconsistent state between the two devices

The unpair flow from Paired Devices screen (via SmsCommandHandler) sends an UNPAIR SMS to notify the other device. Device History unpair should follow the same protocol.

## Solution

Modify `DeviceHistoryViewModel.unpairDevice()` to:

1. Send UNPAIR SMS command to the remote device via SmsSender
2. Then end any active sessions
3. Then archive the device locally

This matches the behavior when unpairing through other flows. The SmsSender already has `sendUnpair(phoneNumber, role)` method that should be called.

Need to inject SmsSender into DeviceHistoryViewModel or route through a use case/handler that has access to SmsSender.

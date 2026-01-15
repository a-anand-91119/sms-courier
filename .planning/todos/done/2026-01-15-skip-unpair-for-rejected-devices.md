---
created: 2026-01-15T18:10
title: Skip UNPAIR SMS when deleting rejected devices
area: logic
files:
  - app/src/main/java/dev/notyouraverage/smscourier/viewmodels/PairedDevicesViewModel.kt:33-51
---

## Problem

When deleting a device from the paired devices list, `PairedDevicesViewModel.deleteDevice()` always sends an UNPAIR SMS to the other device. However, if the device has `REJECTED` status (meaning we rejected their pairing request), there's no need to send UNPAIR because:

1. The pairing was never established
2. The other device already knows we rejected them (they received PAIR_REJECTED)
3. Sending UNPAIR to a rejected device is unnecessary network traffic

Similarly, devices with `PENDING_SENT` status (we sent a request that hasn't been responded to) may not need UNPAIR notification.

## Solution

In `deleteDevice()`, check the device status before sending UNPAIR:

```kotlin
fun deleteDevice(device: PairedDevice) {
    viewModelScope.launch {
        sessionRepository.endSessionForDevice(device.phoneNumber, "USER")

        // Only send UNPAIR if pairing was actually established
        if (device.status == PairingStatus.APPROVED) {
            val roleToDeleteOnRemote = when (device.role) {
                DeviceRole.SOURCE -> DeviceRole.TARGET
                DeviceRole.TARGET -> DeviceRole.SOURCE
            }
            smsSender.sendUnpair(device.phoneNumber, roleToDeleteOnRemote)
        }

        deviceRepository.deleteByPhoneNumberAndRole(device.phoneNumber, device.role)
    }
}
```

Skip UNPAIR for: REJECTED, PENDING_SENT, PENDING_RECEIVED statuses.

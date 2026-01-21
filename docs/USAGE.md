# Usage Guide

This guide covers all features of SMS Courier in detail.

## Table of Contents

- [Getting Started](#getting-started)
- [Pairing Devices](#pairing-devices)
- [Managing Paired Devices](#managing-paired-devices)
- [Forwarding Sessions](#forwarding-sessions)
- [Settings](#settings)
- [Permissions](#permissions)

## Getting Started

### First Launch

1. Install SMS Courier on both devices
2. Open the app and grant the required permissions:
   - SMS (send and receive)
   - Notifications
3. The app will start its background service automatically

### Understanding Roles

- **Source Device**: Requests and receives forwarded messages
- **Target Device**: Forwards its incoming messages to the source

A single device can be both a source (receiving from one device) and a target (forwarding to another device) simultaneously.

## Pairing Devices

### Sending a Pairing Request (Source Device)

1. Open SMS Courier
2. Tap **Add Device** on the home screen
3. Enter the phone number of the target device
4. Tap **Send Pairing Request**

The target device will receive an SMS command and show a notification.

### Accepting a Pairing Request (Target Device)

1. You'll see a notification: "Pairing request from [number]"
2. Tap the notification or open the app
3. Go to **Pairing Requests**
4. Tap **Approve** next to the request
5. Set a password (this will be needed to start forwarding)
6. Tap **Confirm**

Both devices are now paired.

### Rejecting a Pairing Request

1. Open **Pairing Requests**
2. Tap **Reject** next to the request
3. The requesting device will be notified

## Managing Paired Devices

### Viewing Paired Devices

1. Open SMS Courier
2. Tap **Paired Devices**
3. Use the tabs to switch between:
   - **Source**: Devices you can receive messages from
   - **Target**: Devices you forward messages to

### Removing a Paired Device

1. Go to **Paired Devices**
2. Long-press or tap the menu on a device
3. Tap **Remove**
4. Confirm the removal

The other device will be notified that pairing has been removed.

### Resending a Pairing Request

If your initial request wasn't responded to:

1. Go to **Paired Devices**
2. Find the pending device
3. Tap **Resend Request**

Note: There's a cooldown between resend attempts to prevent spam.

## Forwarding Sessions

### Starting a Forwarding Session

1. Go to **Forwarding Control**
2. Select a paired source device
3. Enter the password (set during pairing approval)
4. Choose the session duration (15, 30, or 60 minutes)
5. Tap **Start Forwarding**

All SMS messages received on the target device will now be forwarded to you.

### During an Active Session

- Forwarded messages appear as regular SMS from the target device
- The message format: `[Original Sender]: Original message content`
- Check active sessions in **Forwarding Control**
- See the time remaining and message count

### Stopping a Forwarding Session

Sessions stop automatically when:
- The duration expires
- You manually tap **Stop Forwarding**
- Either device removes the pairing

To stop manually:
1. Go to **Forwarding Control**
2. Find the active session
3. Tap **Stop**

## Settings

Access settings from the home screen menu.

### Preferences

| Setting | Description |
|---------|-------------|
| Notification Persistence | Keep service notification visible after dismissal |
| Default Forwarding Duration | Pre-selected duration when starting sessions |
| Theme | Light, Dark, or System default |

### Advanced Security

| Setting | Description | Default |
|---------|-------------|---------|
| Lockout Duration | How long a device is locked after too many failed attempts | 15 min |
| Max Failed Attempts | Failed password attempts before lockout | 5 |
| Challenge Expiry | How long an auth challenge remains valid | 2 min |
| Max Pairing Resends | Maximum times you can resend a pairing request | 5 |
| Pairing Resend Cooldown | Wait time between resend attempts | 5 min |
| Auth Request Timeout | How long to wait for auth response | 5 min |

### Permission Status

View which permissions are granted and access system settings to fix any issues.

## Permissions

### Required Permissions

| Permission | Purpose |
|------------|---------|
| RECEIVE_SMS | Detect incoming commands and messages to forward |
| SEND_SMS | Send commands and forwarded messages |
| POST_NOTIFICATIONS | Show pairing requests and service status |
| FOREGROUND_SERVICE | Keep the service running reliably |

### Granting Permissions

If a permission is missing:
1. Go to **Settings** in the app
2. Find the permission in the **Permissions** section
3. Tap **Fix** to open system settings
4. Enable the permission

### Battery Optimization

For reliable operation, disable battery optimization for SMS Courier:
1. Go to Android Settings > Apps > SMS Courier
2. Tap Battery
3. Select "Unrestricted" or "Don't optimize"

# Frequently Asked Questions

## General

### What is SMS Courier?

SMS Courier forwards SMS messages from one Android phone to another using only SMS. No internet connection, cloud servers, or accounts are required.

### Why would I need this?

Common use cases:
- **Dual phone users**: Forward work phone messages to your personal phone
- **Traveling**: Forward messages from your home SIM while abroad
- **Backup phone**: Keep a backup device that receives your important messages
- **Shared numbers**: Forward a business number to multiple team members

### Does it require internet?

No. SMS Courier works entirely over SMS. Both devices just need cellular service.

### Is it free?

Yes, the app is free. However, standard SMS charges from your carrier apply for the commands and forwarded messages.

## Pairing

### Why isn't my pairing request going through?

Check that:
1. You entered the correct phone number (with country code if needed)
2. The target device has SMS Courier installed and running
3. Both devices have SMS permissions granted
4. Neither device is in airplane mode

### Can I pair with more than one device?

Yes. You can:
- Be a **source** for multiple devices (receive from many)
- Be a **target** for multiple devices (forward to many)
- Have both roles with different devices

### What happens if I reject a pairing request?

The requesting device will be notified. They can send another request later.

### How do I change the password?

Currently, you need to remove the pairing and re-pair with a new password.

## Forwarding

### Why aren't messages being forwarded?

Check that:
1. There's an active forwarding session (not expired)
2. The target device's SMS Courier service is running
3. Both devices have cellular signal
4. The password was entered correctly when starting the session

### Can I forward to multiple devices at once?

Currently, each forwarding session is one-to-one. To forward to multiple devices, the target needs to start separate sessions with each source.

### What messages get forwarded?

All incoming SMS messages except:
- Messages that are SMS Courier commands (starting with `SMSC`)
- Messages from the paired device itself

### Can I reply to forwarded messages?

Currently, replies need to be sent from the original device. Forwarded messages are one-way.

### Why did my session stop?

Sessions stop when:
- The duration you selected expires
- You or the target manually stopped it
- The pairing was removed
- The target device restarted and the service didn't restart

## Security

### How secure is the password?

Passwords are hashed using bcrypt, the same algorithm used by many secure websites. Even if someone accessed the device's data, they couldn't recover the original password.

### Can someone intercept my forwarded messages?

Forwarded messages travel over SMS, which has the same security as regular text messages. For additional security, you can enable encryption in settings (uses AES-256).

### What happens after too many wrong passwords?

The device is locked out for a configurable period (default 15 minutes). This prevents brute-force attacks.

### Can I see who tried to start forwarding?

The app tracks failed attempts per device. You can view this in the paired devices list.

## Troubleshooting

### The service keeps stopping

1. Disable battery optimization for SMS Courier
2. On some phones (Xiaomi, Huawei, Samsung), add the app to the protected/allowed list
3. Make sure you have enough storage space

### I'm not getting pairing notifications

1. Check that notification permission is granted
2. Make sure notifications aren't blocked for SMS Courier
3. Check Do Not Disturb settings

### Messages are delayed

SMS delivery depends on your carrier. Delays are usually carrier-related, not app-related. Check:
1. Cellular signal strength on both devices
2. Whether you're in a congested network area

### The app crashed

Please report crashes with:
- Your Android version and device model
- What you were doing when it crashed
- Any error messages shown

## Privacy

### Does SMS Courier collect my data?

No. SMS Courier:
- Has no analytics or tracking
- Doesn't connect to any servers
- Stores all data locally on your device
- Doesn't access your contacts or call history

### Where is my data stored?

All data (paired devices, sessions, settings) is stored locally in the app's private storage. It's encrypted by Android's app sandbox.

### What happens to forwarded messages?

Forwarded messages are sent as regular SMS. The app doesn't store copies of forwarded message content.

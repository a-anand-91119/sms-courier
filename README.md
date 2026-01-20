# SMS Courier

Forward SMS messages securely between your Android devices.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-14%2B-green.svg)](https://developer.android.com/about/versions/14)

## Features

- **Secure Device Pairing** - Pair two devices with password protection
- **On-Demand Forwarding** - Start forwarding sessions when you need them
- **Time-Limited Sessions** - Sessions auto-expire after your chosen duration
- **End-to-End Encryption** - Optional AES-256 encryption for forwarded messages
- **No Internet Required** - Works entirely over SMS, no cloud servers
- **Privacy First** - Your messages never leave the SMS network

## Quick Start

1. **Install** SMS Courier on both devices
2. **Pair** the devices by sending a pairing request
3. **Start Forwarding** when you want to receive messages from the other device

## Installation

### Play Store
*Coming soon*

### Build from Source
```bash
git clone https://github.com/yourusername/SMSCourier.git
cd SMSCourier
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`

## How to Use

### Pairing Two Devices

**On Device A (will receive forwarded messages):**
1. Open SMS Courier
2. Tap "Add Device"
3. Enter Device B's phone number
4. Send pairing request

**On Device B (will forward its messages):**
1. You'll receive a pairing notification
2. Tap "Approve" and set a password
3. Devices are now paired

### Starting a Forwarding Session

**On Device A:**
1. Go to "Forwarding Control"
2. Select the paired device
3. Enter the password and choose duration
4. Tap "Start Forwarding"

Messages received on Device B will now be forwarded to Device A until the session expires.

<details>
<summary><strong>Security Details</strong></summary>

### Password Security
- Passwords are hashed using bcrypt (cost factor 12)
- Never stored in plaintext on either device
- Failed attempts trigger progressive lockout

### Message Encryption
- Optional AES-256-CBC encryption
- PBKDF2 key derivation (10,000 iterations)
- Unique salt and IV per session

### Authentication
- Challenge-response protocol prevents replay attacks
- HMAC-SHA256 verification
- Time-limited authentication challenges

</details>

<details>
<summary><strong>Permissions Explained</strong></summary>

| Permission | Why It's Needed |
|------------|-----------------|
| `RECEIVE_SMS` | Intercept incoming SMS to detect commands and messages to forward |
| `SEND_SMS` | Send forwarded messages and command responses |
| `POST_NOTIFICATIONS` | Show pairing requests and service status |
| `FOREGROUND_SERVICE` | Keep the service running reliably in the background |

SMS Courier only processes messages from paired devices and its own command protocol.

</details>

## Requirements

- Android 14 (API 34) or higher
- SMS capability (phone with active SIM)
- SMS permissions granted

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development setup and guidelines.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

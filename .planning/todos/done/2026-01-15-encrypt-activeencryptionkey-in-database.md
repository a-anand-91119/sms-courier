---
created: 2026-01-15T17:32
title: Encrypt activeEncryptionKey in database
area: security
files:
  - app/src/main/java/dev/notyouraverage/smscourier/data/entities/PairedDevice.kt:56
---

## Problem

The `activeEncryptionKey` field in the `PairedDevice` entity (line 56) stores passwords in plaintext within the Room database. This field is used on SOURCE devices to store the password for decrypting forwarded messages.

**Current implementation:**
```kotlin
// Only for SOURCE role (plaintext password for decryption)
@ColumnInfo(name = "active_encryption_key")
val activeEncryptionKey: String? = null,
```

This represents a security vulnerability as the database file can be accessed by rooted devices or through backups, exposing passwords in plaintext.

## Solution

**Options to consider:**

1. **Encrypt at rest using Android Keystore:**
   - Use Android Keystore to generate encryption keys
   - Encrypt the password before storing in database
   - Decrypt when needed for HMAC operations
   - Keystore keys are hardware-backed and can't be extracted

2. **Use EncryptedSharedPreferences:**
   - Store activeEncryptionKey in EncryptedSharedPreferences instead of Room
   - Reference by phone number
   - Automatically encrypted using Android's security library

3. **Derive key on-demand:**
   - Don't store the password at all
   - Prompt user to re-enter when needed
   - More secure but less convenient

**Recommended approach:** Option 1 (Android Keystore) for balance of security and UX.

**Related security consideration:** The `authKey` field (line 60) stores SHA256(password) which is also sensitive but less critical since it can't directly decrypt messages.

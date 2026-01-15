package dev.notyouraverage.smscourier.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DeviceRole {
    // This device requests forwarding FROM the paired device
    SOURCE,

    // This device forwards SMS TO the paired device
    TARGET,
}

enum class PairingStatus {
    // Source: waiting for target's response
    PENDING_SENT,

    // Target: received request, awaiting user approval
    PENDING_RECEIVED,

    // Pairing complete
    APPROVED,

    // Pairing was rejected
    REJECTED,
}

@Entity(
    tableName = "paired_devices",
    primaryKeys = ["phoneNumber", "device_role"]
)
data class PairedDevice(
    // Normalized E.164 format
    val phoneNumber: String,

    // Optional user-assigned name
    val displayName: String? = null,

    @ColumnInfo(name = "device_role")
    val role: DeviceRole,

    @ColumnInfo(name = "pairing_status")
    val status: PairingStatus,

    // Only for TARGET role (bcrypt hash)
    @ColumnInfo(name = "password_hash")
    val passwordHash: String? = null,

    // Salt for password hashing
    @ColumnInfo(name = "password_salt")
    val passwordSalt: String? = null,

    // Only for SOURCE role (plaintext password for decryption)
    @ColumnInfo(name = "active_encryption_key")
    val activeEncryptionKey: String? = null,

    // SHA256(password) for HMAC challenge-response verification
    @ColumnInfo(name = "auth_key")
    val authKey: String? = null,

    @ColumnInfo(name = "max_forward_duration_minutes")
    val maxForwardDurationMinutes: Int = 30,

    @ColumnInfo(name = "failed_attempts")
    val failedAttempts: Int = 0,

    // Timestamp for lockout
    @ColumnInfo(name = "locked_until")
    val lockedUntil: Long? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_activity_at")
    val lastActivityAt: Long = System.currentTimeMillis(),

    // Rate limiting for resend pairing request
    @ColumnInfo(name = "resend_attempt_count")
    val resendAttemptCount: Int = 0,

    @ColumnInfo(name = "last_resend_attempt_at")
    val lastResendAttemptAt: Long? = null,
)

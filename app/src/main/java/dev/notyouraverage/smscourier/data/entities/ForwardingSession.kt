package dev.notyouraverage.smscourier.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "forwarding_sessions",
    foreignKeys = [
        ForeignKey(
            entity = PairedDevice::class,
            parentColumns = ["phoneNumber"],
            childColumns = ["device_phone_number"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("device_phone_number")],
)
data class ForwardingSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "device_phone_number")
    val devicePhoneNumber: String,

    @ColumnInfo(name = "started_at")
    val startedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Int,

    @ColumnInfo(name = "expires_at")
    val expiresAt: Long,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    // "USER", "TIMEOUT", "REMOTE"
    @ColumnInfo(name = "stopped_by")
    val stoppedBy: String? = null,

    @ColumnInfo(name = "messages_forwarded")
    val messagesForwarded: Int = 0,

    // Plain password for encrypting/decrypting forwarded messages
    @ColumnInfo(name = "encryption_key")
    val encryptionKey: String? = null,
)

package dev.notyouraverage.smscourier.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "forwarded_messages",
    foreignKeys = [
        ForeignKey(
            entity = ForwardingSession::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["session_id"]),
        Index(value = ["timestamp"]),
        Index(value = ["destination_number"])
    ]
)
data class ForwardedMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "session_id")
    val sessionId: Long,

    @ColumnInfo(name = "sender_number")
    val senderNumber: String,

    @ColumnInfo(name = "destination_number")
    val destinationNumber: String = "",

    @ColumnInfo(name = "message_content")
    val messageContent: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

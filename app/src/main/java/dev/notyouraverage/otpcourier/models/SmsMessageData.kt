package dev.notyouraverage.otpcourier.models

import android.os.Parcel
import android.os.Parcelable

data class SmsMessageData(
    val sender: String?,
    val rawMessage: String?,
    val command: String?,
    val secretPassword: String?,
    val targetPhoneNumber: String? = null,
    val encryptionKey: String? = null,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(sender)
        parcel.writeString(rawMessage)
        parcel.writeString(command)
        parcel.writeString(secretPassword)
        parcel.writeString(targetPhoneNumber)
        parcel.writeString(encryptionKey)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SmsMessageData> {
        override fun createFromParcel(parcel: Parcel): SmsMessageData {
            return SmsMessageData(parcel)
        }

        override fun newArray(size: Int): Array<SmsMessageData?> {
            return arrayOfNulls(size)
        }
    }
}

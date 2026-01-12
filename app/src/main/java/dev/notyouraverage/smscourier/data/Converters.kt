package dev.notyouraverage.smscourier.data

import androidx.room.TypeConverter
import dev.notyouraverage.smscourier.data.entities.DeviceRole
import dev.notyouraverage.smscourier.data.entities.PairingStatus

class Converters {

    @TypeConverter
    fun fromDeviceRole(role: DeviceRole): String = role.name

    @TypeConverter
    fun toDeviceRole(value: String): DeviceRole = DeviceRole.valueOf(value)

    @TypeConverter
    fun fromPairingStatus(status: PairingStatus): String = status.name

    @TypeConverter
    fun toPairingStatus(value: String): PairingStatus = PairingStatus.valueOf(value)
}

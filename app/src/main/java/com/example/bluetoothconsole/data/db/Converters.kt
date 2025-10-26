package com.example.bluetoothconsole.data.db

import androidx.room.TypeConverter
import com.example.bluetoothconsole.data.WidgetType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromWidgetType(value: WidgetType): String = value.name

    @TypeConverter
    fun toWidgetType(value: String): WidgetType = WidgetType.valueOf(value)
}

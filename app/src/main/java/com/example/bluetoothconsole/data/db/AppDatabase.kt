package com.example.bluetoothconsole.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.bluetoothconsole.data.DashboardEntity
import com.example.bluetoothconsole.data.WidgetEntity
import com.example.bluetoothconsole.data.WidgetType

@Database(
    entities = [DashboardEntity::class, WidgetEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dashboardDao(): DashboardDao
}

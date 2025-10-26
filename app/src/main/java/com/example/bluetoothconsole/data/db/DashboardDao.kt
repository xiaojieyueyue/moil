package com.example.bluetoothconsole.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.bluetoothconsole.data.DashboardEntity
import com.example.bluetoothconsole.data.WidgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DashboardDao {
    @Query("SELECT * FROM dashboards")
    fun observeDashboards(): Flow<List<DashboardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDashboard(dashboard: DashboardEntity): Long

    @Update
    suspend fun updateDashboard(dashboard: DashboardEntity)

    @Delete
    suspend fun deleteDashboard(dashboard: DashboardEntity)

    @Query("SELECT * FROM widgets WHERE dashboardId = :dashboardId")
    fun observeWidgets(dashboardId: Long): Flow<List<WidgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWidget(widget: WidgetEntity): Long

    @Update
    suspend fun updateWidget(widget: WidgetEntity)

    @Query("DELETE FROM widgets WHERE id = :widgetId")
    suspend fun deleteWidgetById(widgetId: Long)
}

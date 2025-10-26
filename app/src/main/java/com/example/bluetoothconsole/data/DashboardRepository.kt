package com.example.bluetoothconsole.data

import com.example.bluetoothconsole.data.db.DashboardDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepository @Inject constructor(
    private val dao: DashboardDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun dashboards(): Flow<List<DashboardEntity>> = dao.observeDashboards()

    fun widgets(dashboardId: Long): Flow<List<DashboardWidget>> =
        dao.observeWidgets(dashboardId).map { entities ->
            entities.map { entity ->
                DashboardWidget(
                    entity.id,
                    entity.dashboardId,
                    entity.type,
                    entity.title,
                    entity.x,
                    entity.y,
                    entity.w,
                    entity.h,
                    decode(entity.configuration, entity.type)
                )
            }
        }

    suspend fun createDashboard(name: String): Long =
        dao.insertDashboard(DashboardEntity(name = name))

    suspend fun updateDashboard(dashboard: DashboardEntity) = dao.updateDashboard(dashboard)

    suspend fun deleteDashboard(dashboard: DashboardEntity) = dao.deleteDashboard(dashboard)

    suspend fun upsertWidget(widget: DashboardWidget) {
        val entity = WidgetEntity(
            id = widget.id,
            dashboardId = widget.dashboardId,
            type = widget.type,
            title = widget.title,
            x = widget.x,
            y = widget.y,
            w = widget.w,
            h = widget.h,
            configuration = json.encodeToString(widget.configuration)
        )
        dao.insertWidget(entity)
    }

    suspend fun deleteWidget(id: Long) {
        dao.deleteWidgetById(id)
    }

    private fun decode(configuration: String, type: WidgetType): WidgetConfiguration {
        return when (type) {
            WidgetType.BUTTON -> json.decodeFromString<WidgetConfiguration.ButtonConfig>(configuration)
            WidgetType.TOGGLE -> json.decodeFromString<WidgetConfiguration.ToggleConfig>(configuration)
            WidgetType.SLIDER -> json.decodeFromString<WidgetConfiguration.SliderConfig>(configuration)
            WidgetType.TEXT_INPUT -> json.decodeFromString<WidgetConfiguration.TextInputConfig>(configuration)
            WidgetType.LABEL -> json.decodeFromString<WidgetConfiguration.LabelConfig>(configuration)
            WidgetType.GAUGE -> json.decodeFromString<WidgetConfiguration.GaugeConfig>(configuration)
            WidgetType.JOYSTICK -> json.decodeFromString<WidgetConfiguration.JoystickConfig>(configuration)
        }
    }
}

data class DashboardWidget(
    val id: Long,
    val dashboardId: Long,
    val type: WidgetType,
    val title: String,
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int,
    val configuration: WidgetConfiguration
)

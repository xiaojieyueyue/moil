package com.example.bluetoothconsole.data.dashboard

import com.example.bluetoothconsole.model.DashboardProject
import com.example.bluetoothconsole.model.WidgetConfig
import kotlinx.coroutines.flow.Flow

class DashboardRepository(private val storage: DashboardStorage) {

    val projects: Flow<List<DashboardProject>> = storage.dashboards

    suspend fun upsertProject(project: DashboardProject) {
        storage.update { current ->
            val filtered = current.filterNot { it.id == project.id }
            filtered + project
        }
    }

    suspend fun deleteProject(projectId: String) {
        storage.update { current -> current.filterNot { it.id == projectId } }
    }

    suspend fun addWidget(projectId: String, widget: WidgetConfig) {
        storage.update { current ->
            current.map { project ->
                if (project.id == projectId) {
                    project.copy(widgets = project.widgets + widget)
                } else project
            }
        }
    }

    suspend fun updateWidget(projectId: String, widget: WidgetConfig) {
        storage.update { current ->
            current.map { project ->
                if (project.id == projectId) {
                    project.copy(widgets = project.widgets.map { if (it.id == widget.id) widget else it })
                } else project
            }
        }
    }

    suspend fun removeWidget(projectId: String, widgetId: String) {
        storage.update { current ->
            current.map { project ->
                if (project.id == projectId) {
                    project.copy(widgets = project.widgets.filterNot { it.id == widgetId })
                } else project
            }
        }
    }
}

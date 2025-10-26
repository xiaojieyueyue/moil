package com.example.bluetoothconsole.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetoothconsole.data.DashboardEntity
import com.example.bluetoothconsole.data.DashboardRepository
import com.example.bluetoothconsole.data.DashboardWidget
import com.example.bluetoothconsole.data.WidgetConfiguration
import com.example.bluetoothconsole.data.WidgetType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardListViewModel @Inject constructor(
    private val repository: DashboardRepository
) : ViewModel() {
    private val _selectedDashboardId = MutableStateFlow<Long?>(null)
    val selectedDashboardId: StateFlow<Long?> = _selectedDashboardId.asStateFlow()

    val dashboards: StateFlow<List<DashboardEntity>> = repository.dashboards().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val widgets: StateFlow<List<DashboardWidget>> = _selectedDashboardId
        .flatMapLatest { selectedId ->
            val id = selectedId ?: dashboards.value.firstOrNull()?.id
            if (id != null) {
                repository.widgets(id)
            } else {
                kotlinx.coroutines.flow.flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            dashboards.collect { list ->
                if (list.isNotEmpty() && _selectedDashboardId.value == null) {
                    _selectedDashboardId.value = list.first().id
                }
            }
        }
    }

    fun selectDashboard(id: Long) {
        _selectedDashboardId.value = id
    }

    fun createDashboard(name: String) {
        viewModelScope.launch {
            val id = repository.createDashboard(name)
            _selectedDashboardId.value = id
        }
    }

    fun addWidget(type: WidgetType) {
        val dashboardId = _selectedDashboardId.value ?: dashboards.value.firstOrNull()?.id ?: return
        val config = when (type) {
            WidgetType.BUTTON -> WidgetConfiguration.ButtonConfig(
                onPress = WidgetConfigurationDefaults.text("BUTTON_PRESS"),
                onRelease = WidgetConfigurationDefaults.text("BUTTON_RELEASE")
            )
            WidgetType.TOGGLE -> WidgetConfiguration.ToggleConfig(
                onCommand = WidgetConfigurationDefaults.text("ON"),
                offCommand = WidgetConfigurationDefaults.text("OFF")
            )
            WidgetType.SLIDER -> WidgetConfiguration.SliderConfig(0, 100, "PWM:%d")
            WidgetType.TEXT_INPUT -> WidgetConfiguration.TextInputConfig()
            WidgetType.LABEL -> WidgetConfiguration.LabelConfig()
            WidgetType.GAUGE -> WidgetConfiguration.GaugeConfig(0f, 100f, "TEMP:")
            WidgetType.JOYSTICK -> WidgetConfiguration.JoystickConfig()
        }
        val widget = DashboardWidget(
            id = 0,
            dashboardId = dashboardId,
            type = type,
            title = type.name,
            x = 0,
            y = 0,
            w = 2,
            h = 2,
            configuration = config
        )
        viewModelScope.launch {
            repository.upsertWidget(widget)
        }
    }

    fun upsertWidget(widget: DashboardWidget) {
        viewModelScope.launch {
            repository.upsertWidget(widget)
        }
    }

    fun deleteWidget(id: Long) {
        viewModelScope.launch {
            repository.deleteWidget(id)
        }
    }
}

object WidgetConfigurationDefaults {
    fun text(value: String) = com.example.bluetoothconsole.data.CommandConfig.Text(text = value)
    fun hex(vararg bytes: String) = com.example.bluetoothconsole.data.CommandConfig.Hex(bytes = bytes.toList())
}

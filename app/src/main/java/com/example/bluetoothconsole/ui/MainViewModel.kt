package com.example.bluetoothconsole.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetoothconsole.data.bluetooth.BluetoothConnectionState
import com.example.bluetoothconsole.data.bluetooth.BluetoothController
import com.example.bluetoothconsole.data.bluetooth.BluetoothDeviceInfo
import com.example.bluetoothconsole.data.bluetooth.BluetoothScanMode
import com.example.bluetoothconsole.data.dashboard.DashboardRepository
import com.example.bluetoothconsole.model.CommandPayload
import com.example.bluetoothconsole.model.DashboardProject
import com.example.bluetoothconsole.model.TerminalMessage
import com.example.bluetoothconsole.model.WidgetConfig
import com.example.bluetoothconsole.util.CommandEncoder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val bluetoothController: BluetoothController,
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val _selectedProjectId = MutableStateFlow<String?>(null)
    private val _terminalMessages = MutableStateFlow<List<TerminalMessage>>(emptyList())
    private val _isHexTerminal = MutableStateFlow(false)

    val terminalMessages: StateFlow<List<TerminalMessage>> = _terminalMessages.asStateFlow()
    val isHexTerminal: StateFlow<Boolean> = _isHexTerminal.asStateFlow()

    val projects: StateFlow<List<DashboardProject>> = dashboardRepository.projects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedProject = combine(projects, _selectedProjectId) { projects, id ->
        id?.let { currentId -> projects.firstOrNull { it.id == currentId } } ?: projects.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val connectionState: StateFlow<BluetoothConnectionState> = bluetoothController.connectionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BluetoothConnectionState.Disconnected)

    val scannedDevices: StateFlow<List<BluetoothDeviceInfo>> = bluetoothController.scannedDevices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pairedDevices: StateFlow<List<BluetoothDeviceInfo>> = bluetoothController.pairedDevices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            bluetoothController.incomingData.collect { bytes ->
                _terminalMessages.value = _terminalMessages.value + TerminalMessage(bytes, isOutgoing = false)
            }
        }
    }

    fun toggleTerminalMode() {
        _isHexTerminal.value = !_isHexTerminal.value
    }

    fun selectProject(projectId: String) {
        _selectedProjectId.value = projectId
    }

    fun createProject(name: String) {
        viewModelScope.launch {
            val project = DashboardProject(name = name)
            dashboardRepository.upsertProject(project)
            _selectedProjectId.value = project.id
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            dashboardRepository.deleteProject(projectId)
            if (_selectedProjectId.value == projectId) {
                _selectedProjectId.value = null
            }
        }
    }

    fun addWidget(projectId: String, widget: WidgetConfig) {
        viewModelScope.launch {
            dashboardRepository.addWidget(projectId, widget)
        }
    }

    fun updateWidget(projectId: String, widget: WidgetConfig) {
        viewModelScope.launch { dashboardRepository.updateWidget(projectId, widget) }
    }

    fun removeWidget(projectId: String, widgetId: String) {
        viewModelScope.launch { dashboardRepository.removeWidget(projectId, widgetId) }
    }

    fun startScan(mode: BluetoothScanMode) {
        viewModelScope.launch { bluetoothController.startScan(mode) }
    }

    fun stopScan() {
        bluetoothController.stopScan()
    }

    fun connect(deviceInfo: BluetoothDeviceInfo) {
        viewModelScope.launch { bluetoothController.connect(deviceInfo) }
    }

    fun disconnect() {
        bluetoothController.disconnect()
    }

    fun sendTerminalCommand(text: String, asHex: Boolean) {
        viewModelScope.launch {
            val payload = try {
                if (asHex) {
                    CommandEncoder.hexToBytes(text)
                } else {
                    text.toByteArray()
                }
            } catch (ex: IllegalArgumentException) {
                return@launch
            }
            bluetoothController.sendBytes(payload)
            _terminalMessages.value =
                _terminalMessages.value + TerminalMessage(payload, isOutgoing = true)
        }
    }

    fun sendWidgetCommand(widget: WidgetConfig, command: CommandPayload) {
        viewModelScope.launch {
            bluetoothController.sendPayload(command)
            _terminalMessages.value =
                _terminalMessages.value + TerminalMessage(commandToBytes(command), isOutgoing = true)
        }
    }

    fun sendSliderValue(widget: WidgetConfig.Slider, value: Int) {
        viewModelScope.launch {
            val bytes = CommandEncoder.valueToPayload(value, widget.sendAsHex)
            bluetoothController.sendBytes(bytes)
            _terminalMessages.value =
                _terminalMessages.value + TerminalMessage(bytes, isOutgoing = true)
        }
    }

    fun sendJoystickValue(widget: WidgetConfig.Joystick, x: Int, y: Int) {
        viewModelScope.launch {
            val formatted = if (widget.sendAsHex) {
                byteArrayOf(x.toByte(), y.toByte())
            } else {
                "${'$'}x,${'$'}y".toByteArray()
            }
            bluetoothController.sendBytes(formatted)
            _terminalMessages.value =
                _terminalMessages.value + TerminalMessage(formatted, isOutgoing = true)
        }
    }

    fun appendManualMessage(text: String) {
        val bytes = text.toByteArray()
        _terminalMessages.value = _terminalMessages.value + TerminalMessage(bytes, isOutgoing = true)
    }

    private fun commandToBytes(command: CommandPayload): ByteArray = when (command) {
        is CommandPayload.StringPayload -> command.value.toByteArray()
        is CommandPayload.HexPayload -> CommandEncoder.hexToBytes(command.value)
    }
}

class MainViewModelFactory(
    private val bluetoothController: BluetoothController,
    private val dashboardRepository: DashboardRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(bluetoothController, dashboardRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.example.bluetoothconsole

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.bluetoothconsole.ui.MainViewModel
import com.example.bluetoothconsole.ui.MainViewModelFactory
import com.example.bluetoothconsole.ui.navigation.MainScaffold
import com.example.bluetoothconsole.ui.theme.BluetoothConsoleTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as BluetoothConsoleApplication
        MainViewModelFactory(
            bluetoothController = app.appContainer.bluetoothController,
            dashboardRepository = app.appContainer.dashboardRepository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BluetoothConsoleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreenHost(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainScreenHost(viewModel: MainViewModel) {
    val connectionState by viewModel.connectionState.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val terminalMessages by viewModel.terminalMessages.collectAsState()
    val isHexTerminal by viewModel.isHexTerminal.collectAsState()
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()

    MainScaffold(
        projects = projects,
        selectedProject = selectedProject,
        connectionState = connectionState,
        terminalMessages = terminalMessages,
        isTerminalHexMode = isHexTerminal,
        scannedDevices = scannedDevices,
        pairedDevices = pairedDevices,
        onToggleTerminalMode = viewModel::toggleTerminalMode,
        onCreateProject = viewModel::createProject,
        onSelectProject = viewModel::selectProject,
        onDeleteProject = viewModel::deleteProject,
        onStartScan = viewModel::startScan,
        onStopScan = viewModel::stopScan,
        onConnect = viewModel::connect,
        onDisconnect = viewModel::disconnect,
        onSendTerminalCommand = viewModel::sendTerminalCommand,
        onSendWidgetCommand = viewModel::sendWidgetCommand,
        onSendSliderValue = viewModel::sendSliderValue,
        onSendJoystickValue = viewModel::sendJoystickValue,
        onAddWidget = viewModel::addWidget,
        onUpdateWidget = viewModel::updateWidget,
        onRemoveWidget = viewModel::removeWidget
    )
}

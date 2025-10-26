package com.example.bluetoothconsole.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.bluetoothconsole.data.bluetooth.BluetoothConnectionState
import com.example.bluetoothconsole.data.bluetooth.BluetoothDeviceInfo
import com.example.bluetoothconsole.data.bluetooth.BluetoothScanMode
import com.example.bluetoothconsole.model.CommandPayload
import com.example.bluetoothconsole.model.DashboardProject
import com.example.bluetoothconsole.model.TerminalMessage
import com.example.bluetoothconsole.model.WidgetConfig
import com.example.bluetoothconsole.ui.screens.dashboard.DashboardScreen
import com.example.bluetoothconsole.ui.screens.devices.DeviceManagerScreen
import com.example.bluetoothconsole.ui.screens.terminal.TerminalScreen

sealed class MainDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Dashboard : MainDestination("dashboard", "仪表盘", Icons.Default.Dashboard)
    data object Terminal : MainDestination("terminal", "终端", Icons.Default.Terminal)
    data object Devices : MainDestination("devices", "设备", Icons.Default.Devices)
}

private val destinations = listOf(
    MainDestination.Dashboard,
    MainDestination.Terminal,
    MainDestination.Devices
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    projects: List<DashboardProject>,
    selectedProject: DashboardProject?,
    connectionState: BluetoothConnectionState,
    terminalMessages: List<TerminalMessage>,
    isTerminalHexMode: Boolean,
    scannedDevices: List<BluetoothDeviceInfo>,
    pairedDevices: List<BluetoothDeviceInfo>,
    onToggleTerminalMode: () -> Unit,
    onCreateProject: (String) -> Unit,
    onSelectProject: (String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onStartScan: (BluetoothScanMode) -> Unit,
    onStopScan: () -> Unit,
    onConnect: (BluetoothDeviceInfo) -> Unit,
    onDisconnect: () -> Unit,
    onSendTerminalCommand: (String, Boolean) -> Unit,
    onSendWidgetCommand: (WidgetConfig, CommandPayload) -> Unit,
    onSendSliderValue: (WidgetConfig.Slider, Int) -> Unit,
    onSendJoystickValue: (WidgetConfig.Joystick, Int, Int) -> Unit,
    onAddWidget: (String, WidgetConfig) -> Unit,
    onUpdateWidget: (String, WidgetConfig) -> Unit,
    onRemoveWidget: (String, String) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: MainDestination.Dashboard.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { androidx.compose.material3.Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = MainDestination.Dashboard.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(MainDestination.Dashboard.route) {
                DashboardScreen(
                    projects = projects,
                    selectedProject = selectedProject,
                    terminalMessages = terminalMessages,
                    onCreateProject = onCreateProject,
                    onSelectProject = onSelectProject,
                    onDeleteProject = onDeleteProject,
                    onAddWidget = onAddWidget,
                    onUpdateWidget = onUpdateWidget,
                    onRemoveWidget = onRemoveWidget,
                    onSendWidgetCommand = onSendWidgetCommand,
                    onSendSliderValue = onSendSliderValue,
                    onSendJoystickValue = onSendJoystickValue
                )
            }
            composable(MainDestination.Terminal.route) {
                TerminalScreen(
                    messages = terminalMessages,
                    isHexMode = isTerminalHexMode,
                    onToggleHexMode = onToggleTerminalMode,
                    onSendCommand = onSendTerminalCommand
                )
            }
            composable(MainDestination.Devices.route) {
                DeviceManagerScreen(
                    connectionState = connectionState,
                    scannedDevices = scannedDevices,
                    pairedDevices = pairedDevices,
                    onStartScan = onStartScan,
                    onStopScan = onStopScan,
                    onConnect = onConnect,
                    onDisconnect = onDisconnect
                )
            }
        }
    }
}

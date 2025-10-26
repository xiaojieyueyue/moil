package com.example.bluetoothconsole.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeviceBluetooth
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.bluetoothconsole.ui.devices.DeviceListScreen
import com.example.bluetoothconsole.ui.dashboard.DashboardListScreen
import com.example.bluetoothconsole.ui.terminal.TerminalScreen

@Composable
fun BluetoothConsoleApp() {
    AppTheme {
        val navController = rememberNavController()
        val items = listOf(AppDestination.Devices, AppDestination.Dashboards, AppDestination.Terminal)
        Scaffold(
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    items.forEach { destination ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(imageVector = destination.icon, contentDescription = null)
                            },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = AppDestination.Devices.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(AppDestination.Devices.route) {
                    DeviceListScreen(onNavigateToDashboard = {
                        navController.navigate(AppDestination.Dashboards.route)
                    })
                }
                composable(AppDestination.Dashboards.route) {
                    DashboardListScreen()
                }
                composable(AppDestination.Terminal.route) {
                    TerminalScreen()
                }
            }
        }
    }
}

enum class AppDestination(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String) {
    Devices("devices", Icons.Default.DeviceBluetooth, "设备"),
    Dashboards("dashboards", Icons.Default.Dashboard, "仪表盘"),
    Terminal("terminal", Icons.Default.Terminal, "终端")
}

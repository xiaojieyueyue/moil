package com.example.bluetoothconsole

import android.app.Application
import com.example.bluetoothconsole.data.bluetooth.AndroidBluetoothController
import com.example.bluetoothconsole.data.bluetooth.BluetoothController
import com.example.bluetoothconsole.data.dashboard.DashboardRepository
import com.example.bluetoothconsole.data.dashboard.DashboardStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class BluetoothConsoleApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val storage = DashboardStorage(this, applicationScope)
        val controller: BluetoothController = AndroidBluetoothController(this)
        val repository = DashboardRepository(storage)
        appContainer = AppContainer(controller, repository)
    }
}

data class AppContainer(
    val bluetoothController: BluetoothController,
    val dashboardRepository: DashboardRepository
)

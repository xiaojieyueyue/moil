package com.example.bluetoothconsole.data.bluetooth

import com.example.bluetoothconsole.model.CommandPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

sealed interface BluetoothConnectionState {
    data object Disconnected : BluetoothConnectionState
    data object Connecting : BluetoothConnectionState
    data class Connected(val device: BluetoothDeviceInfo) : BluetoothConnectionState
    data class Error(val message: String?) : BluetoothConnectionState
}

data class BluetoothDeviceInfo(
    val name: String?,
    val address: String,
    val type: BluetoothDeviceType,
    val isPaired: Boolean = false
)

enum class BluetoothDeviceType { BLE, CLASSIC }

enum class BluetoothScanMode { BLE, CLASSIC, ALL }

interface BluetoothController {
    val scannedDevices: StateFlow<List<BluetoothDeviceInfo>>
    val pairedDevices: StateFlow<List<BluetoothDeviceInfo>>
    val connectionState: StateFlow<BluetoothConnectionState>
    val incomingData: Flow<ByteArray>

    suspend fun startScan(mode: BluetoothScanMode)
    fun stopScan()
    suspend fun connect(device: BluetoothDeviceInfo)
    fun disconnect()
    suspend fun sendPayload(payload: CommandPayload)
    suspend fun sendBytes(bytes: ByteArray)
}

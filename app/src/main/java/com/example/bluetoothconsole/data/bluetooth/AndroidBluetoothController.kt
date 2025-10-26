package com.example.bluetoothconsole.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.bluetoothconsole.model.CommandPayload
import com.example.bluetoothconsole.util.CommandEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
class AndroidBluetoothController(context: Context) : BluetoothController {

    private val appContext: Context = context.applicationContext
    private val bluetoothManager: BluetoothManager? =
        appContext.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val appScope = CoroutineScope(Dispatchers.IO)

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceInfo>> = _scannedDevices

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDeviceInfo>> = _pairedDevices

    private val _connectionState =
        MutableStateFlow<BluetoothConnectionState>(BluetoothConnectionState.Disconnected)
    override val connectionState: StateFlow<BluetoothConnectionState> = _connectionState

    private val _incomingData = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    override val incomingData: Flow<ByteArray> = _incomingData

    private val mutex = Mutex()

    private var gatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var socket: BluetoothSocket? = null

    private var currentScanMode: BluetoothScanMode? = null

    private val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { addScannedDevice(it, BluetoothDeviceType.BLE) }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { addScannedDevice(it.device, BluetoothDeviceType.BLE) }
        }

        override fun onScanFailed(errorCode: Int) {
            _connectionState.value =
                BluetoothConnectionState.Error("BLE scan failed: $errorCode")
        }
    }

    private val classicReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let { addScannedDevice(it, BluetoothDeviceType.CLASSIC) }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    if (currentScanMode == BluetoothScanMode.CLASSIC) {
                        currentScanMode = null
                    }
                }
            }
        }
    }

    init {
        updatePairedDevices()
        registerClassicReceiver()
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(appContext, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun addScannedDevice(device: BluetoothDevice, type: BluetoothDeviceType) {
        val info = BluetoothDeviceInfo(
            name = device.name,
            address = device.address,
            type = type,
            isPaired = device.bondState == BluetoothDevice.BOND_BONDED
        )
        _scannedDevices.value = _scannedDevices.value
            .filterNot { it.address == info.address }
            .plus(info)
    }

    private fun registerClassicReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        appContext.registerReceiver(classicReceiver, filter)
    }

    private fun updatePairedDevices() {
        val bonded = adapter?.bondedDevices?.map {
            BluetoothDeviceInfo(it.name, it.address, resolveType(it), true)
        } ?: emptyList()
        _pairedDevices.value = bonded
    }

    private fun resolveType(device: BluetoothDevice): BluetoothDeviceType {
        return when (device.type) {
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> BluetoothDeviceType.CLASSIC
            BluetoothDevice.DEVICE_TYPE_DUAL -> BluetoothDeviceType.CLASSIC
            BluetoothDevice.DEVICE_TYPE_LE -> BluetoothDeviceType.BLE
            else -> BluetoothDeviceType.CLASSIC
        }
    }

    override suspend fun startScan(mode: BluetoothScanMode) {
        if (adapter == null || !adapter.isEnabled) {
            _connectionState.value =
                BluetoothConnectionState.Error("Bluetooth adapter is not enabled")
            return
        }
        mutex.withLock {
            currentScanMode = mode
            _scannedDevices.value = emptyList()
            if (mode == BluetoothScanMode.BLE || mode == BluetoothScanMode.ALL) {
                if (hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
                    adapter.bluetoothLeScanner?.startScan(scanCallback)
                } else {
                    _connectionState.value =
                        BluetoothConnectionState.Error("Missing BLUETOOTH_SCAN permission")
                }
            }
            if (mode == BluetoothScanMode.CLASSIC || mode == BluetoothScanMode.ALL) {
                if (hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
                    if (adapter.isDiscovering) {
                        adapter.cancelDiscovery()
                    }
                    adapter.startDiscovery()
                } else {
                    _connectionState.value =
                        BluetoothConnectionState.Error("Missing BLUETOOTH_SCAN permission")
                }
            }
        }
    }

    override fun stopScan() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) return
        adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        if (adapter?.isDiscovering == true) {
            adapter.cancelDiscovery()
        }
        currentScanMode = null
    }

    override suspend fun connect(device: BluetoothDeviceInfo) {
        mutex.withLock {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                _connectionState.value =
                    BluetoothConnectionState.Error("Missing BLUETOOTH_CONNECT permission")
                return
            }
            disconnectInternal()
            val bluetoothDevice = adapter?.getRemoteDevice(device.address)
            if (bluetoothDevice == null) {
                _connectionState.value =
                    BluetoothConnectionState.Error("Device not found")
                return
            }
            _connectionState.value = BluetoothConnectionState.Connecting
            when (device.type) {
                BluetoothDeviceType.BLE -> connectGatt(bluetoothDevice)
                BluetoothDeviceType.CLASSIC -> connectClassic(bluetoothDevice)
            }
        }
    }

    private fun connectGatt(device: BluetoothDevice) {
        gatt = device.connectGatt(appContext, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    _connectionState.value =
                        BluetoothConnectionState.Connected(device.toInfo(BluetoothDeviceType.BLE))
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    _connectionState.value = BluetoothConnectionState.Disconnected
                    cleanupGatt()
                } else if (status != BluetoothGatt.GATT_SUCCESS) {
                    _connectionState.value = BluetoothConnectionState.Error("Gatt error: $status")
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val characteristic = gatt.services
                        .flatMap { it.characteristics }
                        .firstOrNull {
                            it.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0 ||
                                it.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0
                        }
                    writeCharacteristic = characteristic
                    characteristic?.let { enableNotifications(gatt, it) }
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                _incomingData.tryEmit(value)
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    _incomingData.tryEmit(value)
                }
            }
        })
    }

    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
            gatt.setCharacteristicNotification(characteristic, true)
            characteristic.descriptors.forEach { descriptor ->
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
        }
    }

    private fun connectClassic(device: BluetoothDevice) {
        appScope.launch {
            try {
                val socket = device.createRfcommSocketToServiceRecord(sppUuid)
                adapter?.cancelDiscovery()
                socket.connect()
                this@AndroidBluetoothController.socket = socket
                _connectionState.value =
                    BluetoothConnectionState.Connected(device.toInfo(BluetoothDeviceType.CLASSIC))
                listenClassic(socket)
            } catch (e: IOException) {
                _connectionState.value = BluetoothConnectionState.Error(e.message)
                cleanupClassic()
            }
        }
    }

    private fun listenClassic(socket: BluetoothSocket) {
        appScope.launch {
            try {
                val buffer = ByteArray(1024)
                val input = socket.inputStream
                while (true) {
                    val bytes = input.read(buffer)
                    if (bytes <= 0) break
                    _incomingData.emit(buffer.copyOf(bytes))
                }
            } catch (e: IOException) {
                _connectionState.value = BluetoothConnectionState.Error(e.message)
            } finally {
                disconnect()
            }
        }
    }

    override fun disconnect() {
        appScope.launch {
            mutex.withLock { disconnectInternal() }
        }
    }

    private fun disconnectInternal() {
        cleanupGatt()
        cleanupClassic()
        _connectionState.value = BluetoothConnectionState.Disconnected
    }

    private fun cleanupGatt() {
        runCatching { gatt?.close() }
        gatt = null
        writeCharacteristic = null
    }

    private fun cleanupClassic() {
        runCatching { socket?.close() }
        socket = null
    }

    override suspend fun sendPayload(payload: CommandPayload) {
        val bytes = when (payload) {
            is CommandPayload.StringPayload -> payload.value.toByteArray()
            is CommandPayload.HexPayload -> CommandEncoder.hexToBytes(payload.value)
        }
        sendBytes(bytes)
    }

    override suspend fun sendBytes(bytes: ByteArray) {
        mutex.withLock {
            when (val state = connectionState.value) {
                is BluetoothConnectionState.Connected -> {
                    when (state.device.type) {
                        BluetoothDeviceType.BLE -> sendGatt(bytes)
                        BluetoothDeviceType.CLASSIC -> sendClassic(bytes)
                    }
                }
                else -> _connectionState.value =
                    BluetoothConnectionState.Error("No device connected")
            }
        }
    }

    private fun sendGatt(bytes: ByteArray) {
        val characteristic = writeCharacteristic
        val gatt = gatt
        if (characteristic != null && gatt != null) {
            characteristic.value = bytes
            gatt.writeCharacteristic(characteristic)
        } else {
            _connectionState.value =
                BluetoothConnectionState.Error("Writable characteristic not found")
        }
    }

    private fun sendClassic(bytes: ByteArray) {
        try {
            socket?.outputStream?.write(bytes)
            socket?.outputStream?.flush()
        } catch (e: IOException) {
            _connectionState.value = BluetoothConnectionState.Error(e.message)
        }
    }

    private fun BluetoothDevice.toInfo(type: BluetoothDeviceType) = BluetoothDeviceInfo(
        name = name,
        address = address,
        type = type,
        isPaired = bondState == BluetoothDevice.BOND_BONDED
    )

    fun clear() {
        disconnect()
        runCatching { appContext.unregisterReceiver(classicReceiver) }
    }

}

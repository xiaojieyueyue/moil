package com.example.bluetoothconsole.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

sealed interface BluetoothDeviceType {
    val address: String
    val name: String?

    data class Classic(
        override val address: String,
        override val name: String?
    ) : BluetoothDeviceType

    data class LowEnergy(
        override val address: String,
        override val name: String?
    ) : BluetoothDeviceType
}

data class BluetoothMessage(
    val content: ByteArray,
    val timestamp: Long = System.currentTimeMillis()
)

interface BluetoothController {
    val scannedDevices: MutableStateFlow<List<BluetoothDeviceType>>
    val connectedDevice: MutableStateFlow<BluetoothDeviceType?>
    val incomingMessages: MutableSharedFlow<BluetoothMessage>

    fun hasScanPermission(): Boolean
    fun startScan()
    fun stopScan()
    fun connect(device: BluetoothDeviceType)
    fun disconnect()
    fun sendBytes(payload: ByteArray)
}

val LocalBluetoothController = staticCompositionLocalOf<BluetoothController> {
    error("BluetoothController not provided")
}

@SuppressLint("MissingPermission")
class AndroidBluetoothController(private val context: Context) : BluetoothController {
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private var discoveryRegistered = false

    override val scannedDevices = MutableStateFlow<List<BluetoothDeviceType>>(emptyList())
    override val connectedDevice = MutableStateFlow<BluetoothDeviceType?>(null)
    override val incomingMessages = MutableSharedFlow<BluetoothMessage>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val scope = CoroutineScope(Dispatchers.IO)

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val device = intent?.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
            mergeDevice(BluetoothDeviceType.Classic(device.address, device.name))
        }
    }

    private var gatt: BluetoothGatt? = null
    private var bluetoothSocket: BluetoothSocket? = null

    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val type = BluetoothDeviceType.LowEnergy(device.address, device.name)
            mergeDevice(type)
        }
    }

    private fun mergeDevice(device: BluetoothDeviceType) {
        val current = scannedDevices.value.toMutableList()
        if (current.none { it.address == device.address }) {
            current += device
            scannedDevices.value = current
        }
    }

    override fun hasScanPermission(): Boolean {
        val scanPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_SCAN
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
        return ContextCompat.checkSelfPermission(context, scanPermission) ==
            PackageManager.PERMISSION_GRANTED
    }

    override fun startScan() {
        if (!hasScanPermission()) return
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return
        scannedDevices.value = emptyList()
        bluetoothAdapter.bondedDevices?.forEach {
            mergeDevice(BluetoothDeviceType.Classic(it.address, it.name))
        }
        if (!discoveryRegistered) {
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(discoveryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(discoveryReceiver, filter)
            }
            discoveryRegistered = true
        }
        bluetoothAdapter.startDiscovery()
        bluetoothAdapter.bluetoothLeScanner?.startScan(leScanCallback)
    }

    override fun stopScan() {
        bluetoothAdapter?.cancelDiscovery()
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(leScanCallback)
        if (discoveryRegistered) {
            context.unregisterReceiver(discoveryReceiver)
            discoveryRegistered = false
        }
    }

    override fun connect(device: BluetoothDeviceType) {
        when (device) {
            is BluetoothDeviceType.Classic -> connectClassic(device)
            is BluetoothDeviceType.LowEnergy -> connectBle(device)
        }
    }

    private fun connectClassic(device: BluetoothDeviceType.Classic) {
        stopScan()
        val btDevice = bluetoothAdapter?.getRemoteDevice(device.address) ?: return
        scope.launch {
            try {
                val socket =
                    btDevice.createRfcommSocketToServiceRecord(SPP_UUID)
                bluetoothSocket = socket
                bluetoothAdapter?.cancelDiscovery()
                socket.connect()
                connectedDevice.value = device
                listenClassic(socket)
            } catch (ex: IOException) {
                connectedDevice.value = null
            }
        }
    }

    private fun listenClassic(socket: BluetoothSocket) {
        scope.launch {
            val buffer = ByteArray(1024)
            try {
                while (true) {
                    val count = socket.inputStream.read(buffer)
                    if (count == -1) break
                    val data = buffer.copyOf(count)
                    incomingMessages.emit(BluetoothMessage(data))
                }
            } catch (ex: IOException) {
                // ignore
            } finally {
                disconnect()
            }
        }
    }

    private fun connectBle(device: BluetoothDeviceType.LowEnergy) {
        stopScan()
        val btDevice = bluetoothAdapter?.getRemoteDevice(device.address) ?: return
        gatt = btDevice.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    connectedDevice.value = device
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    connectedDevice.value = null
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) return
                gatt.services.forEach { service ->
                    service.characteristics.forEach { characteristic ->
                        val props = characteristic.properties
                        if (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                            gatt.setCharacteristicNotification(characteristic, true)
                            characteristic.descriptors?.forEach { descriptor ->
                                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                gatt.writeDescriptor(descriptor)
                            }
                        }
                    }
                }
            }
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                scope.launch {
                    incomingMessages.emit(BluetoothMessage(characteristic.value))
                }
            }
        })
    }

    override fun disconnect() {
        bluetoothSocket?.let {
            try {
                it.close()
            } catch (_: IOException) {
            }
            bluetoothSocket = null
        }
        gatt?.close()
        gatt = null
        connectedDevice.value = null
    }

    override fun sendBytes(payload: ByteArray) {
        bluetoothSocket?.let {
            scope.launch {
                try {
                    it.outputStream.write(payload)
                    it.outputStream.flush()
                } catch (_: IOException) {
                }
            }
            return
        }
        val characteristic = findWritableCharacteristic()
        if (characteristic != null) {
            characteristic.value = payload
            gatt?.writeCharacteristic(characteristic)
        }
    }

    private fun findWritableCharacteristic(): BluetoothGattCharacteristic? {
        val services = gatt?.services ?: return null
        services.forEach { service ->
            service.characteristics.forEach { characteristic ->
                val properties = characteristic.properties
                val supportsWrite =
                    properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
                if (supportsWrite) {
                    return characteristic
                }
            }
        }
        return null
    }

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}

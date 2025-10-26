package com.example.bluetoothconsole.ui.devices

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bluetoothconsole.bluetooth.BluetoothController
import com.example.bluetoothconsole.bluetooth.BluetoothDeviceType
import com.example.bluetoothconsole.bluetooth.LocalBluetoothController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DeviceListScreen(
    onNavigateToDashboard: () -> Unit,
    viewModel: DeviceListViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    bluetoothController: BluetoothController = LocalBluetoothController.current
) {
    val devices by bluetoothController.scannedDevices.collectAsState()
    val connected by bluetoothController.connectedDevice.collectAsState()

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val permissionState = rememberMultiplePermissionsState(permissions)

    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            bluetoothController.startScan()
        } else {
            permissionState.launchMultiplePermissionRequest()
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text(text = "蓝牙设备") })
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = { bluetoothController.startScan() }) {
                Text(text = "扫描")
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(devices) { device ->
                    DeviceItem(
                        device = device,
                        isConnected = connected?.address == device.address,
                        onConnect = { bluetoothController.connect(device) }
                    )
                }
            }
            connected?.let {
                Button(onClick = onNavigateToDashboard) {
                    Text(text = "打开仪表盘")
                }
            }
        }
    }
}

@Composable
private fun DeviceItem(
    device: BluetoothDeviceType,
    isConnected: Boolean,
    onConnect: () -> Unit
) {
    Card(onClick = onConnect) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = device.name ?: "未知设备", style = MaterialTheme.typography.titleMedium)
            Text(text = device.address, style = MaterialTheme.typography.bodySmall)
            Text(text = if (isConnected) "已连接" else "未连接", color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

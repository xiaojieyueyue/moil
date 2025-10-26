package com.example.bluetoothconsole.ui.screens.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bluetoothconsole.data.bluetooth.BluetoothConnectionState
import com.example.bluetoothconsole.data.bluetooth.BluetoothDeviceInfo
import com.example.bluetoothconsole.data.bluetooth.BluetoothDeviceType
import com.example.bluetoothconsole.data.bluetooth.BluetoothScanMode

@Composable
fun DeviceManagerScreen(
    connectionState: BluetoothConnectionState,
    scannedDevices: List<BluetoothDeviceInfo>,
    pairedDevices: List<BluetoothDeviceInfo>,
    onStartScan: (BluetoothScanMode) -> Unit,
    onStopScan: () -> Unit,
    onConnect: (BluetoothDeviceInfo) -> Unit,
    onDisconnect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ConnectionStatusSection(connectionState, onDisconnect)
        ScanControls(onStartScan, onStopScan)
        DeviceLists(scannedDevices = scannedDevices, pairedDevices = pairedDevices, onConnect = onConnect)
    }
}

@Composable
private fun ConnectionStatusSection(
    connectionState: BluetoothConnectionState,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("连接状态", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = when (connectionState) {
                        BluetoothConnectionState.Disconnected -> "未连接"
                        BluetoothConnectionState.Connecting -> "正在连接..."
                        is BluetoothConnectionState.Connected -> "已连接 ${connectionState.device.name ?: connectionState.device.address}"
                        is BluetoothConnectionState.Error -> "错误: ${connectionState.message ?: "未知"}"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (connectionState is BluetoothConnectionState.Connected) {
                OutlinedButton(onClick = onDisconnect) { Text("断开") }
            }
        }
    }
}

@Composable
private fun ScanControls(
    onStartScan: (BluetoothScanMode) -> Unit,
    onStopScan: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("扫描设备", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onStartScan(BluetoothScanMode.ALL) }) { Text("全部") }
                OutlinedButton(onClick = { onStartScan(BluetoothScanMode.BLE) }) { Text("BLE") }
                OutlinedButton(onClick = { onStartScan(BluetoothScanMode.CLASSIC) }) { Text("经典") }
                OutlinedButton(onClick = onStopScan) { Text("停止") }
            }
        }
    }
}

@Composable
private fun DeviceLists(
    scannedDevices: List<BluetoothDeviceInfo>,
    pairedDevices: List<BluetoothDeviceInfo>,
    onConnect: (BluetoothDeviceInfo) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("已配对设备", style = MaterialTheme.typography.titleMedium)
        DeviceList(devices = pairedDevices, onConnect = onConnect)
        Divider()
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("扫描结果", style = MaterialTheme.typography.titleMedium)
        }
        DeviceList(devices = scannedDevices, onConnect = onConnect)
    }
}

@Composable
private fun DeviceList(
    devices: List<BluetoothDeviceInfo>,
    onConnect: (BluetoothDeviceInfo) -> Unit
) {
    if (devices.isEmpty()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            tonalElevation = 2.dp
        ) {
            Text(
                text = "暂无设备",
                modifier = Modifier.padding(16.dp)
            )
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(devices, key = { it.address }) { device ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(device.name ?: "未命名设备", fontWeight = FontWeight.Bold)
                        Text("地址: ${'$'}{device.address}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "类型: ${'$'}{if (device.type == BluetoothDeviceType.BLE) "BLE" else "SPP"}${if (device.isPaired) " (已配对)" else ""}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { onConnect(device) }) { Text("连接") }
                    }
                }
            }
        }
    }
}

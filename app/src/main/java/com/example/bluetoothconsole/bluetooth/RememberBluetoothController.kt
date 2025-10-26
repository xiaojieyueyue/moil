package com.example.bluetoothconsole.bluetooth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberBluetoothController(): BluetoothController {
    val context = LocalContext.current.applicationContext
    return remember { AndroidBluetoothController(context) }
}

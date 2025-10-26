package com.example.bluetoothconsole

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import com.example.bluetoothconsole.bluetooth.LocalBluetoothController
import com.example.bluetoothconsole.bluetooth.rememberBluetoothController
import com.example.bluetoothconsole.ui.BluetoothConsoleApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val bluetoothController = rememberBluetoothController()
            CompositionLocalProvider(LocalBluetoothController provides bluetoothController) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    BluetoothConsoleApp()
                }
            }
        }
    }
}

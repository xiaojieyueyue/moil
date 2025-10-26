package com.example.bluetoothconsole.ui.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bluetoothconsole.bluetooth.BluetoothController
import com.example.bluetoothconsole.bluetooth.LocalBluetoothController
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun TerminalScreen(bluetoothController: BluetoothController = LocalBluetoothController.current) {
    val messages = remember { mutableStateListOf<String>() }
    var input by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(bluetoothController) {
        bluetoothController.incomingMessages.collect { message ->
            val text = message.content.decodeToString()
            messages.add(text)
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("串口终端") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                messages.forEach { line ->
                    Text(line)
                }
            }
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("发送数据 (文本或 HEX，例如 AA BB CC)") }
            )
            Button(onClick = {
                scope.launch {
                    val data = parseInput(input)
                    bluetoothController.sendBytes(data)
                    input = ""
                }
            }) {
                Text("发送")
            }
        }
    }
}

private fun parseInput(value: String): ByteArray {
    val hexParts = value.split(" ", ",").mapNotNull { part ->
        part.trim().takeIf { it.isNotEmpty() }
    }
    val isHex = hexParts.all { part -> part.matches(Regex("[0-9a-fA-F]{2}")) }
    return if (isHex && hexParts.isNotEmpty()) {
        hexParts.map { it.toInt(16).toByte() }.toByteArray()
    } else {
        value.encodeToByteArray()
    }
}

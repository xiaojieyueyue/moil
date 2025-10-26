package com.example.bluetoothconsole.ui.screens.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bluetoothconsole.model.TerminalMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TerminalScreen(
    messages: List<TerminalMessage>,
    isHexMode: Boolean,
    onToggleHexMode: () -> Unit,
    onSendCommand: (String, Boolean) -> Unit
) {
    val inputState = remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HeaderSection(isHexMode = isHexMode, onToggleHexMode = onToggleHexMode)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                reverseLayout = true
            ) {
                items(messages, key = { it.timestamp }) { message ->
                    TerminalMessageRow(message = message, isHexMode = isHexMode)
                }
            }
        }
        CommandInputRow(
            input = inputState.value,
            onInputChange = { inputState.value = it },
            onSend = {
                if (inputState.value.isNotBlank()) {
                    onSendCommand(inputState.value, isHexMode)
                    inputState.value = ""
                }
            }
        )
    }
}

@Composable
private fun HeaderSection(isHexMode: Boolean, onToggleHexMode: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("串口终端", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (isHexMode) "显示模式：HEX" else "显示模式：文本",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("HEX 模式")
            Switch(checked = isHexMode, onCheckedChange = { onToggleHexMode() })
        }
    }
}

@Composable
private fun TerminalMessageRow(message: TerminalMessage, isHexMode: Boolean) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val time = formatter.format(Date(message.timestamp))
    val background = if (message.isOutgoing) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(vertical = 6.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(if (message.isOutgoing) "发送" else "接收", fontWeight = FontWeight.Bold)
            Text(time, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            text = if (isHexMode) message.content.toHexString() else message.content.decodeToStringSafely(),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun CommandInputRow(
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier.weight(1f),
            label = { Text("输入指令") }
        )
        IconButton(onClick = onSend) {
            Icon(Icons.Default.Send, contentDescription = "发送")
        }
    }
}

private fun ByteArray.toHexString(): String = joinToString(" ") { byte -> "%02X".format(byte) }

private fun ByteArray.decodeToStringSafely(): String = runCatching { decodeToString() }.getOrDefault(joinToString(" ") { "%02X".format(it) })

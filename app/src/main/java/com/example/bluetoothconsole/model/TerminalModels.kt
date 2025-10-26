package com.example.bluetoothconsole.model

data class TerminalMessage(
    val content: ByteArray,
    val timestamp: Long = System.currentTimeMillis(),
    val isOutgoing: Boolean
)

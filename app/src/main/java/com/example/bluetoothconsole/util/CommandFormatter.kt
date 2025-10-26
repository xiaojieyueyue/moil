package com.example.bluetoothconsole.util

import com.example.bluetoothconsole.data.CommandConfig

object CommandFormatter {
    fun toBytes(config: CommandConfig, value: String? = null): ByteArray {
        return when (config) {
            is CommandConfig.Text -> (value ?: config.text).encodeToByteArray()
            is CommandConfig.Hex -> config.bytes
                .mapNotNull { byteString ->
                    byteString.removePrefix("0x").removePrefix("0X").toIntOrNull(16)?.toByte()
                }
                .toByteArray()
        }
    }
}

package com.example.bluetoothconsole.util

object CommandEncoder {
    fun hexToBytes(input: String): ByteArray {
        val sanitized = input
            .replace("0x", "", ignoreCase = true)
            .replace(" ", "")
            .replace(",", "")
            .replace("-", "")
        if (sanitized.isEmpty()) return byteArrayOf()
        require(sanitized.length % 2 == 0) { "Hex string must have even length" }
        return sanitized.chunked(2)
            .map { it.uppercase().toInt(16).toByte() }
            .toByteArray()
    }

    fun valueToPayload(value: Int, asHex: Boolean): ByteArray {
        return if (asHex) {
            byteArrayOf(value.toByte())
        } else {
            value.toString().toByteArray()
        }
    }
}

package com.example.bluetoothconsole.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class DashboardProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val widgets: List<WidgetConfig> = emptyList()
)

@Serializable
data class WidgetPosition(
    val column: Int,
    val row: Int,
    val columnSpan: Int = 1,
    val rowSpan: Int = 1
)

@Serializable
sealed class WidgetConfig {
    abstract val id: String
    abstract val title: String
    abstract val position: WidgetPosition

    @Serializable
    @SerialName("button")
    data class Button(
        override val id: String = UUID.randomUUID().toString(),
        override val title: String,
        override val position: WidgetPosition,
        val onPressCommand: CommandPayload,
        val onReleaseCommand: CommandPayload?
    ) : WidgetConfig()

    @Serializable
    @SerialName("toggle")
    data class Toggle(
        override val id: String = UUID.randomUUID().toString(),
        override val title: String,
        override val position: WidgetPosition,
        val onCommand: CommandPayload,
        val offCommand: CommandPayload
    ) : WidgetConfig()

    @Serializable
    @SerialName("slider")
    data class Slider(
        override val id: String = UUID.randomUUID().toString(),
        override val title: String,
        override val position: WidgetPosition,
        val minValue: Int,
        val maxValue: Int,
        val sendAsHex: Boolean
    ) : WidgetConfig()

    @Serializable
    @SerialName("textInput")
    data class TextInput(
        override val id: String = UUID.randomUUID().toString(),
        override val title: String,
        override val position: WidgetPosition,
        val defaultText: String = ""
    ) : WidgetConfig()

    @Serializable
    @SerialName("label")
    data class Label(
        override val id: String = UUID.randomUUID().toString(),
        override val title: String,
        override val position: WidgetPosition,
        val filterPrefix: String? = null
    ) : WidgetConfig()

    @Serializable
    @SerialName("gauge")
    data class Gauge(
        override val id: String = UUID.randomUUID().toString(),
        override val title: String,
        override val position: WidgetPosition,
        val minValue: Float = 0f,
        val maxValue: Float = 100f,
        val filterPrefix: String? = null
    ) : WidgetConfig()

    @Serializable
    @SerialName("joystick")
    data class Joystick(
        override val id: String = UUID.randomUUID().toString(),
        override val title: String,
        override val position: WidgetPosition,
        val sendContinuously: Boolean = true,
        val sendAsHex: Boolean = false
    ) : WidgetConfig()
}

@Serializable
sealed class CommandPayload {
    abstract val value: String

    @Serializable
    @SerialName("string")
    data class StringPayload(override val value: String) : CommandPayload()

    @Serializable
    @SerialName("hex")
    data class HexPayload(override val value: String) : CommandPayload()
}

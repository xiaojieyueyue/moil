package com.example.bluetoothconsole.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "dashboards")
data class DashboardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "widgets")
data class WidgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dashboardId: Long,
    val type: WidgetType,
    val title: String,
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int,
    val configuration: String
)

enum class WidgetType {
    BUTTON,
    TOGGLE,
    SLIDER,
    TEXT_INPUT,
    LABEL,
    GAUGE,
    JOYSTICK
}

@Serializable
sealed interface WidgetConfiguration {
    @Serializable
    data class ButtonConfig(
        val onPress: CommandConfig,
        val onRelease: CommandConfig? = null,
        val label: String = "按钮"
    ) : WidgetConfiguration

    @Serializable
    data class ToggleConfig(
        val onCommand: CommandConfig,
        val offCommand: CommandConfig,
        val label: String = "开关"
    ) : WidgetConfiguration

    @Serializable
    data class SliderConfig(
        val min: Int,
        val max: Int,
        val commandFormat: String
    ) : WidgetConfiguration

    @Serializable
    data class TextInputConfig(
        val placeholder: String = "发送数据"
    ) : WidgetConfiguration

    @Serializable
    data class LabelConfig(
        val filterPrefix: String = "",
        val fallbackText: String = "等待数据"
    ) : WidgetConfiguration

    @Serializable
    data class GaugeConfig(
        val min: Float,
        val max: Float,
        val filterPrefix: String
    ) : WidgetConfiguration

    @Serializable
    data class JoystickConfig(
        val commandFormat: String = "X:%d,Y:%d",
        val step: Int = 10
    ) : WidgetConfiguration
}

@Serializable
sealed interface CommandConfig {
    val mode: CommandMode

    @Serializable
    data class Text(override val mode: CommandMode = CommandMode.TEXT, val text: String) : CommandConfig

    @Serializable
    data class Hex(override val mode: CommandMode = CommandMode.HEX, val bytes: List<String>) : CommandConfig
}

enum class CommandMode {
    TEXT,
    HEX
}

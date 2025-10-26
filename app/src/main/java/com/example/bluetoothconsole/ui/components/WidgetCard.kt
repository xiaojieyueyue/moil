package com.example.bluetoothconsole.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bluetoothconsole.model.CommandPayload
import com.example.bluetoothconsole.model.TerminalMessage
import com.example.bluetoothconsole.model.WidgetConfig

@Composable
fun WidgetCard(
    widget: WidgetConfig,
    terminalMessages: List<TerminalMessage>,
    onCommand: (WidgetConfig, CommandPayload) -> Unit,
    onSliderChange: (WidgetConfig.Slider, Int) -> Unit,
    onJoystickChange: (WidgetConfig.Joystick, Int, Int) -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(widget.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "编辑") }
                    IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = "删除") }
                }
            }
            when (widget) {
                is WidgetConfig.Button -> ButtonWidget(widget, onCommand)
                is WidgetConfig.Toggle -> ToggleWidget(widget, onCommand)
                is WidgetConfig.Slider -> SliderWidget(widget, onSliderChange)
                is WidgetConfig.TextInput -> TextInputWidget(widget, onCommand)
                is WidgetConfig.Label -> LabelWidget(widget, terminalMessages)
                is WidgetConfig.Gauge -> GaugeWidget(widget, terminalMessages)
                is WidgetConfig.Joystick -> JoystickWidget(widget, onJoystickChange)
            }
        }
    }
}

@Composable
private fun ButtonWidget(widget: WidgetConfig.Button, onCommand: (WidgetConfig, CommandPayload) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.medium)
            .pointerInput(widget.id) {
                detectTapGestures(
                    onPress = {
                        onCommand(widget, widget.onPressCommand)
                        val release = try {
                            tryAwaitRelease()
                            true
                        } catch (_: Throwable) {
                            false
                        }
                        if (release) {
                            widget.onReleaseCommand?.let { onCommand(widget, it) }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(widget.title, color = Color.White, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun ToggleWidget(widget: WidgetConfig.Toggle, onCommand: (WidgetConfig, CommandPayload) -> Unit) {
    val checkedState = remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Switch(checked = checkedState.value, onCheckedChange = {
            checkedState.value = it
            onCommand(widget, if (it) widget.onCommand else widget.offCommand)
        })
        Text(if (checkedState.value) "已打开" else "已关闭")
    }
}

@Composable
private fun SliderWidget(widget: WidgetConfig.Slider, onSliderChange: (WidgetConfig.Slider, Int) -> Unit) {
    val sliderValue = remember { mutableFloatStateOf(widget.minValue.toFloat()) }
    Column {
        Slider(
            value = sliderValue.floatValue,
            valueRange = widget.minValue.toFloat()..widget.maxValue.toFloat(),
            onValueChange = {
                sliderValue.floatValue = it
                onSliderChange(widget, it.toInt())
            }
        )
        Text("数值: ${'$'}{sliderValue.floatValue.toInt()}")
    }
}

@Composable
private fun TextInputWidget(widget: WidgetConfig.TextInput, onCommand: (WidgetConfig, CommandPayload) -> Unit) {
    val textState = remember { mutableStateOf(widget.defaultText) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = textState.value,
            onValueChange = { textState.value = it },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        TextButton(onClick = {
            onCommand(widget, CommandPayload.StringPayload(textState.value))
        }) {
            Icon(Icons.Default.Send, contentDescription = "发送")
            Spacer(modifier = Modifier.size(4.dp))
            Text("发送")
        }
    }
}

@Composable
private fun LabelWidget(widget: WidgetConfig.Label, terminalMessages: List<TerminalMessage>) {
    val latest = terminalMessages
        .filter { !it.isOutgoing }
        .map { it.content.decodeToString() }
        .lastOrNull { message ->
            widget.filterPrefix?.let { prefix -> message.startsWith(prefix) } ?: true
        } ?: "等待数据..."
    Text(latest, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun GaugeWidget(widget: WidgetConfig.Gauge, terminalMessages: List<TerminalMessage>) {
    val latest = terminalMessages
        .filter { !it.isOutgoing }
        .map { it.content.decodeToString() }
        .mapNotNull { message ->
            val filtered = widget.filterPrefix?.let { prefix ->
                if (message.startsWith(prefix)) message.removePrefix(prefix) else null
            } ?: message
            filtered.trim().toFloatOrNull()
        }
        .lastOrNull() ?: widget.minValue
    val range = (widget.maxValue - widget.minValue).takeIf { it != 0f } ?: 1f
    val ratio = ((latest - widget.minValue) / range).coerceIn(0f, 1f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(progress = ratio)
        Spacer(modifier = Modifier.height(8.dp))
        Text(String.format("%.1f", latest))
    }
}

@Composable
private fun JoystickWidget(widget: WidgetConfig.Joystick, onJoystickChange: (WidgetConfig.Joystick, Int, Int) -> Unit) {
    BoxWithConstraints(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
        val diameterPx = with(LocalDensity.current) { maxWidth.toPx() }
        val centerOffset = Offset(diameterPx / 2f, diameterPx / 2f)
        val lastOffset = remember { mutableStateOf(centerOffset) }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), shape = CircleShape)
                .pointerInput(widget.id) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            lastOffset.value = offset
                            if (widget.sendContinuously) {
                                val (x, y) = calculateJoystick(offset, diameterPx)
                                onJoystickChange(widget, x, y)
                            }
                        },
                        onDrag = { change, _ ->
                            lastOffset.value = change.position
                            if (widget.sendContinuously) {
                                val (x, y) = calculateJoystick(change.position, diameterPx)
                                onJoystickChange(widget, x, y)
                            }
                        },
                        onDragEnd = {
                            val (x, y) = calculateJoystick(lastOffset.value, diameterPx)
                            if (!widget.sendContinuously) {
                                onJoystickChange(widget, x, y)
                            }
                            onJoystickChange(widget, 0, 0)
                            lastOffset.value = centerOffset
                        }
                    )
                }
        )
        Canvas(modifier = Modifier.size(60.dp)) {
            drawCircle(color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun calculateJoystick(offset: Offset, diameterPx: Float): Pair<Int, Int> {
    val radius = diameterPx / 2f
    val centeredX = ((offset.x - radius) / radius).coerceIn(-1f, 1f)
    val centeredY = ((offset.y - radius) / radius).coerceIn(-1f, 1f)
    val xValue = (centeredX * 100).toInt()
    val yValue = (centeredY * -100).toInt()
    return xValue to yValue
}

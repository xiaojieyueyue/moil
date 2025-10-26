package com.example.bluetoothconsole.ui.dashboard

import android.view.MotionEvent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.dp
import com.example.bluetoothconsole.bluetooth.BluetoothController
import com.example.bluetoothconsole.bluetooth.LocalBluetoothController
import com.example.bluetoothconsole.data.CommandConfig
import com.example.bluetoothconsole.data.DashboardWidget
import com.example.bluetoothconsole.data.WidgetConfiguration
import com.example.bluetoothconsole.data.WidgetType
import com.example.bluetoothconsole.util.CommandFormatter
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardListScreen(
    viewModel: DashboardListViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    bluetoothController: BluetoothController = LocalBluetoothController.current
) {
    val dashboards by viewModel.dashboards.collectAsState()
    val selectedId by viewModel.selectedDashboardId.collectAsState()
    val widgets by viewModel.widgets.collectAsState()

    var showAddDashboard by remember { mutableStateOf(false) }
    var showWidgetPicker by remember { mutableStateOf(false) }
    var editingWidget by remember { mutableStateOf<DashboardWidget?>(null) }

    var latestMessage by remember { mutableStateOf("") }

    LaunchedEffect(bluetoothController) {
        bluetoothController.incomingMessages.collectLatest { message ->
            latestMessage = message.content.decodeToString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("仪表盘") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showWidgetPicker = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                dashboards.forEach { dashboard ->
                    val isSelected = selectedId == dashboard.id
                    OutlinedButton(
                        onClick = { viewModel.selectDashboard(dashboard.id) },
                        modifier = Modifier.weight(1f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Text(dashboard.name)
                    }
                }
                OutlinedButton(onClick = { showAddDashboard = true }) {
                    Text("新建")
                }
            }
            Text(text = "最新数据：$latestMessage", style = MaterialTheme.typography.bodyMedium)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(widgets, key = { it.id }) { widget ->
                    DashboardWidgetCard(
                        widget = widget,
                        onEdit = { editingWidget = widget },
                        onDelete = { viewModel.deleteWidget(widget.id) },
                        onAction = { payload -> bluetoothController.sendBytes(payload) },
                        latestMessage = latestMessage
                    )
                }
            }
        }
    }

    if (showAddDashboard) {
        DashboardNameDialog(onDismiss = { showAddDashboard = false }) { name ->
            viewModel.createDashboard(name)
            showAddDashboard = false
        }
    }

    if (showWidgetPicker) {
        WidgetPickerDialog(onDismiss = { showWidgetPicker = false }) { type ->
            viewModel.addWidget(type)
            showWidgetPicker = false
        }
    }

    editingWidget?.let { widget ->
        WidgetEditorDialog(widget = widget, onDismiss = { editingWidget = null }) { updated ->
            viewModel.upsertWidget(updated)
            editingWidget = null
        }
    }
}

@Composable
private fun DashboardWidgetCard(
    widget: DashboardWidget,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAction: (ByteArray) -> Unit,
    latestMessage: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(widget.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            }
            when (val config = widget.configuration) {
                is WidgetConfiguration.ButtonConfig -> {
                    var isPressed by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isPressed) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(8.dp)
                            )
                            .pointerInteropFilter { event ->
                                when (event.action) {
                                    MotionEvent.ACTION_DOWN -> {
                                        isPressed = true
                                        onAction(CommandFormatter.toBytes(config.onPress))
                                        true
                                    }
                                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                        isPressed = false
                                        config.onRelease?.let { onAction(CommandFormatter.toBytes(it)) }
                                        true
                                    }
                                    else -> false
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(config.label, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                is WidgetConfiguration.ToggleConfig -> {
                    var toggled by remember { mutableStateOf(false) }
                    Button(onClick = {
                        toggled = !toggled
                        val command = if (toggled) config.onCommand else config.offCommand
                        onAction(CommandFormatter.toBytes(command))
                    }) {
                        Text(if (toggled) "关闭" else "开启")
                    }
                }
                is WidgetConfiguration.SliderConfig -> {
                    var value by remember { mutableStateOf((config.min + config.max) / 2f) }
                    Slider(
                        value = value,
                        onValueChange = {
                            value = it
                            val formatted = config.commandFormat.format(it.toInt())
                            onAction(CommandFormatter.toBytes(CommandConfig.Text(text = formatted)))
                        },
                        valueRange = config.min.toFloat()..config.max.toFloat()
                    )
                }
                is WidgetConfiguration.TextInputConfig -> {
                    var text by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text(config.placeholder) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = {
                        onAction(text.encodeToByteArray())
                        text = ""
                    }) {
                        Text("发送")
                    }
                }
                is WidgetConfiguration.LabelConfig -> {
                    val display = if (config.filterPrefix.isNotEmpty() && latestMessage.startsWith(config.filterPrefix)) {
                        latestMessage.removePrefix(config.filterPrefix)
                    } else if (config.filterPrefix.isEmpty()) {
                        latestMessage
                    } else {
                        config.fallbackText
                    }
                    Text(display)
                }
                is WidgetConfiguration.GaugeConfig -> {
                    val value = latestMessage.substringAfter(config.filterPrefix, "0").toFloatOrNull() ?: 0f
                    val percentage = ((value - config.min) / (config.max - config.min)).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .height(80.dp)
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(percentage)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        )
                        Text(
                            text = "%.1f".format(value),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
                is WidgetConfiguration.JoystickConfig -> {
                    Joystick(config = config, onMove = { x, y ->
                        val formatted = config.commandFormat.format(x, y)
                        onAction(CommandFormatter.toBytes(CommandConfig.Text(text = formatted)))
                    })
                }
            }
        }
    }
}

@Composable
private fun DashboardNameDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建仪表盘") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") })
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun WidgetPickerDialog(onDismiss: () -> Unit, onSelected: (WidgetType) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择组件类型") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WidgetType.values().forEach { type ->
                    Button(onClick = { onSelected(type) }) {
                        Text(type.name)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun WidgetEditorDialog(
    widget: DashboardWidget,
    onDismiss: () -> Unit,
    onSave: (DashboardWidget) -> Unit
) {
    var title by remember { mutableStateOf(widget.title) }
    var configuration by remember { mutableStateOf(widget.configuration) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑组件") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题") })
                when (val config = configuration) {
                    is WidgetConfiguration.ButtonConfig -> {
                        CommandEditor(label = "按下指令", command = config.onPress) {
                            configuration = config.copy(onPress = it)
                        }
                        CommandEditor(label = "松开指令", command = config.onRelease ?: config.onPress) {
                            configuration = config.copy(onRelease = it)
                        }
                        OutlinedTextField(value = config.label, onValueChange = { configuration = config.copy(label = it) }, label = { Text("按钮文字") })
                    }
                    is WidgetConfiguration.ToggleConfig -> {
                        CommandEditor("打开指令", config.onCommand) { configuration = config.copy(onCommand = it) }
                        CommandEditor("关闭指令", config.offCommand) { configuration = config.copy(offCommand = it) }
                        OutlinedTextField(value = config.label, onValueChange = { configuration = config.copy(label = it) }, label = { Text("标签") })
                    }
                    is WidgetConfiguration.SliderConfig -> {
                        OutlinedTextField(value = config.min.toString(), onValueChange = {
                            configuration = config.copy(min = it.toIntOrNull() ?: config.min)
                        }, label = { Text("最小值") })
                        OutlinedTextField(value = config.max.toString(), onValueChange = {
                            configuration = config.copy(max = it.toIntOrNull() ?: config.max)
                        }, label = { Text("最大值") })
                        OutlinedTextField(value = config.commandFormat, onValueChange = { configuration = config.copy(commandFormat = it) }, label = { Text("指令格式") })
                    }
                    is WidgetConfiguration.TextInputConfig -> {
                        OutlinedTextField(value = config.placeholder, onValueChange = { configuration = config.copy(placeholder = it) }, label = { Text("占位提示") })
                    }
                    is WidgetConfiguration.LabelConfig -> {
                        OutlinedTextField(value = config.filterPrefix, onValueChange = { configuration = config.copy(filterPrefix = it) }, label = { Text("过滤前缀") })
                        OutlinedTextField(value = config.fallbackText, onValueChange = { configuration = config.copy(fallbackText = it) }, label = { Text("默认文本") })
                    }
                    is WidgetConfiguration.GaugeConfig -> {
                        OutlinedTextField(value = config.filterPrefix, onValueChange = { configuration = config.copy(filterPrefix = it) }, label = { Text("过滤前缀") })
                        OutlinedTextField(value = config.min.toString(), onValueChange = { configuration = config.copy(min = it.toFloatOrNull() ?: config.min) }, label = { Text("最小值") })
                        OutlinedTextField(value = config.max.toString(), onValueChange = { configuration = config.copy(max = it.toFloatOrNull() ?: config.max) }, label = { Text("最大值") })
                    }
                    is WidgetConfiguration.JoystickConfig -> {
                        OutlinedTextField(value = config.commandFormat, onValueChange = { configuration = config.copy(commandFormat = it) }, label = { Text("指令格式") })
                        OutlinedTextField(value = config.step.toString(), onValueChange = { configuration = config.copy(step = it.toIntOrNull() ?: config.step) }, label = { Text("步进") })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(widget.copy(title = title, configuration = configuration)) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun CommandEditor(
    label: String,
    command: CommandConfig,
    onCommandChange: (CommandConfig) -> Unit
) {
    val isHex = command is CommandConfig.Hex
    Text(label)
    if (isHex) {
        var hex by remember(command) { mutableStateOf(command.bytes.joinToString(" ")) }
        OutlinedTextField(value = hex, onValueChange = {
            hex = it
            val parts = it.split(*arrayOf(" ", ","), ignoreCase = true)
                .mapNotNull { part -> part.trim().takeIf { it.isNotEmpty() } }
            onCommandChange(CommandConfig.Hex(bytes = parts))
        }, label = { Text("HEX 字节，以空格分隔") })
        TextButton(onClick = { onCommandChange(CommandConfig.Text(text = "")) }) {
            Text("切换为文本")
        }
    } else {
        val textCommand = command as CommandConfig.Text
        OutlinedTextField(value = textCommand.text, onValueChange = {
            onCommandChange(CommandConfig.Text(text = it))
        }, label = { Text("文本指令") })
        TextButton(onClick = { onCommandChange(CommandConfig.Hex(bytes = emptyList())) }) {
            Text("切换为 HEX")
        }
    }
}

@Composable
private fun Joystick(
    config: WidgetConfiguration.JoystickConfig,
    onMove: (Int, Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { onMove(0, config.step) }) { Text("上") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onMove(-config.step, 0) }) { Text("左") }
            Button(onClick = { onMove(0, 0) }) { Text("中") }
            Button(onClick = { onMove(config.step, 0) }) { Text("右") }
        }
        Button(onClick = { onMove(0, -config.step) }) { Text("下") }
    }
}

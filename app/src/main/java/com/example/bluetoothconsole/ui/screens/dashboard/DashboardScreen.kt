package com.example.bluetoothconsole.ui.screens.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bluetoothconsole.model.CommandPayload
import com.example.bluetoothconsole.model.DashboardProject
import com.example.bluetoothconsole.model.TerminalMessage
import com.example.bluetoothconsole.model.WidgetConfig
import com.example.bluetoothconsole.ui.components.WidgetCard
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.MutableState
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    projects: List<DashboardProject>,
    selectedProject: DashboardProject?,
    terminalMessages: List<TerminalMessage>,
    onCreateProject: (String) -> Unit,
    onSelectProject: (String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onAddWidget: (String, WidgetConfig) -> Unit,
    onUpdateWidget: (String, WidgetConfig) -> Unit,
    onRemoveWidget: (String, String) -> Unit,
    onSendWidgetCommand: (WidgetConfig, CommandPayload) -> Unit,
    onSendSliderValue: (WidgetConfig.Slider, Int) -> Unit,
    onSendJoystickValue: (WidgetConfig.Joystick, Int, Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val showProjectDialog = remember { mutableStateOf(false) }
    val newProjectName = remember { mutableStateOf("") }
    val showWidgetDialog = remember { mutableStateOf(false) }
    val editingWidget = remember { mutableStateOf<WidgetConfig?>(null) }

    LaunchedEffect(projects.size) {
        if (projects.isEmpty()) {
            showProjectDialog.value = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("仪表盘", fontWeight = FontWeight.Bold)
                        Text(
                            text = selectedProject?.name ?: "未选择项目",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { showProjectDialog.value = true }) {
                        Text("新建项目")
                    }
                    if (selectedProject != null) {
                        TextButton(onClick = { onDeleteProject(selectedProject.id) }) {
                            Text("删除")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedProject != null) {
                FloatingActionButton(onClick = {
                    editingWidget.value = null
                    showWidgetDialog.value = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = "添加组件")
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)) {
            ProjectSelector(
                projects = projects,
                selectedProject = selectedProject,
                onSelectProject = onSelectProject
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                selectedProject?.widgets?.let { widgets ->
                    items(widgets, key = { it.id }) { widget ->
                        WidgetCard(
                            widget = widget,
                            terminalMessages = terminalMessages,
                            onCommand = onSendWidgetCommand,
                            onSliderChange = onSendSliderValue,
                            onJoystickChange = onSendJoystickValue,
                            onEdit = {
                                editingWidget.value = widget
                                showWidgetDialog.value = true
                            },
                            onRemove = { onRemoveWidget(selectedProject.id, widget.id) }
                        )
                    }
                }
            }
        }
    }

    if (showProjectDialog.value) {
        AlertDialog(
            onDismissRequest = { showProjectDialog.value = false },
            title = { Text("创建项目") },
            text = {
                OutlinedTextField(
                    value = newProjectName.value,
                    onValueChange = { newProjectName.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("项目名称") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newProjectName.value.isNotBlank()) {
                        onCreateProject(newProjectName.value.trim())
                        newProjectName.value = ""
                    }
                    showProjectDialog.value = false
                }) {
                    Text("创建")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProjectDialog.value = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showWidgetDialog.value && selectedProject != null) {
        WidgetConfigDialog(
            initialConfig = editingWidget.value,
            onDismiss = { showWidgetDialog.value = false },
            onConfirm = { config ->
                scope.launch {
                    if (editingWidget.value == null) {
                        onAddWidget(selectedProject.id, config)
                    } else {
                        onUpdateWidget(selectedProject.id, config)
                    }
                }
                showWidgetDialog.value = false
            }
        )
    }
}

@Composable
private fun ProjectSelector(
    projects: List<DashboardProject>,
    selectedProject: DashboardProject?,
    onSelectProject: (String) -> Unit
) {
    if (projects.isEmpty()) {
        Text("请创建一个项目", style = MaterialTheme.typography.bodyMedium)
        return
    }
    val expanded = remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded.value = true }) {
            Text(selectedProject?.name ?: "选择项目")
        }
        DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
            projects.forEach { project ->
                DropdownMenuItem(
                    text = { Text(project.name) },
                    onClick = {
                        onSelectProject(project.id)
                        expanded.value = false
                    }
                )
            }
        }
    }
}

@Composable
private fun WidgetConfigDialog(
    initialConfig: WidgetConfig?,
    onDismiss: () -> Unit,
    onConfirm: (WidgetConfig) -> Unit
) {
    val typeState = remember { mutableStateOf(WidgetTypeOption.fromConfig(initialConfig)) }
    val titleState = remember { mutableStateOf(initialConfig?.title ?: "") }
    val columnState = remember { mutableStateOf(initialConfig?.position?.column?.toString() ?: "0") }
    val rowState = remember { mutableStateOf(initialConfig?.position?.row?.toString() ?: "0") }
    val colSpanState = remember { mutableStateOf(initialConfig?.position?.columnSpan?.toString() ?: "1") }
    val rowSpanState = remember { mutableStateOf(initialConfig?.position?.rowSpan?.toString() ?: "1") }

    val buttonPressCommand = remember {
        mutableStateOf(initialConfig.asButton()?.onPressCommand?.toCommandState() ?: CommandState())
    }
    val buttonReleaseCommand = remember {
        mutableStateOf(initialConfig.asButton()?.onReleaseCommand?.toCommandState() ?: CommandState())
    }
    val toggleOnCommand = remember {
        mutableStateOf(initialConfig.asToggle()?.onCommand?.toCommandState() ?: CommandState())
    }
    val toggleOffCommand = remember {
        mutableStateOf(initialConfig.asToggle()?.offCommand?.toCommandState() ?: CommandState())
    }
    val sliderRange = remember {
        mutableStateOf(initialConfig.asSlider()?.let { it.minValue to it.maxValue } ?: (0 to 100))
    }
    val sliderHex = remember { mutableStateOf(initialConfig.asSlider()?.sendAsHex ?: false) }
    val textDefault = remember { mutableStateOf(initialConfig.asTextInput()?.defaultText ?: "") }
    val labelFilter = remember { mutableStateOf(initialConfig.asLabel()?.filterPrefix ?: "") }
    val gaugeRange = remember {
        mutableStateOf(initialConfig.asGauge()?.let { it.minValue to it.maxValue } ?: (0f to 100f))
    }
    val gaugeFilter = remember { mutableStateOf(initialConfig.asGauge()?.filterPrefix ?: "") }
    val joystickHex = remember { mutableStateOf(initialConfig.asJoystick()?.sendAsHex ?: false) }
    val joystickContinuous = remember { mutableStateOf(initialConfig.asJoystick()?.sendContinuously ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialConfig == null) "添加组件" else "编辑组件") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = titleState.value,
                    onValueChange = { titleState.value = it },
                    label = { Text("标题") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WidgetTypeOption.values().forEach { option ->
                        FilterChip(
                            selected = typeState.value == option,
                            onClick = { typeState.value = option },
                            label = { Text(option.label) }
                        )
                    }
                }
                PositionEditor(columnState, rowState, colSpanState, rowSpanState)
                when (typeState.value) {
                    WidgetTypeOption.Button -> ButtonConfigContent(buttonPressCommand, buttonReleaseCommand)
                    WidgetTypeOption.Toggle -> ToggleConfigContent(toggleOnCommand, toggleOffCommand)
                    WidgetTypeOption.Slider -> SliderConfigContent(sliderRange, sliderHex)
                    WidgetTypeOption.TextInput -> TextInputConfigContent(textDefault)
                    WidgetTypeOption.Label -> LabelConfigContent(labelFilter)
                    WidgetTypeOption.Gauge -> GaugeConfigContent(gaugeRange, gaugeFilter)
                    WidgetTypeOption.Joystick -> JoystickConfigContent(joystickHex, joystickContinuous)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (titleState.value.isBlank()) return@TextButton
                if (typeState.value == WidgetTypeOption.Button && buttonPressCommand.value.text.isBlank()) return@TextButton
                if (typeState.value == WidgetTypeOption.Toggle && (
                        toggleOnCommand.value.text.isBlank() || toggleOffCommand.value.text.isBlank()
                    )
                ) return@TextButton
                val position = WidgetPosition(
                    column = columnState.value.toIntOrNull() ?: 0,
                    row = rowState.value.toIntOrNull() ?: 0,
                    columnSpan = colSpanState.value.toIntOrNull() ?: 1,
                    rowSpan = rowSpanState.value.toIntOrNull() ?: 1
                )
                val widgetId = initialConfig?.id ?: UUID.randomUUID().toString()
                val sliderMin = minOf(sliderRange.value.first, sliderRange.value.second)
                val sliderMax = maxOf(sliderRange.value.first, sliderRange.value.second)
                val gaugeMin = minOf(gaugeRange.value.first, gaugeRange.value.second)
                val gaugeMax = maxOf(gaugeRange.value.first, gaugeRange.value.second)
                val widget = when (typeState.value) {
                    WidgetTypeOption.Button -> WidgetConfig.Button(
                        id = widgetId,
                        title = titleState.value,
                        position = position,
                        onPressCommand = buttonPressCommand.value.toPayload(),
                        onReleaseCommand = buttonReleaseCommand.value.takeIf { it.text.isNotBlank() }?.toPayload()
                    )
                    WidgetTypeOption.Toggle -> WidgetConfig.Toggle(
                        id = widgetId,
                        title = titleState.value,
                        position = position,
                        onCommand = toggleOnCommand.value.toPayload(),
                        offCommand = toggleOffCommand.value.toPayload()
                    )
                    WidgetTypeOption.Slider -> WidgetConfig.Slider(
                        id = widgetId,
                        title = titleState.value,
                        position = position,
                        minValue = sliderMin,
                        maxValue = sliderMax,
                        sendAsHex = sliderHex.value
                    )
                    WidgetTypeOption.TextInput -> WidgetConfig.TextInput(
                        id = widgetId,
                        title = titleState.value,
                        position = position,
                        defaultText = textDefault.value
                    )
                    WidgetTypeOption.Label -> WidgetConfig.Label(
                        id = widgetId,
                        title = titleState.value,
                        position = position,
                        filterPrefix = labelFilter.value.takeIf { it.isNotBlank() }
                    )
                    WidgetTypeOption.Gauge -> WidgetConfig.Gauge(
                        id = widgetId,
                        title = titleState.value,
                        position = position,
                        minValue = gaugeMin,
                        maxValue = gaugeMax,
                        filterPrefix = gaugeFilter.value.takeIf { it.isNotBlank() }
                    )
                    WidgetTypeOption.Joystick -> WidgetConfig.Joystick(
                        id = widgetId,
                        title = titleState.value,
                        position = position,
                        sendContinuously = joystickContinuous.value,
                        sendAsHex = joystickHex.value
                    )
                }
                onConfirm(widget)
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun PositionEditor(
    columnState: MutableState<String>,
    rowState: MutableState<String>,
    colSpanState: MutableState<String>,
    rowSpanState: MutableState<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("网格位置", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = columnState.value,
                onValueChange = { columnState.value = it.filterDigits() },
                label = { Text("列") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = rowState.value,
                onValueChange = { rowState.value = it.filterDigits() },
                label = { Text("行") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = colSpanState.value,
                onValueChange = { colSpanState.value = it.filterDigits() },
                label = { Text("列跨度") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = rowSpanState.value,
                onValueChange = { rowSpanState.value = it.filterDigits() },
                label = { Text("行跨度") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ButtonConfigContent(pressCommand: MutableState<CommandState>, releaseCommand: MutableState<CommandState>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CommandInputField(label = "按下指令", state = pressCommand)
        CommandInputField(label = "松开指令", state = releaseCommand, allowEmpty = true)
    }
}

@Composable
private fun ToggleConfigContent(onCommand: MutableState<CommandState>, offCommand: MutableState<CommandState>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CommandInputField(label = "打开指令", state = onCommand)
        CommandInputField(label = "关闭指令", state = offCommand)
    }
}

@Composable
private fun SliderConfigContent(rangeState: MutableState<Pair<Int, Int>>, isHex: MutableState<Boolean>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = rangeState.value.first.toString(),
                onValueChange = { newValue ->
                    val min = newValue.toIntOrNull() ?: rangeState.value.first
                    rangeState.value = min to rangeState.value.second
                },
                label = { Text("最小值") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = rangeState.value.second.toString(),
                onValueChange = { newValue ->
                    val max = newValue.toIntOrNull() ?: rangeState.value.second
                    rangeState.value = rangeState.value.first to max
                },
                label = { Text("最大值") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("以 HEX 发送")
            Switch(checked = isHex.value, onCheckedChange = { isHex.value = it })
        }
    }
}

@Composable
private fun TextInputConfigContent(defaultText: MutableState<String>) {
    OutlinedTextField(
        value = defaultText.value,
        onValueChange = { defaultText.value = it },
        label = { Text("默认文本") },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun LabelConfigContent(filterPrefix: MutableState<String>) {
    OutlinedTextField(
        value = filterPrefix.value,
        onValueChange = { filterPrefix.value = it },
        label = { Text("过滤前缀 (可选)") },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun GaugeConfigContent(rangeState: MutableState<Pair<Float, Float>>, filterPrefix: MutableState<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = rangeState.value.first.toString(),
                onValueChange = { text ->
                    val min = text.toFloatOrNull() ?: rangeState.value.first
                    rangeState.value = min to rangeState.value.second
                },
                label = { Text("最小值") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = rangeState.value.second.toString(),
                onValueChange = { text ->
                    val max = text.toFloatOrNull() ?: rangeState.value.second
                    rangeState.value = rangeState.value.first to max
                },
                label = { Text("最大值") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = filterPrefix.value,
            onValueChange = { filterPrefix.value = it },
            label = { Text("过滤前缀 (可选)") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun JoystickConfigContent(isHex: MutableState<Boolean>, isContinuous: MutableState<Boolean>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("连续发送")
            Switch(checked = isContinuous.value, onCheckedChange = { isContinuous.value = it })
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("以 HEX 发送")
            Switch(checked = isHex.value, onCheckedChange = { isHex.value = it })
        }
    }
}

@Composable
private fun CommandInputField(label: String, state: MutableState<CommandState>, allowEmpty: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = state.value.text,
            onValueChange = { state.value = state.value.copy(text = it) },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            isError = !allowEmpty && state.value.text.isBlank(),
            supportingText = {
                Text(if (state.value.isHex) "HEX 格式 (例如 AA01FF)" else "字符串格式")
            }
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("HEX 模式")
            Switch(
                checked = state.value.isHex,
                onCheckedChange = { state.value = state.value.copy(isHex = it) }
            )
        }
    }
}

private enum class WidgetTypeOption(val label: String) {
    Button("按钮"),
    Toggle("开关"),
    Slider("滑块"),
    TextInput("文本"),
    Label("显示"),
    Gauge("仪表"),
    Joystick("摇杆");

    companion object {
        fun fromConfig(config: WidgetConfig?): WidgetTypeOption = when (config) {
            is WidgetConfig.Button -> Button
            is WidgetConfig.Toggle -> Toggle
            is WidgetConfig.Slider -> Slider
            is WidgetConfig.TextInput -> TextInput
            is WidgetConfig.Label -> Label
            is WidgetConfig.Gauge -> Gauge
            is WidgetConfig.Joystick -> Joystick
            else -> Button
        }
    }
}

private data class CommandState(var text: String = "", var isHex: Boolean = false) {
    fun toPayload(): CommandPayload = if (isHex) {
        CommandPayload.HexPayload(text)
    } else {
        CommandPayload.StringPayload(text)
    }
}

private fun CommandPayload.toCommandState(): CommandState = when (this) {
    is CommandPayload.StringPayload -> CommandState(value, false)
    is CommandPayload.HexPayload -> CommandState(value, true)
}

private fun WidgetConfig?.asButton(): WidgetConfig.Button? = this as? WidgetConfig.Button
private fun WidgetConfig?.asToggle(): WidgetConfig.Toggle? = this as? WidgetConfig.Toggle
private fun WidgetConfig?.asSlider(): WidgetConfig.Slider? = this as? WidgetConfig.Slider
private fun WidgetConfig?.asTextInput(): WidgetConfig.TextInput? = this as? WidgetConfig.TextInput
private fun WidgetConfig?.asLabel(): WidgetConfig.Label? = this as? WidgetConfig.Label
private fun WidgetConfig?.asGauge(): WidgetConfig.Gauge? = this as? WidgetConfig.Gauge
private fun WidgetConfig?.asJoystick(): WidgetConfig.Joystick? = this as? WidgetConfig.Joystick

private fun String.filterDigits(): String {
    if (isEmpty()) return ""
    val filtered = StringBuilder()
    forEachIndexed { index, c ->
        if (c.isDigit() || (c == '-' && index == 0)) {
            filtered.append(c)
        }
    }
    return filtered.toString()
}

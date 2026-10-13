package com.example.coursetable.ui.screens

import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.EditText
import android.widget.NumberPicker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.example.coursetable.R
import com.example.coursetable.core.design.KejingColors
import com.example.coursetable.core.design.KejingShape
import com.example.coursetable.core.design.kejingGradientBackground
import com.example.coursetable.domain.model.CoursePeriodSchedule
import com.example.coursetable.domain.model.ScheduleDefaults
import com.example.coursetable.ui.ScheduleUiState
import com.example.coursetable.ui.components.zhShortName
import java.time.DayOfWeek
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddCourseScreen(
    uiState: ScheduleUiState,
    onBack: () -> Unit,
    onSave: (
        name: String,
        dayOfWeek: DayOfWeek,
        startTime: LocalTime,
        endTime: LocalTime,
        location: String,
        teacher: String,
        weeks: IntRange,
    ) -> Unit,
) {
    val totalWeeks = uiState.selectedSemester?.totalWeeks ?: ScheduleDefaults.TOTAL_WEEKS
    val periods = CoursePeriodSchedule.periods
    var name by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedDay by remember(uiState.selectedDay) { mutableStateOf(uiState.selectedDay) }
    var weekStart by remember(uiState.selectedWeek) { mutableIntStateOf(uiState.selectedWeek) }
    var weekEnd by remember(uiState.selectedWeek) { mutableIntStateOf(uiState.selectedWeek) }
    var startPeriod by remember { mutableIntStateOf(1) }
    var endPeriod by remember { mutableIntStateOf(2) }

    val start = periods.first { it.number == startPeriod }
    val end = periods.first { it.number == endPeriod }
    val canSave = name.isNotBlank() && weekEnd >= weekStart && endPeriod >= startPeriod

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        containerColor = KejingColors.CanvasTop.copy(alpha = 0f),
        contentColor = KejingColors.Ink,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KejingColors.CanvasTop.copy(alpha = 0f),
                    titleContentColor = KejingColors.Ink,
                    navigationIconContentColor = KejingColors.Ink,
                    actionIconContentColor = KejingColors.Ink,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                title = { Text("新增课程", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(
                        enabled = canSave,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = KejingColors.Ink,
                            disabledContentColor = KejingColors.Ink.copy(alpha = 0.38f),
                        ),
                        onClick = {
                            onSave(
                                name.trim(),
                                selectedDay,
                                start.start,
                                end.end,
                                location,
                                teacher,
                                weekStart..weekEnd,
                            )
                            onBack()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "保存课程",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .kejingGradientBackground()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                FormLabel("课程信息")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    IndustrialTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("课程名称") },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IndustrialTextField(
                            value = teacher,
                            onValueChange = { teacher = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("任课教师") },
                        )
                        IndustrialTextField(
                            value = location,
                            onValueChange = { location = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("上课地点") },
                        )
                    }
                }
            }
            item {
                FormLabel("星期")
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = selectedDay == day,
                            onClick = { selectedDay = day },
                            label = { Text(day.zhShortName()) },
                            leadingIcon = if (selectedDay == day) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                    )
                                }
                            } else {
                                null
                            },
                            shape = KejingShape.Pill,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.Transparent,
                                labelColor = KejingColors.Ink,
                                selectedContainerColor = KejingColors.Orange.copy(alpha = 0.16f),
                                selectedLabelColor = KejingColors.Ink,
                                selectedLeadingIconColor = KejingColors.Ink,
                            ),
                        )
                    }
                }
            }
            item {
                FormLabel("周次")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberWheel(
                        label = "开始周",
                        value = weekStart,
                        range = 1..totalWeeks,
                        itemText = { "第 $it 周" },
                        onValueChange = { weekStart = it.coerceAtMost(weekEnd) },
                        modifier = Modifier.weight(1f),
                    )
                    NumberWheel(
                        label = "结束周",
                        value = weekEnd,
                        range = 1..totalWeeks,
                        itemText = { "第 $it 周" },
                        onValueChange = { weekEnd = it.coerceAtLeast(weekStart) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                FormLabel("节次")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberWheel(
                        label = "开始节",
                        value = startPeriod,
                        range = periods.first().number..periods.last().number,
                        itemText = { "第 $it 节" },
                        onValueChange = { startPeriod = it.coerceAtMost(endPeriod) },
                        modifier = Modifier.weight(1f),
                    )
                    NumberWheel(
                        label = "结束节",
                        value = endPeriod,
                        range = periods.first().number..periods.last().number,
                        itemText = { "第 $it 节" },
                        onValueChange = { endPeriod = it.coerceAtLeast(startPeriod) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = KejingColors.Ink,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

@Composable
internal fun NumberWheel(
    label: String,
    value: Int,
    range: IntRange,
    itemText: (Int) -> String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = KejingColors.Ink.copy(alpha = 0.68f),
        )
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(116.dp)
                .clipToBounds(),
            factory = { context ->
                NumberPicker(ContextThemeWrapper(context, R.style.KejingNumberPicker)).apply {
                    descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                    wrapSelectorWheel = false
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    isVerticalFadingEdgeEnabled = false
                    setFadingEdgeLength(0)
                    overScrollMode = View.OVER_SCROLL_NEVER
                    clipChildren = true
                    setWheelTextColor(AndroidColor.BLACK)
                }
            },
            update = { picker ->
                val labels = range.map(itemText).toTypedArray()
                if (picker.minValue != range.first || picker.maxValue != range.last ||
                    picker.displayedValues?.contentEquals(labels) != true) {
                    picker.displayedValues = null
                    picker.minValue = range.first
                    picker.maxValue = range.last
                    picker.displayedValues = labels
                }
                if (picker.value != value.coerceIn(range)) picker.value = value.coerceIn(range)
                picker.setOnValueChangedListener { _, _, newValue -> onValueChange(newValue) }
            },
        )
    }
}

private fun NumberPicker.setWheelTextColor(color: Int) {
    findViewById<EditText>(resources.getIdentifier("number_picker_input", "id", "android"))
        ?.apply {
            setTextColor(color)
            setHintTextColor(color)
        }
    runCatching {
        NumberPicker::class.java.getDeclaredField("mTextColor").apply {
            isAccessible = true
            setInt(this@setWheelTextColor, color)
        }
        NumberPicker::class.java.getDeclaredField("mSelectorWheelPaint").apply {
            isAccessible = true
            (get(this@setWheelTextColor) as Paint).color = color
        }
    }
    invalidate()
}

@Composable
internal fun IndustrialTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        label = label,
        keyboardOptions = KeyboardOptions.Default,
        shape = KejingShape.Control,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = KejingColors.Ink,
            unfocusedTextColor = KejingColors.Ink,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedBorderColor = KejingColors.Ink,
            unfocusedBorderColor = KejingColors.Ink.copy(alpha = 0.45f),
            focusedLabelColor = KejingColors.Ink,
            unfocusedLabelColor = KejingColors.Ink.copy(alpha = 0.68f),
            cursorColor = KejingColors.Ink,
        ),
    )
}

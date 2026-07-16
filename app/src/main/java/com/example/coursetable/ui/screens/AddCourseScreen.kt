package com.example.coursetable.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.coursetable.core.design.KejingColors
import com.example.coursetable.core.design.KejingShape
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
    var name by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedDay by remember(uiState.selectedDay) { mutableStateOf(uiState.selectedDay) }
    var weekStart by remember(uiState.selectedWeek) { mutableStateOf(uiState.selectedWeek.toString()) }
    var weekEnd by remember(uiState.selectedWeek) { mutableStateOf(uiState.selectedWeek.toString()) }
    var startTimeText by remember { mutableStateOf("08:30") }
    var endTimeText by remember { mutableStateOf("10:05") }

    val parsedWeekStart = weekStart.toIntOrNull()
    val parsedWeekEnd = weekEnd.toIntOrNull()
    val parsedStartTime = startTimeText.parseTimeOrNull()
    val parsedEndTime = endTimeText.parseTimeOrNull()
    val canSave = name.isNotBlank() &&
        parsedWeekStart != null &&
        parsedWeekEnd != null &&
        parsedWeekStart in 1..totalWeeks.coerceAtLeast(1) &&
        parsedWeekEnd in parsedWeekStart..totalWeeks.coerceAtLeast(parsedWeekStart) &&
        parsedStartTime != null &&
        parsedEndTime != null &&
        parsedEndTime.isAfter(parsedStartTime)

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        containerColor = KejingColors.Ink,
        contentColor = KejingColors.Porcelain,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KejingColors.Ink,
                    titleContentColor = KejingColors.Porcelain,
                    navigationIconContentColor = KejingColors.Orange,
                    actionIconContentColor = KejingColors.Orange,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                title = {
                    Text(
                        text = "新增课程",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                actions = {
                    IconButton(
                        enabled = canSave,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = KejingColors.Orange,
                            disabledContentColor = KejingColors.Graphite,
                        ),
                        onClick = {
                            onSave(
                                name.trim(),
                                selectedDay,
                                parsedStartTime!!,
                                parsedEndTime!!,
                                location,
                                teacher,
                                parsedWeekStart!!..parsedWeekEnd!!,
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
                .padding(paddingValues),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                FormSection(title = "课程信息") {
                    Text(
                        text = "COURSE",
                        style = MaterialTheme.typography.labelMedium,
                        color = KejingColors.Orange,
                        fontWeight = FontWeight.SemiBold,
                    )
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
                FormSection(title = "星期") {
                    Text(
                        text = "DAY",
                        style = MaterialTheme.typography.labelMedium,
                        color = KejingColors.Orange,
                        fontWeight = FontWeight.SemiBold,
                    )
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
                                shape = KejingShape.Pill,
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = KejingColors.Black,
                                    labelColor = KejingColors.Porcelain,
                                    selectedContainerColor = KejingColors.Graphite,
                                    selectedLabelColor = KejingColors.Black,
                                ),
                            )
                        }
                    }
                }
            }
            item {
                FormSection(title = "周次和时间") {
                    Text(
                        text = "TIME",
                        style = MaterialTheme.typography.labelMedium,
                        color = KejingColors.Orange,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IndustrialTextField(
                            value = weekStart,
                            onValueChange = { weekStart = it.filter(Char::isDigit).take(2) },
                            modifier = Modifier.weight(1f),
                            label = { Text("开始周") },
                            supportingText = { Text("1-$totalWeeks") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = parsedWeekStart == null || parsedWeekStart !in 1..totalWeeks.coerceAtLeast(1),
                        )
                        IndustrialTextField(
                            value = weekEnd,
                            onValueChange = { weekEnd = it.filter(Char::isDigit).take(2) },
                            modifier = Modifier.weight(1f),
                            label = { Text("结束周") },
                            supportingText = { Text("可与开始周相同") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = parsedWeekEnd == null || parsedWeekStart == null || parsedWeekEnd < parsedWeekStart,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IndustrialTextField(
                            value = startTimeText,
                            onValueChange = { startTimeText = it.take(5) },
                            modifier = Modifier.weight(1f),
                            label = { Text("开始时间") },
                            supportingText = { Text("HH:mm") },
                            isError = parsedStartTime == null,
                        )
                        IndustrialTextField(
                            value = endTimeText,
                            onValueChange = { endTimeText = it.take(5) },
                            modifier = Modifier.weight(1f),
                            label = { Text("结束时间") },
                            supportingText = { Text("HH:mm") },
                            isError = parsedEndTime == null || parsedStartTime != null && !parsedEndTime.isAfter(parsedStartTime),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(KejingColors.Black, KejingShape.Widget)
            .border(1.dp, KejingColors.Graphite, KejingShape.Widget)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = KejingColors.Porcelain,
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
}

@Composable
private fun IndustrialTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        label = label,
        supportingText = supportingText,
        keyboardOptions = keyboardOptions,
        isError = isError,
        shape = KejingShape.Control,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = KejingColors.Porcelain,
            unfocusedTextColor = KejingColors.Porcelain,
            focusedContainerColor = KejingColors.Ink,
            unfocusedContainerColor = KejingColors.Ink,
            errorContainerColor = KejingColors.Ink,
            focusedBorderColor = KejingColors.Orange,
            unfocusedBorderColor = KejingColors.Graphite,
            errorBorderColor = KejingColors.Orange,
            focusedLabelColor = KejingColors.Orange,
            unfocusedLabelColor = KejingColors.Graphite,
            errorLabelColor = KejingColors.Orange,
            focusedSupportingTextColor = KejingColors.Graphite,
            unfocusedSupportingTextColor = KejingColors.Graphite,
            errorSupportingTextColor = KejingColors.Orange,
            cursorColor = KejingColors.Orange,
            errorCursorColor = KejingColors.Orange,
        ),
    )
}

private fun String.parseTimeOrNull(): LocalTime? {
    val parts = split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    return runCatching { LocalTime.of(hour, minute) }.getOrNull()
}

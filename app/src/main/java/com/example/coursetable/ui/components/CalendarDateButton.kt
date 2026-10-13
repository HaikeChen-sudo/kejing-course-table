package com.example.coursetable.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.coursetable.core.design.KejingColors
import com.example.coursetable.core.design.KejingShape
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarDateButton(label: String, date: LocalDate?, onSelect: (LocalDate) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, shape = KejingShape.Control) {
        Text("$label：${date?.format(DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.CHINA)) ?: "点击设置"}")
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = (date ?: LocalDate.now()).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(colors = DatePickerDefaults.colors(containerColor = KejingColors.Paper), tonalElevation = 0.dp,
            shape = KejingShape.Widget, onDismissRequest = { open = false }, confirmButton = {
            TextButton(enabled = state.selectedDateMillis != null, onClick = {
                state.selectedDateMillis?.let { onSelect(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                open = false
            }) { Text("确定") }
        }, dismissButton = { TextButton(onClick = { open = false }) { Text("取消") } }) {
            DatePicker(state = state, colors = DatePickerDefaults.colors(
                containerColor = KejingColors.Paper,
                selectedDayContainerColor = KejingColors.Orange,
                selectedDayContentColor = KejingColors.Ink,
                selectedYearContainerColor = KejingColors.Orange,
                selectedYearContentColor = KejingColors.Ink,
                todayDateBorderColor = KejingColors.Ink,
                todayContentColor = KejingColors.Ink,
            ))
        }
    }
}

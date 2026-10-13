package com.example.coursetable.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.ui.text.font.FontWeight
import com.example.coursetable.core.design.KejingColors
import com.example.coursetable.core.design.KejingShape
import androidx.compose.ui.unit.dp
import com.example.coursetable.domain.model.*
import com.example.coursetable.ui.components.CalendarDateButton
import com.example.coursetable.core.design.kejingGradientBackground
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveCourseScreen(semester: SemesterSchedule?, course: Course?, onBack: () -> Unit,
    onSave: suspend (String, String, LocalDate, LocalTime, LocalTime, String) -> String?) {
    if (semester == null || course == null) {
        Column(Modifier.padding(24.dp)) { Text("课程已不存在"); TextButton(onClick = onBack) { Text("返回") } }
        return
    }
    var dateText by rememberSaveable(course.id) { mutableStateOf((semester.dateOf(course) ?: LocalDate.now()).toString()) }
    var startText by rememberSaveable(course.id) { mutableStateOf(course.startTime.toString()) }
    var endText by rememberSaveable(course.id) { mutableStateOf(course.endTime.toString()) }
    val periods = CoursePeriodSchedule.periods
    var startPeriod by rememberSaveable(course.id) { mutableIntStateOf(periods.minBy { kotlin.math.abs(it.start.toSecondOfDay() - course.startTime.toSecondOfDay()) }.number) }
    var endPeriod by rememberSaveable(course.id) { mutableIntStateOf(periods.minBy { kotlin.math.abs(it.end.toSecondOfDay() - course.endTime.toSecondOfDay()) }.number.coerceAtLeast(startPeriod)) }
    var location by rememberSaveable(course.id) { mutableStateOf(course.location) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun updatePeriods(first: Int, last: Int) {
        startPeriod = first
        endPeriod = last
        startText = periods.first { it.number == first }.start.toString()
        endText = periods.first { it.number == last }.end.toString()
    }
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent, topBar = {
        TopAppBar(colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent), title = { Text("移动这次课程") }, navigationIcon = {
            IconButton(onClick = onBack, enabled = !saving) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
            }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().kejingGradientBackground().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(course.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("原安排：${semester.dateOf(course) ?: "日期未设置"}  ${course.startTime}–${course.endTime}")
            Text("仅移动这一次，保留名称、教师和颜色，其他日期的课程不变。")
            CalendarDateButton("目标日期", LocalDate.parse(dateText)) { dateText = it.toString() }
            Text("节次", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberWheel("开始节", startPeriod, periods.first().number..endPeriod,
                    { "第 $it 节" }, { if (!saving) updatePeriods(it, endPeriod) }, Modifier.weight(1f))
                NumberWheel("结束节", endPeriod, startPeriod..periods.last().number,
                    { "第 $it 节" }, { if (!saving) updatePeriods(startPeriod, it) }, Modifier.weight(1f))
            }
            Text("$startText–$endText", style = MaterialTheme.typography.bodySmall)
            if (LocalTime.parse(startText) != periods.first { it.number == startPeriod }.start ||
                LocalTime.parse(endText) != periods.first { it.number == endPeriod }.end) {
                Text("原课时间不完全对应标准节次。仅改日期或地点会保留原时间；调整节次后使用标准时间。", style = MaterialTheme.typography.bodySmall)
            }
            IndustrialTextField(value = location, onValueChange = { if (!saving) location = it }, label = { Text("上课地点") }, modifier = Modifier.fillMaxWidth())
            val date = LocalDate.parse(dateText)
            val start = LocalTime.parse(startText)
            val end = LocalTime.parse(endText)
            val conflicts = semester.coursesOn(date).filter { it.id != course.id && start < it.endTime && end > it.startTime }
            if (conflicts.isNotEmpty()) Text("时间与 ${conflicts.joinToString { it.name }} 重叠，请确认安排。", color = MaterialTheme.colorScheme.error)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = KejingShape.Control,
                colors = ButtonDefaults.buttonColors(containerColor = KejingColors.Orange, contentColor = KejingColors.Ink),
                enabled = !saving, onClick = {
                saving = true
                scope.launch {
                    error = onSave(semester.id, course.id, date, start, end, location)
                    saving = false
                    if (error == null) onBack()
                }
            }) { Text(if (saving) "保存中…" else "确认移动") }
        }
    }
}

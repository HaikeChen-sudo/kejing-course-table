package com.example.coursetable.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coursetable.core.design.KejingColors
import com.example.coursetable.core.design.KejingShape
import com.example.coursetable.core.design.kejingGradientBackground
import com.example.coursetable.core.design.palette
import com.example.coursetable.domain.model.Course
import com.example.coursetable.ui.components.EmptyState
import com.example.coursetable.ui.components.zhShortName
import java.time.format.DateTimeFormatter

private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    course: Course?,
    onBack: () -> Unit,
    onDelete: (String) -> Unit,
    onMove: () -> Unit = {},
    date: java.time.LocalDate? = null,
) {
    var showDeleteDialog by remember(course?.id) { mutableStateOf(false) }

    if (showDeleteDialog && course != null) {
        DeleteCourseDialog(
            courseName = course.name,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                onDelete(course.id)
            },
        )
    }

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
                title = {
                    Text(
                        text = "课程详情",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
        },
    ) { paddingValues ->
        if (course == null) {
            EmptyState(
                title = "没有找到这门课",
                subtitle = "课程可能已经被删除或当前学期已切换",
                iconColor = KejingColors.Ink,
                titleColor = KejingColors.Ink,
                subtitleColor = KejingColors.Ink.copy(alpha = 0.68f),
                modifier = Modifier
                    .fillMaxSize()
                    .kejingGradientBackground()
                    .padding(paddingValues)
                    .padding(20.dp),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .kejingGradientBackground()
                .padding(paddingValues),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                CourseHero(course = course)
            }
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DetailRow(label = "上课时间", value = course.timeText())
                    date?.let { DetailRow(label = "日期", value = it.toString()) }
                    if (course.scheduledDate == null) DetailRow(label = "周次", value = course.weeksText())
                    DetailRow(label = "上课地点", value = course.location)
                    DetailRow(label = "任课教师", value = course.teacher)
                }
            }
            item {
                Button(onClick = onMove, modifier = Modifier.fillMaxWidth(), shape = KejingShape.Control,
                    colors = ButtonDefaults.buttonColors(containerColor = KejingColors.Orange, contentColor = KejingColors.Ink)) {
                    Text("调整这次课的日期、节次和地点")
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KejingColors.Orange,
                        contentColor = KejingColors.Black,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("删除这次课程")
                }
            }
        }
    }
}

@Composable
private fun CourseHero(course: Course) {
    val palette = course.colorSeed.palette()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = palette.container,
        contentColor = palette.content,
        shape = KejingShape.Widget,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = course.name,
                style = MaterialTheme.typography.headlineSmall,
                color = palette.accent,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
            )
            Text(
                text = "${course.dayOfWeek.zhShortName()} · ${course.startTime.format(TimeFormatter)}-${course.endTime.format(TimeFormatter)}",
                style = MaterialTheme.typography.titleMedium,
                color = palette.content,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = KejingColors.CanvasTop,
        contentColor = KejingColors.Ink,
        shape = KejingShape.Control,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(0.34f),
                style = MaterialTheme.typography.labelLarge,
                color = KejingColors.Ink.copy(alpha = 0.68f),
            )
            Text(
                text = value,
                modifier = Modifier.weight(0.66f),
                style = MaterialTheme.typography.bodyLarge,
                color = KejingColors.Ink,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun DeleteCourseDialog(
    courseName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KejingColors.CanvasBottom,
        titleContentColor = KejingColors.Ink,
        textContentColor = KejingColors.Ink,
        title = { Text("删除课程") },
        text = { Text("确定删除这一次「$courseName」吗？其他日期的同名课程保留。") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KejingColors.Orange,
                    contentColor = KejingColors.Black,
                ),
            ) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

private fun Course.timeText(): String {
    return "${dayOfWeek.zhShortName()} ${startTime.format(TimeFormatter)}-${endTime.format(TimeFormatter)}"
}

private fun Course.weeksText(): String {
    return if (weeks.first == weeks.last) {
        "第 ${weeks.first} 周"
    } else {
        "第 ${weeks.first}-${weeks.last} 周"
    }
}

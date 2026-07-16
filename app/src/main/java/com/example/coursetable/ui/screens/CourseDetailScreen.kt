package com.example.coursetable.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coursetable.core.design.KejingColors
import com.example.coursetable.core.design.KejingShape
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(20.dp),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
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
                    DetailRow(label = "周次", value = course.weeksText())
                    DetailRow(label = "上课地点", value = course.location)
                    DetailRow(label = "任课教师", value = course.teacher)
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
                    Text("删除课程")
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
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
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
        color = KejingColors.Black,
        contentColor = KejingColors.Porcelain,
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
                color = KejingColors.Graphite,
            )
            Text(
                text = value,
                modifier = Modifier.weight(0.66f),
                style = MaterialTheme.typography.bodyLarge,
                color = KejingColors.Porcelain,
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
        title = { Text("删除课程") },
        text = { Text("确定删除「$courseName」吗？删除后会从当前学期课表中移除。") },
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

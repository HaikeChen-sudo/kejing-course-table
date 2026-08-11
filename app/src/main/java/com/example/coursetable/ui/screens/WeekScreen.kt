package com.example.coursetable.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coursetable.core.design.KejingColors
import com.example.coursetable.core.design.KejingShape
import com.example.coursetable.core.design.palette
import com.example.coursetable.domain.model.Course
import com.example.coursetable.ui.ScheduleUiState
import com.example.coursetable.ui.components.zhShortName
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekScreen(
    uiState: ScheduleUiState,
    onSelectDay: (DayOfWeek) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onOpenAddCourse: () -> Unit,
    onOpenSemesterManager: () -> Unit,
    onOpenCourseDetail: (String) -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            onMessageShown()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        containerColor = Color.Transparent,
        contentColor = KejingColors.Ink,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = KejingColors.Ink,
                    actionIconContentColor = KejingColors.Ink,
                ),
                title = {
                    Text(
                        text = "第 ${uiState.selectedWeek} 周",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                actions = {
                    IconButton(onClick = onOpenAddCourse) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "新增课程",
                        )
                    }
                    IconButton(onClick = onOpenSemesterManager) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = "学期管理",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(KejingColors.CanvasTop, KejingColors.CanvasBottom),
                    ),
                )
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
        ) {
            TimelineGrid(
                uiState = uiState,
                onSelectDay = onSelectDay,
                onPreviousWeek = onPreviousWeek,
                onNextWeek = onNextWeek,
                onOpenCourseDetail = onOpenCourseDetail,
            )
        }
    }
}

@Composable
private fun TimelineGrid(
    uiState: ScheduleUiState,
    onSelectDay: (DayOfWeek) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onOpenCourseDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val verticalScroll = rememberScrollState()
    var horizontalDrag by remember { mutableFloatStateOf(0f) }
    val timeColumnWidth = 36.dp
    val slotHeight = 112.dp
    val periodSlots = StandardPeriodSlots
    val totalHeight = slotHeight * periodSlots.size.toFloat()

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Spacer(Modifier.width(timeColumnWidth))
            DayOfWeek.entries.forEach { day ->
                val date = uiState.weekStartDate.plusDays(
                    (day.value - DayOfWeek.MONDAY.value).toLong(),
                )
                val isToday = date == LocalDate.now()
                val selected = isToday || day == uiState.selectedDay
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectDay(day) }
                        .padding(vertical = 7.dp, horizontal = 1.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = day.zhShortName(),
                        style = MaterialTheme.typography.labelMedium,
                        color = KejingColors.Ink.copy(alpha = if (selected) 1f else 0.68f),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${date.monthValue}/${date.dayOfMonth}",
                        style = MaterialTheme.typography.labelSmall,
                        color = KejingColors.Ink.copy(alpha = if (selected) 1f else 0.62f),
                        fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(uiState.selectedWeek) {
                    detectHorizontalDragGestures(
                        onDragStart = { horizontalDrag = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            horizontalDrag += dragAmount
                        },
                        onDragEnd = {
                            when {
                                horizontalDrag >= 72f -> onPreviousWeek()
                                horizontalDrag <= -72f -> onNextWeek()
                            }
                        },
                    )
                }
                .verticalScroll(verticalScroll),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeight),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                TimeColumn(
                    width = timeColumnWidth,
                    totalHeight = totalHeight,
                    slotHeight = slotHeight,
                    periodSlots = periodSlots,
                )
                DayOfWeek.entries.forEach { day ->
                    DayTimelineColumn(
                        courses = uiState.weekCourses[day].orEmpty(),
                        totalHeight = totalHeight,
                        slotHeight = slotHeight,
                        periodSlots = periodSlots,
                        onOpenCourseDetail = onOpenCourseDetail,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeColumn(
    width: Dp,
    totalHeight: Dp,
    slotHeight: Dp,
    periodSlots: List<PeriodSlot>,
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(totalHeight),
    ) {
        periodSlots.forEachIndexed { index, slot ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(slotHeight)
                    .offset(y = slotHeight * index.toFloat())
                    .padding(top = 6.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = slot.start.format(TimeFormatter),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        lineHeight = 10.sp,
                    ),
                    textAlign = TextAlign.End,
                    color = KejingColors.Ink,
                )
                Text(
                    text = slot.end.format(TimeFormatter),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        lineHeight = 10.sp,
                    ),
                    textAlign = TextAlign.End,
                    color = KejingColors.Ink.copy(alpha = 0.62f),
                )
            }
        }
    }
}

@Composable
private fun DayTimelineColumn(
    courses: List<Course>,
    totalHeight: Dp,
    slotHeight: Dp,
    periodSlots: List<PeriodSlot>,
    onOpenCourseDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .height(totalHeight)
            .dashedHorizontalLines(slotHeight = slotHeight, rowCount = periodSlots.size),
    ) {
        layoutOverlappingCourses(courses).forEach { item ->
            val course = item.course
            val startSlot = periodSlots.firstOverlappingSlotIndex(course) ?: return@forEach
            val slotSpan = periodSlots.overlappingSlotSpan(course).coerceAtLeast(1)
            val itemWidth = maxWidth / item.columnCount
            CourseBlock(
                course = course,
                detailLevel = when {
                    slotSpan >= 2 -> CourseDetailLevel.Full
                    else -> CourseDetailLevel.Medium
                },
                onClick = { onOpenCourseDetail(course.id) },
                modifier = Modifier
                    .width(itemWidth)
                    .height(slotHeight * slotSpan.toFloat())
                    .padding(horizontal = 1.dp, vertical = 2.dp)
                    .offset(
                        x = itemWidth * item.column,
                        y = slotHeight * startSlot.toFloat(),
                    ),
            )
        }
    }
}

@Composable
private fun CourseBlock(
    course: Course,
    detailLevel: CourseDetailLevel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = course.colorSeed.palette()
    Column(
        modifier = modifier
            .background(palette.container, KejingShape.Control)
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = course.name.toCompactCourseName(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                lineHeight = 12.sp,
            ),
            fontWeight = FontWeight.SemiBold,
            color = palette.content,
            maxLines = if (detailLevel == CourseDetailLevel.Full) 3 else 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (detailLevel == CourseDetailLevel.Full && course.teacher != "未识别") {
            Text(
                text = course.teacher,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    lineHeight = 9.sp,
                ),
                color = palette.support,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (detailLevel == CourseDetailLevel.Full || course.location.length <= 8) {
            Text(
                text = course.location.removePrefix("曹妃甸校区"),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    lineHeight = 9.sp,
                ),
                color = palette.support,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private enum class CourseDetailLevel {
    Medium,
    Full,
}

private data class PositionedCourse(
    val course: Course,
    val column: Int,
    val columnCount: Int,
)

private fun layoutOverlappingCourses(courses: List<Course>): List<PositionedCourse> {
    if (courses.isEmpty()) return emptyList()
    val groups = mutableListOf<MutableList<Course>>()
    var currentGroup = mutableListOf<Course>()
    var groupEnd: LocalTime? = null

    courses.sortedWith(compareBy<Course> { it.startTime }.thenBy { it.endTime }).forEach { course ->
        val currentEnd = groupEnd
        if (currentGroup.isEmpty() || currentEnd == null || course.startTime < currentEnd) {
            currentGroup += course
            groupEnd = listOfNotNull(groupEnd, course.endTime).maxOrNull()
        } else {
            groups += currentGroup
            currentGroup = mutableListOf(course)
            groupEnd = course.endTime
        }
    }
    if (currentGroup.isNotEmpty()) groups += currentGroup

    return groups.flatMap { group ->
        val columnEnds = mutableListOf<LocalTime>()
        val placed = group.map { course ->
            val reusableColumn = columnEnds.indexOfFirst { end -> !course.startTime.isBefore(end) }
            val column = if (reusableColumn >= 0) {
                columnEnds[reusableColumn] = course.endTime
                reusableColumn
            } else {
                columnEnds += course.endTime
                columnEnds.lastIndex
            }
            course to column
        }
        val columnCount = columnEnds.size.coerceAtLeast(1)
        placed.map { (course, column) ->
            PositionedCourse(
                course = course,
                column = column,
                columnCount = columnCount,
            )
        }
    }
}

private data class PeriodSlot(
    val start: LocalTime,
    val end: LocalTime,
)

private val StandardPeriodSlots = listOf(
    PeriodSlot(LocalTime.of(8, 30), LocalTime.of(9, 15)),
    PeriodSlot(LocalTime.of(9, 20), LocalTime.of(10, 5)),
    PeriodSlot(LocalTime.of(10, 25), LocalTime.of(11, 10)),
    PeriodSlot(LocalTime.of(11, 15), LocalTime.of(12, 0)),
    PeriodSlot(LocalTime.of(14, 0), LocalTime.of(14, 45)),
    PeriodSlot(LocalTime.of(14, 50), LocalTime.of(15, 35)),
    PeriodSlot(LocalTime.of(15, 55), LocalTime.of(16, 40)),
    PeriodSlot(LocalTime.of(16, 45), LocalTime.of(17, 30)),
)

private fun List<PeriodSlot>.firstOverlappingSlotIndex(course: Course): Int? {
    return indexOfFirst { slot ->
        course.startTime.isBefore(slot.end) && course.endTime.isAfter(slot.start)
    }.takeIf { it >= 0 }
}

private fun List<PeriodSlot>.overlappingSlotSpan(course: Course): Int {
    return count { slot ->
        course.startTime.isBefore(slot.end) && course.endTime.isAfter(slot.start)
    }
}

private fun String.toCompactCourseName(): String {
    return replace(Regex("_[0-9A-Za-z-]+$"), "")
}

private fun Modifier.dashedHorizontalLines(slotHeight: Dp, rowCount: Int): Modifier {
    return drawBehind {
        val dash = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 3.dp.toPx()))
        val strokeWidth = 0.5.dp.toPx()
        repeat(rowCount + 1) { index ->
            val y = slotHeight.toPx() * index
            drawLine(
                color = KejingColors.Ink.copy(alpha = 0.42f),
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(size.width, y),
                strokeWidth = strokeWidth,
                pathEffect = dash,
            )
        }
    }
}

package com.example.coursetable.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
import com.example.coursetable.ui.components.SemesterPicker
import com.example.coursetable.ui.components.zhShortName
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DateFormatter = DateTimeFormatter.ofPattern("M/d")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekScreen(
    uiState: ScheduleUiState,
    onSelectDay: (DayOfWeek) -> Unit,
    onSelectSemester: (String) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onResetToCurrentWeek: () -> Unit,
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
        containerColor = KejingColors.Ink,
        contentColor = KejingColors.Porcelain,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KejingColors.Ink,
                    titleContentColor = KejingColors.Porcelain,
                    actionIconContentColor = KejingColors.Orange,
                    navigationIconContentColor = KejingColors.Orange,
                ),
                title = {
                    SemesterPicker(
                        semesters = uiState.semesters,
                        selectedSemester = uiState.selectedSemester,
                        onSelectSemester = onSelectSemester,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
        ) {
            WeekSwitcher(
                uiState = uiState,
                onPreviousWeek = onPreviousWeek,
                onNextWeek = onNextWeek,
                onResetToCurrentWeek = onResetToCurrentWeek,
            )
            TimelineGrid(
                uiState = uiState,
                onSelectDay = onSelectDay,
                onOpenCourseDetail = onOpenCourseDetail,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun WeekSwitcher(
    uiState: ScheduleUiState,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onResetToCurrentWeek: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .shadow(14.dp, KejingShape.Widget, clip = false)
            .background(KejingColors.Black, KejingShape.Widget)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPreviousWeek) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = "上一周",
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "第 ${uiState.selectedWeek} 周",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = KejingColors.Orange,
            )
            Text(
                text = "${uiState.weekStartDate.format(DateFormatter)}-${uiState.weekStartDate.plusDays(6).format(DateFormatter)} · ${uiState.selectedSemester?.displayName.orEmpty()}",
                style = MaterialTheme.typography.labelSmall,
                color = KejingColors.Paper,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onResetToCurrentWeek) {
                Text("本周")
            }
            IconButton(onClick = onNextWeek) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "下一周",
                )
            }
        }
    }
}

@Composable
private fun TimelineGrid(
    uiState: ScheduleUiState,
    onSelectDay: (DayOfWeek) -> Unit,
    onOpenCourseDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val verticalScroll = rememberScrollState()
    val timeColumnWidth = 30.dp
    val slotHeight = 96.dp
    val semesterCourses = uiState.selectedSemester?.courses.orEmpty()
    val periodSlots = remember(semesterCourses) { semesterCourses.toCompressedPeriodSlots() }
    val totalHeight = slotHeight * periodSlots.size.toFloat()

    Column(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Spacer(Modifier.width(timeColumnWidth))
            DayOfWeek.entries.forEach { day ->
                val date = uiState.weekStartDate.plusDays((day.value - DayOfWeek.MONDAY.value).toLong())
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
                        color = if (selected) KejingColors.Orange else KejingColors.Porcelain,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = date.format(DateFormatter),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) KejingColors.Orange else KejingColors.Paper,
                        fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
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
                    color = KejingColors.Porcelain,
                )
                Text(
                    text = slot.end.format(TimeFormatter),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        lineHeight = 10.sp,
                    ),
                    textAlign = TextAlign.End,
                    color = KejingColors.Paper,
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
            .shadow(6.dp, KejingShape.Control, clip = false)
            .background(KejingColors.Track, KejingShape.Control)
    ) {
        periodSlots.forEachIndexed { index, _ ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .offset(y = slotHeight * index.toFloat())
                    .background(KejingColors.Graphite.copy(alpha = 0.2f)),
            )
        }
        layoutOverlappingCourses(courses).forEach { item ->
            val course = item.course
            val startSlot = periodSlots.firstOverlappingSlotIndex(course) ?: return@forEach
            val slotSpan = periodSlots.overlappingSlotSpan(course).coerceAtLeast(1)
            val itemWidth = maxWidth / item.columnCount
            CourseBlock(
                course = course,
                detailLevel = when {
                    slotSpan >= 2 -> CourseDetailLevel.Full
                    slotHeight >= 78.dp -> CourseDetailLevel.Medium
                    else -> CourseDetailLevel.Compact
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
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = palette.container,
        contentColor = palette.content,
        shape = KejingShape.Control,
        tonalElevation = 0.dp,
        shadowElevation = 5.dp,
    ) {
        Column(
            modifier = Modifier
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
                maxLines = when (detailLevel) {
                    CourseDetailLevel.Full -> 3
                    CourseDetailLevel.Medium -> 2
                    CourseDetailLevel.Compact -> 3
                },
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
            if (detailLevel != CourseDetailLevel.Compact || course.location.length <= 8) {
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
}

private enum class CourseDetailLevel {
    Compact,
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

private val DefaultPeriodSlots = listOf(
    PeriodSlot(LocalTime.of(8, 0), LocalTime.of(9, 40)),
    PeriodSlot(LocalTime.of(10, 0), LocalTime.of(11, 40)),
    PeriodSlot(LocalTime.of(14, 0), LocalTime.of(15, 40)),
    PeriodSlot(LocalTime.of(16, 0), LocalTime.of(17, 40)),
    PeriodSlot(LocalTime.of(19, 0), LocalTime.of(20, 40)),
)

private fun List<Course>.toCompressedPeriodSlots(): List<PeriodSlot> {
    if (isEmpty()) return DefaultPeriodSlots

    val courseSlots = mapNotNull { course ->
        if (course.endTime.isAfter(course.startTime)) {
            PeriodSlot(course.startTime, course.endTime)
        } else {
            null
        }
    }
        .distinct()
        .sortedWith(compareBy<PeriodSlot> { it.start }.thenBy { it.end })

    if (courseSlots.isEmpty()) return DefaultPeriodSlots

    return courseSlots
        .filterNot { slot ->
            courseSlots.any { other ->
                other != slot &&
                    !other.start.isBefore(slot.start) &&
                    !other.end.isAfter(slot.end) &&
                    (other.start != slot.start || other.end != slot.end)
            }
        }
        .ifEmpty { DefaultPeriodSlots }
        .take(12)
}

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

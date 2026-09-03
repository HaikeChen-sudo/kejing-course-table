package com.example.coursetable.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import com.example.coursetable.core.design.KejingColors
import com.example.coursetable.core.design.KejingShape
import com.example.coursetable.core.design.palette
import com.example.coursetable.domain.model.Course
import com.example.coursetable.domain.model.CoursePeriodSchedule
import com.example.coursetable.ui.ScheduleUiState
import com.example.coursetable.ui.components.zhShortName
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlin.math.abs

private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val RealtimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekScreen(
    uiState: ScheduleUiState,
    onSelectDay: (DayOfWeek) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onResetToCurrentWeek: () -> Unit,
    onOpenAddCourse: () -> Unit,
    onOpenSemesterManager: () -> Unit,
    onOpenCourseDetail: (String) -> Unit,
    onMessageShown: () -> Unit,
    importSuccessMessage: String? = null,
    onImportSuccessMessageShown: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("week_display", Context.MODE_PRIVATE)
    }
    var courseTextScale by rememberSaveable {
        mutableFloatStateOf(preferences.getFloat("course_text_scale", 1f))
    }
    var courseCardHeight by rememberSaveable {
        mutableFloatStateOf(preferences.getFloat("course_card_height", 84f))
    }
    var isDisplaySettingsOpen by rememberSaveable { mutableStateOf(false) }
    // 网格时间低频刷新（5 秒一次），避免整棵时间网格每秒重组
    val gridNow = rememberGridTime()
    val backgroundBrush = remember {
        Brush.verticalGradient(listOf(KejingColors.CanvasTop, KejingColors.CanvasBottom))
    }
    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarMessage = message
            onMessageShown()
        }
    }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            snackbarMessage = null
        }
    }
    LaunchedEffect(importSuccessMessage) {
        importSuccessMessage?.let { message ->
            onImportSuccessMessageShown()
            snackbarHostState.showSnackbar(message)
        }
    }

    if (isDisplaySettingsOpen) {
        WeekDisplaySettingsDialog(
            courseTextScale = courseTextScale,
            courseCardHeight = courseCardHeight,
            onTextScaleChange = {
                courseTextScale = it
                preferences.edit().putFloat("course_text_scale", it).apply()
            },
            onCardHeightChange = {
                courseCardHeight = it
                preferences.edit().putFloat("course_card_height", it).apply()
            },
            onDismiss = { isDisplaySettingsOpen = false },
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        containerColor = Color.Transparent,
        contentColor = KejingColors.Ink,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = KejingColors.Ink,
                    actionIconContentColor = KejingColors.Ink,
                ),
                title = {
                    Column {
                        Text(
                            text = "第 ${uiState.selectedWeek} 周",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        WeekClockLabel()
                    }
                },
                actions = {
                    if (uiState.selectedWeek != uiState.currentWeek) {
                        IconButton(onClick = onResetToCurrentWeek) {
                            Icon(
                                imageVector = Icons.Rounded.Today,
                                contentDescription = "回到本周",
                            )
                        }
                    }
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
                    IconButton(onClick = { isDisplaySettingsOpen = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "调节课表显示",
                        )
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackbarData ->
                Snackbar(
                    snackbarData = snackbarData,
                    containerColor = KejingColors.CanvasBottom,
                    contentColor = KejingColors.Ink,
                    actionColor = KejingColors.Ink,
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(paddingValues)
                .padding(horizontal = 8.dp),
        ) {
            TimelineGrid(
                uiState = uiState,
                now = gridNow,
                onSelectDay = onSelectDay,
                onPreviousWeek = onPreviousWeek,
                onNextWeek = onNextWeek,
                onOpenCourseDetail = onOpenCourseDetail,
                courseTextScale = courseTextScale,
                courseCardHeight = courseCardHeight,
            )
        }
    }
}

@Composable
private fun WeekDisplaySettingsDialog(
    courseTextScale: Float,
    courseCardHeight: Float,
    onTextScaleChange: (Float) -> Unit,
    onCardHeightChange: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KejingColors.CanvasBottom,
        titleContentColor = KejingColors.Ink,
        textContentColor = KejingColors.Ink,
        title = { Text("课表显示") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("课程字号 ${"%.0f".format(courseTextScale * 100)}%")
                Slider(
                    value = courseTextScale,
                    onValueChange = onTextScaleChange,
                    valueRange = 0.8f..1.5f,
                    steps = 6,
                )
                Text("课程卡片长度 ${courseCardHeight.toInt()} dp")
                Slider(
                    value = courseCardHeight,
                    onValueChange = onCardHeightChange,
                    valueRange = 64f..120f,
                    steps = 6,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onTextScaleChange(1f)
                    onCardHeightChange(84f)
                },
            ) {
                Text("恢复默认")
            }
        },
    )
}

@Composable
private fun TimelineGrid(
    uiState: ScheduleUiState,
    now: LocalDateTime,
    onSelectDay: (DayOfWeek) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onOpenCourseDetail: (String) -> Unit,
    courseTextScale: Float,
    courseCardHeight: Float,
    modifier: Modifier = Modifier,
) {
    val verticalScroll = rememberScrollState()
    val today = now.toLocalDate()
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var pageWidth by remember { mutableIntStateOf(0) }
    val canGoToPreviousWeek by rememberUpdatedState(uiState.selectedWeek > 1)
    val canGoToNextWeek by rememberUpdatedState(
        uiState.selectedWeek < (uiState.selectedSemester?.totalWeeks ?: 1),
    )
    val weekDragState = rememberDraggableState { delta ->
        val limit = pageWidth.toFloat()
        if (limit > 0f) {
            val nextOffset = (dragOffset + delta).coerceIn(-limit, limit)
            dragOffset = when {
                nextOffset > 0f && !canGoToPreviousWeek -> 0f
                nextOffset < 0f && !canGoToNextWeek -> 0f
                else -> nextOffset
            }
        }
    }
    val timeColumnWidth = 30.dp
    val slotHeight = courseCardHeight.dp
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
                val isToday = date == today
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
                        color = KejingColors.Ink.copy(alpha = if (isToday) 1f else 0.38f),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${date.monthValue}/${date.dayOfMonth}",
                        style = MaterialTheme.typography.labelSmall,
                        color = KejingColors.Ink.copy(alpha = if (isToday) 1f else 0.34f),
                        fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { pageWidth = it.width }
                .draggable(
                    state = weekDragState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        val width = pageWidth.toFloat()
                        val shouldChangeWeek = abs(dragOffset) >= width * 0.16f || abs(velocity) >= 550f
                        if (!shouldChangeWeek || width == 0f) {
                            animate(
                                initialValue = dragOffset,
                                targetValue = 0f,
                                animationSpec = tween(80),
                            ) { value, _ -> dragOffset = value }
                            return@draggable
                        }

                        val direction = if (dragOffset == 0f) {
                            if (velocity > 0f) 1 else -1
                        } else {
                            if (dragOffset > 0f) 1 else -1
                        }
                        val canChangeWeek = if (direction > 0) {
                            canGoToPreviousWeek
                        } else {
                            canGoToNextWeek
                        }
                        if (!canChangeWeek) {
                            animate(
                                initialValue = dragOffset,
                                targetValue = 0f,
                                animationSpec = tween(80),
                            ) { value, _ -> dragOffset = value }
                            return@draggable
                        }
                        animate(
                            initialValue = dragOffset,
                            targetValue = direction * width,
                            animationSpec = tween(55),
                        ) { value, _ -> dragOffset = value }
                        if (direction > 0) onPreviousWeek() else onNextWeek()
                        dragOffset = -direction * width
                        animate(
                            initialValue = dragOffset,
                            targetValue = 0f,
                            animationSpec = tween(95),
                        ) { value, _ -> dragOffset = value }
                    },
                )
        ) {
            // 七天共用一次约束测量，避免每列分别触发子组合布局。
            val dayWidth = ((maxWidth - timeColumnWidth - 14.dp) / 7).coerceAtLeast(0.dp)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationX = dragOffset }
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
                            date = uiState.weekStartDate.plusDays(
                                (day.value - DayOfWeek.MONDAY.value).toLong(),
                            ),
                            now = now,
                            today = today,
                            courses = uiState.weekCourses[day].orEmpty(),
                            totalHeight = totalHeight,
                            slotHeight = slotHeight,
                            periodSlots = periodSlots,
                            onOpenCourseDetail = onOpenCourseDetail,
                            courseTextScale = courseTextScale,
                            columnWidth = dayWidth,
                            modifier = Modifier.width(dayWidth),
                        )
                    }
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
    date: LocalDate,
    now: LocalDateTime,
    today: LocalDate,
    courses: List<Course>,
    totalHeight: Dp,
    slotHeight: Dp,
    periodSlots: List<PeriodSlot>,
    onOpenCourseDetail: (String) -> Unit,
    courseTextScale: Float,
    columnWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val positionedCourses = remember(courses) { layoutOverlappingCourses(courses) }
    Box(
        modifier = modifier
            .height(totalHeight)
            .then(
                if (date == today) {
                    Modifier.background(
                        Brush.verticalGradient(
                            listOf(Color.White, KejingColors.Paper),
                        ),
                    )
                } else {
                    Modifier
                },
            )
            .dashedHorizontalLines(slotHeight = slotHeight, rowCount = periodSlots.size),
    ) {
        positionedCourses.forEach { item ->
            val course = item.course
            val startSlot = periodSlots.firstOverlappingSlotIndex(course) ?: return@forEach
            val slotSpan = periodSlots.overlappingSlotSpan(course).coerceAtLeast(1)
            val itemWidth = columnWidth / item.columnCount
            CourseBlock(
                course = course,
                status = course.weekStatusAt(date, now),
                onClick = { onOpenCourseDetail(course.id) },
                textScale = courseTextScale,
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
    status: WeekCourseStatus,
    onClick: () -> Unit,
    textScale: Float,
    modifier: Modifier = Modifier,
) {
    val palette = remember(course.colorSeed) { course.colorSeed.palette() }
    val hasDepth = status !is WeekCourseStatus.Completed
    val thickness = 3.dp
    // 进度条在课程块内动画自驱：网格 5 秒刷新一次，动画把中间时间补平，保持平滑
    val progress = remember(course.id) { Animatable(0f) }
    LaunchedEffect(status) {
        when (status) {
            is WeekCourseStatus.Active -> {
                val totalMs = java.time.Duration.between(course.startTime, course.endTime)
                    .toMillis()
                    .coerceAtLeast(1)
                progress.snapTo(status.elapsedFraction)
                val remainingMs = (totalMs * (1f - status.elapsedFraction)).toInt().coerceAtLeast(1)
                progress.animateTo(1f, animationSpec = tween(remainingMs, easing = LinearEasing))
            }
            else -> progress.snapTo(0f)
        }
    }
    val containerColor = when (status) {
        is WeekCourseStatus.Completed -> KejingColors.Graphite
        is WeekCourseStatus.Active -> palette.container
        is WeekCourseStatus.Upcoming -> palette.container.copy(alpha = 0.68f)
    }
    val contentColor = when (status) {
        is WeekCourseStatus.Completed -> KejingColors.Porcelain.copy(alpha = 0.66f)
        is WeekCourseStatus.Active -> palette.content
        is WeekCourseStatus.Upcoming -> palette.content.copy(alpha = 0.82f)
    }
    val supportColor = contentColor.copy(alpha = 0.82f)
    Box(
        modifier = modifier,
    ) {
        if (hasDepth) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = thickness)
                    .background(
                        color = KejingColors.Graphite.copy(alpha = 0.66f),
                        shape = KejingShape.Control,
                    ),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (hasDepth) thickness else 0.dp)
                .background(containerColor, KejingShape.Control)
                .clickable(onClick = onClick),
        ) {
            if (hasDepth) {
                CourseMaterialOverlay(
                    palette = palette,
                    status = status,
                    hasDepth = hasDepth,
                    progress = progress.value,
                    modifier = Modifier.matchParentSize(),
                )
            }
            Column(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = course.name.toCompactCourseName(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp * textScale,
                        lineHeight = 12.sp * textScale,
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                )
                if (course.teacher != "未识别") {
                    Text(
                        text = course.teacher,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp * textScale,
                            lineHeight = 9.sp * textScale,
                        ),
                        color = supportColor,
                    )
                }
                Text(
                    text = course.location.removePrefix("曹妃甸校区"),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp * textScale,
                        lineHeight = 9.sp * textScale,
                    ),
                    color = supportColor,
                )
            }
        }
    }
}

@Composable
private fun CourseMaterialOverlay(
    palette: com.example.coursetable.core.design.CoursePalette,
    status: WeekCourseStatus,
    hasDepth: Boolean,
    progress: Float,
    modifier: Modifier,
) {
    Spacer(
        modifier = modifier.drawWithCache {
            val corner = 6.dp.toPx()
            // 静态装饰（渐变、斜纹、圆角描边）只构建一次，随尺寸变化才重建
            val gradientBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.22f),
                    Color.Transparent,
                    palette.accent.copy(alpha = 0.08f),
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height),
            )
            val stripeStep = 14.dp.toPx()
            val stripesPath = Path().apply {
                var stripe = -size.height
                while (stripe < size.width) {
                    moveTo(stripe, 0f)
                    lineTo(stripe + size.height, size.height)
                    stripe += stripeStep
                }
            }
            val progressPath = Path().apply {
                addRoundRect(
                    RoundRect(Rect(Offset.Zero, size), CornerRadius(corner, corner)),
                )
            }
            onDrawBehind {
                if (hasDepth) {
                    drawRoundRect(
                        brush = gradientBrush,
                        cornerRadius = CornerRadius(corner, corner),
                    )
                    drawPath(
                        path = stripesPath,
                        color = Color.White.copy(alpha = 0.045f),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.3f),
                        cornerRadius = CornerRadius(corner, corner),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.38f),
                        start = Offset(corner, 0f),
                        end = Offset(size.width - corner, 0f),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                if (status is WeekCourseStatus.Active) {
                    // 裁圆角，让黑色进度块的顶部两角跟随课程块圆角
                    val progressY = size.height * progress
                    drawContext.canvas.save()
                    drawContext.canvas.clipPath(progressPath)
                    drawRect(
                        color = KejingColors.Black.copy(alpha = 0.88f),
                        size = Size(size.width, progressY),
                    )
                    drawLine(
                        color = palette.accent,
                        start = Offset(0f, progressY),
                        end = Offset(size.width, progressY),
                        strokeWidth = 1.5.dp.toPx(),
                    )
                    drawContext.canvas.restore()
                }
            }
        },
    )
}

private sealed interface WeekCourseStatus {
    data object Completed : WeekCourseStatus
    data class Active(val elapsedFraction: Float) : WeekCourseStatus
    data object Upcoming : WeekCourseStatus
}

private fun Course.weekStatusAt(date: LocalDate, now: LocalDateTime): WeekCourseStatus {
    val today = now.toLocalDate()
    if (date.isBefore(today)) return WeekCourseStatus.Completed
    if (date.isAfter(today)) return WeekCourseStatus.Upcoming

    val currentTime = now.toLocalTime()
    if (!currentTime.isBefore(endTime)) return WeekCourseStatus.Completed
    if (currentTime.isBefore(startTime)) return WeekCourseStatus.Upcoming
    val totalSeconds = java.time.Duration.between(startTime, endTime).seconds.coerceAtLeast(1)
    val elapsedSeconds = java.time.Duration.between(startTime, currentTime).seconds.coerceAtLeast(0)
    return WeekCourseStatus.Active(
        elapsedFraction = (elapsedSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f),
    )
}

@Composable
private fun rememberWeekCurrentTime(): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1_000)
        }
    }
    return now
}

/** 顶部时钟：每秒刷新，独立组件，避免带动整个页面重组。 */
@Composable
private fun WeekClockLabel() {
    val now = rememberWeekCurrentTime()
    Text(
        text = now.format(RealtimeFormatter),
        style = MaterialTheme.typography.labelSmall,
        color = KejingColors.Ink.copy(alpha = 0.66f),
        fontWeight = FontWeight.SemiBold,
    )
}

/** 课程进度由卡片自身连续动画驱动，网格只需低频校时。 */
@Composable
private fun rememberGridTime(): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(30_000)
        }
    }
    return now
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

private val StandardPeriodSlots = CoursePeriodSchedule.periods.map { period ->
    PeriodSlot(period.start, period.end)
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

private fun Modifier.dashedHorizontalLines(slotHeight: Dp, rowCount: Int): Modifier {
    return drawWithCache {
        val dash = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 3.dp.toPx()))
        val strokeWidth = 0.5.dp.toPx()
        onDrawBehind {
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
}

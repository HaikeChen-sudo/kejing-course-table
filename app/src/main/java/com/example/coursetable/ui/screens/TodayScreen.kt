package com.example.coursetable.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coursetable.R
import com.example.coursetable.core.design.KejingColors
import com.example.coursetable.core.design.KejingShape
import com.example.coursetable.core.design.palette
import com.example.coursetable.ui.ScheduleUiState
import com.example.coursetable.ui.components.EmptyState
import com.example.coursetable.domain.model.Course
import com.example.coursetable.domain.model.SemesterSchedule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.layout.Layout
import com.example.coursetable.domain.model.weekAt

private val ClockFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
private val CourseTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun TodayScreen(
    uiState: ScheduleUiState,
    onOpenCourseDetail: (String) -> Unit,
    onMessageShown: () -> Unit,
    onTitleBounds: (Rect) -> Unit = {},
    contentRevealTick: Int = 0,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    // 启动内容 staggered fade-in：只播一次；切 tab 返回不重播（rememberSaveable 锁存）
    var revealPlayed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(contentRevealTick) {
        if (contentRevealTick > 0 && !revealPlayed) revealPlayed = true
    }
    val now = rememberCurrentTime()
    val nowDate = now.toLocalDate()
    val selectedSemester = uiState.selectedSemester
    // 今日课程按实时系统时间计算，改系统时间后立即生效
    val todayCourses = remember(uiState.coursesByDate, nowDate) {
        uiState.coursesByDate[nowDate].orEmpty()
    }
    val currentWeek = currentWeekFor(selectedSemester, nowDate) ?: uiState.currentWeek
    val backgroundBrush = remember {
        Brush.verticalGradient(listOf(KejingColors.CanvasTop, KejingColors.CanvasBottom))
    }
    val statusTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
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

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        containerColor = Color.Transparent,
        contentColor = KejingColors.Ink,
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = statusTopPadding + 14.dp,
                end = 20.dp,
                bottom = 14.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                TodayHeader(
                    uiState = uiState,
                    currentWeek = currentWeek,
                    now = now,
                    onTitleBounds = onTitleBounds,
                    contentRevealTrigger = contentRevealTick,
                    revealPlayed = revealPlayed,
                )
            }
            if (todayCourses.isEmpty()) {
                item {
                    StaggeredReveal(
                        trigger = contentRevealTick,
                        played = revealPlayed,
                        delayMs = 50L,
                    ) {
                        EmptyState(
                            title = "今天没有课程",
                            subtitle = "可以去本周页面查看完整安排",
                            iconColor = KejingColors.Ink,
                            titleColor = KejingColors.Ink,
                            subtitleColor = KejingColors.Ink.copy(alpha = 0.68f),
                        )
                    }
                }
            } else {
                itemsIndexed(todayCourses, key = { _, course -> course.id }) { index, course ->
                    StaggeredReveal(
                        trigger = contentRevealTick,
                        played = revealPlayed,
                        delayMs = 50L + index * 45L,
                    ) {
                        TodayCourseCard(
                            course = course,
                            now = now.toLocalTime(),
                            onClick = { onOpenCourseDetail(course.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayHeader(
    uiState: ScheduleUiState,
    currentWeek: Int,
    now: LocalDateTime,
    onTitleBounds: (Rect) -> Unit,
    contentRevealTrigger: Int,
    revealPlayed: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    modifier = Modifier.onGloballyPositioned { onTitleBounds(it.boundsInWindow()) },
                    style = MaterialTheme.typography.displaySmall,
                    color = KejingColors.Ink,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                )
                StaggeredReveal(
                    trigger = contentRevealTrigger,
                    played = revealPlayed,
                    delayMs = 0L,
                ) {
                    Text(
                        text = uiState.selectedSemester?.displayName ?: "暂无学期",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KejingColors.Ink.copy(alpha = 0.68f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            StaggeredReveal(
                trigger = contentRevealTrigger,
                played = revealPlayed,
                delayMs = 40L,
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = now.format(ClockFormatter),
                        color = KejingColors.Ink,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "第 $currentWeek 周",
                        color = KejingColors.Ink.copy(alpha = 0.66f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayCourseCard(
    course: Course,
    now: LocalTime,
    onClick: () -> Unit,
) {
    val state = course.timeStateAt(now)
    val completed = state is TodayCourseState.Completed
    var expanded by rememberSaveable(course.id, completed) { mutableStateOf(false) }
    val compact = completed && !expanded
    // palette 按颜色种子缓存，避免每秒时钟刷新时整卡重复计算配色
    val palette = remember(course.colorSeed) { course.colorSeed.palette() }
    val containerColor = when (state) {
        is TodayCourseState.Completed -> KejingColors.Black
        is TodayCourseState.Active -> palette.container
        is TodayCourseState.Upcoming -> palette.container.copy(alpha = 0.58f)
    }
    val contentColor = when (state) {
        is TodayCourseState.Completed -> KejingColors.Porcelain.copy(alpha = 0.72f)
        is TodayCourseState.Active -> palette.content
        is TodayCourseState.Upcoming -> palette.content.copy(alpha = 0.82f)
    }

    Surface(
        onClick = { if (completed) expanded = !expanded else onClick() },
        modifier = Modifier.fillMaxWidth(),
        color = containerColor,
        contentColor = contentColor,
        shape = KejingShape.Card,
        border = if (state is TodayCourseState.Active) {
            BorderStroke(1.dp, palette.accent.copy(alpha = 0.8f))
        } else {
            null
        },
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box {
            if (state is TodayCourseState.Active) {
                Spacer(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(KejingShape.Card)
                        .drawWithCache {
                            // 圆角 path 只构建一次，避免每帧重建
                            val cardRadius = 10.dp.toPx() // 对应 KejingShape.Card 的圆角
                            val progressPath = Path().apply {
                                addRoundRect(
                                    RoundRect(
                                        rect = androidx.compose.ui.geometry.Rect(
                                            androidx.compose.ui.geometry.Offset.Zero,
                                            size,
                                        ),
                                        cornerRadius = CornerRadius(cardRadius, cardRadius),
                                    ),
                                )
                            }
                            onDrawBehind {
                                // Canvas 内再裁一次圆角，保证黑色进度块的顶部两角是圆角
                                val progressY = size.height * state.elapsedFraction
                                drawContext.canvas.save()
                                drawContext.canvas.clipPath(progressPath)
                                drawRect(
                                    color = KejingColors.Black.copy(alpha = 0.9f),
                                    size = androidx.compose.ui.geometry.Size(size.width, progressY),
                                )
                                drawLine(
                                    color = palette.accent,
                                    start = androidx.compose.ui.geometry.Offset(0f, progressY),
                                    end = androidx.compose.ui.geometry.Offset(size.width, progressY),
                                    strokeWidth = 2.dp.toPx(),
                                )
                                drawContext.canvas.restore()
                            }
                        },
                )
            }
            TodayCourseContent(
                course = course,
                contentColor = contentColor,
                statusLabel = course.statusLabelAt(now, state),
                compact = compact,
            )
        }
    }
}

@Composable
private fun TodayCourseContent(
    course: Course,
    contentColor: Color,
    statusLabel: String?,
    compact: Boolean,
) {
    val progress = animateFloatAsState(if (compact) 1f else 0f, tween(240), label = "completed course")
    // 字体只排版一次；缩放与平移在图层上进行，高度只由这一处动画控制。
    Layout(content = {
            Text(course.name, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                color = contentColor, maxLines = if (compact) 1 else 3, overflow = TextOverflow.Ellipsis)
            val formatter = remember { DateTimeFormatter.ofPattern("H:mm") }
            Text("${course.startTime.format(formatter)}–${course.endTime.format(formatter)}",
                fontSize = 14.sp, color = contentColor)
            Column(modifier = if (compact) Modifier.clearAndSetSemantics {} else Modifier,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                statusLabel?.let { Text(it, color = contentColor, style = MaterialTheme.typography.labelMedium) }
                TodayMetadataRow(Icons.Rounded.LocationOn, course.location, contentColor)
                TodayMetadataRow(Icons.Rounded.Person, course.teacher, contentColor.copy(alpha = 0.84f))
            }
        }, modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp).fillMaxWidth().clipToBounds()) { measurables, constraints ->
            val width = constraints.maxWidth
            val time = measurables[1].measure(constraints.copy(minWidth = 0, minHeight = 0))
            val gap = 12.dp.roundToPx()
            val title = measurables[0].measure(constraints.copy(minWidth = 0, minHeight = 0,
                maxWidth = if (compact) ((width - time.width * 12f / 14f - gap) / (15f / 22f)).toInt().coerceAtLeast(0) else width))
            val details = measurables[2].measure(constraints.copy(minWidth = 0, minHeight = 0))
            val p = progress.value
            val titleScale = 1f - p * (1f - 15f / 22f)
            val timeScale = 1f - p * (1f - 12f / 14f)
            val headerHeight = title.height + 4.dp.roundToPx() + time.height
            val expandedHeight = headerHeight + 8.dp.roundToPx() + details.height
            val compactHeight = maxOf(title.height * 15f / 22f, time.height * 12f / 14f)
            val height = (expandedHeight + (compactHeight - expandedHeight) * p).toInt()
            layout(width, height) {
                title.placeRelativeWithLayer(0, ((compactHeight - title.height * titleScale) / 2f * p).toInt()) {
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = titleScale; scaleY = titleScale
                }
                time.placeRelativeWithLayer(((width - time.width * timeScale) * p).toInt(),
                    ((title.height + 4.dp.roundToPx()) * (1f - p) + (compactHeight - time.height * timeScale) / 2f * p).toInt()) {
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = timeScale; scaleY = timeScale
                }
                if (p < 1f) details.placeRelativeWithLayer(0, headerHeight + 8.dp.roundToPx()) { alpha = 1f - p }
            }
        }
}

@Composable
private fun TodayMetadataRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.padding(1.dp), tint = color)
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

/** 启动内容 staggered fade-in：纯透明度，220ms 快速顺滑，无位移/缩放。
 *  trigger > 0 才播放；played 为 true 时直接全显（切 tab 返回不重播）。 */
@Composable
private fun StaggeredReveal(
    trigger: Int,
    played: Boolean,
    delayMs: Long,
    content: @Composable () -> Unit,
) {
    val alpha = remember { Animatable(if (played) 1f else 0f) }
    LaunchedEffect(trigger) {
        if (played) {
            alpha.snapTo(1f)
        } else if (trigger > 0) {
            delay(delayMs)
            alpha.animateTo(1f, animationSpec = tween(220, easing = FastOutSlowInEasing))
        }
    }
    Box(Modifier.graphicsLayer { this.alpha = alpha.value }) { content() }
}

private sealed interface TodayCourseState {
    data object Completed : TodayCourseState
    data class Active(val elapsedFraction: Float) : TodayCourseState
    data object Upcoming : TodayCourseState
}

private fun Course.timeStateAt(now: LocalTime): TodayCourseState {
    if (!now.isBefore(endTime)) return TodayCourseState.Completed
    if (now.isBefore(startTime)) return TodayCourseState.Upcoming
    val totalSeconds = java.time.Duration.between(startTime, endTime).seconds.coerceAtLeast(1)
    val elapsedSeconds = java.time.Duration.between(startTime, now).seconds.coerceAtLeast(0)
    return TodayCourseState.Active((elapsedSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f))
}

private fun Course.statusLabelAt(now: LocalTime, state: TodayCourseState): String? {
    return when (state) {
        TodayCourseState.Completed -> "已结束"
        is TodayCourseState.Active -> null
        TodayCourseState.Upcoming -> {
            val minutes = java.time.Duration.between(now, startTime).toMinutes().coerceAtLeast(0)
            "还有 ${minutes / 60}小时${minutes % 60}分"
        }
    }
}

@Composable
private fun rememberCurrentTime(): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1_000)
        }
    }
    return now
}

private fun currentWeekFor(semester: SemesterSchedule?, date: LocalDate): Int? {
    return semester?.weekAt(date)
}

package com.example.coursetable.ui

import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.coursetable.ui.screens.AddCourseScreen
import com.example.coursetable.ui.screens.CourseDetailScreen
import com.example.coursetable.ui.screens.PortalWebLoginScreen
import com.example.coursetable.ui.screens.SchoolSelectScreen
import com.example.coursetable.ui.screens.SemesterManagerScreen
import com.example.coursetable.ui.screens.TodayScreen
import com.example.coursetable.ui.screens.WeekScreen
import com.example.coursetable.ui.screens.MemoEditorScreen
import com.example.coursetable.ui.screens.MemoScreen
import com.example.coursetable.core.design.KejingColors
import com.example.coursetable.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed class AppRoute(val value: String, val label: String) {
    data object Today : AppRoute("today", "今日")
    data object Week : AppRoute("week", "本周")
    data object Memo : AppRoute("memo", "备忘录")
    data object MemoEditor : AppRoute("memo_editor/{memoId}", "编辑备忘录") {
        fun createRoute(memoId: String): String = "memo_editor/${Uri.encode(memoId)}"
    }
    data object AddCourse : AppRoute("add_course", "新增课程")
    data object CourseDetail : AppRoute("course_detail/{courseId}", "课程详情") {
        fun createRoute(courseId: String): String = "course_detail/${Uri.encode(courseId)}"
    }
    data object SemesterManager : AppRoute("semester_manager", "学期管理")
    data object SchoolSelect : AppRoute("school_select", "选择学校")
    data object PortalWebLogin : AppRoute("portal_web_login/{schoolCode}", "网页登录") {
        fun createRoute(schoolCode: String): String = "portal_web_login/${Uri.encode(schoolCode)}"
    }
}

private const val ImportSuccessMessageKey = "import_success_message"

@Composable
fun CourseTableApp(viewModel: ScheduleViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.importCompleted) {
        if (uiState.importCompleted) {
            val importSuccessMessage = uiState.message ?: "导入成功"
            navController.navigate(AppRoute.Week.value) {
                popUpTo(AppRoute.Today.value)
                launchSingleTop = true
            }
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.set(ImportSuccessMessageKey, importSuccessMessage)
            viewModel.consumeImportCompleted()
        }
    }
    val gradientBrush = remember {
        Brush.verticalGradient(listOf(KejingColors.CanvasTop, KejingColors.CanvasBottom))
    }
    // 启动过渡层：数据加载与标题动画并行，数据 ready 前不揭示课表内容。
    // 关键设计：Intro 标题全程不淡出，到位后与主页标题像素重合再整体移除（切换不可见）；
    // 内容揭示由渐隐 veil（与主页同背景色）完成，主页内容提前在 veil 下布局好。
    // rememberSaveable：旋转/重建后不再重播动画
    var splashVisible by rememberSaveable { mutableStateOf(true) }
    var homeTitleBounds by remember { mutableStateOf<Rect?>(null) }
    var splashTitleBounds by remember { mutableStateOf<Rect?>(null) }
    val splashProgress = remember { Animatable(0f) }
    val revealAlpha = remember { Animatable(1f) }
    // 内容揭示触发器：标题移动后半段置 1，TodayScreen 据此做克制 staggered fade-in
    var homeContentTick by remember { mutableStateOf(0) }
    // 用 rememberUpdatedState 让协程读到最新的数据状态，避免捕获旧快照
    val dataReady by rememberUpdatedState(uiState.selectedSemester != null)
    LaunchedEffect(homeTitleBounds, splashTitleBounds) {
        if (homeTitleBounds == null || splashTitleBounds == null) return@LaunchedEffect
        delay(120) // 阶段一：中央稳定，保证首帧
        // 阶段二：标题平移到主页标题位置（与数据加载并行，不等待数据）
        // 标题移动后半段触发首页内容 staggered fade-in（只改透明度，克制快速）
        launch {
            delay(190)
            homeContentTick++
        }
        splashProgress.animateTo(1f, animationSpec = tween(320, easing = FastOutSlowInEasing))
        // 阶段三：标题到位后等数据 ready（本地读取极快，兜底 1500ms 防异常卡死）
        var waitedMs = 0
        while (!dataReady && waitedMs < 1500) {
            delay(16)
            waitedMs += 16
        }
        // 阶段四：veil 渐隐揭示主页内容；标题不参与淡出，两层重叠后整体移除
        revealAlpha.animateTo(0f, animationSpec = tween(280, easing = FastOutSlowInEasing))
        delay(30)
        splashVisible = false
    }
    val bottomRoutes = listOf(AppRoute.Today, AppRoute.Week, AppRoute.Memo)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = bottomRoutes.any { route ->
        currentDestination?.hierarchy?.any { it.route == route.value } == true
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush),
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0.dp),
            containerColor = KejingColors.CanvasTop.copy(alpha = 0f),
            contentColor = KejingColors.Ink,
            bottomBar = {
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KejingColors.CanvasBottom)
                        .navigationBarsPadding()
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier
                            .width(322.dp)
                            .height(56.dp)
                            .shadow(5.dp, CircleShape, clip = false),
                        color = KejingColors.NavigationPill,
                        contentColor = KejingColors.Ink,
                        shape = CircleShape,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            bottomRoutes.forEach { route ->
                                val selected = currentDestination
                                    ?.hierarchy
                                    ?.any { it.route == route.value } == true
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (route == AppRoute.Today) {
                                                viewModel.resetToCurrentWeek()
                                            }
                                            val alreadyOnRoute = currentDestination
                                                ?.hierarchy
                                                ?.any { it.route == route.value } == true
                                            if (!alreadyOnRoute) {
                                                navController.navigate(route.value) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                        .padding(vertical = 5.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(1.dp),
                                ) {
                                    Icon(
                                        imageVector = when (route) {
                                            AppRoute.Today -> Icons.Rounded.Today
                                            AppRoute.Week -> Icons.Rounded.CalendarMonth
                                            AppRoute.Memo -> Icons.Rounded.Description
                                            AppRoute.MemoEditor -> Icons.Rounded.Description
                                            AppRoute.AddCourse -> Icons.Rounded.CalendarMonth
                                            AppRoute.CourseDetail -> Icons.Rounded.CalendarMonth
                                            AppRoute.SemesterManager -> Icons.Rounded.CalendarMonth
                                            AppRoute.SchoolSelect -> Icons.Rounded.CalendarMonth
                                            AppRoute.PortalWebLogin -> Icons.Rounded.CalendarMonth
                                        },
                                        contentDescription = route.label,
                                        tint = KejingColors.Ink.copy(alpha = if (selected) 1f else 0.56f),
                                    )
                                    Text(
                                        text = route.label,
                                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                        color = KejingColors.Ink.copy(alpha = if (selected) 1f else 0.56f),
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            },
        ) { innerPadding ->
            NavHost(
            navController = navController,
            startDestination = AppRoute.Today.value,
            modifier = Modifier
                .background(gradientBrush)
                .padding(innerPadding),
            enterTransition = { pushIn() },
            exitTransition = { pushOut() },
            popEnterTransition = { popIn() },
            popExitTransition = { popOut() },
            ) {
            composable(
                route = AppRoute.Today.value,
                enterTransition = { tabIn() },
                exitTransition = { tabOut() },
                popEnterTransition = { popIn() },
                popExitTransition = { popOut() },
            ) {
                TodayScreen(
                    uiState = uiState,
                    onOpenCourseDetail = { courseId ->
                        navController.navigate(AppRoute.CourseDetail.createRoute(courseId))
                    },
                    onMessageShown = viewModel::clearMessage,
                    onTitleBounds = { homeTitleBounds = it },
                    contentRevealTick = homeContentTick,
                )
            }
            composable(
                route = AppRoute.Week.value,
                enterTransition = { tabIn() },
                exitTransition = { tabOut() },
                popEnterTransition = { popIn() },
                popExitTransition = { popOut() },
            ) { entry ->
                val importSuccessMessage = remember(entry) {
                    entry.savedStateHandle.remove<String>(ImportSuccessMessageKey)
                }
                WeekScreen(
                    uiState = uiState,
                    onSelectDay = viewModel::selectDay,
                    onPreviousWeek = viewModel::previousWeek,
                    onNextWeek = viewModel::nextWeek,
                    onResetToCurrentWeek = viewModel::resetToCurrentWeek,
                    onOpenAddCourse = { navController.navigate(AppRoute.AddCourse.value) },
                    onOpenSemesterManager = { navController.navigate(AppRoute.SemesterManager.value) },
                    onOpenCourseDetail = { courseId ->
                        navController.navigate(AppRoute.CourseDetail.createRoute(courseId))
                    },
                    onMessageShown = viewModel::clearMessage,
                    importSuccessMessage = importSuccessMessage,
                )
            }
            composable(
                route = AppRoute.Memo.value,
                enterTransition = { tabIn() },
                exitTransition = { tabOut() },
                popEnterTransition = { popIn() },
                popExitTransition = { popOut() },
            ) {
                MemoScreen(
                    memos = uiState.memos,
                    onOpenMemo = { memoId ->
                        navController.navigate(AppRoute.MemoEditor.createRoute(memoId))
                    },
                    onCreateMemo = {
                        navController.navigate(AppRoute.MemoEditor.createRoute("new"))
                    },
                )
            }
            composable(
                route = AppRoute.MemoEditor.value,
                arguments = listOf(
                    navArgument("memoId") { type = NavType.StringType },
                ),
            ) { entry ->
                val memoId = entry.arguments?.getString("memoId").orEmpty()
                val memo = uiState.memos.firstOrNull { it.id == memoId }
                MemoEditorScreen(
                    memo = memo,
                    onBack = navController::popBackStack,
                    onSave = { title, content ->
                        viewModel.saveMemo(memo?.id, title, content)
                    },
                    onDelete = {
                        memo?.let { viewModel.deleteMemo(it.id) }
                        navController.popBackStack()
                    },
                )
            }
            composable(AppRoute.AddCourse.value) {
                AddCourseScreen(
                    uiState = uiState,
                    onBack = navController::popBackStack,
                    onSave = viewModel::addManualCourse,
                )
            }
            composable(AppRoute.SemesterManager.value) {
                SemesterManagerScreen(
                    uiState = uiState,
                    onBack = navController::popBackStack,
                    onSelectSemester = viewModel::selectSemester,
                    onImportClick = viewModel::openImportDialog,
                    onOpenPortalWebLogin = {
                        navController.navigate(AppRoute.SchoolSelect.value)
                    },
                    onDeleteSemester = viewModel::deleteSemester,
                    onDismissImport = viewModel::dismissImportDialog,
                    onImport = viewModel::syncFromPortal,
                    onMessageShown = viewModel::clearMessage,
                )
            }
            composable(
                route = AppRoute.CourseDetail.value,
                arguments = listOf(
                    navArgument("courseId") { type = NavType.StringType },
                ),
            ) { entry ->
                val courseId = entry.arguments?.getString("courseId").orEmpty()
                val course = uiState.selectedSemester?.courses?.firstOrNull { it.id == courseId }
                CourseDetailScreen(
                    course = course,
                    onBack = navController::popBackStack,
                    onDelete = { id ->
                        viewModel.deleteCourse(id)
                        navController.popBackStack()
                    },
                )
            }
            composable(AppRoute.SchoolSelect.value) {
                SchoolSelectScreen(
                    onBack = navController::popBackStack,
                    onSchoolSelected = { schoolCode ->
                        navController.navigate(AppRoute.PortalWebLogin.createRoute(schoolCode))
                    },
                )
            }
            composable(
                route = AppRoute.PortalWebLogin.value,
                arguments = listOf(
                    navArgument("schoolCode") { type = NavType.StringType },
                ),
            ) { entry ->
                val schoolCode = entry.arguments?.getString("schoolCode").orEmpty()
                PortalWebLoginScreen(
                    onBack = navController::popBackStack,
                    onCapturePage = { url, title, html ->
                        viewModel.captureManualPortalPage(url, title, html)
                        navController.popBackStack()
                    },
                    schoolCode = schoolCode,
                )
            }
            }
        }

        if (splashVisible) {
            SplashOverlay(
                gradientBrush = gradientBrush,
                homeTitleBounds = homeTitleBounds,
                splashTitleBounds = splashTitleBounds,
                splashProgress = splashProgress,
                revealAlpha = revealAlpha,
                onSplashTitleBounds = { splashTitleBounds = it },
            )
        }
    }
}

/** 启动过渡层：渐隐 veil 盖住主页内容，Coursia 标题居中并平移到主页标题位置，全程不淡出。
 *  动画值在 graphicsLayer 的 draw 阶段读取，只重绘不重组，避免带动整个 App 每帧重组。 */
@Composable
private fun SplashOverlay(
    gradientBrush: Brush,
    homeTitleBounds: Rect?,
    splashTitleBounds: Rect?,
    splashProgress: Animatable<Float, AnimationVector1D>,
    revealAlpha: Animatable<Float, AnimationVector1D>,
    onSplashTitleBounds: (Rect) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        // 内容遮罩 veil：与主页同背景色，渐隐后主页内容从下方露出；背景恒一致，无黑帧白帧
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
                .graphicsLayer {
                    alpha = revealAlpha.value
                },
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            // 与主页标题同款样式（displaySmall + Bold + Ink），纯平移与主页标题重合，切换不可见
            Text(
                text = stringResource(R.string.app_name),
                modifier = Modifier
                    .onGloballyPositioned { onSplashTitleBounds(it.boundsInWindow()) }
                    .graphicsLayer {
                        val dx = (homeTitleBounds?.left ?: 0f) - (splashTitleBounds?.left ?: 0f)
                        val dy = (homeTitleBounds?.top ?: 0f) - (splashTitleBounds?.top ?: 0f)
                        translationX = dx * splashProgress.value
                        translationY = dy * splashProgress.value
                    },
                style = MaterialTheme.typography.displaySmall,
                color = KejingColors.Ink,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            )
        }
    }
}

// 底栏切页不保留旧页面做淡入淡出，避免周课表和其他页面在同一帧重复绘制。
private fun pushIn(): EnterTransition = fadeIn(tween(140))
private fun pushOut(): ExitTransition = fadeOut(tween(100))
private fun popIn(): EnterTransition = fadeIn(tween(140))
private fun popOut(): ExitTransition = fadeOut(tween(100))
private fun tabIn(): EnterTransition = fadeIn(tween(140), initialAlpha = 0.72f)
private fun tabOut(): ExitTransition = fadeOut(tween(100), targetAlpha = 0.72f)

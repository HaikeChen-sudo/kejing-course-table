package com.example.coursetable.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
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
import com.example.coursetable.core.design.KejingColors
import com.example.coursetable.core.design.KejingShape

private sealed class AppRoute(val value: String, val label: String) {
    data object Today : AppRoute("today", "今日")
    data object Week : AppRoute("week", "本周")
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

@Composable
fun CourseTableApp(viewModel: ScheduleViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bottomRoutes = listOf(AppRoute.Today, AppRoute.Week)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = bottomRoutes.any { route ->
        currentDestination?.hierarchy?.any { it.route == route.value } == true
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = KejingColors.Ink,
        contentColor = KejingColors.Porcelain,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier
                        .background(KejingColors.Ink)
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .shadow(16.dp, KejingShape.Widget, clip = false),
                    containerColor = KejingColors.Black,
                    contentColor = KejingColors.Porcelain,
                    tonalElevation = 0.dp,
                ) {
                    bottomRoutes.forEach { route ->
                        val selected = currentDestination
                            ?.hierarchy
                            ?.any { it.route == route.value } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(route.value) {
                                    popUpTo(navController.graph.findStartDestination().id)
                                    launchSingleTop = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = when (route) {
                                        AppRoute.Today -> Icons.Rounded.Today
                                        AppRoute.Week -> Icons.Rounded.CalendarMonth
                                        AppRoute.AddCourse -> Icons.Rounded.CalendarMonth
                                        AppRoute.CourseDetail -> Icons.Rounded.CalendarMonth
                                        AppRoute.SemesterManager -> Icons.Rounded.CalendarMonth
                                        AppRoute.SchoolSelect -> Icons.Rounded.CalendarMonth
                                        AppRoute.PortalWebLogin -> Icons.Rounded.CalendarMonth
                                    },
                                    contentDescription = route.label,
                                )
                            },
                            label = { Text(route.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = KejingColors.Orange,
                                selectedTextColor = KejingColors.Orange,
                                indicatorColor = KejingColors.Black,
                                unselectedIconColor = KejingColors.Graphite,
                                unselectedTextColor = KejingColors.Graphite,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.Today.value,
            modifier = Modifier
                .background(KejingColors.Ink)
                .padding(innerPadding),
        ) {
            composable(AppRoute.Today.value) {
                TodayScreen(
                    uiState = uiState,
                    onOpenCourseDetail = { courseId ->
                        navController.navigate(AppRoute.CourseDetail.createRoute(courseId))
                    },
                    onMessageShown = viewModel::clearMessage,
                )
            }
            composable(AppRoute.Week.value) {
                WeekScreen(
                    uiState = uiState,
                    onSelectDay = viewModel::selectDay,
                    onSelectSemester = viewModel::selectSemester,
                    onPreviousWeek = viewModel::previousWeek,
                    onNextWeek = viewModel::nextWeek,
                    onResetToCurrentWeek = viewModel::resetToCurrentWeek,
                    onOpenAddCourse = {
                        navController.navigate(AppRoute.AddCourse.value)
                    },
                    onOpenSemesterManager = {
                        navController.navigate(AppRoute.SemesterManager.value)
                    },
                    onOpenCourseDetail = { courseId ->
                        navController.navigate(AppRoute.CourseDetail.createRoute(courseId))
                    },
                    onMessageShown = viewModel::clearMessage,
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
}

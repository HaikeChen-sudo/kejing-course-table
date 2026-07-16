package com.example.coursetable.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coursetable.R
import com.example.coursetable.core.design.KejingColors
import com.example.coursetable.ui.ScheduleUiState
import com.example.coursetable.ui.components.CourseCard
import com.example.coursetable.ui.components.EmptyState

@Composable
fun TodayScreen(
    uiState: ScheduleUiState,
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 30.dp),
            verticalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            item {
                TodayHeader(uiState = uiState)
            }
            if (uiState.todayCourses.isEmpty()) {
                item {
                    EmptyState(
                        title = "今天没有课程",
                        subtitle = "可以去本周页面查看完整安排",
                    )
                }
            } else {
                items(uiState.todayCourses, key = { it.id }) { course ->
                    CourseCard(
                        course = course,
                        onClick = { onOpenCourseDetail(course.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayHeader(uiState: ScheduleUiState) {
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
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displaySmall,
                    color = KejingColors.Porcelain,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                )
                Text(
                    text = uiState.selectedSemester?.displayName ?: "暂无学期",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KejingColors.Graphite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "第 ${uiState.selectedWeek} 周",
                color = KejingColors.Orange,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

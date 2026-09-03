package com.example.coursetable

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.view.WindowCompat
import com.example.coursetable.core.design.KejingColors
import com.example.coursetable.data.AppContainer
import com.example.coursetable.ui.CourseTableApp
import com.example.coursetable.ui.ScheduleViewModel
import com.example.coursetable.ui.theme.CourseTableTheme

class MainActivity : ComponentActivity() {
    private val appContainer by lazy { AppContainer(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                KejingColors.CanvasTop.toArgb(),
                KejingColors.CanvasTop.toArgb(),
            ),
            navigationBarStyle = SystemBarStyle.light(
                KejingColors.CanvasBottom.toArgb(),
                KejingColors.CanvasBottom.toArgb(),
            ),
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = KejingColors.CanvasTop.toArgb()
        window.navigationBarColor = KejingColors.CanvasBottom.toArgb()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            CourseTableTheme {
                val viewModel: ScheduleViewModel = viewModel(
                    factory = ScheduleViewModel.Factory(
                        repository = appContainer.scheduleRepository,
                        portalClient = appContainer.portalClient,
                        manualPortalPageStore = appContainer.manualPortalPageStore,
                        manualPortalScheduleParser = appContainer.manualPortalScheduleParser,
                        memoStore = appContainer.memoStore,
                    ),
                )
                CourseTableApp(viewModel = viewModel)
            }
        }
    }
}

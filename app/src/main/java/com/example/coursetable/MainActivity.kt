package com.example.coursetable

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coursetable.data.AppContainer
import com.example.coursetable.ui.CourseTableApp
import com.example.coursetable.ui.ScheduleViewModel
import com.example.coursetable.ui.theme.CourseTableTheme

class MainActivity : ComponentActivity() {
    private val appContainer by lazy { AppContainer(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(Color.BLACK),
        )
        setContent {
            CourseTableTheme {
                val viewModel: ScheduleViewModel = viewModel(
                    factory = ScheduleViewModel.Factory(
                        repository = appContainer.scheduleRepository,
                        portalClient = appContainer.portalClient,
                        manualPortalPageStore = appContainer.manualPortalPageStore,
                        manualPortalScheduleParser = appContainer.manualPortalScheduleParser,
                    ),
                )
                CourseTableApp(viewModel = viewModel)
            }
        }
    }
}

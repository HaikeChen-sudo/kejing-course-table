package com.example.coursetable.data

import android.content.Context
import com.example.coursetable.data.local.LocalScheduleFileStore
import com.example.coursetable.data.local.ManualPortalPageStore
import com.example.coursetable.data.remote.AcademicPortalClient
import com.example.coursetable.data.remote.AdapterBackedAcademicPortalClient
import com.example.coursetable.data.remote.DemoAcademicPortalAdapter
import com.example.coursetable.data.remote.ManualPortalScheduleParser
import com.example.coursetable.data.repository.FileBackedScheduleRepository
import com.example.coursetable.data.repository.ScheduleRepository

class AppContainer(context: Context) {
    private val fileStore = LocalScheduleFileStore(context)

    val scheduleRepository: ScheduleRepository = FileBackedScheduleRepository(fileStore)
    val manualPortalPageStore = ManualPortalPageStore(context)
    val manualPortalScheduleParser = ManualPortalScheduleParser()
    val portalClient: AcademicPortalClient = AdapterBackedAcademicPortalClient(
        adapters = listOf(DemoAcademicPortalAdapter()),
    )
}

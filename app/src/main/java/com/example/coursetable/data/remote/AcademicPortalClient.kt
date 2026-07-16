package com.example.coursetable.data.remote

import com.example.coursetable.domain.model.SemesterSchedule

interface AcademicPortalClient {
    suspend fun fetchCurrentSemester(request: PortalSyncRequest = PortalSyncRequest()): SemesterSchedule
}

class AdapterBackedAcademicPortalClient(
    private val adapters: List<AcademicPortalAdapter>,
    private val mapper: PortalScheduleMapper = PortalScheduleMapper(),
) : AcademicPortalClient {
    override suspend fun fetchCurrentSemester(request: PortalSyncRequest): SemesterSchedule {
        val adapter = adapters.firstOrNull { it.schoolCode == request.schoolCode }
            ?: error("未找到 ${request.schoolCode} 的教务系统适配器")
        return mapper.map(adapter.fetchSemester(request))
    }
}

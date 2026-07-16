package com.example.coursetable.data.remote

interface AcademicPortalAdapter {
    val schoolCode: String
    val schoolName: String

    suspend fun fetchSemester(request: PortalSyncRequest): PortalSemesterRecord
}

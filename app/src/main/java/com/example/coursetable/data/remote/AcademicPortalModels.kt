package com.example.coursetable.data.remote

import com.example.coursetable.domain.model.CourseColorSeed
import java.time.DayOfWeek
import java.time.LocalTime

data class PortalSyncRequest(
    val schoolCode: String = "demo",
    val account: String = "",
    val password: String = "",
)

data class PortalCourseRecord(
    val sourceId: String,
    val name: String,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val location: String,
    val teacher: String,
    val weekStart: Int,
    val weekEnd: Int,
    val colorSeed: CourseColorSeed,
)

data class PortalSemesterRecord(
    val sourceId: String,
    val displayName: String,
    val folderName: String,
    val currentWeek: Int,
    val totalWeeks: Int,
    val courses: List<PortalCourseRecord>,
)

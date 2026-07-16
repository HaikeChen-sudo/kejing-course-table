package com.example.coursetable.data.remote

import com.example.coursetable.domain.model.CourseColorSeed
import com.example.coursetable.domain.model.ScheduleDefaults
import java.time.DayOfWeek
import java.time.LocalTime
import kotlinx.coroutines.delay

class DemoAcademicPortalAdapter : AcademicPortalAdapter {
    override val schoolCode: String = "demo"
    override val schoolName: String = "示例教务系统"

    override suspend fun fetchSemester(request: PortalSyncRequest): PortalSemesterRecord {
        delay(800)
        return PortalSemesterRecord(
            sourceId = "portal-2026-spring",
            displayName = "2025-2026 春季学期",
            folderName = "2025-2026_spring",
            currentWeek = 12,
            totalWeeks = ScheduleDefaults.TOTAL_WEEKS,
            courses = listOf(
                PortalCourseRecord(
                    sourceId = "portal-ai",
                    name = "人工智能导论",
                    dayOfWeek = DayOfWeek.MONDAY,
                    startTime = LocalTime.of(8, 30),
                    endTime = LocalTime.of(10, 5),
                    location = "致远楼 A302",
                    teacher = "周老师",
                    weekStart = 1,
                    weekEnd = 16,
                    colorSeed = CourseColorSeed.Cyan,
                ),
                PortalCourseRecord(
                    sourceId = "portal-mobile",
                    name = "移动应用开发",
                    dayOfWeek = DayOfWeek.WEDNESDAY,
                    startTime = LocalTime.of(14, 0),
                    endTime = LocalTime.of(15, 35),
                    location = "实验楼 508",
                    teacher = "陈老师",
                    weekStart = 1,
                    weekEnd = 18,
                    colorSeed = CourseColorSeed.Violet,
                ),
            ),
        )
    }
}

package com.example.coursetable.data.remote

import com.example.coursetable.domain.model.Course
import com.example.coursetable.domain.model.ScheduleDefaults
import com.example.coursetable.domain.model.SemesterSchedule

class PortalScheduleMapper {
    fun map(record: PortalSemesterRecord): SemesterSchedule {
        return SemesterSchedule(
            id = record.sourceId,
            displayName = record.displayName,
            folderName = record.folderName,
            currentWeek = record.currentWeek,
            totalWeeks = record.totalWeeks.coerceAtLeast(ScheduleDefaults.TOTAL_WEEKS),
            courses = record.courses.map { course ->
                Course(
                    id = course.sourceId,
                    name = course.name.trim(),
                    dayOfWeek = course.dayOfWeek,
                    startTime = course.startTime,
                    endTime = course.endTime,
                    location = course.location.trim(),
                    teacher = course.teacher.trim(),
                    weeks = course.weekStart..course.weekEnd,
                    colorSeed = course.colorSeed,
                )
            },
        )
    }
}

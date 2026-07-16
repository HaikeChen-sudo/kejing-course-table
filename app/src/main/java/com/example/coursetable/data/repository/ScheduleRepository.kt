package com.example.coursetable.data.repository

import com.example.coursetable.domain.model.ImportMode
import com.example.coursetable.domain.model.Course
import com.example.coursetable.domain.model.SemesterSchedule
import kotlinx.coroutines.flow.StateFlow

interface ScheduleRepository {
    val schedules: StateFlow<List<SemesterSchedule>>
    val selectedSemesterId: StateFlow<String?>

    fun selectSemester(id: String)
    suspend fun importSchedule(schedule: SemesterSchedule, mode: ImportMode)
    suspend fun addCourseToSelected(course: Course): Boolean
    suspend fun deleteCourseFromSelected(courseId: String): Boolean
    suspend fun deleteSchedule(id: String): Boolean
}

package com.example.coursetable.domain.model

import java.time.LocalDate

data class SemesterSchedule(
    val id: String,
    val displayName: String,
    val folderName: String,
    val currentWeek: Int,
    val totalWeeks: Int,
    val semesterStartDate: LocalDate? = null,
    val courses: List<Course>,
    val customStartDate: Boolean = false,
)

enum class ImportMode {
    OverwriteCurrent,
    CreateNewFolder,
}

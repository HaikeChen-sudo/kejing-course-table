package com.example.coursetable.domain.model

import java.time.DayOfWeek
import java.time.LocalTime

data class Course(
    val id: String,
    val name: String,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val location: String,
    val teacher: String,
    val weeks: IntRange,
    val colorSeed: CourseColorSeed,
)

enum class CourseColorSeed {
    Coral,
    Mint,
    Amber,
    Violet,
    Cyan,
    Lime,
}

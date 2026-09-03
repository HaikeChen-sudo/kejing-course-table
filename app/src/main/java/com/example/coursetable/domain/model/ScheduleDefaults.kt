package com.example.coursetable.domain.model

import java.time.LocalTime

object ScheduleDefaults {
    const val TOTAL_WEEKS: Int = 21
}

data class CoursePeriod(
    val number: Int,
    val start: LocalTime,
    val end: LocalTime,
)

object CoursePeriodSchedule {
    val periods = listOf(
        CoursePeriod(1, LocalTime.of(8, 30), LocalTime.of(9, 15)),
        CoursePeriod(2, LocalTime.of(9, 20), LocalTime.of(10, 5)),
        CoursePeriod(3, LocalTime.of(10, 25), LocalTime.of(11, 10)),
        CoursePeriod(4, LocalTime.of(11, 15), LocalTime.of(12, 0)),
        CoursePeriod(5, LocalTime.of(14, 0), LocalTime.of(14, 45)),
        CoursePeriod(6, LocalTime.of(14, 50), LocalTime.of(15, 35)),
        CoursePeriod(7, LocalTime.of(15, 55), LocalTime.of(16, 40)),
        CoursePeriod(8, LocalTime.of(16, 45), LocalTime.of(17, 30)),
        CoursePeriod(9, LocalTime.of(19, 0), LocalTime.of(19, 45)),
        CoursePeriod(10, LocalTime.of(19, 50), LocalTime.of(20, 35)),
    )
}

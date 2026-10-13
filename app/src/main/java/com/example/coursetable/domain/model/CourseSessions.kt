package com.example.coursetable.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

// 保留导入来源 ID；同一课程的各周拥有稳定 ID，重复读取不会再次拆分。
fun Course.sessions(): List<Course> = if (weeks.first == weeks.last) listOf(this) else
    weeks.map { week -> copy(id = "$id::week:$week", weeks = week..week) }

fun SemesterSchedule.withIndividualSessions(): SemesterSchedule =
    if (courses.all { it.weeks.first == it.weeks.last }) this
    else copy(courses = courses.flatMap(Course::sessions))

fun SemesterSchedule.firstMonday(): LocalDate? =
    semesterStartDate?.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun SemesterSchedule.dateOf(course: Course): LocalDate? = course.scheduledDate
    ?: firstMonday()?.plusWeeks((course.weeks.first - 1).toLong())
        ?.plusDays((course.dayOfWeek.value - 1).toLong())

fun SemesterSchedule.weekAt(date: LocalDate): Int {
    val start = semesterStartDate ?: return currentWeek
    if (date < start) return 0
    return (ChronoUnit.WEEKS.between(firstMonday(), date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))) + 1).toInt()
}

fun SemesterSchedule.coursesOn(date: LocalDate): List<Course> = courses.filter {
    dateOf(it) == date && (it.scheduledDate != null || semesterStartDate == null || date >= semesterStartDate)
}.sortedBy { it.startTime }

fun SemesterSchedule.moveSession(id: String, date: LocalDate, start: LocalTime, end: LocalTime, location: String): SemesterSchedule {
    require(end > start) { "结束时间必须晚于开始时间" }
    val monday = requireNotNull(firstMonday()) { "请先设置开学日期" }
    require(date >= requireNotNull(semesterStartDate) && date < monday.plusWeeks(totalWeeks.toLong())) { "请选择本学期内的日期" }
    require(courses.any { it.id == id }) { "课程已不存在" }
    val week = (ChronoUnit.WEEKS.between(monday, date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))) + 1).toInt()
    return copy(courses = courses.map {
        if (it.id != id) it else it.copy(scheduledDate = date, dayOfWeek = date.dayOfWeek,
            startTime = start, endTime = end, weeks = week..week, location = location)
    })
}

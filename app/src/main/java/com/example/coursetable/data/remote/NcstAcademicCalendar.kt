package com.example.coursetable.data.remote

import com.example.coursetable.domain.model.SemesterSchedule
import java.time.LocalDate

private val knownNcstTermStarts = mapOf(
    "2025-2026-1" to LocalDate.of(2025, 9, 1),
    "2025-2026-2" to LocalDate.of(2026, 3, 2),
    "2026-2027-1" to LocalDate.of(2026, 8, 31),
)

internal fun ScheduleParseResult.withNcstAcademicCalendar(
    snapshot: PortalPageSnapshot,
): ScheduleParseResult {
    val schedule = schedule ?: return this
    if (snapshot.readAcademicTermMetadata().semesterStartDate != null) return this
    val startDate = snapshot.ncstTermKey()?.let(knownNcstTermStarts::get) ?: return this
    return copy(
        schedule = schedule.copy(semesterStartDate = startDate),
        diagnostics = diagnostics + "Applied NCST academic calendar",
    )
}

internal fun SemesterSchedule.withKnownNcstTermStartDate(): SemesterSchedule {
    if (customStartDate) return this
    if (!folderName.startsWith("manual_ncst-portal")) return this
    val startDate = displayName.ncstTermKey()?.let(knownNcstTermStarts::get) ?: return this
    val firstCourseWeek = firstScheduledWeek()
    val repairedWeek = when {
        currentWeek >= totalWeeks -> firstCourseWeek
        currentWeek == 1 && firstCourseWeek > 1 -> firstCourseWeek
        else -> currentWeek
    }
    return if (semesterStartDate == startDate && repairedWeek == currentWeek) {
        this
    } else {
        copy(semesterStartDate = startDate, currentWeek = repairedWeek)
    }
}

internal fun ScheduleParseResult.withNcstHistoricalSemesterDefaults(
    snapshot: PortalPageSnapshot,
): ScheduleParseResult {
    val schedule = schedule ?: return this
    if (!snapshot.url.contains("/calendarSemesterCurriculum/")) return this
    if (snapshot.readAcademicTermMetadata().currentWeek != null) return this
    return copy(
        schedule = schedule.copy(currentWeek = schedule.firstScheduledWeek()),
        diagnostics = diagnostics + "Defaulted NCST historical semester to its first course week",
    )
}

private fun SemesterSchedule.firstScheduledWeek(): Int {
    return courses.minOfOrNull { course -> course.weeks.first }
        ?.coerceIn(1, totalWeeks)
        ?: 1
}

private fun PortalPageSnapshot.ncstTermKey(): String? {
    text.readSelectedSemesterName()?.ncstTermKey()?.let { return it }
    val match = Regex("""(20\d{2})\s*[-—]\s*(20\d{2})\s*[-_]\s*([12])\s*[-_]\s*\d+""")
        .find(html)
        ?: return null
    return "${match.groupValues[1]}-${match.groupValues[2]}-${match.groupValues[3]}"
}

private fun String.ncstTermKey(): String? {
    Regex("""(20\d{2})\s*[-—]\s*(20\d{2})\s*[-_]\s*([12])""")
        .find(this)
        ?.let { return "${it.groupValues[1]}-${it.groupValues[2]}-${it.groupValues[3]}" }

    val academicYear = Regex("""(20\d{2})\s*[-—]\s*(20\d{2})""").find(this) ?: return null
    val term = when {
        contains("第二学期") || contains("春") -> "2"
        contains("第一学期") || contains("秋") -> "1"
        else -> return null
    }
    return "${academicYear.groupValues[1]}-${academicYear.groupValues[2]}-$term"
}

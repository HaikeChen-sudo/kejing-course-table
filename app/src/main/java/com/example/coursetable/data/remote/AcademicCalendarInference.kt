package com.example.coursetable.data.remote

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

internal data class InferredAcademicCalendar(
    val currentWeek: Int,
    val semesterStartDate: LocalDate,
    val pageWeekStart: LocalDate?,
)

internal fun inferAcademicCalendar(
    text: String,
    explicitCurrentWeek: Int? = null,
    explicitPageWeekStart: LocalDate? = null,
    anchor: LocalDate = LocalDate.now(),
): InferredAcademicCalendar {
    val pageWeekStart = explicitPageWeekStart
        ?: text.findPageWeekStart(anchor)
    val explicitSemesterStart = text.findExplicitSemesterStart(anchor)
    val detectedCurrentWeek = explicitCurrentWeek
        ?: text.findCurrentWeekOrNull()
    val estimatedSemesterStart = text.guessAcademicSemesterStart(pageWeekStart ?: anchor, anchor)

    val currentWeek = detectedCurrentWeek
        ?: if (explicitSemesterStart != null && pageWeekStart != null) {
            weekBetween(explicitSemesterStart, pageWeekStart)
        } else {
            null
        }
        ?: pageWeekStart?.let { weekStart ->
            weekBetween(text.guessAcademicSemesterStart(weekStart, anchor), weekStart)
        }
        ?: 1

    val semesterStartDate = when {
        pageWeekStart != null -> pageWeekStart.minusWeeks((currentWeek - 1).toLong())
        explicitSemesterStart != null -> explicitSemesterStart
        detectedCurrentWeek != null && text.hasAcademicTermHint() -> estimatedSemesterStart
        detectedCurrentWeek != null -> anchor
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .minusWeeks((currentWeek - 1).toLong())
        else -> estimatedSemesterStart
    }

    return InferredAcademicCalendar(
        currentWeek = currentWeek.coerceIn(1, 30),
        semesterStartDate = semesterStartDate,
        pageWeekStart = pageWeekStart,
    )
}

internal fun String.findCurrentWeekOrNull(): Int? {
    val patterns = listOf(
        Regex("当前(?:教学)?周\\s*[:：]?\\s*第?\\s*(\\d{1,2})\\s*周?"),
        Regex("本周\\s*[:：]?\\s*第?\\s*(\\d{1,2})\\s*周?"),
        Regex("教学周\\s*[:：]?\\s*第?\\s*(\\d{1,2})\\s*周?"),
        Regex("20\\d{2}\\s*[-—]\\s*20\\d{2}\\s*[春秋]\\s*第\\s*(\\d{1,2})\\s*周"),
        Regex("[春秋]季?学期\\s*第\\s*(\\d{1,2})\\s*周"),
        Regex("week\\s*(\\d{1,2})", RegexOption.IGNORE_CASE),
    )
    return patterns.firstNotNullOfOrNull { pattern ->
        pattern.find(this)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }?.coerceIn(1, 30)
}

internal fun String.findPageWeekStart(anchor: LocalDate = LocalDate.now()): LocalDate? {
    findFullDateRangeStart()?.let {
        return it.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    findChineseDateRangeStart(anchor)?.let {
        return it.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    return null
}

private fun String.findExplicitSemesterStart(anchor: LocalDate): LocalDate? {
    val labelDatePatterns = listOf(
        Regex("(?:开学|学期开始|学期起始|校历开始|教学周开始|第一周|第\\s*1\\s*周)[^\\n\\r]{0,24}?(20\\d{2})\\s*[-/.年]\\s*(\\d{1,2})\\s*[-/.月]\\s*(\\d{1,2})\\s*日?"),
        Regex("(20\\d{2})\\s*[-/.年]\\s*(\\d{1,2})\\s*[-/.月]\\s*(\\d{1,2})\\s*日?[^\\n\\r]{0,24}?(?:开学|学期开始|学期起始|校历开始|教学周开始|第一周|第\\s*1\\s*周)"),
    )
    labelDatePatterns.firstNotNullOfOrNull { pattern ->
        pattern.find(this)?.toLocalDateFromGroups()
    }?.let {
        return it.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    val shortDate = Regex("(?:开学|学期开始|学期起始|第一周|第\\s*1\\s*周)[^\\n\\r]{0,24}?(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*日?")
        .find(this)
    if (shortDate != null) {
        val month = shortDate.groupValues[1].toIntOrNull() ?: return null
        val day = shortDate.groupValues[2].toIntOrNull() ?: return null
        return runCatching {
            LocalDate.of(resolveYearForMonth(month, anchor), month, day)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        }.getOrNull()
    }

    return null
}

private fun String.guessAcademicSemesterStart(pageWeekStart: LocalDate, anchor: LocalDate): LocalDate {
    val academicYear = Regex("(20\\d{2})\\s*[-—]\\s*(20\\d{2})").find(this)
    val firstYear = academicYear?.groupValues?.getOrNull(1)?.toIntOrNull()
    val secondYear = academicYear?.groupValues?.getOrNull(2)?.toIntOrNull()

    val semesterNumber = Regex("第?\\s*([12])\\s*学期").find(this)
        ?.groupValues
        ?.getOrNull(1)
    val lowerText = lowercase(Locale.US)
    val isSpring = contains("春") || semesterNumber == "2" || lowerText.contains("spring")
    val isAutumn = contains("秋") || semesterNumber == "1" ||
        lowerText.contains("fall") || lowerText.contains("autumn")

    val estimated = when {
        isSpring -> LocalDate.of(secondYear ?: pageWeekStart.year, 2, 24)
        isAutumn -> LocalDate.of(firstYear ?: pageWeekStart.year, 8, 31)
        pageWeekStart.monthValue in 1..7 -> LocalDate.of(pageWeekStart.year, 2, 24)
        else -> LocalDate.of(pageWeekStart.year, 8, 31)
    }
    return estimated.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}

private fun String.hasAcademicTermHint(): Boolean {
    return Regex("20\\d{2}\\s*[-—]\\s*20\\d{2}").containsMatchIn(this) ||
        contains("春") ||
        contains("秋") ||
        Regex("第?\\s*[12]\\s*学期").containsMatchIn(this) ||
        lowercase(Locale.US).let { it.contains("spring") || it.contains("fall") || it.contains("autumn") }
}

private fun String.findFullDateRangeStart(): LocalDate? {
    val match = Regex("(20\\d{2})\\s*[-/.年]\\s*(\\d{1,2})\\s*[-/.月]\\s*(\\d{1,2})\\s*日?\\s*(?:至|到|[-—~])")
        .find(this)
        ?: return null
    return match.toLocalDateFromGroups()
}

private fun String.findChineseDateRangeStart(anchor: LocalDate): LocalDate? {
    val match = Regex("(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*日\\s*(?:至|到|[-—~])")
        .find(this)
        ?: return null
    val month = match.groupValues[1].toIntOrNull() ?: return null
    val day = match.groupValues[2].toIntOrNull() ?: return null
    return runCatching {
        LocalDate.of(resolveYearForMonth(month, anchor), month, day)
    }.getOrNull()
}

private fun MatchResult.toLocalDateFromGroups(): LocalDate? {
    return runCatching {
        LocalDate.of(
            groupValues[1].toInt(),
            groupValues[2].toInt(),
            groupValues[3].toInt(),
        )
    }.getOrNull()
}

private fun resolveYearForMonth(month: Int, anchor: LocalDate): Int {
    return when {
        month in 9..12 && anchor.monthValue in 1..8 -> anchor.year - 1
        month in 1..8 && anchor.monthValue in 9..12 -> anchor.year + 1
        else -> anchor.year
    }
}

private fun estimateCurrentWeek(anchor: LocalDate): Int {
    val estimatedStart = if (anchor.monthValue in 9..12) {
        LocalDate.of(anchor.year, 8, 31)
    } else {
        LocalDate.of(anchor.year, 2, 24)
    }.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return weekBetween(estimatedStart, anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
}

private fun weekBetween(startWeekMonday: LocalDate, targetWeekMonday: LocalDate): Int {
    return (ChronoUnit.WEEKS.between(startWeekMonday, targetWeekMonday) + 1)
        .toInt()
        .coerceIn(1, 30)
}

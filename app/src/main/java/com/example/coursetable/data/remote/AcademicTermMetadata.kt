package com.example.coursetable.data.remote

import java.time.LocalDate

internal data class AcademicTermMetadata(
    val displayName: String? = null,
    val currentWeek: Int? = null,
    val totalWeeks: Int? = null,
    val semesterStartDate: LocalDate? = null,
) {
    val isEmpty: Boolean
        get() = displayName == null && currentWeek == null &&
            totalWeeks == null && semesterStartDate == null
}

internal fun PortalPageSnapshot.readAcademicTermMetadata(): AcademicTermMetadata {
    val page = listOf(title, text, html).joinToString("\n")
    val semesterName = text.readSelectedSemesterName() ?: page.readSemesterName()
    return AcademicTermMetadata(
        displayName = semesterName,
        currentWeek = page.readAcademicWeek(),
        totalWeeks = page.readTotalWeeks(),
        semesterStartDate = page.readSemesterStartDate(),
    )
}

internal fun String.readSelectedSemesterName(): String? {
    val code = Regex(
        """已选字段\[[^\]]+]\[[^\]]+]\s*:\s*(20\d{2})\s*[-—]\s*(20\d{2})\s*[-_]\s*([12])""",
    ).find(this) ?: return null
    val term = if (code.groupValues[3] == "1") "一" else "二"
    return "${code.groupValues[1]}-${code.groupValues[2]}学年第${term}学期"
}

internal fun ScheduleParseResult.withAcademicTermMetadata(
    snapshot: PortalPageSnapshot,
): ScheduleParseResult {
    val schedule = schedule ?: return this
    val metadata = snapshot.readAcademicTermMetadata()
    if (metadata.isEmpty) return this

    val lastCourseWeek = schedule.courses.maxOfOrNull { it.weeks.last } ?: 1
    val updatedSchedule = schedule.copy(
        displayName = metadata.displayName ?: schedule.displayName,
        currentWeek = metadata.currentWeek ?: schedule.currentWeek,
        totalWeeks = metadata.totalWeeks
            ?.coerceAtLeast(lastCourseWeek)
            ?: schedule.totalWeeks,
        semesterStartDate = metadata.semesterStartDate ?: schedule.semesterStartDate,
    )
    return copy(
        schedule = updatedSchedule,
        diagnostics = diagnostics + "Read academic term metadata",
    )
}

private fun String.readSemesterName(): String? {
    readQuotedValue(listOf("semesterName", "termName", "xnxqmc", "semester", "term"))
        ?.takeIf { it.hasSemesterHint() }
        ?.let { return it.normalizeSemesterName() }

    Regex("""(20\d{2}\s*[-—]\s*20\d{2}\s*学年\s*(?:第?\s*[一二三四五六七八九十\d]+\s*学期|[春秋](?:季)?学期))""")
        .find(this)
        ?.groupValues
        ?.getOrNull(1)
        ?.let { return it.normalizeSemesterName() }

    val code = Regex("""(20\d{2})\s*[-—]\s*(20\d{2})\s*[-_]\s*([12])""")
        .find(this)
        ?: return null
    return "${code.groupValues[1]}-${code.groupValues[2]}学年第${if (code.groupValues[3] == "1") "一" else "二"}学期"
}

private fun String.readAcademicWeek(): Int? {
    findCurrentWeekOrNull()?.let { return it }
    readIntegerValue(listOf("currentWeek", "currentTeachingWeek", "teachingWeek", "dqz", "xq"))
        ?.takeIf { it in 1..30 }
        ?.let { return it }
    return null
}

private fun String.readTotalWeeks(): Int? {
    readIntegerAfterLabels(listOf("教学周数", "总教学周", "学期周数", "总周数"))
        ?.takeIf { it in 1..30 }
        ?.let { return it }
    readIntegerValue(listOf("totalWeeks", "teachingWeeks", "weekCount", "weekTotal", "zxs"))
        ?.takeIf { it in 1..30 }
        ?.let { return it }
    return null
}

private fun String.readSemesterStartDate(): LocalDate? {
    readDateValue(listOf("semesterStartDate", "semesterBeginDate", "termStartDate", "schoolStartDate"))
        ?.let { return it }
    readDateAfterLabels(listOf("开学日期", "开学时间", "学期开始", "学期起始", "校历开始", "教学周开始", "第一周开始"))
        ?.let { return it }
    return null
}

private fun String.readQuotedValue(keys: List<String>): String? {
    return keys.firstNotNullOfOrNull { key ->
        Regex("""["']?${Regex.escape(key)}["']?\s*[:=]\s*["']([^"'\r\n]+)["']""", RegexOption.IGNORE_CASE)
            .find(this)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }
}

private fun String.readIntegerValue(keys: List<String>): Int? {
    return keys.firstNotNullOfOrNull { key ->
        Regex("""["']?${Regex.escape(key)}["']?\s*[:=]\s*["']?(\d{1,2})""", RegexOption.IGNORE_CASE)
            .find(this)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
    }
}

private fun String.readIntegerAfterLabels(labels: List<String>): Int? {
    val labelGroup = labels.joinToString("|") { Regex.escape(it) }
    return Regex("""(?:$labelGroup)[^\d]{0,16}(\d{1,2})\s*周?""")
        .find(this)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
}

private fun String.readDateValue(keys: List<String>): LocalDate? {
    return keys.firstNotNullOfOrNull { key ->
        Regex("""["']?${Regex.escape(key)}["']?\s*[:=]\s*["']?((?:20\d{2})\s*[-/.年]\s*\d{1,2}\s*[-/.月]\s*\d{1,2}\s*日?)""", RegexOption.IGNORE_CASE)
            .find(this)
            ?.groupValues
            ?.getOrNull(1)
            ?.toLocalDateOrNull()
    }
}

private fun String.readDateAfterLabels(labels: List<String>): LocalDate? {
    val labelGroup = labels.joinToString("|") { Regex.escape(it) }
    return Regex("""(?:$labelGroup)[^\d]{0,24}((?:20\d{2})\s*[-/.年]\s*\d{1,2}\s*[-/.月]\s*\d{1,2}\s*日?)""")
        .find(this)
        ?.groupValues
        ?.getOrNull(1)
        ?.toLocalDateOrNull()
}

private fun String.toLocalDateOrNull(): LocalDate? {
    val parts = Regex("""(20\d{2})\s*[-/.年]\s*(\d{1,2})\s*[-/.月]\s*(\d{1,2})""").find(this)
        ?: return null
    return runCatching {
        LocalDate.of(parts.groupValues[1].toInt(), parts.groupValues[2].toInt(), parts.groupValues[3].toInt())
    }.getOrNull()
}

private fun String.hasSemesterHint(): Boolean {
    return contains("学期") || contains("春") || contains("秋") ||
        Regex("""20\d{2}\s*[-—]\s*20\d{2}""").containsMatchIn(this)
}

private fun String.normalizeSemesterName(): String {
    return replace(Regex("""\s+"""), " ").trim()
}

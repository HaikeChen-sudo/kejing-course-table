package com.example.coursetable.data.remote

import com.example.coursetable.domain.model.Course
import com.example.coursetable.domain.model.CourseColorSeed
import com.example.coursetable.domain.model.ScheduleDefaults
import com.example.coursetable.domain.model.SemesterSchedule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

class ManualPortalScheduleParser(
    private val parsers: List<SchoolScheduleParser> = listOf(
        NcstPortalScheduleParser(),
        XzcitPortalScheduleParser(),
        ZfsoftScheduleParser(),
        GenericTableScheduleParser(),
    ),
) {
    fun parse(url: String, title: String, html: String): SemesterSchedule? {
        val snapshot = PortalPageSnapshot.from(
            url = url,
            fallbackTitle = title,
            rawPayload = html,
        )
        scheduleParserLog("Snapshot: url=${snapshot.url}, tables=${snapshot.tables.size}, text length=${snapshot.text.length}, html length=${snapshot.html.length}")

        val results = parsers.map { parser ->
            val result = parser.parse(snapshot)
            scheduleParserLog("Parser ${parser.parserId}: confidence=${result.confidence}, diagnostics=${result.diagnostics}, courses=${result.schedule?.courses?.size ?: 0}")
            result
        }

        return results
            .filter { it.isUsable }
            .maxByOrNull { it.confidence }
            ?.schedule
    }

    /**
     * 解析并返回详细诊断信息
     */
    fun parseWithDiagnostics(url: String, title: String, html: String): Pair<SemesterSchedule?, String> {
        val snapshot = PortalPageSnapshot.from(
            url = url,
            fallbackTitle = title,
            rawPayload = html,
        )

        val diagnostics = mutableListOf<String>()
        diagnostics.add("页面URL: ${snapshot.url.take(50)}")
        diagnostics.add("表格数量: ${snapshot.tables.size}")
        diagnostics.add("HTML长度: ${snapshot.html.length}")
        diagnostics.add("文本长度: ${snapshot.text.length}")

        val results = parsers.map { parser ->
            val result = parser.parse(snapshot)
            diagnostics.add("${parser.parserId}: confidence=${result.confidence}, courses=${result.schedule?.courses?.size ?: 0}, ${result.diagnostics.firstOrNull() ?: ""}")
            result
        }

        val bestResult = results
            .filter { it.isUsable }
            .maxByOrNull { it.confidence }

        return if (bestResult != null) {
            bestResult.schedule to "成功: ${bestResult.schedule?.courses?.size} 门课程"
        } else {
            null to diagnostics.joinToString("\n")
        }
    }
}

interface SchoolScheduleParser {
    val parserId: String
    fun parse(snapshot: PortalPageSnapshot): ScheduleParseResult
}

data class ScheduleParseResult(
    val schedule: SemesterSchedule?,
    val confidence: Int,
    val diagnostics: List<String> = emptyList(),
) {
    val isUsable: Boolean = schedule != null && confidence >= 70
}

data class PortalPageSnapshot(
    val url: String,
    val title: String,
    val text: String,
    val html: String,
    val tables: List<DomTable>,
) {
    companion object {
        fun from(url: String, fallbackTitle: String, rawPayload: String): PortalPageSnapshot {
            val payload = runCatching { JSONTokener(rawPayload).nextValue() as? JSONObject }.getOrNull()
            return if (payload != null) {
                val html = payload.optString("html", rawPayload)
                val text = payload.optString("text").ifBlank { html.stripHtml() }
                PortalPageSnapshot(
                    url = payload.optString("url", url),
                    title = payload.optString("title", fallbackTitle).ifBlank { fallbackTitle },
                    text = text,
                    html = html,
                    tables = payload.optJSONArray("tables").toDomTables(),
                )
            } else {
                PortalPageSnapshot(
                    url = url,
                    title = fallbackTitle,
                    text = rawPayload.stripHtml(),
                    html = rawPayload,
                    tables = extractHtmlTables(rawPayload),
                )
            }
        }
    }
}

data class DomTable(
    val rows: List<List<DomCell>>,
)

data class DomCell(
    val text: String,
    val rowSpan: Int,
    val colSpan: Int,
)

private data class ExpandedCell(
    val text: String,
    val originRow: Int,
    val originColumn: Int,
    val rowSpan: Int,
    val colSpan: Int,
)

private data class DayColumn(
    val columnIndex: Int,
    val dayOfWeek: DayOfWeek,
    val date: LocalDate?,
)

private data class CourseCandidate(
    val text: String,
    val rowIndex: Int,
    val columnIndex: Int,
    val day: DayColumn,
    val rowContext: List<String>,
)

private data class PeriodTimeTable(
    private val ranges: List<Pair<IntRange, Pair<LocalTime, LocalTime>>>,
) {
    fun resolve(periods: IntRange): Pair<LocalTime, LocalTime>? {
        ranges.firstOrNull { it.first == periods }?.let { return it.second }

        val firstPeriod = ranges.firstOrNull { periods.first in it.first }
        val lastPeriod = ranges.firstOrNull { periods.last in it.first }
        if (firstPeriod != null && lastPeriod != null) {
            return firstPeriod.second.first to lastPeriod.second.second
        }
        return null
    }

    companion object {
        fun from(text: String): PeriodTimeTable {
            val matches = mutableListOf<Pair<IntRange, Pair<LocalTime, LocalTime>>>()
            val rangePattern = Regex(
                "(?:第\\s*)?(\\d{1,2})(?:\\s*[-~到至]\\s*(\\d{1,2}))?\\s*节[^\\d]{0,12}" +
                    "(\\d{1,2})[:：](\\d{2})\\s*[-~至到]\\s*(\\d{1,2})[:：](\\d{2})",
            )
            rangePattern.findAll(text).forEach { match ->
                val startPeriod = match.groupValues[1].toIntOrNull() ?: return@forEach
                val endPeriod = match.groupValues[2].toIntOrNull() ?: startPeriod
                val timeRange = match.groupValues.toTimeRangeAt(3) ?: return@forEach
                matches += (startPeriod..endPeriod) to timeRange
            }
            return PeriodTimeTable(matches.distinctBy { "${it.first.first}-${it.first.last}" })
        }
    }
}

class NcstPortalScheduleParser : SchoolScheduleParser by GenericTableScheduleParser(
    parserId = "ncst-portal",
    schoolKeywords = listOf("xjw1.ncst.edu.cn", "华北理工", "华北理工大学"),
)

class GenericTableScheduleParser(
    override val parserId: String = "generic-table",
    private val schoolKeywords: List<String> = emptyList(),
) : SchoolScheduleParser {

    override fun parse(snapshot: PortalPageSnapshot): ScheduleParseResult {
        scheduleParserLog("[$parserId] Parsing: tables=${snapshot.tables.size}, keywords=$schoolKeywords")
        if (schoolKeywords.isNotEmpty()) {
            val haystack = "${snapshot.url}\n${snapshot.title}\n${snapshot.text}\n${snapshot.html}"
            val matchedKeywords = schoolKeywords.filter { haystack.contains(it, ignoreCase = true) }
            scheduleParserLog("[$parserId] Matched keywords: $matchedKeywords")
            if (matchedKeywords.isEmpty()) {
                return ScheduleParseResult(null, 0, listOf("School keywords not matched"))
            }
        }
        scheduleParserLog("[$parserId] Found ${snapshot.tables.size} tables")
        snapshot.tables.forEachIndexed { index, table ->
            scheduleParserLog("[$parserId] Table $index: ${table.rows.size} rows")
            if (table.rows.isNotEmpty()) {
                scheduleParserLog("[$parserId] Table $index first row: ${table.rows.first().map { it.text.take(20) }}")
            }
        }
        val tableResults = snapshot.tables.mapNotNull { table ->
            val result = parseTable(snapshot = snapshot, table = table)
            scheduleParserLog("[$parserId] parseTable result: confidence=${result?.confidence}, courses=${result?.schedule?.courses?.size}")
            result
        }
        return tableResults.maxByOrNull { it.confidence }
            ?: ScheduleParseResult(
                schedule = null,
                confidence = 0,
                diagnostics = listOf("No table-like schedule found"),
            )
    }

    private fun parseTable(snapshot: PortalPageSnapshot, table: DomTable): ScheduleParseResult? {
        val matrix = table.expandMatrix()
        if (matrix.isEmpty()) return null
        val readableText = "${snapshot.title}\n${snapshot.text}\n${snapshot.html.stripHtml()}\n${matrix.toReadableText()}"
        val headerIndex = matrix.indexOfFirst { row ->
            row.count { it.text.toDayOfWeek() != null || it.text.toHeaderDate(readableText) != null } >= 4
        }
        if (headerIndex < 0) return null

        val dayColumns = matrix[headerIndex].mapIndexedNotNull { index, cell ->
            val date = cell.text.toHeaderDate(readableText)
            val day = cell.text.toDayOfWeek() ?: date?.dayOfWeek
            day?.let {
                DayColumn(
                    columnIndex = index,
                    dayOfWeek = it,
                    date = date,
                )
            }
        }
        if (dayColumns.size < 4) return null

        val firstDayColumn = dayColumns.minOf { it.columnIndex }
        // 记录已经处理过的单元格（通过 originRow 和 originColumn），避免 expandMatrix 导致的重复
        val processedCells = mutableSetOf<Pair<Int, Int>>()
        val candidates = matrix
            .drop(headerIndex + 1)
            .flatMapIndexed { offset, row ->
                val rowIndex = headerIndex + 1 + offset
                val rowContext = row.take(firstDayColumn).map { it.text }.filter { it.isNotBlank() }
                dayColumns.mapNotNull { day ->
                    val cell = row.getOrNull(day.columnIndex) ?: return@mapNotNull null
                    if (!cell.text.looksLikeCourse()) return@mapNotNull null
                    // 跳过已经处理过的单元格
                    val cellKey = cell.originRow to cell.originColumn
                    if (!processedCells.add(cellKey)) return@mapNotNull null
                    CourseCandidate(
                        text = cell.text,
                        rowIndex = rowIndex,
                        columnIndex = day.columnIndex,
                        day = day,
                        rowContext = rowContext,
                    )
                }
            }

        val periodTimeTable = PeriodTimeTable.from(readableText)
        val courses = candidates.flatMap { candidate ->
            splitCourseCell(candidate.text).flatMap { courseText ->
                parseCourseCandidate(
                    candidate = candidate.copy(text = courseText),
                    periodTimeTable = periodTimeTable,
                )
            }
        }.distinctBy {
            "${it.name}-${it.dayOfWeek}-${it.startTime}-${it.endTime}-${it.location}-${it.teacher}-${it.weeks}"
        }

        if (courses.isEmpty()) return null
        val confidence = scoreParse(
            dayColumns = dayColumns,
            candidates = candidates,
            courses = courses,
        )
        if (confidence < 70) {
            return ScheduleParseResult(
                schedule = null,
                confidence = confidence,
                diagnostics = listOf("Low confidence: $confidence"),
            )
        }

        val pageWeekStart = dayColumns.mapNotNull { it.date }.minOrNull()
            ?.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            ?: readableText.findPageWeekStart()
        val calendar = inferAcademicCalendar(
            text = readableText,
            explicitPageWeekStart = pageWeekStart,
        )

        val timestamp = System.currentTimeMillis()
        return ScheduleParseResult(
            confidence = confidence,
            schedule = SemesterSchedule(
                id = "manual-${parserId}-$timestamp",
                displayName = readableText.guessSemesterName(snapshot.title),
                folderName = "manual_${parserId}_$timestamp",
                currentWeek = calendar.currentWeek,
                totalWeeks = courses.inferTotalWeeks(),
                semesterStartDate = calendar.semesterStartDate,
                courses = courses,
            ),
            diagnostics = listOf("Imported ${courses.size} courses with confidence $confidence"),
        )
    }

    private fun parseCourseCandidate(
        candidate: CourseCandidate,
        periodTimeTable: PeriodTimeTable,
    ): List<Course> {
        val text = candidate.text
        val lines = text.toCourseLines()
        if (lines.isEmpty()) return emptyList()

        val name = extractCourseName(lines) ?: return emptyList()
        val teacher = extractAfterLabels(
            lines = lines,
            labels = listOf("任课教师", "授课教师", "教师", "老师", "任课"),
            stopLabels = listOf("上课地点", "地点", "教室", "校区", "周次", "周", "节次", "节"),
        )
            ?: lines.firstOrNull { it.endsWith("老师") }
            ?: extractTeacherLine(lines, name)
            ?: ""
        val location = extractAfterLabels(
            lines = lines,
            labels = listOf("上课地点", "地点", "教室"),
            stopLabels = listOf("任课教师", "授课教师", "教师", "老师", "任课", "周次", "周", "节次", "节"),
        )
            ?: lines.firstOrNull { line ->
                listOf("校区", "教学楼", "实验楼", "楼", "馆", "实验", "室").any { line.contains(it) }
            }
            ?: ""

        val weekRanges = parseWeekRanges(text)
        val timeRange = parseTimeRange(text)
            ?: parsePeriodRange(text)?.let(periodTimeTable::resolve)
            ?: parsePeriodRange(text)?.toDefaultTimeRange()
            ?: parseTimeRange(candidate.rowContext.joinToString("\n"))
            ?: parsePeriodRange(candidate.rowContext.joinToString("\n"))?.let(periodTimeTable::resolve)
            ?: parsePeriodRange(candidate.rowContext.joinToString("\n"))?.toDefaultTimeRange()
            ?: return emptyList()

        return weekRanges.mapIndexed { index, weeks ->
            Course(
                id = "manual-${candidate.day.dayOfWeek.name.lowercase(Locale.US)}-${candidate.rowIndex}-${candidate.columnIndex}-${index}-${name.hashCode()}",
                name = name.take(40),
                dayOfWeek = candidate.day.dayOfWeek,
                startTime = timeRange.first,
                endTime = timeRange.second,
                location = location.ifBlank { "未识别" }.take(40),
                teacher = teacher.ifBlank { "未识别" }.take(32),
                weeks = weeks,
                colorSeed = CourseColorSeed.entries[name.hashCode().absoluteValue() % CourseColorSeed.entries.size],
            )
        }
    }

    private fun scoreParse(
        dayColumns: List<DayColumn>,
        candidates: List<CourseCandidate>,
        courses: List<Course>,
    ): Int {
        var score = 20
        score += (dayColumns.size * 8).coerceAtMost(56)
        score += (courses.size * 5).coerceAtMost(35)
        if (dayColumns.any { it.date != null }) score += 15
        if (courses.all { it.startTime != it.endTime }) score += 10
        if (courses.count { it.teacher != "未识别" || it.location != "未识别" } >= courses.size.coerceAtMost(3)) {
            score += 12
        }
        if (candidates.size > courses.size * 2) score -= 18
        return score.coerceIn(0, 100)
    }
}

private fun JSONArray?.toDomTables(): List<DomTable> {
    if (this == null) return emptyList()
    return List(length()) { tableIndex ->
        val table = optJSONArray(tableIndex) ?: JSONArray()
        DomTable(
            rows = List(table.length()) { rowIndex ->
                val row = table.optJSONArray(rowIndex) ?: JSONArray()
                List(row.length()) { cellIndex ->
                    val cell = row.optJSONObject(cellIndex) ?: JSONObject()
                    DomCell(
                        text = cell.optString("text").normalizeCellText(),
                        rowSpan = cell.optInt("rowSpan", 1).coerceAtLeast(1),
                        colSpan = cell.optInt("colSpan", 1).coerceAtLeast(1),
                    )
                }
            },
        )
    }
}

private fun DomTable.expandMatrix(): List<List<ExpandedCell>> {
    val occupied = mutableMapOf<Pair<Int, Int>, ExpandedCell>()
    val result = mutableListOf<List<ExpandedCell>>()
    rows.forEachIndexed { rowIndex, row ->
        val expandedRow = mutableListOf<ExpandedCell>()
        var columnIndex = 0
        row.forEachIndexed { sourceColumn, cell ->
            while (occupied.containsKey(rowIndex to columnIndex)) {
                expandedRow.add(occupied.getValue(rowIndex to columnIndex))
                columnIndex++
            }
            val expandedCell = ExpandedCell(
                text = cell.text,
                originRow = rowIndex,
                originColumn = sourceColumn,
                rowSpan = cell.rowSpan,
                colSpan = cell.colSpan,
            )
            repeat(cell.colSpan) {
                expandedRow.add(expandedCell)
                for (rowOffset in 1 until cell.rowSpan) {
                    occupied[(rowIndex + rowOffset) to (columnIndex + it)] = expandedCell
                }
            }
            columnIndex += cell.colSpan
        }
        while (occupied.containsKey(rowIndex to columnIndex)) {
            expandedRow.add(occupied.getValue(rowIndex to columnIndex))
            columnIndex++
        }
        if (expandedRow.any { it.text.isNotBlank() }) {
            result.add(expandedRow)
        }
    }
    return result
}

private fun extractHtmlTables(html: String): List<DomTable> {
    val htmlOptions = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    val rowRegex = Regex("<tr\\b[^>]*>(.*?)</tr>", htmlOptions)
    val cellRegex = Regex("<t[dh]\\b([^>]*)>(.*?)</t[dh]>", htmlOptions)
    val tables = Regex("<table\\b[^>]*>(.*?)</table>", htmlOptions)
        .findAll(html)
        .map { tableMatch ->
            DomTable(
                rows = rowRegex.findAll(tableMatch.groupValues[1]).map { rowMatch ->
                    cellRegex.findAll(rowMatch.groupValues[1]).map { cellMatch ->
                        DomCell(
                            text = cellMatch.groupValues[2].stripHtml().normalizeCellText(),
                            rowSpan = cellMatch.groupValues[1].extractSpan("rowspan"),
                            colSpan = cellMatch.groupValues[1].extractSpan("colspan"),
                        )
                    }.toList()
                }.filter { it.isNotEmpty() }.toList(),
            )
        }
        .filter { it.rows.isNotEmpty() }
        .toList()
    return tables
}

private fun List<List<ExpandedCell>>.toReadableText(): String {
    return joinToString("\n") { row ->
        row.joinToString("\t") { it.text }
    }
}

private fun String.extractSpan(name: String): Int {
    return Regex("$name\\s*=\\s*['\"]?(\\d+)", RegexOption.IGNORE_CASE)
        .find(this)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
        ?.coerceAtLeast(1)
        ?: 1
}

private fun String.stripHtml(): String {
    // 先处理带 title 属性的 <p> 标签，将标签和值合并到同一行
    // 匹配模式：<p>...<span title="xxx">...</span><font>value</font>...</p>
    val withLabels = replace(
        Regex("""<p>(.*?)</p>""", RegexOption.DOT_MATCHES_ALL),
    ) { match ->
        val pContent = match.groupValues[1]
        // 提取 title 属性
        val titleMatch = Regex("""title="([^"]*)"""").find(pContent)
        if (titleMatch != null) {
            val title = titleMatch.groupValues[1].trim()
            // 提取文本内容（去除所有HTML标签），将多个空格替换为单个空格
            val textValue = pContent
                .replace(Regex("<[^>]+>"), "")
                .replace("&nbsp;", " ")
                .replace(Regex("\\s{2,}"), " ")
                .trim()
            val label = when {
                title.contains("教师") -> "教师"
                title.contains("上课地点") || title.contains("地点") -> "上课地点"
                title.contains("校区") -> "校区"
                title.contains("节/周") -> null
                title.contains("教学班名称") -> "教学班"
                title.contains("教学班组成") -> "教学班组成"
                title.contains("考核") -> "考核方式"
                title.contains("选课备注") -> "选课备注"
                title.contains("学时组成") -> "课程学时组成"
                title.contains("周学时") -> "周学时"
                title.contains("总学时") -> "总学时"
                title.contains("学分") -> "学分"
                else -> title.ifBlank { null }
            }
            if (label != null && textValue.isNotBlank()) "\n$label：$textValue\n"
            else if (label != null) "\n$label：\n"
            else "\n$textValue\n"
        } else {
            // 没有 title 属性的 <p> 标签，直接提取文本
            val textValue = pContent
                .replace(Regex("<[^>]+>"), "")
                .replace("&nbsp;", " ")
                .replace(Regex("\\s{2,}"), " ")
                .trim()
            "\n$textValue\n"
        }
    }
    return withLabels
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</div>|</li>|</td>|</th>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<[^>]+>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .normalizeCellText()
}

private fun String.normalizeCellText(): String {
    return replace("\r", "")
        .lines()
        .flatMap { line -> line.split(Regex("\\s{2,}")) }
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString("\n")
}

private fun String.toDayOfWeek(): DayOfWeek? {
    return when {
        contains("星期一") || contains("周一") || equals("一") -> DayOfWeek.MONDAY
        contains("星期二") || contains("周二") || equals("二") -> DayOfWeek.TUESDAY
        contains("星期三") || contains("周三") || equals("三") -> DayOfWeek.WEDNESDAY
        contains("星期四") || contains("周四") || equals("四") -> DayOfWeek.THURSDAY
        contains("星期五") || contains("周五") || equals("五") -> DayOfWeek.FRIDAY
        contains("星期六") || contains("周六") || equals("六") -> DayOfWeek.SATURDAY
        contains("星期日") || contains("星期天") || contains("周日") || contains("周天") || equals("日") -> DayOfWeek.SUNDAY
        else -> null
    }
}

private fun String.toHeaderDate(context: String): LocalDate? {
    parseFullDate()?.let { return it }
    // 只匹配月/日格式，不匹配年份格式（如 2025-2026）
    val short = Regex("(?<![0-9])(\\d{1,2})\\s*[/月.-]\\s*(\\d{1,2})(?![0-9])").find(this) ?: return null
    val month = short.groupValues[1].toInt()
    val day = short.groupValues[2].toInt()
    // 月份必须在1-12之间，日期必须在1-31之间
    if (month !in 1..12 || day !in 1..31) return null
    val year = context.inferYearForMonth(month)
    return runCatching {
        LocalDate.of(year, month, day)
    }.getOrNull()
}

private fun String.inferYearForMonth(month: Int): Int {
    parseDateRangeStart()?.let { return it.year }
    parseFullDate()?.let { return it.year }
    val academicYear = Regex("(20\\d{2})\\s*[-—]\\s*(20\\d{2})").find(this)
    if (academicYear != null) {
        val firstYear = academicYear.groupValues[1].toInt()
        val secondYear = academicYear.groupValues[2].toInt()
        return if (month in 1..8) secondYear else firstYear
    }
    return LocalDate.now().year
}

private fun String.looksLikeCourse(): Boolean {
    val text = trim()
    if (text.length < 3) return false
    if (toDayOfWeek() != null) return false
    if (text.matches(Regex("[0-9第\\-~:：节\\s/年月日]+"))) return false
    val signals = listOf("周", "节", "教师", "老师", "教室", "校区", "楼", "馆", "实验", "上课", "地点", "任课")
    return signals.any { text.contains(it) } || text.lines().size >= 3
}

private fun splitCourseCell(text: String): List<String> {
    val chunks = text
        .split(Regex("\\n\\s*\\n+|[-=]{3,}"))
        .map { it.trim() }
        .filter { it.isNotBlank() }
    if (chunks.size > 1) return chunks

    val lines = text.toCourseLines()
    if (lines.size < 6) return listOf(text.trim())

    val groups = mutableListOf<MutableList<String>>()
    lines.forEach { line ->
        val shouldStartNew = groups.isNotEmpty() &&
            line.isLikelyCourseTitleLine() &&
            groups.last().any { it.containsCourseDetailSignal() }
        if (shouldStartNew) {
            groups.add(mutableListOf(line))
        } else {
            if (groups.isEmpty()) groups.add(mutableListOf())
            groups.last() += line
        }
    }
    return groups.map { it.joinToString("\n").trim() }.filter { it.isNotBlank() }
}

private fun String.toCourseLines(): List<String> {
    val labelLookahead = listOf(
        "任课教师",
        "授课教师",
        "教师",
        "老师",
        "上课地点",
        "地点",
        "教室",
        "校区",
        "周次",
        "节次",
    ).joinToString("|")
    return replace(Regex("([;；,，]\\s*|\\s{2,})(?=($labelLookahead))"), "\n")
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

private fun extractCourseName(lines: List<String>): String? {
    return lines.firstOrNull { line ->
        !line.contains("周") &&
            !line.contains("节") &&
            !line.contains("教师") &&
            !line.contains("任课") &&
            !line.contains("教室") &&
            !line.contains("地点") &&
            !line.contains("校区") &&
            !line.matches(Regex("[0-9\\-~:,，、/年月日\\s]+"))
    }?.trim()
}

private fun extractTeacherLine(lines: List<String>, courseName: String): String? {
    return lines.firstOrNull { line ->
        line != courseName &&
            line.length <= 24 &&
            !line.containsCourseDetailSignal() &&
            !line.contains("校区") &&
            !line.contains("座") &&
            !line.contains("楼") &&
            !line.contains("室") &&
            line.any { it == '*' || it in '\u4e00'..'\u9fff' }
    }?.replace("*", "")?.trim()
}

private fun extractAfterLabels(
    lines: List<String>,
    labels: List<String>,
    stopLabels: List<String>,
): String? {
    lines.forEach { line ->
        labels.forEach { label ->
            val index = line.indexOf(label)
            if (index >= 0) {
                val rawValue = line
                    .substring(index + label.length)
                    .trimStart('：', ':', ' ', '\t')
                    .trim()
                return rawValue
                    .trimBeforeAny(stopLabels)
                    .ifBlank { null }
            }
        }
    }
    return null
}

private fun String.trimBeforeAny(labels: List<String>): String {
    val cut = labels
        .mapNotNull { label ->
            val index = indexOf(label)
            index.takeIf { it > 0 }
        }
        .minOrNull()
        ?: length
    return substring(0, cut)
        .trim(' ', '\t', '，', ',', '；', ';', '。')
}

private fun String.isLikelyCourseTitleLine(): Boolean {
    return length in 2..40 &&
        !containsCourseDetailSignal() &&
        !matches(Regex("[0-9\\-~:,，、/年月日\\s]+"))
}

private fun String.containsCourseDetailSignal(): Boolean {
    val signals = listOf("教师", "老师", "任课", "地点", "教室", "校区", "周", "节", "时间")
    return signals.any { contains(it) }
}

private fun parseWeekRanges(text: String): List<IntRange> {
    val normalized = text
        .replace("，", ",")
    // 尝试从 "周次：" 后面提取
    val weekText = Regex("周次\\s*[:：]?\\s*([^\\n\\r<]*)")
        .find(normalized)
        ?.groupValues
        ?.getOrNull(1)

    if (weekText != null) {
        val ranges = weekText
            .substringBefore("周")
            .split(',', '、', '\n', '\r')
            .mapNotNull { token -> token.toWeekRangeOrNull() }
        if (ranges.isNotEmpty()) return ranges
    }

    // 支持 "1-8,10-13,16-18周" 这种只有最后一个片段带“周”的写法。
    val compactWeekList = Regex(
        "((?:\\d{1,2}\\s*[-~到至]\\s*\\d{1,2}|\\d{1,2})(?:\\s*[,，、]\\s*(?:\\d{1,2}\\s*[-~到至]\\s*\\d{1,2}|\\d{1,2}))+)[\\s]*周",
    ).find(normalized)
        ?.groupValues
        ?.getOrNull(1)
        ?.split(',', '，', '、')
        ?.mapNotNull { it.toWeekRangeOrNull() }
        .orEmpty()
    if (compactWeekList.isNotEmpty()) return compactWeekList

    // 尝试提取所有 "X-Y周" 或 "X周" 格式
    val allWeekRanges = Regex("(\\d{1,2})\\s*[-~到至]\\s*(\\d{1,2})\\s*周")
        .findAll(normalized)
        .map { match ->
            val start = match.groupValues[1].toIntOrNull() ?: return@map null
            val end = match.groupValues[2].toIntOrNull() ?: start
            start.coerceAtLeast(1)..end.coerceAtLeast(start)
        }
        .filterNotNull()
        .toList()

    if (allWeekRanges.isNotEmpty()) return allWeekRanges

    // 尝试提取单个周数
    val singleWeekRanges = Regex("(\\d{1,2})\\s*周")
        .findAll(normalized)
        .map { match ->
            val week = match.groupValues[1].toIntOrNull() ?: return@map null
            week..week
        }
        .filterNotNull()
        .toList()

    if (singleWeekRanges.isNotEmpty()) return singleWeekRanges

    return listOf(parseWeeks(text))
}

private fun String.toWeekRangeOrNull(): IntRange? {
    val match = Regex("第?\\s*(\\d{1,2})(?:\\s*[-~到至]\\s*(\\d{1,2}))?")
        .find(trim())
        ?: return null
    val start = match.groupValues[1].toIntOrNull() ?: return null
    val end = match.groupValues[2].toIntOrNull() ?: start
    return start.coerceAtLeast(1)..end.coerceAtLeast(start)
}

private fun parseWeeks(text: String): IntRange {
    val rangePatterns = listOf(
        Regex("周次\\s*[:：]?\\s*(\\d{1,2})\\s*[-~到至]\\s*(\\d{1,2})"),
        Regex("(\\d{1,2})\\s*[-~到至]\\s*(\\d{1,2})\\s*周"),
        Regex("第\\s*(\\d{1,2})\\s*[-~到至]\\s*(\\d{1,2})\\s*周"),
    )
    rangePatterns.forEach { pattern ->
        val match = pattern.find(text)
        if (match != null) {
            val start = match.groupValues[1].toInt()
            val end = match.groupValues[2].toInt()
            return start.coerceAtLeast(1)..end.coerceAtLeast(start)
        }
    }
    val single = Regex("第?\\s*(\\d{1,2})\\s*周").find(text)
    val week = single?.groupValues?.getOrNull(1)?.toIntOrNull()
    return if (week != null) week..week else 1..ScheduleDefaults.TOTAL_WEEKS
}

private fun parseTimeRange(text: String): Pair<LocalTime, LocalTime>? {
    val match = Regex("(\\d{1,2})[:：](\\d{2})\\s*[-~至到]\\s*(\\d{1,2})[:：](\\d{2})").find(text)
        ?: return null
    return match.groupValues.toTimeRangeAt(1)
}

private fun List<String>.toTimeRangeAt(startIndex: Int): Pair<LocalTime, LocalTime>? {
    return runCatching {
        LocalTime.of(get(startIndex).toInt(), get(startIndex + 1).toInt()) to
            LocalTime.of(get(startIndex + 2).toInt(), get(startIndex + 3).toInt())
    }.getOrNull()
}

private fun parsePeriodRange(text: String): IntRange? {
    val labeled = Regex("节次\\s*[:：]?\\s*(\\d{1,2})\\s*[-~到至]\\s*(\\d{1,2})").find(text)
    if (labeled != null) {
        return labeled.groupValues[1].toInt()..labeled.groupValues[2].toInt()
    }
    val range = Regex("第?\\s*(\\d{1,2})\\s*[-~到至]\\s*(\\d{1,2})\\s*节").find(text)
    if (range != null) {
        return range.groupValues[1].toInt()..range.groupValues[2].toInt()
    }
    val single = Regex("第\\s*(\\d{1,2})\\s*节").find(text)
    val period = single?.groupValues?.getOrNull(1)?.toIntOrNull()
    return period?.let { it..it }
}

private fun IntRange.toDefaultTimeRange(): Pair<LocalTime, LocalTime>? {
    val periodStart = mapOf(
        1 to LocalTime.of(8, 0),
        2 to LocalTime.of(8, 55),
        3 to LocalTime.of(10, 10),
        4 to LocalTime.of(11, 5),
        5 to LocalTime.of(13, 30),
        6 to LocalTime.of(14, 25),
        7 to LocalTime.of(15, 30),
        8 to LocalTime.of(16, 25),
        9 to LocalTime.of(18, 30),
        10 to LocalTime.of(19, 25),
        11 to LocalTime.of(20, 15),
        12 to LocalTime.of(21, 10),
    )
    val start = periodStart[first] ?: return null
    val endStart = periodStart[last] ?: start
    return start to endStart.plusMinutes(45)
}

private fun String.parseDateRangeStart(): LocalDate? {
    val full = Regex("(20\\d{2})[-/.](\\d{1,2})[-/.](\\d{1,2})\\s*(?:至|到|-|—|~)")
        .find(this)
    if (full != null) {
        return runCatching {
            LocalDate.of(full.groupValues[1].toInt(), full.groupValues[2].toInt(), full.groupValues[3].toInt())
        }.getOrNull()
    }
    val cn = Regex("(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*日\\s*(?:至|到|-|—|~)").find(this)
    if (cn != null) {
        val now = LocalDate.now()
        return runCatching {
            LocalDate.of(now.year, cn.groupValues[1].toInt(), cn.groupValues[2].toInt())
        }.getOrNull()
    }
    return null
}

private fun String.parseFullDate(): LocalDate? {
    val match = Regex("(20\\d{2})[-/.](\\d{1,2})[-/.](\\d{1,2})").find(this) ?: return null
    return runCatching {
        LocalDate.of(match.groupValues[1].toInt(), match.groupValues[2].toInt(), match.groupValues[3].toInt())
    }.getOrNull()
}

private fun String.guessSemesterName(title: String): String {
    val semester = Regex("(20\\d{2}\\s*[-—]\\s*20\\d{2}\\s*[春秋]季?学期)").find(this)
        ?.groupValues
        ?.getOrNull(1)
    return semester ?: title.ifBlank { "导入课表" }
}

private fun Int.absoluteValue(): Int {
    return if (this == Int.MIN_VALUE) 0 else kotlin.math.abs(this)
}

private fun List<Course>.inferTotalWeeks(): Int {
    return maxOfOrNull { it.weeks.last }
        ?.coerceAtLeast(ScheduleDefaults.TOTAL_WEEKS)
        ?: ScheduleDefaults.TOTAL_WEEKS
}

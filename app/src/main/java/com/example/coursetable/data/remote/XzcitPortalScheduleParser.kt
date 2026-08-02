package com.example.coursetable.data.remote

import com.example.coursetable.domain.model.Course
import com.example.coursetable.domain.model.CourseColorSeed
import com.example.coursetable.domain.model.ScheduleDefaults
import com.example.coursetable.domain.model.SemesterSchedule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

/**
 * 徐州工业职业技术学院课表解析器
 * 适配正方教务系统（jwpt.xzcit.cn）
 */
class XzcitPortalScheduleParser(
    private val zfsoftParser: ZfsoftScheduleParser = ZfsoftScheduleParser(),
    private val genericParser: GenericTableScheduleParser = GenericTableScheduleParser(
        parserId = "xzcit-portal",
        schoolKeywords = listOf(
            "jwpt.xzcit.cn",
            "徐州工业职业技术学院",
            "xzcit",
            "个人课表查询",  // 正方教务系统通用标识
            "zfsoft",  // 正方教务系统标识
            "zftal-ui",  // 正方教务系统UI框架
            "kbgrid_table_0",  // 课表网格表格ID
            "kblist_table",  // 课表列表表格ID
        ),
    ),
) : SchoolScheduleParser {
    override val parserId: String = "xzcit-portal"

    override fun parse(snapshot: PortalPageSnapshot): ScheduleParseResult {
        val haystack = "${snapshot.url}\n${snapshot.title}\n${snapshot.text}\n${snapshot.html}"
        val isXzcit = listOf("jwpt.xzcit.cn", "徐州工业职业技术学院", "xzcit")
            .any { haystack.contains(it, ignoreCase = true) }
        if (!isXzcit) {
            return ScheduleParseResult(null, 0, listOf("Xzcit keywords not matched"))
        }

        val zfsoftResult = zfsoftParser.parse(snapshot)
        if (zfsoftResult.isUsable) {
            return zfsoftResult.copy(
                confidence = 100,
                diagnostics = zfsoftResult.diagnostics + "Xzcit zfsoft parser selected",
            )
        }

        return genericParser.parse(snapshot).let { result ->
            if (result.isUsable) {
                result.copy(
                    confidence = (result.confidence - 5).coerceAtLeast(70),
                    diagnostics = result.diagnostics + "Xzcit generic fallback selected",
                )
            } else {
                result
            }
        }
    }
}

/**
 * 正方教务系统课表详细解析器
 * 专门处理正方教务系统的课表格式
 */
class ZfsoftScheduleParser : SchoolScheduleParser {
    override val parserId: String = "zfsoft-schedule"

    override fun parse(snapshot: PortalPageSnapshot): ScheduleParseResult {
        val html = snapshot.html
        val text = snapshot.text

        scheduleParserLog("[$parserId] Parsing: tables=${snapshot.tables.size}, html length=${html.length}, text length=${text.length}")

        // 检查是否是正方教务系统
        if (!isZfsoftSystem(html, text)) {
            scheduleParserLog("[$parserId] Not a zfsoft system")
            return ScheduleParseResult(null, 0, listOf("Not a zfsoft system"))
        }
        scheduleParserLog("[$parserId] Detected as zfsoft system")

        val readablePageText = text.ifBlank { html.stripZfsoftHtml() }
        val coursesFromKbGridHtml = parseKbGridHtml(html)
        scheduleParserLog("[$parserId] kbgrid html: ${coursesFromKbGridHtml.size} courses")
        if (coursesFromKbGridHtml.isNotEmpty()) {
            val semesterName = extractSemesterName(readablePageText)
            val calendar = inferAcademicCalendar(readablePageText)
            val totalWeeks = coursesFromKbGridHtml.inferTotalWeeks()

            return ScheduleParseResult(
                schedule = SemesterSchedule(
                    id = "zfsoft-${System.currentTimeMillis()}",
                    displayName = semesterName,
                    folderName = "zfsoft_${System.currentTimeMillis()}",
                    currentWeek = calendar.currentWeek,
                    totalWeeks = totalWeeks,
                    semesterStartDate = calendar.semesterStartDate,
                    courses = coursesFromKbGridHtml,
                ),
                confidence = 98,
                diagnostics = listOf("Parsed ${coursesFromKbGridHtml.size} courses from zfsoft kbgrid html"),
            )
        }

        val coursesFromGroupedListHtml = parseGroupedListHtml(html)
        scheduleParserLog("[$parserId] grouped list html: ${coursesFromGroupedListHtml.size} courses")
        if (coursesFromGroupedListHtml.isNotEmpty()) {
            val semesterName = extractSemesterName(readablePageText)
            val calendar = inferAcademicCalendar(readablePageText)
            val totalWeeks = coursesFromGroupedListHtml.inferTotalWeeks()

            return ScheduleParseResult(
                schedule = SemesterSchedule(
                    id = "zfsoft-${System.currentTimeMillis()}",
                    displayName = semesterName,
                    folderName = "zfsoft_${System.currentTimeMillis()}",
                    currentWeek = calendar.currentWeek,
                    totalWeeks = totalWeeks,
                    semesterStartDate = calendar.semesterStartDate,
                    courses = coursesFromGroupedListHtml,
                ),
                confidence = 94,
                diagnostics = listOf("Parsed ${coursesFromGroupedListHtml.size} courses from zfsoft grouped list html"),
            )
        }

        // 尝试从详细列表格式解析
        val coursesFromList = parseDetailListFormat(text)
        scheduleParserLog("[$parserId] Detail list: ${coursesFromList.size} courses")
        if (coursesFromList.isNotEmpty()) {
            val semesterName = extractSemesterName(text)
            val calendar = inferAcademicCalendar(text)
            val totalWeeks = coursesFromList.inferTotalWeeks()

            return ScheduleParseResult(
                schedule = SemesterSchedule(
                    id = "zfsoft-${System.currentTimeMillis()}",
                    displayName = semesterName,
                    folderName = "zfsoft_${System.currentTimeMillis()}",
                    currentWeek = calendar.currentWeek,
                    totalWeeks = totalWeeks,
                    semesterStartDate = calendar.semesterStartDate,
                    courses = coursesFromList,
                ),
                confidence = 85,
                diagnostics = listOf("Parsed ${coursesFromList.size} courses from zfsoft detail list"),
            )
        }

        // 尝试从网格格式解析
        scheduleParserLog("[$parserId] Trying grid format: ${snapshot.tables.size} tables")
        val coursesFromGrid = parseGridFormat(snapshot)
        scheduleParserLog("[$parserId] Grid format: ${coursesFromGrid.size} courses")
        if (coursesFromGrid.isNotEmpty()) {
            val semesterName = extractSemesterName(text)
            val calendar = inferAcademicCalendar(text)
            val totalWeeks = coursesFromGrid.inferTotalWeeks()

            return ScheduleParseResult(
                schedule = SemesterSchedule(
                    id = "zfsoft-${System.currentTimeMillis()}",
                    displayName = semesterName,
                    folderName = "zfsoft_${System.currentTimeMillis()}",
                    currentWeek = calendar.currentWeek,
                    totalWeeks = totalWeeks,
                    semesterStartDate = calendar.semesterStartDate,
                    courses = coursesFromGrid,
                ),
                confidence = 80,
                diagnostics = listOf("Parsed ${coursesFromGrid.size} courses from zfsoft grid"),
            )
        }

        return ScheduleParseResult(null, 0, listOf("No courses found in zfsoft format"))
    }

    private fun isZfsoftSystem(html: String, text: String): Boolean {
        val indicators = listOf(
            "zfsoft",
            "正方",
            "个人课表查询",
            "zftal-ui",
            "教学班组成",
            "考核方式",
        )
        val combined = "$html\n$text"
        return indicators.any { combined.contains(it, ignoreCase = true) }
    }

    /**
     * 解析详细列表格式（第二个表格）
     * 格式：星期一 1-2 课程名 周数：... 校区：... 上课地点：... 教师：...
     */
    private fun parseDetailListFormat(text: String): List<Course> {
        val courses = mutableListOf<Course>()
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        var currentDay: DayOfWeek? = null

        var i = 0
        while (i < lines.size) {
            val line = lines[i]

            // 检测星期几
            val day = line.toDayOfWeek()
            if (day != null) {
                currentDay = day
                i++
                continue
            }

            // 检测节次行（如 "1-2", "3-4", "5-6", "7-8", "9-10"）
            val periodMatch = Regex("^(\\d{1,2})\\s*[-~]\\s*(\\d{1,2})$").find(line)
            if (periodMatch != null && currentDay != null) {
                val startPeriod = periodMatch.groupValues[1].toInt()
                val endPeriod = periodMatch.groupValues[2].toInt()
                val timeRange = periodToTimeRange(startPeriod, endPeriod)

                // 下一行是课程详情
                if (i + 1 < lines.size) {
                    val courseLine = lines[i + 1]
                    val parsedCourses = parseCoursesFromDetailLine(
                        text = courseLine,
                        dayOfWeek = currentDay,
                        startTime = timeRange.first,
                        endTime = timeRange.second,
                    )
                    courses.addAll(parsedCourses)
                    i += 2
                    continue
                }
            }

            // 单行课程详情（没有明确的节次行）
            if (currentDay != null && line.contains("周数：") && line.contains("教师")) {
                val parsedCourses = parseCoursesFromDetailLine(
                    text = line,
                    dayOfWeek = currentDay,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(8, 45),
                )
                courses.addAll(parsedCourses)
            }

            i++
        }

        return courses.distinctBy {
            "${it.name}-${it.dayOfWeek}-${it.startTime}-${it.endTime}-${it.weeks}"
        }
    }

    /**
     * 从详细行解析课程信息
     * 格式：课程名★ 周数：X-Y周 校区：XXX 上课地点：XXX 教师：XXX
     */
    private fun parseCoursesFromDetailLine(
        text: String,
        dayOfWeek: DayOfWeek,
        startTime: LocalTime,
        endTime: LocalTime,
    ): List<Course> {
        // 提取课程名称（去掉【调】前缀和★后缀）
        val nameMatch = Regex("(?:【[^】]*】)?([^★]+)★?").find(text)
        val name = nameMatch?.groupValues?.getOrNull(1)?.trim() ?: return emptyList()
        if (name.isBlank()) return emptyList()

        // 提取周数
        val weekRanges = parseWeekRangesFromZfsoft(text)
        if (weekRanges.isEmpty()) return emptyList()

        val campus = extractLabeledValue(
            text = text,
            labels = listOf("校区"),
            stopLabels = zfsoftStopLabels - "校区",
        ).orEmpty()
        val room = extractLabeledValue(
            text = text,
            labels = listOf("上课地点", "地点", "教室"),
            stopLabels = zfsoftStopLabels - "上课地点" - "地点" - "教室",
        ).orEmpty()
        val teacher = extractLabeledValue(
            text = text,
            labels = listOf("教师", "任课教师", "授课教师", "老师"),
            stopLabels = zfsoftStopLabels - "教师" - "任课教师" - "授课教师" - "老师",
        ).orEmpty()
        val location = mergeCampusAndRoom(campus, room)

        return weekRanges.map { weeks ->
            Course(
                id = "xzcit-${dayOfWeek.name.lowercase(Locale.US)}-${startTime}-${name.hashCode()}-${weeks.first}-${weeks.last}",
                name = name.take(40),
                dayOfWeek = dayOfWeek,
                startTime = startTime,
                endTime = endTime,
                location = location.ifBlank { "未识别" }.take(40),
                teacher = teacher.ifBlank { "未识别" }.take(32),
                weeks = weeks,
                colorSeed = CourseColorSeed.entries[name.hashCode().absoluteValue() % CourseColorSeed.entries.size],
            )
        }
    }

    /**
     * 解析正方教务系统的周数格式
     * 支持：2-5周,7-9周,11-19周 和 4-6周(双),7-8周
     */
    private fun parseWeekRangesFromZfsoft(text: String): List<IntRange> {
        val weekSection = extractLabeledValue(
            text = text,
            labels = listOf("周数", "周次"),
            stopLabels = zfsoftStopLabels - "周数" - "周次",
        )

        if (weekSection != null) {
            return weekSection
                .split(',', '，', '、')
                .flatMap { token -> token.toZfsoftWeekRanges() }
                .ifEmpty { listOf(1..ScheduleDefaults.TOTAL_WEEKS) }
        }

        val afterPeriodExpression = text
            .replace(
                Regex("[（(]\\s*\\d{1,2}\\s*[-~到至]\\s*\\d{1,2}\\s*节\\s*[）)]"),
                " ",
            )
            .trim()
        if (afterPeriodExpression != text.trim()) {
            val ranges = afterPeriodExpression
                .split(',', '，', '、')
                .flatMap { token -> token.toZfsoftWeekRanges() }
            if (ranges.isNotEmpty()) return ranges
        }

        val compactWeekList = Regex(
            "((?:\\d{1,2}\\s*[-~到至]\\s*\\d{1,2}|\\d{1,2}\\s*周?\\s*[（(]\\s*[单双]\\s*[）)])(?:\\s*[,，、]\\s*(?:\\d{1,2}\\s*[-~到至]\\s*\\d{1,2}|\\d{1,2}\\s*周?\\s*[（(]\\s*[单双]\\s*[）)]|\\d{1,2}))+)[\\s]*周?",
        ).find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.split(',', '，', '、')
            ?.flatMap { token -> token.toZfsoftWeekRanges() }
            .orEmpty()
        if (compactWeekList.isNotEmpty()) return compactWeekList

        val inlineWeeks = Regex("(\\d{1,2})(?:\\s*[-~]\\s*(\\d{1,2}))?\\s*周\\s*(?:[（(]\\s*([单双])\\s*[）)])?")
            .findAll(text)
        val ranges = inlineWeeks.flatMap { match ->
            val start = match.groupValues[1].toIntOrNull() ?: return@flatMap emptyList()
            val end = match.groupValues[2].toIntOrNull() ?: start
            val parity = match.groupValues.getOrNull(3).orEmpty().ifBlank { null }
            buildZfsoftWeekRanges(start, end, parity)
        }.toList()

        if (ranges.isNotEmpty()) return ranges

        // 尝试提取单个周数
        val singleWeeks = Regex("(\\d{1,2})\\s*周").findAll(text)
        val singleRanges = singleWeeks.map { match ->
            val week = match.groupValues[1].toIntOrNull() ?: return@map null
            week..week
        }.filterNotNull().toList()

        if (singleRanges.isNotEmpty()) return singleRanges

        // 默认使用完整学期周数
        return listOf(1..ScheduleDefaults.TOTAL_WEEKS)
    }

    /**
     * 解析网格格式（第一个表格）
     */
    private fun parseKbGridHtml(html: String): List<Course> {
        if (!html.contains("kbgrid_table", ignoreCase = true)) return emptyList()
        val courses = mutableListOf<Course>()
        val cellRegex = Regex(
            "<td\\b([^>]*)\\bid\\s*=\\s*[\"'](\\d{1,2})-(\\d{1,2})[\"'][^>]*>(.*?)</td>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        val courseBlockRegex = Regex(
            "<div\\b[^>]*class\\s*=\\s*[\"'][^\"']*\\btimetable_con\\b[^\"']*[\"'][^>]*>(.*?)</div>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )

        cellRegex.findAll(html).forEach { cellMatch ->
            val cellAttributes = cellMatch.groupValues[1]
            val day = cellMatch.groupValues[2].toIntOrNull()?.let { dayValue ->
                runCatching { DayOfWeek.of(dayValue) }.getOrNull()
            } ?: return@forEach
            val defaultPeriod = cellMatch.groupValues[3].toIntOrNull() ?: return@forEach
            val rowSpan = cellAttributes.extractZfsoftSpan("rowspan")
            val defaultPeriods = defaultPeriod..(defaultPeriod + rowSpan - 1)
            val cellHtml = cellMatch.groupValues[4]

            courseBlockRegex.findAll(cellHtml).forEach { blockMatch ->
                courses += parseKbGridCourseBlock(
                    blockHtml = blockMatch.groupValues[1],
                    dayOfWeek = day,
                    defaultPeriods = defaultPeriods,
                )
            }
        }

        return courses.distinctBy {
            "${it.name}-${it.dayOfWeek}-${it.startTime}-${it.endTime}-${it.location}-${it.teacher}-${it.weeks}"
        }.sortedWith(
            compareBy<Course> { it.dayOfWeek.value }
                .thenBy { it.startTime }
                .thenBy { it.name },
        )
    }

    private fun parseKbGridCourseBlock(
        blockHtml: String,
        dayOfWeek: DayOfWeek,
        defaultPeriods: IntRange,
    ): List<Course> = parseZfsoftCourseBlock(
        blockHtml = blockHtml,
        dayOfWeek = dayOfWeek,
        defaultPeriods = defaultPeriods,
        source = "kbgrid",
    )

    private fun parseGroupedListHtml(html: String): List<Course> {
        if (!html.contains("timetable_con", ignoreCase = true)) return emptyList()

        val courses = mutableListOf<Course>()
        val tbodyRegex = Regex(
            "<tbody\\b[^>]*\\bid\\s*=\\s*[\"']xq_(\\d)[\"'][^>]*>(.*?)</tbody>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        val periodSegmentRegex = Regex(
            "<td\\b[^>]*\\bid\\s*=\\s*[\"']jc_(\\d)-(\\d{1,2})-(\\d{1,2})[\"'][^>]*>.*?(?=<td\\b[^>]*\\bid\\s*=\\s*[\"']jc_\\d-|</tbody>|\\z)",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )

        tbodyRegex.findAll(html).forEach { bodyMatch ->
            val day = bodyMatch.groupValues[1].toIntOrNull()?.let { value ->
                runCatching { DayOfWeek.of(value) }.getOrNull()
            } ?: return@forEach
            val bodyHtml = bodyMatch.groupValues[2]

            periodSegmentRegex.findAll(bodyHtml).forEach { segmentMatch ->
                val segmentDay = segmentMatch.groupValues[1].toIntOrNull()
                if (segmentDay != null && segmentDay != day.value) return@forEach
                val startPeriod = segmentMatch.groupValues[2].toIntOrNull() ?: return@forEach
                val endPeriod = segmentMatch.groupValues[3].toIntOrNull() ?: startPeriod
                val defaultPeriods = startPeriod..endPeriod

                extractZfsoftCourseBlocks(segmentMatch.value).forEach { blockHtml ->
                    courses += parseZfsoftCourseBlock(
                        blockHtml = blockHtml,
                        dayOfWeek = day,
                        defaultPeriods = defaultPeriods,
                        source = "grouped",
                    )
                }
            }
        }

        return courses.distinctBy {
            "${it.name}-${it.dayOfWeek}-${it.startTime}-${it.endTime}-${it.location}-${it.teacher}-${it.weeks}"
        }.sortedWith(
            compareBy<Course> { it.dayOfWeek.value }
                .thenBy { it.startTime }
                .thenBy { it.name },
        )
    }

    private fun parseZfsoftCourseBlock(
        blockHtml: String,
        dayOfWeek: DayOfWeek,
        defaultPeriods: IntRange,
        source: String,
    ): List<Course> {
        val titleHtml = Regex(
            "<(?:u|span|div)\\b[^>]*class\\s*=\\s*[\"'][^\"']*\\btitle\\b[^\"']*[\"'][^>]*>(.*?)</(?:u|span|div)>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).find(blockHtml)?.groupValues?.getOrNull(1)
        val name = titleHtml
            ?.stripZfsoftHtml()
            ?.replace(Regex("^【[^】]*】"), "")
            ?.replace("★", "")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return emptyList()

        val periodAndWeekText = blockHtml.extractZfsoftParagraph("节/周")
        val blockText = blockHtml.stripZfsoftHtml()
        val weekText = periodAndWeekText.ifBlank { blockText }
        val paragraphLocation = blockHtml.extractZfsoftParagraph("上课地点")
            .replace(Regex("\\s+"), " ")
            .trim()
        val paragraphTeacher = blockHtml.extractZfsoftParagraph("教师")
            .replace("*", "")
            .replace(Regex("\\s+"), " ")
            .trim()
        val campus = extractLabeledValue(
            text = blockText,
            labels = listOf("校区"),
            stopLabels = zfsoftStopLabels - "校区",
        ).orEmpty()
        val room = extractLabeledValue(
            text = blockText,
            labels = listOf("上课地点", "地点", "教室"),
            stopLabels = zfsoftStopLabels - "上课地点" - "地点" - "教室",
        ).orEmpty()
        val teacher = paragraphTeacher.ifBlank {
            extractLabeledValue(
                text = blockText,
                labels = listOf("教师", "任课教师", "授课教师", "老师"),
                stopLabels = zfsoftStopLabels - "教师" - "任课教师" - "授课教师" - "老师",
            ).orEmpty()
        }
        val location = mergeCampusAndRoom(
            campus = campus,
            room = room.ifBlank { paragraphLocation },
        )
        val periods = weekText.parseZfsoftPeriodRange() ?: defaultPeriods
        val weekRanges = parseWeekRangesFromZfsoft(weekText)
        val timeRange = periodToTimeRange(periods.first, periods.last)

        return weekRanges.mapIndexed { index, weeks ->
            Course(
                id = "xzcit-$source-${dayOfWeek.name.lowercase(Locale.US)}-${periods.first}-${periods.last}-${name.hashCode()}-${weeks.first}-${weeks.last}-$index",
                name = name.take(40),
                dayOfWeek = dayOfWeek,
                startTime = timeRange.first,
                endTime = timeRange.second,
                location = location.ifBlank { "未识别" }.take(40),
                teacher = teacher.ifBlank { "未识别" }.take(32),
                weeks = weeks,
                colorSeed = CourseColorSeed.entries[name.hashCode().absoluteValue() % CourseColorSeed.entries.size],
            )
        }
    }

    private fun extractZfsoftCourseBlocks(html: String): List<String> {
        val blockRegex = Regex(
            "<div\\b[^>]*class\\s*=\\s*[\"'][^\"']*\\btimetable_con\\b[^\"']*[\"'][^>]*>(.*?)</div>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        return blockRegex.findAll(html).map { it.groupValues[1] }.toList()
    }

    private fun parseGridFormat(snapshot: PortalPageSnapshot): List<Course> {
        val courses = mutableListOf<Course>()

        scheduleParserLog("[zfsoft] parseGridFormat: ${snapshot.tables.size} tables")
        for ((tableIndex, table) in snapshot.tables.withIndex()) {
            val matrix = table.expandZfsoftMatrix()
            scheduleParserLog("[zfsoft] Table $tableIndex: ${matrix.size} expanded rows")
            if (matrix.size < 3) {
                scheduleParserLog("[zfsoft] Table $tableIndex: skipped (< 3 rows)")
                continue
            }

            val headerIndex = matrix.indexOfFirst { row ->
                row.count { it.text.toDayOfWeek() != null } >= 4
            }
            if (headerIndex < 0) {
                scheduleParserLog("[zfsoft] Table $tableIndex: skipped (no weekday header)")
                continue
            }
            val headerRow = matrix[headerIndex]
            scheduleParserLog("[zfsoft] Table $tableIndex header: ${headerRow.map { it.text.take(10) }}")
            val dayColumns = headerRow.mapIndexedNotNull { index, cell ->
                val day = cell.text.toDayOfWeek()
                day?.let { it to index }
            }
            scheduleParserLog("[zfsoft] Table $tableIndex dayColumns: $dayColumns")

            if (dayColumns.size < 4) {
                scheduleParserLog("[zfsoft] Table $tableIndex: skipped (< 4 day columns)")
                continue
            }

            val firstDayColumn = dayColumns.minOf { it.second }
            val periodTimeTable = buildZfsoftPeriodTimeTable(matrix, firstDayColumn, headerIndex + 1)
            val processedCells = mutableSetOf<String>()

            for (rowIndex in headerIndex + 1 until matrix.size) {
                val row = matrix[rowIndex]
                val rowContext = row.take(firstDayColumn)
                    .map { it.text }
                    .filter { it.isNotBlank() }
                val defaultPeriods = rowContext.joinToString("\n").parseZfsoftPeriodRange()

                for ((day, colIndex) in dayColumns) {
                    val cell = row.getOrNull(colIndex) ?: continue
                    val cellText = cell.text.trim()
                    if (!cellText.looksLikeZfsoftCourseCell()) continue

                    val cellKey = "${cell.originRow}-${cell.originColumn}-${day.value}"
                    if (!processedCells.add(cellKey)) continue

                    val normalizedText = normalizeCellTextForParsing(cellText)
                    scheduleParserLog("[zfsoft] Table $tableIndex row $rowIndex col $colIndex: normalizedText='${normalizedText.take(50)}'")
                    val cellCourses = parseGridCell(
                        text = normalizedText,
                        dayOfWeek = day,
                        defaultPeriods = defaultPeriods,
                        periodTimeTable = periodTimeTable,
                    )
                    scheduleParserLog("[zfsoft] Table $tableIndex row $rowIndex col $colIndex: ${cellCourses.size} courses")
                    courses.addAll(cellCourses)
                }
            }
        }

        return courses.distinctBy {
            "${it.name}-${it.dayOfWeek}-${it.startTime}-${it.endTime}-${it.weeks}"
        }
    }

    /**
     * 将多行单元格文本转换为单行格式
     * 将 "教师：赵璐" 和 "上课地点：九里校区 B01-505" 等标签值对合并到主文本中
     * 格式：课程名★ (1-2节)2-5周,7-9周 地点 教师
     * 注意：当单元格包含多个课程时，每个课程独立处理
     */
    private fun normalizeCellTextForParsing(text: String): String {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.size <= 1) return text

        // 将多个课程分割开
        val courses = mutableListOf<MutableList<String>>()
        var currentCourse = mutableListOf<String>()

        for (line in lines) {
            // 检测新课程的开始（以 ★ 结尾的行，或者是 【调】 开头的行）
            if ((line.endsWith("★") || line.startsWith("【调】")) && currentCourse.isNotEmpty()) {
                courses.add(currentCourse)
                currentCourse = mutableListOf()
            }
            currentCourse.add(line)
        }
        if (currentCourse.isNotEmpty()) {
            courses.add(currentCourse)
        }

        // 如果只有一个课程，直接处理
        if (courses.size <= 1) {
            return normalizeSingleCourseText(lines)
        }

        // 多个课程，用分隔符分开
        return courses.joinToString("\n---\n") { courseLines ->
            normalizeSingleCourseText(courseLines)
        }
    }

    /**
     * 将单个课程的多行文本转换为单行格式
     */
    private fun normalizeSingleCourseText(lines: List<String>): String {
        val mainParts = mutableListOf<String>()
        var location = ""
        var teacher = ""
        var campus = ""

        for (line in lines) {
            when {
                line.startsWith("教师：") || line.startsWith("教师:") -> {
                    teacher = line.substringAfter("：").substringAfter(":").trim()
                }
                line.startsWith("上课地点：") || line.startsWith("上课地点:") -> {
                    location = line.substringAfter("：").substringAfter(":").trim()
                }
                line.startsWith("校区：") || line.startsWith("校区:") -> {
                    campus = line.substringAfter("：").substringAfter(":").trim()
                }
                line.startsWith("教学班") || line.startsWith("考核") || line.startsWith("选课") ||
                line.startsWith("课程学时") || line.startsWith("周学时") || line.startsWith("总学时") ||
                line.startsWith("学分") -> {
                    // 跳过其他非关键信息
                }
                else -> {
                    mainParts.add(line)
                }
            }
        }

        // 构建结果：主文本 + 地点 + 教师
        val result = StringBuilder()
        result.append(mainParts.joinToString(" "))
        if (campus.isNotBlank()) result.append("  校区：$campus")
        if (location.isNotBlank()) result.append("  上课地点：$location")
        if (teacher.isNotBlank()) result.append("  教师：$teacher")

        return result.toString().trim()
    }

    /**
     * 解析网格单元格中的课程
     * 格式：课程名★ (1-2节)2-5周,7-9周 上课地点 教师
     */
    private fun parseGridCell(
        text: String,
        dayOfWeek: DayOfWeek,
        defaultPeriods: IntRange?,
        periodTimeTable: ZfsoftPeriodTimeTable,
    ): List<Course> {
        val courses = mutableListOf<Course>()

        // 分割多个课程（支持多种分隔符）
        val courseTexts = text.split(Regex("\\n---\\n|\\n\\s*\\n+|[-=]{3,}"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        for (courseText in courseTexts) {
            val cellCourses = parseSingleGridCourse(
                text = courseText,
                dayOfWeek = dayOfWeek,
                defaultPeriods = defaultPeriods,
                periodTimeTable = periodTimeTable,
            )
            courses.addAll(cellCourses)
        }

        return courses
    }

    /**
     * 解析单个网格课程
     * 格式：课程名★ (1-2节)2-5周,7-9周 地点 教师
     * 返回一个课程列表，每个课程对应一个连续的周数范围
     */
    private fun parseSingleGridCourse(
        text: String,
        dayOfWeek: DayOfWeek,
        defaultPeriods: IntRange?,
        periodTimeTable: ZfsoftPeriodTimeTable,
    ): List<Course> {
        // 提取课程名称
        val name = text.extractZfsoftCourseName() ?: return emptyList()
        if (name.isBlank()) return emptyList()

        // 提取节次
        val periods = text.parseZfsoftPeriodRange() ?: defaultPeriods ?: return emptyList()
        val timeRange = periodTimeTable.resolve(periods)
            ?: periodToTimeRange(periods.first, periods.last)

        // 提取周数
        val weekRanges = parseWeekRangesFromZfsoft(text)
        if (weekRanges.isEmpty()) return emptyList()

        val teacher = extractLabeledValue(
            text = text,
            labels = listOf("教师", "任课教师", "授课教师", "老师"),
            stopLabels = zfsoftStopLabels - "教师" - "任课教师" - "授课教师" - "老师",
        ).orEmpty()
        val campus = extractLabeledValue(
            text = text,
            labels = listOf("校区"),
            stopLabels = zfsoftStopLabels - "校区",
        ).orEmpty()
        val room = extractLabeledValue(
            text = text,
            labels = listOf("上课地点", "地点", "教室"),
            stopLabels = zfsoftStopLabels - "上课地点" - "地点" - "教室",
        ).orEmpty()

        val locationTeacher = if (teacher.isBlank() || room.isBlank()) {
            extractLocationAndTeacher(text.removeZfsoftCourseSyntax(name))
        } else {
            "" to ""
        }
        val location = mergeCampusAndRoom(
            campus = campus,
            room = room.ifBlank { locationTeacher.first },
        )
        val finalTeacher = teacher.ifBlank { locationTeacher.second }

        // 为每个周数范围创建一个课程
        return weekRanges.mapIndexed { index, weeks ->
            Course(
                id = "xzcit-grid-${dayOfWeek.name.lowercase(Locale.US)}-${periods.first}-${periods.last}-${name.hashCode()}-${index}",
                name = name.take(40),
                dayOfWeek = dayOfWeek,
                startTime = timeRange.first,
                endTime = timeRange.second,
                location = location.ifBlank { "未识别" }.take(40),
                teacher = finalTeacher.ifBlank { "未识别" }.take(32),
                weeks = weeks,
                colorSeed = CourseColorSeed.entries[name.hashCode().absoluteValue() % CourseColorSeed.entries.size],
            )
        }
    }

    /**
     * 从文本中提取地点和教师
     * 地点通常包含 "校区"、"楼"、"室"、"B" 等关键词
     * 教师通常是中文名字（2-4个字）
     */
    private fun extractLocationAndTeacher(text: String): Pair<String, String> {
        if (text.isBlank()) return "" to ""

        // 尝试用多个空格分割
        val parts = text.split(Regex("\\s{2,}"))
        if (parts.size >= 2) {
            return parts[0].trim() to parts[1].trim()
        }

        // 如果只有一个部分，尝试用单个空格分割
        val singleSpaceParts = text.split(" ")
        if (singleSpaceParts.size >= 2) {
            // 找到地点和教师的分界点
            // 地点通常包含 "校区"、"楼"、"室"、"B"、"Z"、"F"、"未排" 等关键词
            val locationKeywords = listOf("校区", "楼", "室", "B", "Z", "F", "实验", "教学", "体育", "图书馆", "未排", "在线")
            var splitIndex = -1
            for (i in singleSpaceParts.indices) {
                val part = singleSpaceParts[i]
                if (locationKeywords.any { part.contains(it) }) {
                    splitIndex = i
                }
            }

            if (splitIndex >= 0 && splitIndex < singleSpaceParts.size - 1) {
                val location = singleSpaceParts.take(splitIndex + 1).joinToString(" ")
                val teacher = singleSpaceParts.drop(splitIndex + 1).joinToString(" ")
                return location.trim() to teacher.trim()
            }

            // 如果找不到分界点，假设最后一部分是教师
            val location = singleSpaceParts.dropLast(1).joinToString(" ")
            val teacher = singleSpaceParts.last()
            return location.trim() to teacher.trim()
        }

        // 如果只有一个词，返回空
        return text to ""
    }

    private fun String.extractZfsoftParagraph(titleKeyword: String): String {
        val paragraphRegex = Regex(
            "<p\\b[^>]*>(.*?)</p>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        return paragraphRegex.findAll(this)
            .firstOrNull { paragraph ->
                paragraph.groupValues[1].contains("title=\"$titleKeyword") ||
                    paragraph.groupValues[1].contains("title='$titleKeyword")
            }
            ?.groupValues
            ?.getOrNull(1)
            ?.stripZfsoftHtml()
            ?.trim()
            .orEmpty()
    }

    private fun String.extractZfsoftSpan(name: String): Int {
        return Regex("$name\\s*=\\s*['\"]?(\\d+)", RegexOption.IGNORE_CASE)
            .find(this)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?.coerceAtLeast(1)
            ?: 1
    }

    private fun String.stripZfsoftHtml(): String {
        return replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</p>|</div>|</td>|</th>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("[ \\t\\x0B\\f\\r]+"), " ")
            .lines()
            .joinToString("\n") { it.trim() }
            .replace(Regex("\\n{2,}"), "\n")
            .trim()
    }

    private fun String.looksLikeZfsoftCourseCell(): Boolean {
        val normalized = trim()
        if (normalized.isBlank() || normalized == "无") return false
        if (normalized.toDayOfWeek() != null) return false
        val signals = listOf("★", "周", "节", "教师", "上课地点", "校区", "教学班", "考核方式")
        return signals.any { normalized.contains(it) } && normalized.any { it in '\u4e00'..'\u9fff' }
    }

    private fun String.extractZfsoftCourseName(): String? {
        val cleaned = replace(Regex("^【[^】]*】"), "").trim()
        val beforeStar = cleaned.substringBefore("★").trim()
        val candidate = beforeStar
            .ifBlank { cleaned.substringBefore("(").substringBefore("（").trim() }
            .trim(' ', '　', ':', '：')
        if (candidate.isBlank()) return null
        if (candidate.contains("周数") || candidate.contains("教师") || candidate.contains("上课地点")) return null
        return candidate
    }

    private fun String.parseZfsoftPeriodRange(): IntRange? {
        val patterns = listOf(
            Regex("[（(]\\s*(\\d{1,2})\\s*[-~到至]\\s*(\\d{1,2})\\s*节\\s*[）)]"),
            Regex("节次\\s*[:：]?\\s*(\\d{1,2})\\s*[-~到至]\\s*(\\d{1,2})"),
            Regex("第?\\s*(\\d{1,2})\\s*[-~到至]\\s*(\\d{1,2})\\s*节"),
            Regex("(?m)^\\s*(\\d{1,2})\\s*[-~]\\s*(\\d{1,2})\\s*$"),
        )
        patterns.forEach { pattern ->
            val match = pattern.find(this)
            if (match != null) {
                val start = match.groupValues[1].toIntOrNull() ?: return@forEach
                val end = match.groupValues[2].toIntOrNull() ?: start
                if (start in 1..20 && end in start..20) return start..end
            }
        }
        val single = Regex("第?\\s*(\\d{1,2})\\s*节").find(this)
            ?: Regex("(?m)^\\s*(\\d{1,2})\\s*$").find(this)
        val period = single?.groupValues?.getOrNull(1)?.toIntOrNull()
        return period?.takeIf { it in 1..20 }?.let { it..it }
    }

    private fun String.parseZfsoftTimeRange(): Pair<LocalTime, LocalTime>? {
        val match = Regex("(\\d{1,2})[:：](\\d{2})\\s*[-~至到]\\s*(\\d{1,2})[:：](\\d{2})").find(this)
            ?: return null
        return runCatching {
            LocalTime.of(match.groupValues[1].toInt(), match.groupValues[2].toInt()) to
                LocalTime.of(match.groupValues[3].toInt(), match.groupValues[4].toInt())
        }.getOrNull()
    }

    private fun String.removeZfsoftCourseSyntax(courseName: String): String {
        return replace(courseName, "")
            .replace(Regex("【[^】]*】"), " ")
            .replace("★", " ")
            .replace(Regex("[（(]\\s*\\d{1,2}\\s*[-~到至]\\s*\\d{1,2}\\s*节\\s*[）)]"), " ")
            .replace(Regex("\\d{1,2}(?:\\s*[-~]\\s*\\d{1,2})?\\s*周\\s*(?:[（(]\\s*[单双]\\s*[）)])?\\s*[,，、]?"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun String.toZfsoftWeekRanges(): List<IntRange> {
        val match = Regex("(\\d{1,2})(?:\\s*[-~]\\s*(\\d{1,2}))?\\s*周?\\s*(?:[（(]\\s*([单双])\\s*[）)])?")
            .find(this)
            ?: return emptyList()
        val start = match.groupValues[1].toIntOrNull() ?: return emptyList()
        val end = match.groupValues[2].toIntOrNull() ?: start
        val parity = match.groupValues.getOrNull(3).orEmpty().ifBlank { null }
        return buildZfsoftWeekRanges(start, end, parity)
    }

    private fun buildZfsoftWeekRanges(start: Int, end: Int, parity: String?): List<IntRange> {
        val safeStart = start.coerceAtLeast(1)
        val safeEnd = end.coerceAtLeast(safeStart)
        val weeks = when (parity) {
            "单" -> (safeStart..safeEnd).filter { it % 2 == 1 }
            "双" -> (safeStart..safeEnd).filter { it % 2 == 0 }
            else -> return listOf(safeStart..safeEnd)
        }
        return weeks.map { it..it }.ifEmpty { emptyList() }
    }

    private val zfsoftStopLabels = listOf(
        "周数",
        "周次",
        "校区",
        "上课地点",
        "地点",
        "教室",
        "教师",
        "任课教师",
        "授课教师",
        "老师",
        "教学班",
        "教学班组成",
        "考核方式",
        "选课备注",
        "课程学时组成",
        "周学时",
        "总学时",
        "学分",
    )

    private fun extractLabeledValue(
        text: String,
        labels: List<String>,
        stopLabels: List<String>,
    ): String? {
        labels.forEach { label ->
            val match = Regex("${Regex.escape(label)}\\s*[:：]").find(text)
            if (match != null) {
                val raw = text.substring(match.range.last + 1).trim()
                val cut = stopLabels
                    .mapNotNull { stop ->
                        Regex("${Regex.escape(stop)}\\s*[:：]")
                            .find(raw)
                            ?.range
                            ?.first
                            ?.takeIf { it > 0 }
                    }
                    .minOrNull()
                    ?: raw.length
                return raw.substring(0, cut)
                    .trim(' ', '　', ',', '，', ';', '；', '。')
                    .ifBlank { null }
            }
        }
        return null
    }

    private fun mergeCampusAndRoom(campus: String, room: String): String {
        return when {
            campus.isBlank() -> room
            room.isBlank() -> campus
            room.contains(campus) -> room
            else -> "$campus $room"
        }.trim()
    }

    private data class ZfsoftExpandedCell(
        val text: String,
        val originRow: Int,
        val originColumn: Int,
    )

    private fun DomTable.expandZfsoftMatrix(): List<List<ZfsoftExpandedCell>> {
        val occupied = mutableMapOf<Pair<Int, Int>, ZfsoftExpandedCell>()
        val result = mutableListOf<List<ZfsoftExpandedCell>>()
        rows.forEachIndexed { rowIndex, row ->
            val expandedRow = mutableListOf<ZfsoftExpandedCell>()
            var columnIndex = 0
            row.forEachIndexed { sourceColumn, cell ->
                while (occupied.containsKey(rowIndex to columnIndex)) {
                    expandedRow += occupied.getValue(rowIndex to columnIndex)
                    columnIndex++
                }
                val expandedCell = ZfsoftExpandedCell(
                    text = cell.text,
                    originRow = rowIndex,
                    originColumn = sourceColumn,
                )
                repeat(cell.colSpan) { colOffset ->
                    expandedRow += expandedCell
                    for (rowOffset in 1 until cell.rowSpan) {
                        occupied[(rowIndex + rowOffset) to (columnIndex + colOffset)] = expandedCell
                    }
                }
                columnIndex += cell.colSpan
            }
            while (occupied.containsKey(rowIndex to columnIndex)) {
                expandedRow += occupied.getValue(rowIndex to columnIndex)
                columnIndex++
            }
            if (expandedRow.any { it.text.isNotBlank() }) result += expandedRow
        }
        return result
    }

    private class ZfsoftPeriodTimeTable(
        private val ranges: List<Pair<IntRange, Pair<LocalTime, LocalTime>>>,
    ) {
        fun resolve(periods: IntRange): Pair<LocalTime, LocalTime>? {
            ranges.firstOrNull { it.first == periods }?.let { return it.second }
            val first = ranges.firstOrNull { periods.first in it.first }
            val last = ranges.firstOrNull { periods.last in it.first }
            if (first != null && last != null) {
                return first.second.first to last.second.second
            }
            return null
        }
    }

    private fun buildZfsoftPeriodTimeTable(
        matrix: List<List<ZfsoftExpandedCell>>,
        firstDayColumn: Int,
        firstBodyRow: Int,
    ): ZfsoftPeriodTimeTable {
        val ranges = matrix.drop(firstBodyRow)
            .mapNotNull { row ->
                val context = row.take(firstDayColumn)
                    .map { it.text }
                    .filter { it.isNotBlank() }
                    .joinToString("\n")
                val periods = context.parseZfsoftPeriodRange() ?: return@mapNotNull null
                val timeRange = context.parseZfsoftTimeRange() ?: return@mapNotNull null
                periods to timeRange
            }
            .distinctBy { "${it.first.first}-${it.first.last}" }
        return ZfsoftPeriodTimeTable(ranges)
    }

    /**
     * 节次转时间
     */
    private fun periodToTimeRange(startPeriod: Int, endPeriod: Int): Pair<LocalTime, LocalTime> {
        val periodGroups = listOf(
            1..2 to (LocalTime.of(8, 0) to LocalTime.of(9, 40)),
            3..4 to (LocalTime.of(10, 0) to LocalTime.of(11, 40)),
            5..6 to (LocalTime.of(14, 0) to LocalTime.of(15, 40)),
            7..8 to (LocalTime.of(16, 0) to LocalTime.of(17, 40)),
            9..10 to (LocalTime.of(19, 0) to LocalTime.of(20, 40)),
        )
        val start = periodGroups.firstOrNull { startPeriod in it.first }?.second?.first
            ?: LocalTime.of(8, 0)
        val end = periodGroups.lastOrNull { endPeriod in it.first }?.second?.second
            ?: start.plusMinutes(100)
        return start to end
    }

    /**
     * 提取学期名称
     */
    private fun extractSemesterName(text: String): String {
        val match = Regex("(20\\d{2})\\s*[-—]\\s*(20\\d{2})\\s*学年\\s*第?\\s*(\\d)\\s*学期").find(text)
        if (match != null) {
            val year1 = match.groupValues[1]
            val year2 = match.groupValues[2]
            val semester = match.groupValues[3]
            val season = if (semester == "1") "秋季" else "春季"
            return "$year1-$year2 ${season}学期"
        }
        return "导入课表"
    }

    private fun List<Course>.inferTotalWeeks(): Int {
        return maxOfOrNull { it.weeks.last }
            ?.coerceAtLeast(ScheduleDefaults.TOTAL_WEEKS)
            ?: ScheduleDefaults.TOTAL_WEEKS
    }

    /**
     * 字符串转星期几
     */
    private fun String.toDayOfWeek(): DayOfWeek? {
        return when {
            contains("星期一") || contains("周一") || equals("一") -> DayOfWeek.MONDAY
            contains("星期二") || contains("周二") || equals("二") -> DayOfWeek.TUESDAY
            contains("星期三") || contains("周三") || equals("三") -> DayOfWeek.WEDNESDAY
            contains("星期四") || contains("周四") || equals("四") -> DayOfWeek.THURSDAY
            contains("星期五") || contains("周五") || equals("五") -> DayOfWeek.FRIDAY
            contains("星期六") || contains("周六") || equals("六") -> DayOfWeek.SATURDAY
            contains("星期日") || contains("星期天") || contains("周日") || equals("日") -> DayOfWeek.SUNDAY
            else -> null
        }
    }

    /**
     * IntRange 列表合并为连续范围列表
     */
    private fun List<Int>.toRangeList(): List<IntRange> {
        if (isEmpty()) return emptyList()
        val sorted = this.sorted()
        val ranges = mutableListOf<IntRange>()
        var start = sorted.first()
        var end = sorted.first()

        for (i in 1 until sorted.size) {
            if (sorted[i] == end + 1) {
                end = sorted[i]
            } else {
                ranges.add(start..end)
                start = sorted[i]
                end = sorted[i]
            }
        }
        ranges.add(start..end)
        return ranges
    }

    private fun Int.absoluteValue(): Int {
        return if (this == Int.MIN_VALUE) 0 else kotlin.math.abs(this)
    }
}

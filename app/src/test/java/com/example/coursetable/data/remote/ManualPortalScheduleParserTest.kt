package com.example.coursetable.data.remote

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.json.JSONArray
import org.json.JSONObject

class ManualPortalScheduleParserTest {
    private val parser = ManualPortalScheduleParser()

    @Test
    fun parsesDatesNonEightStartAndRowspanWithoutColumnDrift() {
        val html = """
            <html>
              <body>
                <div>2025-2026 春季学期 第3周 2026/3/2 - 2026/3/8</div>
                <table>
                  <tr>
                    <th>时间</th>
                    <th>周一 2026/3/2</th>
                    <th>周二 2026/3/3</th>
                    <th>周三 2026/3/4</th>
                    <th>周四 2026/3/5</th>
                    <th>周五 2026/3/6</th>
                  </tr>
                  <tr>
                    <td rowspan="2">07:30-09:05</td>
                    <td>高等数学<br/>任课教师：林老师<br/>上课地点：A101<br/>1-16周</td>
                    <td></td>
                    <td></td>
                    <td></td>
                    <td></td>
                  </tr>
                  <tr>
                    <td></td>
                    <td>大学英语<br/>教师：王老师<br/>地点：B202<br/>3-18周</td>
                    <td></td>
                    <td></td>
                    <td></td>
                  </tr>
                </table>
              </body>
            </html>
        """.trimIndent()

        val schedule = parser.parse(
            url = "http://xjw1.ncst.edu.cn/",
            title = "课表",
            html = html,
        )

        assertNotNull(schedule)
        schedule!!
        assertEquals(3, schedule.currentWeek)
        assertEquals(LocalDate.of(2026, 2, 16), schedule.semesterStartDate)
        assertEquals(2, schedule.courses.size)

        val math = schedule.courses.first { it.name == "高等数学" }
        assertEquals(DayOfWeek.MONDAY, math.dayOfWeek)
        assertEquals(LocalTime.of(7, 30), math.startTime)
        assertEquals(LocalTime.of(9, 5), math.endTime)
        assertEquals("A101", math.location)
        assertEquals("林老师", math.teacher)

        val english = schedule.courses.first { it.name == "大学英语" }
        assertEquals(DayOfWeek.TUESDAY, english.dayOfWeek)
        assertEquals(LocalTime.of(7, 30), english.startTime)
        assertEquals(LocalTime.of(9, 5), english.endTime)
        assertEquals("B202", english.location)
        assertEquals("王老师", english.teacher)
    }

    @Test
    fun infersShortHeaderDatesAndSchoolPeriodTimes() {
        val html = """
            <html>
              <body>
                <div>华北理工大学 2025-2026 春季学期 2026/3/9-2026/3/15</div>
                <div>第3-4节 10:00-11:35</div>
                <table>
                  <tr>
                    <th>节次</th>
                    <th>周一 3/9</th>
                    <th>周二 3/10</th>
                    <th>周三 3/11</th>
                    <th>周四 3/12</th>
                    <th>周五 3/13</th>
                  </tr>
                  <tr>
                    <td>节次：3-4</td>
                    <td>线性代数  教师：赵老师  地点：东区B202  周次：1-16</td>
                    <td></td>
                    <td></td>
                    <td></td>
                    <td></td>
                  </tr>
                </table>
              </body>
            </html>
        """.trimIndent()

        val schedule = parser.parse(
            url = "http://xjw1.ncst.edu.cn/",
            title = "课表",
            html = html,
        )

        assertNotNull(schedule)
        schedule!!
        assertEquals(3, schedule.currentWeek)
        assertEquals(LocalDate.of(2026, 2, 23), schedule.semesterStartDate)
        assertEquals(1, schedule.courses.size)

        val algebra = schedule.courses.single()
        assertEquals("线性代数", algebra.name)
        assertEquals(DayOfWeek.MONDAY, algebra.dayOfWeek)
        assertEquals(LocalTime.of(10, 0), algebra.startTime)
        assertEquals(LocalTime.of(11, 35), algebra.endTime)
        assertEquals("东区B202", algebra.location)
        assertEquals("赵老师", algebra.teacher)
        assertEquals(1..16, algebra.weeks)
    }

    @Test
    fun parsesWebViewDomPayloadWithoutTeacherLocationBleed() {
        val table = JSONArray()
            .put(
                JSONArray()
                    .put(JSONObject().put("text", "时间").put("rowSpan", 1).put("colSpan", 1))
                    .put(JSONObject().put("text", "周一 2026/3/16").put("rowSpan", 1).put("colSpan", 1))
                    .put(JSONObject().put("text", "周二 2026/3/17").put("rowSpan", 1).put("colSpan", 1))
                    .put(JSONObject().put("text", "周三 2026/3/18").put("rowSpan", 1).put("colSpan", 1))
                    .put(JSONObject().put("text", "周四 2026/3/19").put("rowSpan", 1).put("colSpan", 1))
                    .put(JSONObject().put("text", "周五 2026/3/20").put("rowSpan", 1).put("colSpan", 1)),
            )
            .put(
                JSONArray()
                    .put(JSONObject().put("text", "13:20-14:55").put("rowSpan", 1).put("colSpan", 1))
                    .put(JSONObject().put("text", "").put("rowSpan", 1).put("colSpan", 1))
                    .put(JSONObject().put("text", "大学物理  教师：钱老师  地点：实验楼301  周次：4-12").put("rowSpan", 1).put("colSpan", 1))
                    .put(JSONObject().put("text", "").put("rowSpan", 1).put("colSpan", 1))
                    .put(JSONObject().put("text", "").put("rowSpan", 1).put("colSpan", 1))
                    .put(JSONObject().put("text", "").put("rowSpan", 1).put("colSpan", 1)),
            )
        val payload = JSONObject()
            .put("url", "http://xjw1.ncst.edu.cn/")
            .put("title", "课表")
            .put("text", "2025-2026 春季学期 第4周 2026/3/16-2026/3/22")
            .put("html", "<html></html>")
            .put("tables", JSONArray().put(table))
            .toString()

        val schedule = parser.parse(
            url = "http://xjw1.ncst.edu.cn/",
            title = "课表",
            html = payload,
        )

        assertNotNull(schedule)
        schedule!!
        assertEquals(4, schedule.currentWeek)
        assertEquals(LocalDate.of(2026, 2, 23), schedule.semesterStartDate)

        val physics = schedule.courses.single()
        assertEquals("大学物理", physics.name)
        assertEquals(DayOfWeek.TUESDAY, physics.dayOfWeek)
        assertEquals(LocalTime.of(13, 20), physics.startTime)
        assertEquals(LocalTime.of(14, 55), physics.endTime)
        assertEquals("实验楼301", physics.location)
        assertEquals("钱老师", physics.teacher)
    }

    @Test
    fun infersSemesterStartFromCurrentWeekAndPageDateRangeForGenericTables() {
        val html = """
            <html>
              <body>
                <div>2025-2026 春季学期 当前教学周：第4周 2026-03-16 至 2026-03-22</div>
                <table>
                  <tr>
                    <th>时间</th>
                    <th>星期一</th>
                    <th>星期二</th>
                    <th>星期三</th>
                    <th>星期四</th>
                    <th>星期五</th>
                  </tr>
                  <tr>
                    <td>08:00-09:40</td>
                    <td>工程制图<br/>教师：陈老师<br/>地点：A101<br/>1-16周</td>
                    <td></td>
                    <td></td>
                    <td></td>
                    <td></td>
                  </tr>
                </table>
              </body>
            </html>
        """.trimIndent()

        val schedule = parser.parse(
            url = "https://example.edu/kbcx",
            title = "课表",
            html = html,
        )

        assertNotNull(schedule)
        schedule!!
        assertEquals(4, schedule.currentWeek)
        assertEquals(LocalDate.of(2026, 2, 23), schedule.semesterStartDate)
    }

    @Test
    fun parsesNcstRenderedCourseCardsWithPeriodSpanTeacherAndCurrentWeek() {
        val html = """
            <html>
              <body>
                <span>2025-2026 春  第15周   星期日</span>
                <table id="courseTable">
                  <thead>
                    <tr>
                      <th colspan="2">节次/时间</th>
                      <th>星期一</th>
                      <th>星期二</th>
                      <th>星期三</th>
                      <th>星期四</th>
                      <th>星期五</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <th rowspan="4">上午</th>
                      <th>第1节(08:30-09:15)</th>
                      <td></td>
                      <td>
                        <div class="class_div" classnum="4">
                          <p>西游记_02</p>
                          <p>闫海波* </p>
                          <p>第14周</p>
                          <p>1-4节</p>
                          <p>曹妃甸校区HB座HB-302</p>
                        </div>
                      </td>
                      <td></td>
                      <td></td>
                      <td></td>
                    </tr>
                    <tr>
                      <th>第2节(09:20-10:05)</th>
                      <td></td><td></td><td></td><td></td><td></td>
                    </tr>
                    <tr>
                      <th>第3节(10:25-11:10)</th>
                      <td>
                        <div class="class_div" classnum="2">
                          <p>高等数学A-2_30</p>
                          <p>杨爱民* </p>
                          <p>1-8,10-13,16-18周</p>
                          <p>3-4节</p>
                          <p>曹妃甸校区HA座HA-205</p>
                        </div>
                      </td>
                      <td></td><td></td><td></td><td></td>
                    </tr>
                    <tr>
                      <th>第4节(11:15-12:00)</th>
                      <td></td><td></td><td></td><td></td><td></td>
                    </tr>
                  </tbody>
                </table>
              </body>
            </html>
        """.trimIndent()

        val schedule = parser.parse(
            url = "https://xjw1.ncst.edu.cn/student/courseSelect/thisSemesterCurriculum/index",
            title = "本学期课程表",
            html = html,
        )

        assertNotNull(schedule)
        schedule!!
        assertEquals(15, schedule.currentWeek)
        assertEquals(LocalDate.of(2026, 2, 23), schedule.semesterStartDate)

        val journey = schedule.courses.single { it.name == "西游记_02" }
        assertEquals(DayOfWeek.TUESDAY, journey.dayOfWeek)
        assertEquals(LocalTime.of(8, 30), journey.startTime)
        assertEquals(LocalTime.of(12, 0), journey.endTime)
        assertEquals(14..14, journey.weeks)
        assertEquals("闫海波", journey.teacher)
        assertEquals("曹妃甸校区HB座HB-302", journey.location)

        val mathRanges = schedule.courses
            .filter { it.name == "高等数学A-2_30" }
            .map { it.weeks }
        assertEquals(listOf(1..8, 10..13, 16..18), mathRanges)
        assertEquals(LocalTime.of(10, 25), schedule.courses.first { it.name == "高等数学A-2_30" }.startTime)
        assertEquals(LocalTime.of(12, 0), schedule.courses.first { it.name == "高等数学A-2_30" }.endTime)
    }

    @Test
    fun readsNewUrpTermMetadataFromPageAndScriptState() {
        val html = """
            <html>
              <head><title>华北理工大学 新URP系统1</title></head>
              <body>
                <div>华北理工大学 新URP系统1</div>
                <div>当前教学周：第3周</div>
                <script>
                  window.__INITIAL_STATE__ = {
                    "semesterName": "2025-2026学年第二学期",
                    "semesterStartDate": "2026-02-25",
                    "totalWeeks": 20
                  };
                </script>
                <table>
                  <tr>
                    <th>节次</th><th>星期一</th><th>星期二</th><th>星期三</th><th>星期四</th><th>星期五</th>
                  </tr>
                  <tr>
                    <td>第1-2节 08:30-10:05</td>
                    <td>大学物理<br/>教师：李老师<br/>地点：A101<br/>1-16周</td>
                    <td></td><td></td><td></td><td></td>
                  </tr>
                </table>
              </body>
            </html>
        """.trimIndent()

        val schedule = parser.parse(
            url = "https://urp.ncst.edu.cn/student/course-table",
            title = "新URP系统1",
            html = html,
        )

        assertNotNull(schedule)
        schedule!!
        assertEquals("2025-2026学年第二学期", schedule.displayName)
        assertEquals(3, schedule.currentWeek)
        assertEquals(20, schedule.totalWeeks)
        assertEquals(LocalDate.of(2026, 2, 25), schedule.semesterStartDate)
        assertEquals(1, schedule.courses.size)
    }

    @Test
    fun usesNcstCalendarWhenHistoricalScheduleHasNoStartDate() {
        val html = """
            <html><body>
              <select id="planCode"><option value="2025-2026-2-1">2025-2026学年春</option></select>
              <table>
                <tr><th>节次</th><th>星期一</th><th>星期二</th><th>星期三</th><th>星期四</th><th>星期五</th></tr>
                <tr><td>第1节 08:30-09:15</td><td>大学物理<br/>教师：李老师<br/>地点：A101<br/>1-16周</td><td></td><td></td><td></td><td></td></tr>
              </table>
            </body></html>
        """.trimIndent()

        val schedule = parser.parse(
            url = "https://xjw1.ncst.edu.cn/student/courseSelect/calendarSemesterCurriculum/index",
            title = "历年学期课表",
            html = html,
        )

        assertNotNull(schedule)
        schedule!!
        assertEquals(LocalDate.of(2026, 3, 2), schedule.semesterStartDate)
        assertEquals(1, schedule.currentWeek)
    }

    @Test
    fun usesFall2026NcstCalendarStartDate() {
        val html = """
            <html><body>
              <select id="planCode"><option value="2026-2027-1-1">2026-2027学年秋</option></select>
              <table>
                <tr><th>节次</th><th>星期一</th><th>星期二</th><th>星期三</th><th>星期四</th><th>星期五</th></tr>
                <tr><td>第1节 08:30-09:15</td><td>高等数学<br/>教师：李老师<br/>地点：A101<br/>1-16周</td><td></td><td></td><td></td><td></td></tr>
              </table>
            </body></html>
        """.trimIndent()

        val schedule = parser.parse(
            url = "https://xjw1.ncst.edu.cn/student/courseSelect/calendarSemesterCurriculum/index",
            title = "历年学期课表",
            html = html,
        )

        assertNotNull(schedule)
        assertEquals(LocalDate.of(2026, 8, 31), schedule!!.semesterStartDate)
        assertEquals(1, schedule.currentWeek)
    }

    @Test
    fun readsSelectedHistoricalSemesterInsteadOfFirstOption() {
        val html = """
            <html><body>
              <div>已选字段[main][planCode]: 2025-2026-1-1 2025-2026学年秋</div>
              <select id="planCode">
                <option value="2025-2026-2-1">2025-2026学年春</option>
                <option value="2025-2026-1-1">2025-2026学年秋</option>
              </select>
              <table>
                <tr><th>节次</th><th>星期一</th><th>星期二</th><th>星期三</th><th>星期四</th><th>星期五</th></tr>
                <tr><td>第1节 08:30-09:15</td><td>大学物理<br/>教师：李老师<br/>地点：A101<br/>1-16周</td><td></td><td></td><td></td><td></td></tr>
              </table>
            </body></html>
        """.trimIndent()

        val schedule = parser.parse(
            url = "https://xjw1.ncst.edu.cn/student/courseSelect/calendarSemesterCurriculum/index",
            title = "历年学期课表",
            html = html,
        )

        assertNotNull(schedule)
        assertEquals("2025-2026学年第一学期", schedule!!.displayName)
        assertEquals(1, schedule.currentWeek)
        assertEquals(LocalDate.of(2025, 9, 1), schedule.semesterStartDate)
    }

    @Test
    fun parsesXzcitZfsoftGridWithRowspanRealTimesAndDetails() {
        val table = JSONArray()
            .put(
                JSONArray()
                    .put(cell("个人课表查询", colSpan = 9)),
            )
            .put(
                JSONArray()
                    .put(cell("节次"))
                    .put(cell("时间"))
                    .put(cell("星期一"))
                    .put(cell("星期二"))
                    .put(cell("星期三"))
                    .put(cell("星期四"))
                    .put(cell("星期五"))
                    .put(cell("星期六"))
                    .put(cell("星期日")),
            )
            .put(
                JSONArray()
                    .put(cell("1"))
                    .put(cell("08:15-09:00"))
                    .put(
                        cell(
                            """
                            高分子材料★
                            (1-2节)2-5周,7-9周,11-19周
                            校区：九里校区
                            上课地点：B01-505
                            教师：赵璐
                            教学班组成：材料241
                            """.trimIndent(),
                            rowSpan = 2,
                        ),
                    )
                    .put(cell(""))
                    .put(cell(""))
                    .put(cell(""))
                    .put(cell(""))
                    .put(cell(""))
                    .put(cell("")),
            )
            .put(
                JSONArray()
                    .put(cell("2"))
                    .put(cell("09:05-09:50"))
                    .put(cell(""))
                    .put(cell(""))
                    .put(cell(""))
                    .put(cell(""))
                    .put(cell(""))
                    .put(cell("")),
            )
            .put(
                JSONArray()
                    .put(cell("3"))
                    .put(cell("10:10-10:55"))
                    .put(cell(""))
                    .put(
                        cell(
                            """
                            形势与政策★
                            (3-4节)4-6周(双)
                            校区：九里校区
                            上课地点：B01-506
                            教师：王强
                            """.trimIndent(),
                            rowSpan = 2,
                        ),
                    )
                    .put(cell(""))
                    .put(cell(""))
                    .put(cell(""))
                    .put(cell(""))
                    .put(cell("")),
            )
            .put(
                JSONArray()
                    .put(cell("4"))
                    .put(cell("11:00-11:45"))
                    .put(cell(""))
                    .put(cell(""))
                    .put(cell(""))
                    .put(cell(""))
                    .put(cell(""))
                    .put(cell("")),
            )
        val payload = JSONObject()
            .put("url", "https://jwpt.xzcit.cn/jwglxt/kbcx/xskbcx_cxXsgrkb.html")
            .put("title", "徐州工业职业技术学院 个人课表查询")
            .put("text", "徐州工业职业技术学院 zfsoft 2025-2026学年第2学期 当前教学周：第4周 2026-03-16 至 2026-03-22")
            .put("html", "<html>zftal-ui kbgrid_table_0</html>")
            .put("tables", JSONArray().put(table))
            .toString()

        val schedule = parser.parse(
            url = "https://jwpt.xzcit.cn/",
            title = "个人课表查询",
            html = payload,
        )

        assertNotNull(schedule)
        schedule!!
        assertEquals(4, schedule.currentWeek)
        assertEquals(LocalDate.of(2026, 2, 23), schedule.semesterStartDate)

        val materialCourses = schedule.courses.filter { it.name == "高分子材料" }
        assertEquals(listOf(2..5, 7..9, 11..19), materialCourses.map { it.weeks })
        val material = materialCourses.first()
        assertEquals(DayOfWeek.MONDAY, material.dayOfWeek)
        assertEquals(LocalTime.of(8, 15), material.startTime)
        assertEquals(LocalTime.of(9, 50), material.endTime)
        assertEquals("九里校区 B01-505", material.location)
        assertEquals("赵璐", material.teacher)

        val policyCourses = schedule.courses.filter { it.name == "形势与政策" }
        assertEquals(listOf(4..4, 6..6), policyCourses.map { it.weeks })
        val policy = policyCourses.first()
        assertEquals(DayOfWeek.TUESDAY, policy.dayOfWeek)
        assertEquals(LocalTime.of(10, 10), policy.startTime)
        assertEquals(LocalTime.of(11, 45), policy.endTime)
        assertEquals("九里校区 B01-506", policy.location)
        assertEquals("王强", policy.teacher)
    }

    @Test
    fun parsesXzcitRawKbGridHtmlIntoRealWeeksAndDetails() {
        val html = """
            <html>
              <head><title>个人课表查询</title></head>
              <body>
                <table id="kbgrid_table_0">
                  <tr><td colspan="9"><h6>2025-2026学年第2学期</h6>朱轩的课表</td></tr>
                  <tr>
                    <td>时间段</td><td>节次</td>
                    <td>星期一</td><td>星期二</td><td>星期三</td><td>星期四</td><td>星期五</td><td>星期六</td><td>星期日</td>
                  </tr>
                  <tr>
                    <td rowspan="4">上午</td>
                    <td>1</td>
                    <td rowspan="2" id="1-1" class="td_wrap">
                      <div class="timetable_con text-left">
                        <u class="title"><font color="blue">【调】大学英语2★</font></u>
                        <p><span title="节/周"></span><font color="blue"> (1-2节)2-5周,7-9周,11-19周</font></p>
                        <p><span title="上课地点"></span><font color="blue"> 九里校区  B01-505</font></p>
                        <p><span title="教师 "></span><font color="blue"> 赵璐</font></p>
                      </div>
                    </td>
                    <td rowspan="2" id="2-1" class="td_wrap">
                      <div class="timetable_con text-left">
                        <u class="title"><font color="blue">【调】电工电子技术★</font></u>
                        <p><span title="节/周"></span><font color="blue"> (1-2节)4-6周(双),7-8周</font></p>
                        <p><span title="上课地点"></span><font color="blue"> 九里校区  B17-311</font></p>
                        <p><span title="教师 "></span><font color="blue"> 毛少文</font></p>
                      </div>
                      <div class="timetable_con text-left">
                        <span class="title"><font color="blue">机械制图★</font></span>
                        <p><span title="节/周"></span><font color="blue"> (1-2节)11-19周</font></p>
                        <p><span title="上课地点"></span><font color="blue"> 九里校区  B17-204</font></p>
                        <p><span title="教师 "></span><font color="blue"> 秋艳</font></p>
                      </div>
                    </td>
                    <td rowspan="4" id="3-1" class="td_wrap">
                      <div class="timetable_con text-left">
                        <span class="title"><font color="blue">机电控制基础技能训练★</font></span>
                        <p><span title="节/周"></span><font color="blue"> (1-4节)2-7周</font></p>
                        <p><span title="上课地点"></span><font color="blue"> 九里校区  B16-403</font></p>
                        <p><span title="教师 "></span><font color="blue"> 樊丽丽</font></p>
                      </div>
                    </td>
                    <td></td><td></td><td></td><td></td><td></td>
                  </tr>
                </table>
              </body>
            </html>
        """.trimIndent()

        val schedule = parser.parse(
            url = "https://jwpt.xzcit.cn/jwglxt/kbcx/xskbcx_cxXsgrkb.html",
            title = "徐州工业职业技术学院 个人课表查询",
            html = html,
        )

        assertNotNull(schedule)
        schedule!!
        val englishCourses = schedule.courses.filter { it.name == "大学英语2" }
        assertEquals(listOf(2..5, 7..9, 11..19), englishCourses.map { it.weeks })
        assertEquals(DayOfWeek.MONDAY, englishCourses.first().dayOfWeek)
        assertEquals(LocalTime.of(8, 0), englishCourses.first().startTime)
        assertEquals(LocalTime.of(9, 40), englishCourses.first().endTime)
        assertEquals("九里校区 B01-505", englishCourses.first().location)
        assertEquals("赵璐", englishCourses.first().teacher)

        val electricCourses = schedule.courses.filter { it.name == "电工电子技术" }
        assertEquals(listOf(4..4, 6..6, 7..8), electricCourses.map { it.weeks })
        assertEquals(DayOfWeek.TUESDAY, electricCourses.first().dayOfWeek)
        assertEquals("九里校区 B17-311", electricCourses.first().location)
        assertEquals("毛少文", electricCourses.first().teacher)

        val training = schedule.courses.single { it.name == "机电控制基础技能训练" }
        assertEquals(2..7, training.weeks)
        assertEquals(DayOfWeek.WEDNESDAY, training.dayOfWeek)
        assertEquals(LocalTime.of(8, 0), training.startTime)
        assertEquals(LocalTime.of(11, 40), training.endTime)
        assertEquals("九里校区 B16-403", training.location)
        assertEquals("樊丽丽", training.teacher)
    }

    @Test
    fun parsesXzcitGroupedListHtmlAsSeparateCourseBlocks() {
        val html = """
            <html>
              <body>
                <div>徐州工业职业技术学院 zfsoft 2025-2026学年第2学期 当前教学周：第4周 2026-03-16 至 2026-03-22</div>
                <table id="kblist_table">
                  <tbody id="xq_2">
                    <tr><td id="xq_rowspan_2" rowspan="3"><span class="week">星期二</span></td></tr>
                    <tr>
                      <td id="jc_2-1-2" rowspan="2"><span class="festival">1-2</span></td>
                      <td>
                        <div class="timetable_con text-left">
                          <u class="title showJxbtkjl"><font color="blue">【调】电工电子技术★</font></u>
                          <p><font color="blue"><span class="glyphicon glyphicon-calendar"></span> 周数：4-6周(双),7-8周</font><font color="blue"><span class="glyphicon glyphicon-tower"></span> 校区:九里校区<span class="glyphicon glyphicon-map-marker"></span> 上课地点：B17-311 </font><font color="blue"><span class="glyphicon glyphicon-user"></span> 教师 ：毛少文</font><font color="blue"><span class="glyphicon glyphicon-home"></span> 教学班：电工电子技术-0001</font></p>
                        </div>
                      </td>
                    </tr>
                    <tr>
                      <td>
                        <div class="timetable_con text-left">
                          <span class="title"><font color="blue">劳动教育实践课★</font></span>
                          <p><font color="blue"><span class="glyphicon glyphicon-calendar"></span> 周数：9周</font><font color="blue"><span class="glyphicon glyphicon-tower"></span> 校区:九里校区<span class="glyphicon glyphicon-map-marker"></span> 上课地点：未排地点 </font><font color="blue"><span class="glyphicon glyphicon-user"></span> 教师 ：何凯文</font><font color="blue"><span class="glyphicon glyphicon-home"></span> 教学班：劳动教育实践课-0045</font></p>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </body>
            </html>
        """.trimIndent()

        val schedule = parser.parse(
            url = "https://jwpt.xzcit.cn/jwglxt/kbcx/xskbcx_cxXsgrkb.html",
            title = "徐州工业职业技术学院 个人课表查询",
            html = html,
        )

        assertNotNull(schedule)
        schedule!!
        assertEquals(4, schedule.currentWeek)

        val electricCourses = schedule.courses.filter { it.name == "电工电子技术" }
        assertEquals(listOf(4..4, 6..6, 7..8), electricCourses.map { it.weeks })
        assertEquals(DayOfWeek.TUESDAY, electricCourses.first().dayOfWeek)
        assertEquals(LocalTime.of(8, 0), electricCourses.first().startTime)
        assertEquals(LocalTime.of(9, 40), electricCourses.first().endTime)
        assertEquals("九里校区 B17-311", electricCourses.first().location)
        assertEquals("毛少文", electricCourses.first().teacher)

        val labor = schedule.courses.single { it.name == "劳动教育实践课" }
        assertEquals(9..9, labor.weeks)
        assertEquals(LocalTime.of(8, 0), labor.startTime)
        assertEquals(LocalTime.of(9, 40), labor.endTime)
        assertEquals("九里校区 未排地点", labor.location)
        assertEquals("何凯文", labor.teacher)
    }

    private fun cell(
        text: String,
        rowSpan: Int = 1,
        colSpan: Int = 1,
    ): JSONObject {
        return JSONObject()
            .put("text", text)
            .put("rowSpan", rowSpan)
            .put("colSpan", colSpan)
    }
}

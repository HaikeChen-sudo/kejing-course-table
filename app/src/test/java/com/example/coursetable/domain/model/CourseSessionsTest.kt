package com.example.coursetable.domain.model

import com.example.coursetable.data.remote.withKnownNcstTermStartDate
import com.example.coursetable.data.local.LocalScheduleFileStore
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class CourseSessionsTest {
    private val course = Course("math", "高等数学", DayOfWeek.MONDAY, LocalTime.of(8, 30), LocalTime.of(10, 5), "A101", "老师", 1..16, CourseColorSeed.Mint)
    private val semester = SemesterSchedule("fall", "2026-2027 秋季学期", "manual_ncst-portal-1", 1, 21, LocalDate.of(2026, 8, 31), listOf(course))

    @Test fun migrationIsIdempotentAndKeepsEverySession() {
        val split = semester.withIndividualSessions()
        assertEquals(16, split.courses.size)
        assertEquals(16, split.courses.map { it.id }.distinct().size)
        assertEquals(split, split.withIndividualSessions())
        assertEquals(listOf("math::week:4"), split.coursesOn(LocalDate.of(2026, 9, 21)).map { it.id })
    }

    @Test fun movingOneSessionKeepsOtherDatesAndContent() {
        val split = semester.withIndividualSessions()
        val original = split.courses[3]
        val moved = split.moveSession(original.id, LocalDate.of(2026, 9, 23), original.startTime, original.endTime, original.location)
        assertTrue(moved.coursesOn(LocalDate.of(2026, 9, 21)).isEmpty())
        val target = moved.coursesOn(LocalDate.of(2026, 9, 23)).single()
        assertEquals(original.copy(scheduledDate = LocalDate.of(2026, 9, 23), dayOfWeek = DayOfWeek.WEDNESDAY), target)
        assertEquals(split.courses.filter { it.id != original.id }, moved.courses.filter { it.id != original.id })
        assertEquals(16, moved.courses.size)
    }

    @Test fun changingCalendarShiftsRegularSessionsButPreservesMovedDate() {
        val split = semester.withIndividualSessions()
        val moved = split.moveSession(split.courses[3].id, LocalDate.of(2026, 9, 23), course.startTime, course.endTime, course.location)
        val changed = moved.copy(semesterStartDate = LocalDate.of(2026, 9, 7), customStartDate = true).withKnownNcstTermStartDate()
        assertEquals(LocalDate.of(2026, 9, 7), changed.semesterStartDate)
        assertEquals(LocalDate.of(2026, 9, 7), changed.dateOf(changed.courses.first()))
        assertEquals(LocalDate.of(2026, 9, 23), changed.dateOf(changed.courses[3]))
        assertEquals(0, changed.weekAt(LocalDate.of(2026, 9, 6)))
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsDatesOutsideSemester() {
        semester.withIndividualSessions().moveSession("math::week:1", LocalDate.of(2027, 9, 1), course.startTime, course.endTime, course.location)
    }

    @Test fun midweekStartKeepsWeekdaysAndHidesEarlierClasses() {
        val start = LocalDate.of(2026, 9, 2)
        val firstWeek = semester.copy(semesterStartDate = start, customStartDate = true,
            courses = DayOfWeek.entries.map { course.copy(id = it.name, dayOfWeek = it, weeks = 1..1) })
        assertEquals(start, firstWeek.withKnownNcstTermStartDate().semesterStartDate)
        assertEquals(LocalDate.of(2026, 8, 31), firstWeek.firstMonday())
        assertTrue(firstWeek.coursesOn(start.minusDays(1)).isEmpty())
        assertEquals(DayOfWeek.WEDNESDAY, firstWeek.coursesOn(start).single().dayOfWeek)
        assertEquals(0, firstWeek.weekAt(start.minusDays(1)))
        assertEquals(1, firstWeek.weekAt(start))
        assertEquals(2, firstWeek.weekAt(LocalDate.of(2026, 9, 7)))
        val restored = LocalScheduleFileStore.decodeSchedule(LocalScheduleFileStore.encodeSchedule(firstWeek))
        assertEquals(firstWeek, restored.withKnownNcstTermStartDate())
    }

    @Test fun savedDateAndMovedSessionSurviveReload() {
        val split = semester.withIndividualSessions().copy(customStartDate = true)
        val moved = split.moveSession(split.courses[0].id, LocalDate.of(2026, 9, 2), course.startTime, course.endTime, "B202")
        val restored = LocalScheduleFileStore.decodeSchedule(LocalScheduleFileStore.encodeSchedule(moved))
        assertEquals(moved, restored)
        assertEquals(moved, restored.withKnownNcstTermStartDate().withIndividualSessions())
    }

    @Test fun oldJsonCanStillBeReadWithoutNewFields() {
        val json = org.json.JSONObject(LocalScheduleFileStore.encodeSchedule(semester))
        json.remove("customStartDate")
        val oldCourse = json.getJSONArray("courses").getJSONObject(0)
        oldCourse.remove("scheduledDate")
        assertEquals(semester, LocalScheduleFileStore.decodeSchedule(json.toString()))
    }
}

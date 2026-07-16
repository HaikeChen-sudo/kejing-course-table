package com.example.coursetable.data.repository

import com.example.coursetable.data.local.LocalScheduleFileStore
import com.example.coursetable.domain.model.Course
import com.example.coursetable.domain.model.CourseColorSeed
import com.example.coursetable.domain.model.ImportMode
import com.example.coursetable.domain.model.ScheduleDefaults
import com.example.coursetable.domain.model.SemesterSchedule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FileBackedScheduleRepository(
    private val fileStore: LocalScheduleFileStore,
) : ScheduleRepository {
    private val initialSchedules = fileStore.loadSchedules().ifEmpty {
        listOf(seedSemester()).also { schedules ->
            schedules.forEach(fileStore::saveSchedule)
        }
    }
    private val _schedules = MutableStateFlow(initialSchedules)
    private val _selectedSemesterId = MutableStateFlow(
        fileStore.loadSelectedSemesterId()
            ?.takeIf { selectedId -> initialSchedules.any { it.id == selectedId } }
            ?: initialSchedules.firstOrNull()?.id,
    )

    override val schedules: StateFlow<List<SemesterSchedule>> = _schedules.asStateFlow()
    override val selectedSemesterId: StateFlow<String?> = _selectedSemesterId.asStateFlow()

    override fun selectSemester(id: String) {
        if (_schedules.value.any { it.id == id }) {
            _selectedSemesterId.value = id
            fileStore.saveSelectedSemesterId(id)
        }
    }

    override suspend fun importSchedule(schedule: SemesterSchedule, mode: ImportMode) {
        val currentId = _selectedSemesterId.value
        val timestamp = System.currentTimeMillis()
        var importedSchedule: SemesterSchedule? = null
        val normalizedSchedule = schedule.normalizedForImport()
        val nextSchedules = when (mode) {
            ImportMode.OverwriteCurrent -> {
                _schedules.value.map {
                    if (it.id == currentId) {
                        normalizedSchedule
                            .copy(id = it.id, folderName = it.folderName)
                            .also { importedSchedule = it }
                    } else {
                        it
                    }
                }
            }

            ImportMode.CreateNewFolder -> {
                val uniqueSchedule = normalizedSchedule.copy(
                    id = "${normalizedSchedule.id}-$timestamp",
                    folderName = "${normalizedSchedule.folderName}_$timestamp",
                )
                importedSchedule = uniqueSchedule
                _schedules.value + uniqueSchedule
            }
        }
        importedSchedule?.let { fileStore.semesterFolder(it.folderName).mkdirs() }
        importedSchedule?.let(fileStore::saveSchedule)
        _schedules.value = nextSchedules
        _selectedSemesterId.value = when (mode) {
            ImportMode.OverwriteCurrent -> currentId ?: nextSchedules.firstOrNull()?.id
            ImportMode.CreateNewFolder -> nextSchedules.lastOrNull()?.id
        }
        fileStore.saveSelectedSemesterId(_selectedSemesterId.value)
    }

    override suspend fun addCourseToSelected(course: Course): Boolean {
        val selectedId = _selectedSemesterId.value ?: return false
        var updatedSchedule: SemesterSchedule? = null
        val nextSchedules = _schedules.value.map { schedule ->
            if (schedule.id == selectedId) {
                schedule.copy(
                    totalWeeks = schedule.totalWeeks
                        .coerceAtLeast(ScheduleDefaults.TOTAL_WEEKS)
                        .coerceAtLeast(course.weeks.last),
                    courses = (schedule.courses + course).sortedWith(
                        compareBy<Course> { it.dayOfWeek.value }
                            .thenBy { it.startTime }
                            .thenBy { it.name },
                    ),
                ).also { updatedSchedule = it }
            } else {
                schedule
            }
        }
        val target = updatedSchedule ?: return false
        fileStore.saveSchedule(target)
        _schedules.value = nextSchedules
        return true
    }

    override suspend fun deleteCourseFromSelected(courseId: String): Boolean {
        val selectedId = _selectedSemesterId.value ?: return false
        var updatedSchedule: SemesterSchedule? = null
        var hasCourse = false
        val nextSchedules = _schedules.value.map { schedule ->
            if (schedule.id == selectedId) {
                hasCourse = schedule.courses.any { it.id == courseId }
                if (hasCourse) {
                    schedule.copy(
                        courses = schedule.courses.filterNot { it.id == courseId },
                    ).also { updatedSchedule = it }
                } else {
                    schedule
                }
            } else {
                schedule
            }
        }
        val target = updatedSchedule ?: return false
        fileStore.saveSchedule(target)
        _schedules.value = nextSchedules
        return true
    }

    override suspend fun deleteSchedule(id: String): Boolean {
        val schedule = _schedules.value.firstOrNull { it.id == id } ?: return false
        if (_schedules.value.size <= 1) return false

        val nextSchedules = _schedules.value.filterNot { it.id == id }
        val deleted = fileStore.deleteScheduleFolder(schedule.folderName)
        _schedules.value = nextSchedules

        if (_selectedSemesterId.value == id) {
            _selectedSemesterId.value = nextSchedules.firstOrNull()?.id
            fileStore.saveSelectedSemesterId(_selectedSemesterId.value)
        }
        return deleted
    }

    private fun seedSemester(): SemesterSchedule {
        return SemesterSchedule(
            id = "2025-2026-fall",
            displayName = "2025-2026 秋季学期",
            folderName = "2025-2026_fall",
            currentWeek = 8,
            totalWeeks = ScheduleDefaults.TOTAL_WEEKS,
            semesterStartDate = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .minusWeeks(7),
            courses = listOf(
                Course(
                    id = "math",
                    name = "高等数学",
                    dayOfWeek = DayOfWeek.MONDAY,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(9, 40),
                    location = "教学楼 B201",
                    teacher = "林老师",
                    weeks = 1..16,
                    colorSeed = CourseColorSeed.Coral,
                ),
                Course(
                    id = "english",
                    name = "大学英语",
                    dayOfWeek = DayOfWeek.MONDAY,
                    startTime = LocalTime.of(10, 10),
                    endTime = LocalTime.of(11, 45),
                    location = "明德楼 104",
                    teacher = "王老师",
                    weeks = 1..18,
                    colorSeed = CourseColorSeed.Mint,
                ),
                Course(
                    id = "network",
                    name = "计算机网络",
                    dayOfWeek = DayOfWeek.TUESDAY,
                    startTime = LocalTime.of(13, 30),
                    endTime = LocalTime.of(15, 10),
                    location = "实验中心 305",
                    teacher = "许老师",
                    weeks = 3..18,
                    colorSeed = CourseColorSeed.Cyan,
                ),
                Course(
                    id = "design",
                    name = "交互设计",
                    dayOfWeek = DayOfWeek.THURSDAY,
                    startTime = LocalTime.of(15, 30),
                    endTime = LocalTime.of(17, 5),
                    location = "设计楼 2F",
                    teacher = "赵老师",
                    weeks = 1..12,
                    colorSeed = CourseColorSeed.Violet,
                ),
                Course(
                    id = "sports",
                    name = "体育",
                    dayOfWeek = DayOfWeek.FRIDAY,
                    startTime = LocalTime.of(9, 0),
                    endTime = LocalTime.of(10, 30),
                    location = "体育馆",
                    teacher = "刘老师",
                    weeks = 1..18,
                    colorSeed = CourseColorSeed.Lime,
                ),
            ),
        )
    }
}

private fun SemesterSchedule.normalizedForImport(): SemesterSchedule {
    val normalizedTotalWeeks = totalWeeks
        .coerceAtLeast(ScheduleDefaults.TOTAL_WEEKS)
        .coerceAtLeast(courses.maxOfOrNull { it.weeks.last } ?: 1)
    return copy(
        currentWeek = currentWeek.coerceIn(1, normalizedTotalWeeks),
        totalWeeks = normalizedTotalWeeks,
        courses = courses.sortedWith(
            compareBy<Course> { it.dayOfWeek.value }
                .thenBy { it.startTime }
                .thenBy { it.name },
        ),
    )
}

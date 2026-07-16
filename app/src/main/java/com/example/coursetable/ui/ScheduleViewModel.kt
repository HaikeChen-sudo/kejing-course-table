package com.example.coursetable.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.coursetable.data.local.ManualPortalPageStore
import com.example.coursetable.data.remote.AcademicPortalClient
import com.example.coursetable.data.remote.ManualPortalScheduleParser
import com.example.coursetable.data.repository.ScheduleRepository
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.json.JSONTokener

data class ScheduleUiState(
    val semesters: List<SemesterSchedule> = emptyList(),
    val selectedSemester: SemesterSchedule? = null,
    val selectedDay: DayOfWeek = LocalDate.now().dayOfWeek,
    val selectedWeek: Int = 1,
    val weekStartDate: LocalDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
    val todayCourses: List<Course> = emptyList(),
    val weekCourses: Map<DayOfWeek, List<Course>> = emptyMap(),
    val isImportDialogOpen: Boolean = false,
    val isSyncing: Boolean = false,
    val message: String? = null,
)

class ScheduleViewModel(
    private val repository: ScheduleRepository,
    private val portalClient: AcademicPortalClient,
    private val manualPortalPageStore: ManualPortalPageStore,
    private val manualPortalScheduleParser: ManualPortalScheduleParser,
) : ViewModel() {
    private val selectedDay = MutableStateFlow(LocalDate.now().dayOfWeek)
    private val selectedWeekOverride = MutableStateFlow<Int?>(null)
    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.schedules,
                repository.selectedSemesterId,
                selectedDay,
                selectedWeekOverride,
            ) { semesters, selectedSemesterId, selectedDay, selectedWeekOverride ->
                val selected = semesters.firstOrNull { it.id == selectedSemesterId }
                    ?: semesters.firstOrNull()
                val totalWeeks = selected?.totalWeeks?.coerceAtLeast(1) ?: 1
                val actualCurrentWeek = selected?.actualWeekForToday()
                val currentWeek = actualCurrentWeek
                    ?.coerceIn(1, totalWeeks)
                    ?: selected?.currentWeek?.coerceIn(1, totalWeeks)
                    ?: 1
                val selectedWeek = selectedWeekOverride
                    ?: currentWeek
                val weekStartDate = selected?.semesterStartDate
                    ?.plusWeeks((selectedWeek - 1).toLong())
                    ?: LocalDate.now()
                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        .plusWeeks((selectedWeek - currentWeek).toLong())
                ScheduleUiState(
                    semesters = semesters,
                    selectedSemester = selected,
                    selectedDay = selectedDay,
                    selectedWeek = selectedWeek,
                    weekStartDate = weekStartDate,
                    todayCourses = selected
                        ?.courses
                        ?.filter {
                            it.dayOfWeek == LocalDate.now().dayOfWeek &&
                                actualCurrentWeek != null &&
                                actualCurrentWeek in 1..totalWeeks &&
                                actualCurrentWeek in it.weeks
                        }
                        ?.sortedBy { it.startTime }
                        .orEmpty(),
                    weekCourses = DayOfWeek.entries.associateWith { day ->
                        selected
                            ?.courses
                            ?.filter { it.dayOfWeek == day && selectedWeek in it.weeks }
                            ?.sortedBy { it.startTime }
                            .orEmpty()
                    },
                    isImportDialogOpen = _uiState.value.isImportDialogOpen,
                    isSyncing = _uiState.value.isSyncing,
                    message = _uiState.value.message,
                )
            }.collect { _uiState.value = it }
        }
    }

    fun selectSemester(id: String) {
        repository.selectSemester(id)
        selectedWeekOverride.value = null
    }

    fun selectDay(day: DayOfWeek) {
        selectedDay.value = day
    }

    fun previousWeek() {
        val state = _uiState.value
        selectedWeekOverride.value = (state.selectedWeek - 1).coerceAtLeast(1)
    }

    fun nextWeek() {
        val state = _uiState.value
        val totalWeeks = state.selectedSemester?.totalWeeks ?: ScheduleDefaults.TOTAL_WEEKS
        selectedWeekOverride.value = (state.selectedWeek + 1).coerceAtMost(totalWeeks)
    }

    fun resetToCurrentWeek() {
        selectedWeekOverride.value = null
    }

    fun addManualCourse(
        name: String,
        dayOfWeek: DayOfWeek,
        startTime: LocalTime,
        endTime: LocalTime,
        location: String,
        teacher: String,
        weeks: IntRange,
    ) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) {
                _uiState.value = _uiState.value.copy(message = "课程名称不能为空")
                return@launch
            }
            if (!endTime.isAfter(startTime)) {
                _uiState.value = _uiState.value.copy(message = "结束时间需要晚于开始时间")
                return@launch
            }
            val course = Course(
                id = "manual-${System.currentTimeMillis()}-${trimmedName.hashCode()}",
                name = trimmedName,
                dayOfWeek = dayOfWeek,
                startTime = startTime,
                endTime = endTime,
                location = location.trim().ifBlank { "未填写地点" },
                teacher = teacher.trim().ifBlank { "未填写教师" },
                weeks = weeks,
                colorSeed = CourseColorSeed.entries[trimmedName.hashCode().absoluteValue() % CourseColorSeed.entries.size],
            )
            val saved = repository.addCourseToSelected(course)
            _uiState.value = _uiState.value.copy(
                message = if (saved) {
                    "已添加课程：${course.name}"
                } else {
                    "添加失败：没有可写入的当前课表"
                },
            )
        }
    }

    fun openImportDialog() {
        _uiState.value = _uiState.value.copy(isImportDialogOpen = true)
    }

    fun dismissImportDialog() {
        _uiState.value = _uiState.value.copy(isImportDialogOpen = false)
    }

    fun syncFromPortal(mode: ImportMode) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncing = true,
                isImportDialogOpen = false,
                message = null,
            )
            runCatching {
                val remoteSchedule = portalClient.fetchCurrentSemester()
                repository.importSchedule(remoteSchedule, mode)
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    message = "已从教务系统同步课表",
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    message = "同步失败：${it.message ?: "请稍后重试"}",
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun deleteCourse(courseId: String) {
        viewModelScope.launch {
            val targetName = _uiState.value.selectedSemester
                ?.courses
                ?.firstOrNull { it.id == courseId }
                ?.name
            val deleted = repository.deleteCourseFromSelected(courseId)
            _uiState.value = _uiState.value.copy(
                message = if (deleted) {
                    "已删除课程${targetName?.let { "：$it" }.orEmpty()}"
                } else {
                    "删除失败：没有找到课程"
                },
            )
        }
    }

    fun captureManualPortalPage(url: String, title: String, html: String) {
        viewModelScope.launch {
            val normalizedUrl = url.ifBlank { "当前页面" }
            val decodedPayload = decodeJavascriptString(html)
            android.util.Log.d("ScheduleParser", "captureManualPortalPage: url=$normalizedUrl, title=$title")
            android.util.Log.d("ScheduleParser", "decodedPayload length: ${decodedPayload.length}")
            android.util.Log.d("ScheduleParser", "decodedPayload starts with: ${decodedPayload.take(100)}")

            val snapshotHtml = extractHtmlFromPayload(decodedPayload)
            android.util.Log.d("ScheduleParser", "snapshotHtml length: ${snapshotHtml.length}")

            val savedFile = manualPortalPageStore.saveLatest(
                url = normalizedUrl,
                title = title,
                html = snapshotHtml,
            )
            val (parsedSchedule, diagnostics) = manualPortalScheduleParser.parseWithDiagnostics(
                url = normalizedUrl,
                title = title,
                html = decodedPayload,
            )

            if (parsedSchedule != null) {
                android.util.Log.d("ScheduleParser", "Success: ${parsedSchedule.courses.size} courses")
                repository.importSchedule(parsedSchedule, ImportMode.CreateNewFolder)
                _uiState.value = _uiState.value.copy(
                    message = "已导入 ${parsedSchedule.courses.size} 门课程：${parsedSchedule.displayName}",
                )
                return@launch
            }

            android.util.Log.d("ScheduleParser", "Failed to parse schedule: $diagnostics")
            val hint = when {
                normalizedUrl.contains("/login", ignoreCase = true) -> "当前仍在登录页，请登录后进入课表页"
                decodedPayload.contains("课表") || decodedPayload.contains("课程") -> {
                    "检测到课程相关内容，但没有识别出课表表格"
                }
                decodedPayload.length < 500 -> "页面内容偏少，可能还没加载完成"
                else -> "没有识别到可导入的课表"
            }
            _uiState.value = _uiState.value.copy(
                message = "$hint\n诊断信息:\n$diagnostics\n已保存快照：${savedFile.name}",
            )
        }
    }

    private fun decodeJavascriptString(value: String): String {
        return runCatching {
            JSONTokener(value).nextValue() as? String
        }.getOrNull() ?: value
    }

    private fun extractHtmlFromPayload(value: String): String {
        return runCatching {
            val payload = JSONTokener(value).nextValue()
            if (payload is org.json.JSONObject) {
                payload.optString("html", value)
            } else {
                value
            }
        }.getOrDefault(value)
    }

    fun deleteSemester(id: String) {
        viewModelScope.launch {
            val targetName = _uiState.value.semesters.firstOrNull { it.id == id }?.displayName
            val deleted = repository.deleteSchedule(id)
            _uiState.value = _uiState.value.copy(
                message = when {
                    deleted -> "已删除${targetName?.let { "「$it」" }.orEmpty()}"
                    _uiState.value.semesters.size <= 1 -> "至少保留一个课表文件夹"
                    else -> "删除失败，请稍后重试"
                },
            )
        }
    }

    class Factory(
        private val repository: ScheduleRepository,
        private val portalClient: AcademicPortalClient,
        private val manualPortalPageStore: ManualPortalPageStore,
        private val manualPortalScheduleParser: ManualPortalScheduleParser,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScheduleViewModel(
                repository = repository,
                portalClient = portalClient,
                manualPortalPageStore = manualPortalPageStore,
                manualPortalScheduleParser = manualPortalScheduleParser,
            ) as T
        }
    }
}

private fun SemesterSchedule.actualWeekForToday(): Int? {
    val startDate = semesterStartDate ?: return currentWeek
    val todayWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (java.time.temporal.ChronoUnit.WEEKS.between(startDate, todayWeekStart) + 1)
        .toInt()
}

private fun Int.absoluteValue(): Int {
    return if (this == Int.MIN_VALUE) 0 else kotlin.math.abs(this)
}

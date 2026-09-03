package com.example.coursetable.data.local

import android.content.Context
import com.example.coursetable.domain.model.Course
import com.example.coursetable.domain.model.CourseColorSeed
import com.example.coursetable.domain.model.SemesterSchedule
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.json.JSONArray
import org.json.JSONObject

class LocalScheduleFileStore(context: Context) {
    private val root: File = File(context.filesDir, "semester_schedules")
    private val selectedSemesterFile: File = File(root, "selected_semester.txt")

    fun semesterFolder(folderName: String): File {
        val normalized = folderName
            .trim()
            .ifBlank { "untitled_semester" }
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(root, normalized)
    }

    fun loadSchedules(): List<SemesterSchedule> {
        root.mkdirs()
        return root
            .listFiles()
            .orEmpty()
            .filter { it.isDirectory }
            .mapNotNull { folder ->
                runCatching {
                    scheduleFile(folder).takeIf { it.exists() }?.readText()?.let(::decodeSchedule)
                }.getOrNull()
            }
            .sortedBy { it.displayName }
    }

    fun saveSchedule(schedule: SemesterSchedule) {
        val folder = semesterFolder(schedule.folderName)
        folder.mkdirs()
        scheduleFile(folder).writeText(encodeSchedule(schedule))
    }

    fun deleteScheduleFolder(folderName: String): Boolean {
        return semesterFolder(folderName).deleteRecursively()
    }

    fun loadSelectedSemesterId(): String? {
        return selectedSemesterFile
            .takeIf { it.exists() }
            ?.readText()
            ?.trim()
            ?.ifBlank { null }
    }

    fun saveSelectedSemesterId(id: String?) {
        root.mkdirs()
        if (id == null) {
            selectedSemesterFile.delete()
        } else {
            selectedSemesterFile.writeText(id)
        }
    }

    private fun scheduleFile(folder: File): File = File(folder, "schedule.json")

    private fun encodeSchedule(schedule: SemesterSchedule): String {
        return JSONObject()
            .put("id", schedule.id)
            .put("displayName", schedule.displayName)
            .put("folderName", schedule.folderName)
            .put("currentWeek", schedule.currentWeek)
            .put("totalWeeks", schedule.totalWeeks)
            .put("semesterStartDate", schedule.semesterStartDate?.toString())
            .put(
                "courses",
                JSONArray().apply {
                    schedule.courses.forEach { course ->
                        put(
                            JSONObject()
                                .put("id", course.id)
                                .put("name", course.name)
                                .put("dayOfWeek", course.dayOfWeek.name)
                                .put("startTime", course.startTime.toString())
                                .put("endTime", course.endTime.toString())
                                .put("location", course.location)
                                .put("teacher", course.teacher)
                                .put("weekStart", course.weeks.first)
                                .put("weekEnd", course.weeks.last)
                                .put("colorSeed", course.colorSeed.name),
                        )
                    }
                },
            )
            .toString(2)
    }

    private fun decodeSchedule(rawJson: String): SemesterSchedule {
        val json = JSONObject(rawJson)
        val courses = json.getJSONArray("courses")
        return SemesterSchedule(
            id = json.getString("id"),
            displayName = json.getString("displayName"),
            folderName = json.getString("folderName"),
            currentWeek = json.getInt("currentWeek"),
            totalWeeks = json.getInt("totalWeeks"),
            semesterStartDate = json.optString("semesterStartDate")
                .takeIf { it.isNotBlank() && it != "null" }
                ?.let(LocalDate::parse),
            courses = List(courses.length()) { index ->
                val course = courses.getJSONObject(index)
                Course(
                    id = course.getString("id"),
                    name = course.getString("name"),
                    dayOfWeek = DayOfWeek.valueOf(course.getString("dayOfWeek")),
                    startTime = LocalTime.parse(course.getString("startTime")),
                    endTime = LocalTime.parse(course.getString("endTime")),
                    location = course.getString("location"),
                    teacher = course.getString("teacher"),
                    weeks = course.getInt("weekStart")..course.getInt("weekEnd"),
                    colorSeed = CourseColorSeed.valueOf(course.getString("colorSeed")),
                )
            },
        )
    }
}

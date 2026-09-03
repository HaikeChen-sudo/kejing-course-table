package com.example.coursetable.domain.model

import java.time.DayOfWeek
import java.time.LocalTime

data class Course(
    val id: String,
    val name: String,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val location: String,
    val teacher: String,
    val weeks: IntRange,
    val colorSeed: CourseColorSeed,
)

enum class CourseColorSeed {
    Coral,
    Mint,
    Amber,
    Violet,
    Cyan,
    Lime,
    Salmon,
    Maroon,
    Sand,
    Rose,
    Grass,
    Sunflower,
    Apricot,
    Azure,
    Forest,
}

/**
 * 按课程名去重后轮换分配颜色：同名课程共用一色，不同课程名尽量不同色。
 * 顺序按星期/时间/名称稳定排序，保证多次计算颜色一致。
 */
fun List<Course>.withDistinctColorSeeds(): List<Course> {
    val seeds = CourseColorSeed.entries
    val sorted = sortedWith(
        compareBy<Course> { it.dayOfWeek.value }
            .thenBy { it.startTime }
            .thenBy { it.name },
    )
    val nameToSeed = sorted
        .map { it.name }
        .distinct()
        .mapIndexed { index, name -> name to seeds[index % seeds.size] }
        .toMap()
    return sorted.map { it.copy(colorSeed = nameToSeed.getValue(it.name)) }
}

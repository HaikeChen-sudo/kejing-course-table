package com.example.coursetable.core.design

import com.example.coursetable.domain.model.CourseColorSeed
import androidx.compose.ui.graphics.Color

data class CoursePalette(
    val container: androidx.compose.ui.graphics.Color,
    val accent: androidx.compose.ui.graphics.Color,
    val content: androidx.compose.ui.graphics.Color,
    val support: androidx.compose.ui.graphics.Color,
)

fun CourseColorSeed.palette(): CoursePalette {
    return when (this) {
        CourseColorSeed.Coral -> glassCoursePalette(Color(0xFFE84C5F), Color(0xFFFF8C9A))
        CourseColorSeed.Mint -> glassCoursePalette(Color(0xFF16A085), Color(0xFF74FFE0))
        CourseColorSeed.Amber -> glassCoursePalette(Color(0xFFE18918), Color(0xFFFFD36E))
        CourseColorSeed.Violet -> glassCoursePalette(Color(0xFF7D5BFF), Color(0xFFC9B8FF))
        CourseColorSeed.Cyan -> glassCoursePalette(Color(0xFF159DD8), Color(0xFF8EE7FF))
        CourseColorSeed.Lime -> glassCoursePalette(Color(0xFF71A91E), Color(0xFFD7FF7A))
    }
}

private fun glassCoursePalette(container: Color, accent: Color): CoursePalette {
    return CoursePalette(
        container = container.copy(alpha = 0.78f),
        accent = accent,
        content = Color.White,
        support = Color.White.copy(alpha = 0.86f),
    )
}

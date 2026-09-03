package com.example.coursetable.core.design

import com.example.coursetable.domain.model.CourseColorSeed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

data class CoursePalette(
    val container: androidx.compose.ui.graphics.Color,
    val accent: androidx.compose.ui.graphics.Color,
    val content: androidx.compose.ui.graphics.Color,
    val support: androidx.compose.ui.graphics.Color,
)

fun CourseColorSeed.palette(): CoursePalette {
    return when (this) {
        CourseColorSeed.Coral -> glassCoursePalette(Color(0xFFC51E2A))
        CourseColorSeed.Mint -> glassCoursePalette(Color(0xFF89D891))
        CourseColorSeed.Amber -> glassCoursePalette(Color(0xFFE7A52D))
        CourseColorSeed.Violet -> glassCoursePalette(Color(0xFF303D67))
        CourseColorSeed.Cyan -> glassCoursePalette(Color(0xFF6F3F8E))
        CourseColorSeed.Lime -> glassCoursePalette(Color(0xFF19BEB8))
        CourseColorSeed.Salmon -> glassCoursePalette(Color(0xFFDA802C))
        CourseColorSeed.Maroon -> glassCoursePalette(Color(0xFF6E2E3A))
        CourseColorSeed.Sand -> glassCoursePalette(Color(0xFF23354C))
        CourseColorSeed.Rose -> glassCoursePalette(Color(0xFFC43F5A))
        CourseColorSeed.Grass -> glassCoursePalette(Color(0xFF6FC448))
        CourseColorSeed.Sunflower -> glassCoursePalette(Color(0xFFFFD343))
        CourseColorSeed.Apricot -> glassCoursePalette(Color(0xFFFF704E))
        CourseColorSeed.Azure -> glassCoursePalette(Color(0xFF007EC4))
        CourseColorSeed.Forest -> glassCoursePalette(Color(0xFF4C9E4F))
    }
}

private fun glassCoursePalette(container: Color): CoursePalette {
    // 文字统一白色，无论卡片深浅
    // 强调色：深色往白提亮、浅色往黑加深，保持色相不变
    val darkContainer = container.luminance() < 0.45f
    val accent = if (darkContainer) {
        lerp(container, Color.White, 0.45f)
    } else {
        lerp(container, Color.Black, 0.3f)
    }
    return CoursePalette(
        container = container.copy(alpha = 0.78f),
        accent = accent,
        content = Color.White,
        support = Color.White.copy(alpha = 0.86f),
    )
}

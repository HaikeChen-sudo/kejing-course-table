package com.example.coursetable.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.coursetable.core.design.KejingColors

private val DarkScheme = darkColorScheme(
    primary = KejingColors.Ink,
    onPrimary = KejingColors.CanvasTop,
    primaryContainer = KejingColors.CanvasBottom,
    onPrimaryContainer = KejingColors.Ink,
    secondary = KejingColors.CanvasBottom,
    onSecondary = KejingColors.Ink,
    secondaryContainer = KejingColors.CanvasBottom,
    onSecondaryContainer = KejingColors.Ink,
    tertiary = KejingColors.CanvasBottom,
    onTertiary = KejingColors.Ink,
    background = KejingColors.CanvasTop,
    onBackground = KejingColors.Ink,
    surface = KejingColors.CanvasBottom,
    onSurface = KejingColors.Ink,
    surfaceVariant = KejingColors.CanvasBottom,
    onSurfaceVariant = KejingColors.Ink,
    surfaceTint = KejingColors.Ink,
    inverseSurface = KejingColors.Ink,
    inverseOnSurface = KejingColors.CanvasTop,
    inversePrimary = KejingColors.CanvasBottom,
    outline = KejingColors.Ink.copy(alpha = 0.4f),
    outlineVariant = KejingColors.Ink.copy(alpha = 0.2f),
    error = KejingColors.Ink,
    onError = KejingColors.CanvasTop,
    errorContainer = KejingColors.CanvasBottom,
    onErrorContainer = KejingColors.Ink,
    scrim = KejingColors.Ink.copy(alpha = 0.32f),
)

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
)

@Composable
fun CourseTableTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkScheme,
        typography = MaterialTheme.typography,
        shapes = AppShapes,
        content = content,
    )
}

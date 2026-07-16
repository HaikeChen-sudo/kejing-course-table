package com.example.coursetable.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.coursetable.core.design.KejingColors

private val DarkScheme = darkColorScheme(
    primary = KejingColors.Orange,
    onPrimary = KejingColors.Black,
    primaryContainer = KejingColors.Orange,
    onPrimaryContainer = KejingColors.Black,
    secondary = KejingColors.Paper,
    onSecondary = KejingColors.Ink,
    secondaryContainer = KejingColors.Graphite,
    onSecondaryContainer = KejingColors.Black,
    tertiary = KejingColors.Porcelain,
    onTertiary = KejingColors.Ink,
    background = KejingColors.Ink,
    onBackground = KejingColors.Porcelain,
    surface = KejingColors.Black,
    onSurface = KejingColors.Porcelain,
    surfaceVariant = KejingColors.Graphite,
    onSurfaceVariant = KejingColors.Paper,
    surfaceTint = KejingColors.Orange,
    inverseSurface = KejingColors.Porcelain,
    inverseOnSurface = KejingColors.Ink,
    inversePrimary = KejingColors.Orange,
    outline = KejingColors.Graphite,
    outlineVariant = KejingColors.Ink,
    error = KejingColors.Orange,
    onError = KejingColors.Black,
    errorContainer = KejingColors.Orange,
    onErrorContainer = KejingColors.Black,
    scrim = KejingColors.Black,
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

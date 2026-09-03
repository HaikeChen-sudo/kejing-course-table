package com.example.coursetable.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object KejingColors {
    val CanvasTop = Color(0xFFF2F4F5)
    val CanvasBottom = Color(0xFF98A0A5)
    val NavigationPill = Color(0xFFE4E7E8)
    val Panel = CanvasBottom
    val PanelSelected = CanvasBottom
    val Ink = Color(0xFF1E2421)
    val Paper = Color(0xFFE5E9EB)
    val Orange = Color(0xFFFF920F)
    val Graphite = Color(0xFF2C322F)
    val Black = Color(0xFF121513)
    val Track = Color(0xFF171C19)
    val Porcelain = Color(0xFFFAFBF6)
}

object KejingShape {
    val Page = RoundedCornerShape(0.dp)
    val Widget = RoundedCornerShape(12.dp)
    val Card = RoundedCornerShape(10.dp)
    val Control = RoundedCornerShape(6.dp)
    val Pill = RoundedCornerShape(6.dp)
}

fun Modifier.kejingGradientBackground(): Modifier {
    return background(
        Brush.verticalGradient(
            listOf(KejingColors.CanvasTop, KejingColors.CanvasBottom),
        ),
    )
}

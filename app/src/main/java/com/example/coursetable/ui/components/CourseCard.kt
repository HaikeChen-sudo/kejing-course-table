package com.example.coursetable.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coursetable.core.design.KejingShape
import com.example.coursetable.core.design.CoursePalette
import com.example.coursetable.core.design.palette
import com.example.coursetable.domain.model.Course
import java.time.format.DateTimeFormatter

private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun CourseCard(
    course: Course,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val palette = course.colorSeed.palette()
    val surfaceModifier = modifier
        .fillMaxWidth()
        .shadow(10.dp, KejingShape.Card, clip = false)
    if (onClick == null) {
        Surface(
            modifier = surfaceModifier,
            color = palette.container,
            contentColor = palette.content,
            shape = KejingShape.Card,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            CourseCardContent(course = course, compact = compact, palette = palette)
        }
    } else {
        Surface(
            onClick = onClick,
            modifier = surfaceModifier,
            color = palette.container,
            contentColor = palette.content,
            shape = KejingShape.Card,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            CourseCardContent(course = course, compact = compact, palette = palette)
        }
    }
}

@Composable
private fun CourseCardContent(
    course: Course,
    compact: Boolean,
    palette: CoursePalette,
) {
    Column(
        modifier = Modifier
            .padding(if (compact) 12.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = course.name,
                    style = if (compact) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.titleLarge
                    },
                    color = palette.content,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp,
                )
                Text(
                    text = "${course.startTime.format(TimeFormatter)}-${course.endTime.format(TimeFormatter)}",
                    color = palette.content,
                    fontSize = if (compact) 12.sp else 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp,
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .background(palette.content.copy(alpha = 0.18f), KejingShape.Control)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (compact) "${course.weeks.first}周" else "第 ${course.weeks.first}-${course.weeks.last} 周",
                    color = palette.content,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetadataRow(
                    icon = Icons.Rounded.LocationOn,
                    text = course.location,
                    palette = palette,
                )
                if (!compact) {
                    MetadataRow(
                        icon = Icons.Rounded.Person,
                        text = course.teacher,
                        palette = palette,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(width = 46.dp, height = 4.dp)
                    .background(palette.accent, KejingShape.Control),
            )
        }
    }
}

@Composable
private fun MetadataRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    palette: CoursePalette,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = palette.support,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = palette.support,
        )
    }
}

package com.example.coursetable.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.coursetable.core.design.KejingColors
import com.example.coursetable.core.design.KejingShape
import com.example.coursetable.domain.model.SemesterSchedule

@Composable
fun SemesterPicker(
    semesters: List<SemesterSchedule>,
    selectedSemester: SemesterSchedule?,
    onSelectSemester: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { expanded = true },
            color = KejingColors.Panel,
            contentColor = KejingColors.Ink,
            shape = KejingShape.Control,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.shadow(8.dp, KejingShape.Control, clip = false),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedSemester?.displayName ?: "选择学期",
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = "展开学期列表",
                    tint = KejingColors.Ink,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .padding(vertical = 4.dp),
            containerColor = KejingColors.Panel,
            shape = KejingShape.Control,
        ) {
            semesters.forEach { semester ->
                DropdownMenuItem(
                    text = { Text(semester.displayName) },
                    onClick = {
                        expanded = false
                        onSelectSemester(semester.id)
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = KejingColors.Ink,
                    ),
                )
            }
        }
    }
}

package com.example.coursetable.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.coursetable.core.design.KejingColors
import com.example.coursetable.core.design.KejingShape
import com.example.coursetable.data.remote.PortalSchoolConfig

/**
 * 学校选择页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolSelectScreen(
    onBack: () -> Unit,
    onSchoolSelected: (String) -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        containerColor = KejingColors.Ink,
        contentColor = KejingColors.Porcelain,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KejingColors.Ink,
                    titleContentColor = KejingColors.Porcelain,
                    navigationIconContentColor = KejingColors.Orange,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                title = { Text("选择学校") },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "请选择你的学校教务系统",
                    style = MaterialTheme.typography.titleLarge,
                    color = KejingColors.Porcelain,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(PortalSchoolConfig.supportedSchools) { school ->
                SchoolCard(
                    school = school,
                    onClick = { onSchoolSelected(school.schoolCode) },
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "提示：选择学校后将打开对应的教务系统登录页面，登录后点击「读取并导入」即可自动解析课表。",
                    style = MaterialTheme.typography.bodySmall,
                    color = KejingColors.Graphite,
                )
            }
        }
    }
}

@Composable
private fun SchoolCard(
    school: PortalSchoolConfig,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = KejingColors.Black,
        contentColor = KejingColors.Porcelain,
        shape = KejingShape.Card,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, KejingColors.Graphite, KejingShape.Card)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.School,
                contentDescription = null,
                tint = KejingColors.Orange,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = school.schoolName,
                    style = MaterialTheme.typography.titleMedium,
                    color = KejingColors.Porcelain,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = school.host,
                    style = MaterialTheme.typography.bodySmall,
                    color = KejingColors.Graphite,
                )
            }
        }
    }
}

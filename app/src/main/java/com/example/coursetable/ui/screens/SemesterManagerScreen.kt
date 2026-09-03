package com.example.coursetable.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.coursetable.core.design.KejingColors
import com.example.coursetable.core.design.KejingShape
import com.example.coursetable.core.design.kejingGradientBackground
import com.example.coursetable.domain.model.ImportMode
import com.example.coursetable.domain.model.SemesterSchedule
import com.example.coursetable.ui.ScheduleUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterManagerScreen(
    uiState: ScheduleUiState,
    onBack: () -> Unit,
    onSelectSemester: (String) -> Unit,
    onImportClick: () -> Unit,
    onOpenPortalWebLogin: () -> Unit,
    onDeleteSemester: (String) -> Unit,
    onDismissImport: () -> Unit,
    onImport: (ImportMode) -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var pendingDeleteSemester by remember { mutableStateOf<SemesterSchedule?>(null) }
    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarMessage = message
            onMessageShown()
        }
    }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            snackbarMessage = null
        }
    }

    if (uiState.isImportDialogOpen) {
        ImportModeDialog(
            isSyncing = uiState.isSyncing,
            onDismiss = onDismissImport,
            onImport = onImport,
        )
    }
    pendingDeleteSemester?.let { semester ->
        DeleteSemesterDialog(
            semester = semester,
            canDelete = uiState.semesters.size > 1,
            onDismiss = { pendingDeleteSemester = null },
            onConfirm = {
                pendingDeleteSemester = null
                onDeleteSemester(semester.id)
            },
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        containerColor = KejingColors.CanvasTop.copy(alpha = 0f),
        contentColor = KejingColors.Ink,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KejingColors.CanvasTop.copy(alpha = 0f),
                    titleContentColor = KejingColors.Ink,
                    navigationIconContentColor = KejingColors.Ink,
                    actionIconContentColor = KejingColors.Ink,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                title = {
                    Text(
                        text = "学期管理",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackbarData ->
                Snackbar(
                    snackbarData = snackbarData,
                    containerColor = KejingColors.CanvasBottom,
                    contentColor = KejingColors.Ink,
                    actionColor = KejingColors.Ink,
                )
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .kejingGradientBackground()
                .padding(paddingValues),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "课表文件夹",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = KejingColors.Ink,
                    )
                    Text(
                        text = "每个学期独立保存，当前选中项会用于首页展示",
                        style = MaterialTheme.typography.bodySmall,
                        color = KejingColors.Ink.copy(alpha = 0.68f),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = onOpenPortalWebLogin,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = KejingColors.CanvasBottom,
                                contentColor = KejingColors.Ink,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Language,
                                contentDescription = null,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("网页登录")
                        }
                        Button(
                            enabled = !uiState.isSyncing,
                            onClick = onImportClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = KejingColors.Orange,
                                contentColor = KejingColors.Black,
                                disabledContainerColor = KejingColors.Graphite,
                                disabledContentColor = KejingColors.Ink,
                            ),
                        ) {
                            if (uiState.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = KejingColors.Black,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.CloudSync,
                                    contentDescription = null,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("导入")
                        }
                    }
                }
            }
            items(uiState.semesters, key = { it.id }) { semester ->
                val selected = semester.id == uiState.selectedSemester?.id
                Surface(
                    onClick = { onSelectSemester(semester.id) },
                    modifier = Modifier.fillMaxWidth(),
                    color = KejingColors.CanvasTop,
                    contentColor = KejingColors.Ink,
                    shape = KejingShape.Card,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Surface(
                            color = KejingColors.CanvasBottom,
                            contentColor = KejingColors.Ink,
                            shape = KejingShape.Control,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Folder,
                                contentDescription = null,
                                modifier = Modifier.padding(10.dp),
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = semester.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = KejingColors.Ink,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "${semester.folderName} · ${semester.courses.size} 门课",
                                style = MaterialTheme.typography.bodySmall,
                                color = KejingColors.Ink.copy(alpha = 0.68f),
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "当前学期",
                                tint = KejingColors.Orange,
                            )
                        }
                        IconButton(
                            enabled = uiState.semesters.size > 1,
                            onClick = { pendingDeleteSemester = semester },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "删除课表文件夹",
                                tint = if (uiState.semesters.size > 1) {
                                    KejingColors.Orange
                                } else {
                                    KejingColors.Ink
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportModeDialog(
    isSyncing: Boolean,
    onDismiss: () -> Unit,
    onImport: (ImportMode) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KejingColors.CanvasBottom,
        titleContentColor = KejingColors.Ink,
        textContentColor = KejingColors.Ink,
        title = { Text("导入新课表") },
        text = {
            Text("从教务系统同步后，可以覆盖当前学期，也可以新建一个独立文件夹保存。")
        },
        confirmButton = {
            FilledTonalButton(
                enabled = !isSyncing,
                onClick = { onImport(ImportMode.CreateNewFolder) },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = KejingColors.Orange,
                    contentColor = KejingColors.Black,
                    disabledContainerColor = KejingColors.Graphite,
                    disabledContentColor = KejingColors.Ink,
                ),
            ) {
                Text("新建文件夹")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    enabled = !isSyncing,
                    onClick = onDismiss,
                ) {
                    Text("取消")
                }
                TextButton(
                    enabled = !isSyncing,
                    onClick = { onImport(ImportMode.OverwriteCurrent) },
                ) {
                    Text("覆盖当前")
                }
            }
        },
    )
}

@Composable
private fun DeleteSemesterDialog(
    semester: SemesterSchedule,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KejingColors.CanvasBottom,
        titleContentColor = KejingColors.Ink,
        textContentColor = KejingColors.Ink,
        title = { Text("删除课表文件夹") },
        text = {
            Text(
                if (canDelete) {
                    "确定删除「${semester.displayName}」吗？对应的本地文件夹和课程数据会一起删除。"
                } else {
                    "至少需要保留一个课表文件夹。"
                },
            )
        },
        confirmButton = {
            Button(
                enabled = canDelete,
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KejingColors.Orange,
                    contentColor = KejingColors.Black,
                    disabledContainerColor = KejingColors.Graphite,
                    disabledContentColor = KejingColors.Ink,
                ),
            ) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

package com.example.coursetable.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.coursetable.core.design.KejingColors
import com.example.coursetable.core.design.KejingShape
import com.example.coursetable.domain.model.Memo
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val MemoTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val MemoDateFormatter = DateTimeFormatter.ofPattern("M月d日")

@Composable
fun MemoScreen(
    memos: List<Memo>,
    onOpenMemo: (String) -> Unit,
    onCreateMemo: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredMemos = remember(memos, query) {
        val keyword = query.trim()
        if (keyword.isBlank()) memos else memos.filter {
            it.title.contains(keyword, ignoreCase = true) ||
                it.content.contains(keyword, ignoreCase = true)
        }
    }
    val background = remember {
        Brush.verticalGradient(listOf(KejingColors.CanvasTop, KejingColors.CanvasBottom))
    }
    val statusTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateMemo,
                containerColor = KejingColors.Orange,
                contentColor = KejingColors.Black,
                shape = KejingShape.Widget,
            ) {
                Icon(Icons.Rounded.EditNote, contentDescription = "新建备忘录")
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = statusTopPadding + 12.dp,
                end = 20.dp,
                bottom = 92.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "备忘录",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = KejingColors.Ink,
                )
            }
            item {
                MemoSearchField(query = query, onQueryChange = { query = it })
            }
            item {
                Text(
                    text = "所有备忘录",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = KejingColors.Ink,
                )
            }
            if (filteredMemos.isEmpty()) {
                item {
                    MemoEmptyState(hasQuery = query.isNotBlank())
                }
            } else {
                items(filteredMemos, key = Memo::id) { memo ->
                    MemoRow(memo = memo, onClick = { onOpenMemo(memo.id) })
                }
            }
            item {
                Text(
                    text = "${filteredMemos.size} 条备忘录",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelMedium,
                    color = KejingColors.Ink.copy(alpha = 0.58f),
                )
            }
        }
    }
}

@Composable
private fun MemoSearchField(query: String, onQueryChange: (String) -> Unit) {
    Surface(
        color = KejingColors.CanvasTop.copy(alpha = 0.58f),
        shape = KejingShape.Widget,
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            placeholder = { Text("搜索") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = KejingColors.Ink,
                focusedTextColor = KejingColors.Ink,
                unfocusedTextColor = KejingColors.Ink,
                focusedPlaceholderColor = KejingColors.Ink.copy(alpha = 0.48f),
                unfocusedPlaceholderColor = KejingColors.Ink.copy(alpha = 0.48f),
                focusedLeadingIconColor = KejingColors.Ink.copy(alpha = 0.54f),
                unfocusedLeadingIconColor = KejingColors.Ink.copy(alpha = 0.54f),
            ),
        )
    }
}

@Composable
private fun MemoRow(memo: Memo, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = memo.title.ifBlank { "无标题" },
            style = MaterialTheme.typography.titleMedium,
            color = KejingColors.Ink,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = memoUpdatedLabel(memo.updatedAtMillis),
                style = MaterialTheme.typography.bodySmall,
                color = KejingColors.Ink.copy(alpha = 0.56f),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = memo.content.lineSequence().firstOrNull { it.isNotBlank() } ?: "无附加文字",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = KejingColors.Ink.copy(alpha = 0.68f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = KejingColors.Ink.copy(alpha = 0.14f))
    }
}

@Composable
private fun MemoEmptyState(hasQuery: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 66.dp, bottom = 82.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.EditNote,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = KejingColors.Ink.copy(alpha = 0.34f),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (hasQuery) "没有找到备忘录" else "还没有备忘录",
                style = MaterialTheme.typography.titleMedium,
                color = KejingColors.Ink.copy(alpha = 0.68f),
            )
            Text(
                text = if (hasQuery) "换个关键词试试" else "点右下角写下第一条",
                style = MaterialTheme.typography.bodyMedium,
                color = KejingColors.Ink.copy(alpha = 0.52f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoEditorScreen(
    memo: Memo?,
    onBack: () -> Unit,
    onSave: (String, String) -> Unit,
    onDelete: () -> Unit,
) {
    var title by rememberSaveable(memo?.id) { mutableStateOf(memo?.title.orEmpty()) }
    var content by rememberSaveable(memo?.id) { mutableStateOf(memo?.content.orEmpty()) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val background = remember {
        Brush.verticalGradient(listOf(KejingColors.CanvasTop, KejingColors.CanvasBottom))
    }
    val saveAndBack = {
        onSave(title, content)
        onBack()
    }
    BackHandler(onBack = saveAndBack)

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = KejingColors.CanvasTop,
            title = { Text("删除备忘录？") },
            text = { Text("删除后不能恢复。") },
            confirmButton = {
                TextButton(onClick = onDelete) { Text("删除", color = KejingColors.Orange) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
            },
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text("备忘录", color = KejingColors.Ink) },
                navigationIcon = {
                    IconButton(onClick = saveAndBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = KejingColors.Ink)
                    }
                },
                actions = {
                    if (memo != null) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "删除", tint = KejingColors.Ink)
                        }
                    }
                    TextButton(onClick = saveAndBack) {
                        Text("完成", color = KejingColors.Ink, fontWeight = FontWeight.SemiBold)
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
        ) {
            if (memo != null) {
                Text(
                    text = memoUpdatedLabel(memo.updatedAtMillis),
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = KejingColors.Ink.copy(alpha = 0.54f),
                )
            }
            EditorField(
                value = title,
                onValueChange = { title = it },
                placeholder = "标题",
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    color = KejingColors.Ink,
                    fontWeight = FontWeight.Bold,
                ),
            )
            EditorField(
                value = content,
                onValueChange = { content = it },
                placeholder = "开始书写",
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = KejingColors.Ink),
            )
        }
    }
}

@Composable
private fun EditorField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textStyle: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        placeholder = { Text(placeholder, style = textStyle, color = KejingColors.Ink.copy(alpha = 0.36f)) },
        textStyle = textStyle,
        singleLine = singleLine,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = KejingColors.Ink,
        ),
    )
}

private fun memoUpdatedLabel(updatedAtMillis: Long): String {
    val dateTime = Instant.ofEpochMilli(updatedAtMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    return if (dateTime.toLocalDate() == LocalDate.now()) {
        "今天 ${dateTime.format(MemoTimeFormatter)}"
    } else {
        dateTime.format(MemoDateFormatter)
    }
}

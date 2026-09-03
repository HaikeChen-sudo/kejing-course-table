package com.example.coursetable.ui

import com.example.coursetable.domain.model.Memo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleViewModelTest {
    @Test
    fun refreshKeepsTheImportCompletionEvent() {
        val refreshed = ScheduleUiState(importCompleted = false, message = null)
        val current = ScheduleUiState(importCompleted = true, message = "导入成功")

        val result = mergeScheduleState(refreshed, current)

        assertTrue(result.importCompleted)
        assertEquals("导入成功", result.message)
    }

    @Test
    fun consumingImportCompletionAlsoClearsItsSuccessMessage() {
        val result = clearImportCompleted(
            ScheduleUiState(importCompleted = true, message = "导入成功：20 门课程"),
        )

        assertEquals(null, result.message)
    }

    @Test
    fun consumingMessageBeforeShowingSnackbarRemovesItFromRefreshState() {
        val result = consumeMessage(ScheduleUiState(message = "已删除「2026 春季」"))

        assertEquals(null, result.message)
    }

    @Test
    fun savingMemoReplacesTheOldCopyAndKeepsNewestFirst() {
        val old = Memo("one", "旧标题", "", 10L)
        val newer = Memo("one", "新标题", "", 30L)
        val second = Memo("two", "另一条", "", 20L)

        val result = upsertMemo(listOf(old, second), newer)

        assertEquals(listOf(newer, second), result)
    }
}

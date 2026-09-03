package com.example.coursetable.data.local

import android.content.Context
import com.example.coursetable.domain.model.Memo
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

class LocalMemoStore(context: Context) {
    private val file = File(context.filesDir, "memos.json")

    fun load(): List<Memo> = runCatching {
        if (!file.exists()) return emptyList()
        val values = JSONArray(file.readText())
        List(values.length()) { index ->
            val value = values.getJSONObject(index)
            Memo(
                id = value.getString("id"),
                title = value.optString("title"),
                content = value.optString("content"),
                updatedAtMillis = value.getLong("updatedAtMillis"),
            )
        }.sortedByDescending(Memo::updatedAtMillis)
    }.getOrDefault(emptyList())

    fun save(memos: List<Memo>) {
        val values = JSONArray().apply {
            memos.forEach { memo ->
                put(
                    JSONObject()
                        .put("id", memo.id)
                        .put("title", memo.title)
                        .put("content", memo.content)
                        .put("updatedAtMillis", memo.updatedAtMillis),
                )
            }
        }
        file.writeText(values.toString())
    }
}

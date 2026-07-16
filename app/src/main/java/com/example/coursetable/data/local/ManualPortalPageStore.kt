package com.example.coursetable.data.local

import android.content.Context
import java.io.File
import org.json.JSONObject

class ManualPortalPageStore(context: Context) {
    private val root: File = File(context.filesDir, "manual_portal_pages")

    init {
        root.mkdirs()
    }

    fun saveLatest(url: String, title: String, html: String): File {
        val htmlFile = File(root, "latest.html")
        val metadataFile = File(root, "latest.json")
        htmlFile.writeText(html)
        metadataFile.writeText(
            JSONObject()
                .put("url", url)
                .put("title", title)
                .put("htmlFile", htmlFile.name)
                .put("htmlLength", html.length)
                .put("savedAt", System.currentTimeMillis())
                .toString(2),
        )
        return htmlFile
    }
}

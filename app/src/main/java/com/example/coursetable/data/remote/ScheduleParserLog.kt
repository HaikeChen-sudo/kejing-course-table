package com.example.coursetable.data.remote

internal fun scheduleParserLog(message: String) {
    runCatching {
        android.util.Log.d("ScheduleParser", message)
    }
}

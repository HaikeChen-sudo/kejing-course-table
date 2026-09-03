package com.example.coursetable.domain.model

data class Memo(
    val id: String,
    val title: String,
    val content: String,
    val updatedAtMillis: Long,
)

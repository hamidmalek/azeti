package com.example.azeti.dto

import java.time.LocalDateTime

data class NoteDto(
    val id: String?,
    val title: String,
    val content: String,
    val expiresAt: LocalDateTime?
)

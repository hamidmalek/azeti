package com.example.azeti.repo

import com.example.azeti.model.Note
import com.example.azeti.model.User
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.UUID

interface NoteRepository : JpaRepository<Note, UUID> {
    fun findTop1000ByUserAndExpiresAtAfterOrExpiresAtIsNullOrderByCreatedAtDesc(
        user: User,
        now: LocalDateTime
    ): List<Note>

    fun findByIdAndUserId(noteId: UUID, userId: UUID): Note?
}

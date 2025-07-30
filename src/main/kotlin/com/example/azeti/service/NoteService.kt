package com.example.azeti.service

import com.example.azeti.dto.NoteDto
import com.example.azeti.model.Note
import com.example.azeti.repo.NoteRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.*

@Service
class NoteService(
    private val noteRepo: NoteRepository,
    private val authService: AuthService,
) {
    fun createNote(dto: NoteDto): NoteDto {
        val savedNote = noteRepo.save(
            Note(
                title = dto.title,
                content = dto.content,
                expiresAt = dto.expiresAt,
                user = authService.getCurrentUser()
            )
        )
        return NoteDto(savedNote.id.toString(), savedNote.title, savedNote.content, savedNote.expiresAt)
    }

    fun updateNote(noteId: UUID, dto: NoteDto): NoteDto {
        val note = noteRepo.findByIdAndUserId(noteId, authService.getCurrentUser().id) ?: run {
            throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
            )
        }

        val updated = note.copy(
            title = dto.title,
            content = dto.content,
            expiresAt = dto.expiresAt
        )
        val savedNote = noteRepo.save(updated)
        return NoteDto(savedNote.id.toString(), savedNote.title, savedNote.content, savedNote.expiresAt)
    }

    fun deleteNote(noteId: UUID) {
        val note = noteRepo.findByIdAndUserId(noteId, authService.getCurrentUser().id) ?: run {
            throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
            )
        }

        if (note.user.id != authService.getCurrentUser().id) throw ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "You are not allowed to modify this note"
        )

        noteRepo.delete(note)
    }


    fun latestNotes(): List<NoteDto> {
        return noteRepo.findTop1000ByUserAndExpiresAtAfterOrExpiresAtIsNullOrderByCreatedAtDesc(
            authService.getCurrentUser(),
            LocalDateTime.now()
        ).map { NoteDto(it.id.toString(), it.title, it.content, it.expiresAt) }
    }
}
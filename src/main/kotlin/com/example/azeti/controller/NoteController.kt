package com.example.azeti.controller

import com.example.azeti.dto.NoteDto
import com.example.azeti.service.NoteService
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/notes")
@Validated
class NoteController(private val noteService: NoteService) {
    @PostMapping
    fun create(@RequestBody dto: NoteDto): NoteDto =
        noteService.createNote(dto)

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: String,
        @RequestBody dto: NoteDto,
    ): NoteDto =
        noteService.updateNote(UUID.fromString(id), dto)

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: String,
    ): ResponseEntity<Void> {
        noteService.deleteNote(UUID.fromString(id))
        return ResponseEntity.noContent().build()
    }


    @GetMapping("/latest")
    fun latest(): List<NoteDto> =
        noteService.latestNotes()
}

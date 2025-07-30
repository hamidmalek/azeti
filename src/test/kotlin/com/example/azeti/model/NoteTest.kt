package com.example.azeti.model

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertTrue

class NoteTest {

    private lateinit var validator: Validator
    private lateinit var validUser: User

    @BeforeEach
    fun setUp() {
        validator = Validation.buildDefaultValidatorFactory().validator
        validUser = User(
            id = UUID.randomUUID(),
            username = "testuser",
            password = "securePassword"
        )
    }

    @Test
    fun `valid note should pass validation`() {
        val note = Note(
            title = "Valid Title",
            content = "Some valid content.",
            user = validUser
        )

        val violations = validator.validate(note)
        assertTrue(violations.isEmpty(), "Expected no validation violations")
    }

    @Test
    fun `blank title should fail validation`() {
        val note = Note(
            title = "   ",
            content = "Valid content",
            user = validUser
        )

        val violations = validator.validate(note)
        assertTrue(violations.any { it.propertyPath.toString() == "title" && it.message.contains("must not be blank") })
    }

    @Test
    fun `title exceeding 30 characters should fail validation`() {
        val longTitle = "A".repeat(31)
        val note = Note(
            title = longTitle,
            content = "Valid content",
            user = validUser
        )

        val violations = validator.validate(note)
        assertTrue(violations.any { it.propertyPath.toString() == "title" })
    }

    @Test
    fun `blank content should pass validation`() {
        val note = Note(
            title = "Valid title",
            content = "   ", // blank but allowed now
            user = validUser
        )

        val violations = validator.validate(note)
        assertTrue(violations.isEmpty(), "Expected no violations for blank content")
    }

    @Test
    fun `content exceeding 255 characters should fail validation`() {
        val longContent = "C".repeat(256)
        val note = Note(
            title = "Valid title",
            content = longContent,
            user = validUser
        )

        val violations = validator.validate(note)
        assertTrue(violations.any { it.propertyPath.toString() == "content" && it.message.contains("at most 255 characters") })
    }

    @Test
    fun `createdAt should be auto-set to now`() {
        val note = Note(
            title = "Title",
            content = "Content",
            user = validUser
        )

        assertTrue(note.createdAt.isBefore(LocalDateTime.now().plusSeconds(1)))
    }

    @Test
    fun `expiresAt can be null`() {
        val note = Note(
            title = "Title",
            content = "Content",
            user = validUser,
            expiresAt = null
        )

        val violations = validator.validate(note)
        assertTrue(violations.isEmpty())
    }
}

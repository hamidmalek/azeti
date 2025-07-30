package com.example.azeti.model

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.hibernate.annotations.UuidGenerator
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "notes")
data class Note(
    @Id
    @Column(name = "id", unique = true)
    val id: UUID = UUID.randomUUID(),

    @field:NotBlank(message = "Title must not be blank")
    @field:Size(max = 30, message = "Title must be at most 30 characters")
    @Column(nullable = false, length = 30)
    val title: String,

    @field:Size(max = 255, message = "Content must be at most 255 characters")
    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = true)
    val expiresAt: LocalDateTime? = null,

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User
)

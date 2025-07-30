package com.example.azeti.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import java.util.*

@Entity
@Table(name = "users")
data class User(
    @Id
    @Column(name = "id", unique = true)
    val id: UUID = UUID.randomUUID(),

    @field:NotBlank(message = "Username is required")
    @Column(nullable = false, unique = true)
    val username: String = "",

    @field:NotBlank(message = "Password is required")
    @Column(nullable = false)
    val password: String = ""
)
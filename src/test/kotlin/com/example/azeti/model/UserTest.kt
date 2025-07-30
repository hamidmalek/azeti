package com.example.azeti.model

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertTrue

class UserTest {

    private lateinit var validator: Validator

    @BeforeEach
    fun setup() {
        val factory = Validation.buildDefaultValidatorFactory()
        validator = factory.validator
    }

    @Test
    fun `valid user passes validation`() {
        val user = User(username = "johndoe", password = "securePassword123")
        val violations = validator.validate(user)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `username is required`() {
        val user = User(username = "", password = "pass")
        val violations = validator.validate(user)
        assertTrue(violations.any { it.propertyPath.toString() == "username" })
    }
}
package com.example.azeti.controller

import jakarta.transaction.Transactional
import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    private val username = "user1"
    private val password = "secret123"
    private val body = """{"username":"$username", "password":"$password"}"""

    private val registerUrl = "/api/auth/register"
    private val loginUrl = "/api/auth/login"

    @BeforeEach
    fun setup() {
        // Register user for login-related tests
        mockMvc.perform(
            post(registerUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `should register a new user`() {
        val newBody = """{"username":"user2", "password":"newpass456"}"""
        mockMvc.perform(
            post(registerUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(newBody)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `should not allow duplicate registration`() {
        mockMvc.perform(
            post(registerUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isConflict) // Or whatever your API returns on duplicate
    }

    @Test
    fun `should login and receive JWT`() {
        mockMvc.perform(
            post(loginUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").exists())
    }

    @Test
    fun `should not login with incorrect password`() {
        val invalidBody = """{"username":"$username", "password":"wrongpass"}"""
        mockMvc.perform(
            post(loginUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidBody)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should not login with non-existent user`() {
        val invalidUser = """{"username":"ghost", "password":"anything"}"""
        mockMvc.perform(
            post(loginUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidUser)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should fail to register with invalid payload`() {
        val invalidBody = """{"username": "only"}""" // Missing password
        mockMvc.perform(
            post(registerUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidBody)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should fail to login with malformed JSON`() {
        val malformedBody = """{"username":"badjson", "password":123""" // Missing closing }
        mockMvc.perform(
            post(loginUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedBody)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should return JWT with expected structure`() {
        val jwtPattern = "^[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+$"
        mockMvc.perform(
            post(loginUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token", matchesPattern(jwtPattern)))
    }
}

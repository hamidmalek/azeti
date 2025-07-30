package com.example.azeti.controller

import com.example.azeti.security.RateLimitingFilter
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.transaction.Transactional
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NoteControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var rateLimitingFilter: RateLimitingFilter

    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    private val password = "testpass"
    private val userA = "alice"
    private val userB = "bob"

    private lateinit var tokenA: String
    private lateinit var tokenB: String

    private val registerUrl = "/api/auth/register"
    private val loginUrl = "/api/auth/login"
    private val notesUrl = "/api/notes"

    @BeforeEach
    fun setup() {
        listOf(userA, userB).forEach {
            mockMvc.post(registerUrl) {
                contentType = MediaType.APPLICATION_JSON
                content = """{"username":"$it", "password":"$password"}"""
            }
        }

        tokenA = loginAndGetToken(userA)
        tokenB = loginAndGetToken(userB)
    }

    private fun loginAndGetToken(username: String): String {
        val response = mockMvc.post(loginUrl) {
            contentType = MediaType.APPLICATION_JSON
            content = """{"username":"$username", "password":"$password"}"""
        }.andReturn()

        return objectMapper.readTree(response.response.contentAsString).get("token").asText()
    }

    @Test
    fun `should create a note`() {
        val note = mapOf("title" to "Secret", "content" to "Top stuff")

        mockMvc.post(notesUrl) {
            header("Authorization", "Bearer $tokenA")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(note)
        }.andExpect {
            status { isOk() }
            jsonPath("$.title").value("Secret")
        }
    }

    @Test
    fun `should return only unexpired notes`() {
        val now = LocalDateTime.now()
        val expiredNote = mapOf("title" to "Old", "content" to "expired", "expiresAt" to now.minusDays(1))
        val validNote = mapOf("title" to "Fresh", "content" to "valid", "expiresAt" to now.plusDays(1))

        listOf(expiredNote, validNote).forEach {
            mockMvc.post(notesUrl) {
                header("Authorization", "Bearer $tokenA")
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(it)
            }
        }

        mockMvc.get("$notesUrl/latest") {
            header("Authorization", "Bearer $tokenA")
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()").value(1)
            jsonPath("$[0].title").value("Fresh")
        }
    }

    @Test
    fun `should not allow unauthenticated access`() {
        val note = mapOf("title" to "Hidden", "content" to "none")

        mockMvc.post(notesUrl) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(note)
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `should isolate notes between users`() {
        val note = mapOf("title" to "Private", "content" to "for alice")

        val result = mockMvc.post(notesUrl) {
            header("Authorization", "Bearer $tokenA")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(note)
        }.andReturn()

        val id = objectMapper.readTree(result.response.contentAsString).get("id")

        mockMvc.get("$notesUrl/latest") {
            header("Authorization", "Bearer $tokenB")
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()").value(0)
        }

        mockMvc.get("$notesUrl/latest") {
            header("Authorization", "Bearer $tokenA")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].id").value(id)
        }
    }

    @Test
    fun `should update own note`() {
        val original = mapOf("title" to "Original", "content" to "unchanged")
        val response = mockMvc.post(notesUrl) {
            header("Authorization", "Bearer $tokenA")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(original)
        }.andReturn()

        val id = objectMapper.readTree(response.response.contentAsString).get("id").asText()

        val update = mapOf("title" to "Updated", "content" to "changed")
        mockMvc.put("$notesUrl/$id") {
            header("Authorization", "Bearer $tokenA")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(update)
        }.andExpect {
            status { isOk() }
            jsonPath("$.title").value("Updated")
        }
    }

    @Test
    fun `should forbid updating another user's note`() {
        rateLimitingFilter.resetBucket(userA)
        val note = mapOf("title" to "Secret", "content" to "hidden")
        val response = mockMvc.post(notesUrl) {
            header("Authorization", "Bearer $tokenA")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(note)
        }.andReturn()

        val id = objectMapper.readTree(response.response.contentAsString).get("id").asText()

        val attempt = mapOf("title" to "Hack", "content" to "oops")
        mockMvc.put("$notesUrl/$id") {
            header("Authorization", "Bearer $tokenB")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(attempt)
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `should delete own note`() {
        rateLimitingFilter.resetBucket(userA)
        val note = mapOf("title" to "Delete me", "content" to "soon")
        val response = mockMvc.post(notesUrl) {
            header("Authorization", "Bearer $tokenA")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(note)
        }.andReturn()

        val content = response.response.contentAsString
        println("Response content: $content")

        val id = objectMapper.readTree(response.response.contentAsString).get("id").asText()

        mockMvc.delete("$notesUrl/$id") {
            header("Authorization", "Bearer $tokenA")
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `should forbid deleting another user's note`() {
        val note = mapOf("title" to "Not yours", "content" to "private")
        val response = mockMvc.post(notesUrl) {
            header("Authorization", "Bearer $tokenA")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(note)
        }.andReturn()

        val id = objectMapper.readTree(response.response.contentAsString).get("id").asText()
        
        mockMvc.delete("$notesUrl/$id") {
            header("Authorization", "Bearer $tokenB")
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `should return max 1000 notes`() {
        val originalEnabled = rateLimitingFilter.enabled
        rateLimitingFilter.enabled = false
        try {

            repeat(1005) {
                val note = mapOf("title" to "Note $it", "content" to "data $it")
                mockMvc.post(notesUrl) {
                    header("Authorization", "Bearer $tokenA")
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(note)
                }
            }

            mockMvc.get("$notesUrl/latest") {
                header("Authorization", "Bearer $tokenA")
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()").value(1000)
            }
        } finally {
            rateLimitingFilter.enabled = originalEnabled
        }
    }

    @Test
    fun `should enforce rate limiting`() {
        rateLimitingFilter.resetBucket(userA)
        repeat(10) {
            mockMvc.get("$notesUrl/latest") {
                header("Authorization", "Bearer $tokenA")
            }.andExpect { status { isOk() } }
        }

        mockMvc.get("$notesUrl/latest") {
            header("Authorization", "Bearer $tokenA")
        }.andExpect {
            status { isTooManyRequests() }
        }
    }

    @Test
    fun `should throw exception when saving note with title over 300 characters`() {
        val note = mapOf("title" to "Long note", "content" to "a".repeat(300))

        mockMvc.post(notesUrl) {
            header("Authorization", "Bearer $tokenA")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(note)
        }.andExpect {
            status { isOk() }
            jsonPath("$.title").value("a".repeat(3100))
        }
    }
}

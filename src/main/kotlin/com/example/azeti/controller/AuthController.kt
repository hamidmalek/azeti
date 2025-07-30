package com.example.azeti.controller

import com.example.azeti.dto.UserDto
import com.example.azeti.model.User
import com.example.azeti.security.JwtTokenProvider
import com.example.azeti.service.AuthService
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
@Validated
class AuthController(private val authService: AuthService, private val tokenProvider: JwtTokenProvider) {

    @PostMapping("/register")
    fun register(
        @Valid @RequestBody req: RegisterRequest
    ): ResponseEntity<UserResponse> {
        val user: User = authService.register(req.username, req.password)
        return ResponseEntity.ok(UserResponse(UserDto(user.username)))
    }

    @PostMapping("/login")
    fun login(@RequestBody req: LoginRequest): Map<String, String> {
        val user = authService.authenticate(req.username, req.password)
        val token = tokenProvider.generateToken(user)
        return mapOf("token" to token)
    }

    data class RegisterRequest(
        @field:NotBlank val username: String,
        @field:NotBlank val password: String
    )

    data class LoginRequest(
        @field:NotBlank val username: String,
        @field:NotBlank val password: String
    )

    data class UserResponse(val user: UserDto)
}

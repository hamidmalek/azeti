package com.example.azeti.service

import com.example.azeti.model.User
import com.example.azeti.repo.UserRepository
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.*

@Service
@Primary
class AuthService(
    private val repo: UserRepository,
    private val passwordEncoder: PasswordEncoder
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val user = repo.findById(UUID.fromString(username)).orElse(null) ?: throw UsernameNotFoundException("Not found")
        return org.springframework.security.core.userdetails.User(user.id.toString(), user.password, listOf())
    }

    @Throws(ResponseStatusException::class)
    fun register(username: String, password: String): User {
        if (repo.findByUsername(username) != null) throw ResponseStatusException(
            HttpStatus.CONFLICT,
            "User already exists"
        )
        return repo.save(User(username = username, password = passwordEncoder.encode(password)))
    }

    fun authenticate(username: String, password: String): User {
        val user = repo.findByUsername(username) ?: throw BadCredentialsException("Invalid")
        if (!passwordEncoder.matches(password, user.password)) throw BadCredentialsException("Invalid")
        return user
    }

    fun getCurrentUser(): User {
        val id = SecurityContextHolder.getContext().authentication?.name
            ?: throw IllegalStateException("No authenticated user found")
        return repo.findById(UUID.fromString(id)).orElse(null) ?: throw IllegalStateException("User not found")
    }
}
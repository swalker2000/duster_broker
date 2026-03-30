package com.duster.security.auth

import com.duster.database.ClientRepository
import com.duster.security.ClientPasswords
import com.duster.security.JwtService
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/auth")
class AuthController(
    private val clientRepository: ClientRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder
) {

    data class LoginRequest(val deviseId: String = "", val password: String = "")

    data class LoginResponse(
        val accessToken: String,
        val tokenType: String = "Bearer",
        val deviseId: String,
        val role: String
    )

    data class MeResponse(val deviseId: String, val role: String)

    @PostMapping("/login")
    fun login(@RequestBody body: LoginRequest): LoginResponse {
        val deviseId = body.deviseId.trim()
        if (deviseId.isBlank() || body.password.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "deviseId and password are required")
        }
        val client = clientRepository.findByDeviseId(deviseId)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")
        if (!ClientPasswords.matches(body.password, client.password, passwordEncoder)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")
        }
        val token = jwtService.createToken(client.deviseId, client.role)
        return LoginResponse(accessToken = token, deviseId = client.deviseId, role = client.role.name)
    }

    @GetMapping("/me")
    fun me(authentication: Authentication): MeResponse {
        val role = authentication.authorities
            .mapNotNull { it.authority?.removePrefix("ROLE_") }
            .firstOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        val deviseId = authentication.name?.takeIf { it.isNotBlank() }
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        return MeResponse(deviseId = deviseId, role = role)
    }
}

package com.duster.security

import com.duster.database.data.client.Role
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    private val jwtProperties: JwtProperties
) {

    private val key: SecretKey by lazy {
        val bytes = jwtProperties.secret.toByteArray(StandardCharsets.UTF_8)
        Keys.hmacShaKeyFor(bytes)
    }

    fun createToken(deviseId: String, role: Role): String {
        val now = Instant.now()
        val exp = now.plusMillis(jwtProperties.expirationMs)
        return Jwts.builder()
            .subject(deviseId)
            .claim(CLAIM_ROLE, role.name)
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .signWith(key)
            .compact()
    }

    fun parseToken(token: String): JwtUserClaims {
        try {
            val payload = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
            val deviseId = payload.subject ?: throw JwtException("missing subject")
            val role = payload[CLAIM_ROLE] as? String ?: throw JwtException("missing role")
            return JwtUserClaims(deviseId = deviseId, role = role)
        } catch (e: ExpiredJwtException) {
            throw JwtException("token expired", e)
        }
    }

    companion object {
        private const val CLAIM_ROLE = "role"
    }
}

data class JwtUserClaims(val deviseId: String, val role: String)

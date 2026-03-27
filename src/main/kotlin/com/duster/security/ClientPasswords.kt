package com.duster.security

import org.springframework.security.crypto.password.PasswordEncoder

object ClientPasswords {

    fun looksLikeBcrypt(value: String): Boolean =
        value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$")

    fun matches(raw: String, stored: String, passwordEncoder: PasswordEncoder): Boolean =
        if (looksLikeBcrypt(stored)) passwordEncoder.matches(raw, stored) else raw == stored
}

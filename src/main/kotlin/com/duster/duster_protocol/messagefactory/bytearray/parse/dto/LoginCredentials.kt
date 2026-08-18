package com.duster.duster_protocol.messagefactory.bytearray.parse.dto

/**
 * Учётные данные логина устройства (аналог тела REST `POST /auth/login`).
 */
data class LoginCredentials(
    val deviseId: String,
    val password: String
)

package com.duster.duster_protocol.messagefactory.bytearray.parse.dto

import com.duster.database.data.client.Role

/**
 * Ответ брокера на логин (аналог JSON `/auth/login` / 401).
 */
data class LoginResult(
    val ok: Boolean,
    val deviseId: String,
    val role: Role,
    val accessToken: String
)

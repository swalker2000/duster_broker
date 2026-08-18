package com.duster.duster_protocol.transport.client

import com.duster.duster_protocol.messagefactory.bytearray.parse.dto.LoginResult

class LoginFailedException(
    val result: LoginResult
) : RuntimeException("login failed for deviseId=${result.deviseId}")

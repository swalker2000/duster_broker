package com.duster.duster_protocol.messagefactory.bytearray.parse.parser

import com.duster.duster_protocol.messagefactory.bytearray.parse.dto.LoginCredentials
import com.duster.duster_protocol.messagefactory.transport.constant.DbpMessageType

abstract class LoginCredentialsParser(
    override val dbpMessageType: DbpMessageType
) : AbstractParser<LoginCredentials>() {

    /** deviseIdLen[2] + passwordLen[2], строки могут быть пустыми. */
    override val minPayloadSize: Int = 4

    override fun parsePayload(payload: List<Int>): LoginCredentials? {
        val (deviseId, afterId) = readUtf8(payload, 0) ?: return null
        val (password, end) = readUtf8(payload, afterId) ?: return null
        if (end != payload.size) {
            return null
        }
        return LoginCredentials(deviseId = deviseId, password = password)
    }
}

object ConsumerLoginParser : LoginCredentialsParser(DbpMessageType.CONSUMER_LOGIN)

object ProducerLoginParser : LoginCredentialsParser(DbpMessageType.PRODUCER_LOGIN)

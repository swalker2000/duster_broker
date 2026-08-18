package com.duster.duster_protocol.messagefactory.bytearray.parse.parser

import com.duster.database.data.client.Role
import com.duster.duster_protocol.messagefactory.bytearray.parse.dto.LoginResult
import com.duster.duster_protocol.messagefactory.transport.constant.DbpMessageType

abstract class LoginResultParser(
    override val dbpMessageType: DbpMessageType
) : AbstractParser<LoginResult>() {

    /** ok[1] + role[1] + deviseIdLen[2] + tokenLen[2]. */
    override val minPayloadSize: Int = 6

    override fun parsePayload(payload: List<Int>): LoginResult? {
        val okByte = payload[0]
        if (okByte != 0 && okByte != 1) {
            return null
        }
        val roles = Role.entries
        val roleOrdinal = payload[1]
        if (roleOrdinal !in roles.indices) {
            return null
        }
        val (deviseId, afterId) = readUtf8(payload, 2) ?: return null
        val (accessToken, end) = readUtf8(payload, afterId) ?: return null
        if (end != payload.size) {
            return null
        }
        val ok = okByte == 1
        return LoginResult(
            ok = ok,
            deviseId = deviseId,
            role = roles[roleOrdinal],
            accessToken = if (ok) accessToken else ""
        )
    }
}

object BrokerConsumerLoginResultParser : LoginResultParser(DbpMessageType.BROKER_CONSUMER_LOGIN_RESULT)

object BrokerProducerLoginResultParser : LoginResultParser(DbpMessageType.BROKER_PRODUCER_LOGIN_RESULT)

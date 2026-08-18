package com.duster.duster_protocol.messagefactory.bytearray.parse.parser

import com.duster.duster_protocol.messagefactory.transport.constant.DbpMessageType

/**
 * Парсер [DbpMessageType.PRODUCER_ASK_MESSAGE_STATUS].
 * Payload: messageId[8].
 */
object ProducerAskMessageStatusParser : AbstractParser<Long>() {

    override val dbpMessageType: DbpMessageType = DbpMessageType.PRODUCER_ASK_MESSAGE_STATUS
    override val minPayloadSize: Int = 8

    override fun parsePayload(payload: List<Int>): Long? {
        if (payload.size != minPayloadSize) {
            return null
        }
        return getLong(payload, 0)
    }
}

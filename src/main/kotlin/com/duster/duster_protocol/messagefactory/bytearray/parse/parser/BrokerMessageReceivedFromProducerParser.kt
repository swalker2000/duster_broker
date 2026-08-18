package com.duster.duster_protocol.messagefactory.bytearray.parse.parser

import com.duster.database.data.message.DeliveryStatus
import com.duster.duster_protocol.messagefactory.transport.constant.DbpMessageType
import com.duster.transport.data.dto.producer.message.ProducerMessageOutDto

/**
 * Парсер [DbpMessageType.BROKER_MESSAGE_RECEIVED_FROM_PRODUCER].
 * Payload: id[8] + tmpId[8] + deliveryStatus[1].
 */
object BrokerMessageReceivedFromProducerParser : AbstractParser<ProducerMessageOutDto>() {

    override val dbpMessageType: DbpMessageType = DbpMessageType.BROKER_MESSAGE_RECEIVED_FROM_PRODUCER
    override val minPayloadSize: Int = 17

    override fun parsePayload(payload: List<Int>): ProducerMessageOutDto? {
        if (payload.size != minPayloadSize) {
            return null
        }
        val id = getLong(payload, 0)
        val tmpId = getLong(payload, 8)
        val statusOrdinal = payload[16]
        val statuses = DeliveryStatus.entries
        if (statusOrdinal !in statuses.indices) {
            return null
        }
        return ProducerMessageOutDto(id = id, tmpId = tmpId, deliveryStatus = statuses[statusOrdinal])
    }
}

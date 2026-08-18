package com.duster.duster_protocol.messagefactory.bytearray.parse.parser

import com.duster.database.data.message.DeliveryStatus
import com.duster.duster_protocol.messagefactory.transport.constant.DbpMessageType
import com.duster.transport.data.dto.consumer.ConsumerMessageInDto

/**
 * Парсер [DbpMessageType.CONSUMER_MESSAGE_RECEIVED].
 * Payload: id[8] + deliveryStatus[1].
 */
object ConsumerMessageReceivedParser : AbstractParser<ConsumerMessageInDto>() {

    override val dbpMessageType: DbpMessageType = DbpMessageType.CONSUMER_MESSAGE_RECEIVED
    override val minPayloadSize: Int = 9

    override fun parsePayload(payload: List<Int>): ConsumerMessageInDto? {
        if (payload.size != minPayloadSize) {
            return null
        }
        val id = getLong(payload, 0)
        val statusOrdinal = payload[8]
        val statuses = DeliveryStatus.entries
        if (statusOrdinal !in statuses.indices) {
            return null
        }
        val status = statuses[statusOrdinal]
        if (!status.canReceiveFromConsumer) {
            return null
        }
        return ConsumerMessageInDto(id = id).apply {
            deliveryStatus = status
        }
    }
}

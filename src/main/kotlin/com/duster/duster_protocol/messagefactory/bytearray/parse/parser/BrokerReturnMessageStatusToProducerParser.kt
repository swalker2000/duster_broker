package com.duster.duster_protocol.messagefactory.bytearray.parse.parser

import com.duster.database.data.message.DeliveryStatus
import com.duster.duster_protocol.messagefactory.bytearray.parse.dto.ProducerDeliveryStatusFrame
import com.duster.duster_protocol.messagefactory.transport.constant.DbpMessageType

/**
 * Парсер [DbpMessageType.BROKER_RETURN_MESSAGE_STATUS_TO_PRODUCER].
 * Payload: deliveryStatus[1] + messageId[8].
 */
object BrokerReturnMessageStatusToProducerParser : AbstractParser<ProducerDeliveryStatusFrame>() {

    override val dbpMessageType: DbpMessageType = DbpMessageType.BROKER_RETURN_MESSAGE_STATUS_TO_PRODUCER
    override val minPayloadSize: Int = 9

    override fun parsePayload(payload: List<Int>): ProducerDeliveryStatusFrame? {
        if (payload.size != minPayloadSize) {
            return null
        }
        val statusOrdinal = payload[0]
        val statuses = DeliveryStatus.entries
        if (statusOrdinal !in statuses.indices) {
            return null
        }
        return ProducerDeliveryStatusFrame(
            messageId = getLong(payload, 1),
            deliveryStatus = statuses[statusOrdinal]
        )
    }
}

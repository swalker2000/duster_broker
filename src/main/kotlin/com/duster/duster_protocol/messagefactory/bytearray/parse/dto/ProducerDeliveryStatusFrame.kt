package com.duster.duster_protocol.messagefactory.bytearray.parse.dto

import com.duster.database.data.message.DeliveryStatus

/**
 * Статус сообщения продюсера: id + [DeliveryStatus]
 * (кадр `BROKER_RETURN_MESSAGE_STATUS_TO_PRODUCER`).
 */
data class ProducerDeliveryStatusFrame(
    val messageId: Long,
    val deliveryStatus: DeliveryStatus
)

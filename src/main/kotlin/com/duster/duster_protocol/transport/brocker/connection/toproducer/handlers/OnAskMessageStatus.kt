package com.duster.duster_protocol.transport.brocker.connection.toproducer.handlers

import com.duster.transport.data.dto.producer.ProducerDeliveryStatusOutDto

fun interface OnAskMessageStatus {

    fun onAskMessageStatus(messageId: String): ProducerDeliveryStatusOutDto
}
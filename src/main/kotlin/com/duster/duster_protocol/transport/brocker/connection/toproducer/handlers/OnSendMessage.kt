package com.duster.duster_protocol.transport.brocker.connection.toproducer.handlers

import com.duster.transport.data.dto.producer.message.ProducerMessageInDto
import com.duster.transport.data.dto.producer.message.ProducerMessageOutDto

fun interface OnSendMessage {

    fun onSendMessage(deviseId: String, message: ProducerMessageInDto): ProducerMessageOutDto
}

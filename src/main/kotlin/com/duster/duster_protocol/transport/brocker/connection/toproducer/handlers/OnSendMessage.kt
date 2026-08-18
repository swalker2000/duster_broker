package com.duster.duster_protocol.transport.brocker.connection.toproducer.handlers

import com.duster.transport.data.dto.producer.message.ProducerMessageInDto

fun interface OnSendMessage {

    fun onSendMessage(message: ProducerMessageInDto)
}
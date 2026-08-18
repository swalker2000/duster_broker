package com.duster.duster_protocol.transport.brocker.connection.toconsumer.handlers

import com.duster.transport.data.dto.consumer.ConsumerMessageInDto

fun interface OnMessageStatusChanged {

    fun onMessageStatusChanged(message: ConsumerMessageInDto)

}
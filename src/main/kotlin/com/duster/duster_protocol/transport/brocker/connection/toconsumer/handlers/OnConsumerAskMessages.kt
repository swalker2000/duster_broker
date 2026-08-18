package com.duster.duster_protocol.transport.brocker.connection.toconsumer.handlers

import com.duster.transport.data.dto.consumer.ConsumerMessageInDto

fun interface OnConsumerAskMessages {

    /**
     * @return null if no messages (и в этом случае брокер возращает специально подготовленный ответ)
     */
    fun onConsumerAskMessages() : ConsumerMessageInDto?
}
package com.duster.duster_protocol.transport.brocker.connection.toconsumer.handlers

import com.duster.transport.data.dto.consumer.ConsumerMessageOutDto

fun interface OnConsumerAskMessages {

    /**
     * @return null if no messages (и в этом случае брокер возвращает специально подготовленный ответ)
     */
    fun onConsumerAskMessages(deviseId: String): ConsumerMessageOutDto?
}

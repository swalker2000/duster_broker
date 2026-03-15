package com.duster.pd

import com.duster.transport.data.dto.producer.message.ProducerMessageInDto
import com.duster.transport.data.dto.producer.message.ProducerMessageOutDto

interface Producer {

    val deviceId: String

    /**
     * Хендлер описывающий алгоритм действий при смене статуса сообщения.
     */
    fun interface OnMessageStatusChange {
        /**
         * @param producerMessageOutDto содержит id, tmpId и новый статус (NOT_DELIVERED, DELIVERED, COMPLETED и т.д.)
         */
        fun newStatusEvent(producerMessageOutDto: ProducerMessageOutDto)
    }

    /**
     * Опубликовать сообщение на consumer.
     * @param message сообщение для отправки (ProducerMessageInDto)
     * @param consumerDeviseId id устройства куда отправлено сообщение.
     * @param onMessageStatusChange подписаться на смену статуса (если передан messageBirthCertificate в message)
     */
    fun publish(consumerDeviseId : String, message: ProducerMessageInDto, onMessageStatusChange: OnMessageStatusChange? = null)

    fun connect()

    fun disconnect()
}
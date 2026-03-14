package com.duster.pd

import com.duster.messagehandler.data.dto.producer.message.ProducerMessageInDto
import com.duster.messagehandler.data.dto.producer.message.ProducerMessageOutDto

interface Producer {

    val deviseId: String

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
     * @param onMessageStatusChange подписаться на смену статуса (если передан messageBirthCertificate в message)
     */
    fun publish(message: ProducerMessageInDto, onMessageStatusChange: OnMessageStatusChange? = null)

    fun connect()

    fun disconnect()
}
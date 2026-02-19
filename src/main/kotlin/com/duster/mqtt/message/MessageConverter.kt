package com.duster.mqtt.message

import com.duster.database.data.Message
import com.duster.mqtt.message.dto.ConsumerMessageOutDto
import com.duster.mqtt.message.dto.ProducerMessageInDto
import org.springframework.stereotype.Component
import java.util.Date

@Component
class MessageConverter {

    /**
     * Получить сообщение сохраняемое в БД.
     * @param delivered было ли сообщение доставлено до consumer.
     */
    fun getMessage(
        producerMessageInDto: ProducerMessageInDto,
        delivered : Boolean = false,
        deliveredError : Boolean = false,
        createdDate : Date = Date(System.currentTimeMillis()),
        deliveredDate : Date? = null
        ): Message {
        val message = Message()

        // Map fields from DTO to Message
        message.deliveryGuarantee = producerMessageInDto.believerGuarantee
        message.command = producerMessageInDto.command
        message.data = producerMessageInDto.data
        message.delivered = delivered
        message.deliveredError = deliveredError
        message.createdDate = createdDate
        message.deliveredDate = deliveredDate
        return message
    }

    fun fromDto(dto: ConsumerMessageOutDto, topic: String): Message {
        return Message().also {
            it.id = dto.id.toInt()
            it.command = dto.command
            it.deliveryGuarantee = dto.believerGuarantee
            it.data = dto.data
            // остальные поля остаются с значениями по умолчанию:
            it.topic = topic
            // createdDate = текущая дата
            // sendDate = текущая дата
            // delivered = false
            // deliveredError = false
            // timePolicy = TimePolicy.CURRENT
        }
    }

    fun getConsumerMessageOutDto(message: Message): ConsumerMessageOutDto {
        return ConsumerMessageOutDto(
        ).apply {
            id = message.id.toLong()
            currentTimestamp = System.currentTimeMillis()
            command = message.command
            believerGuarantee = message.deliveryGuarantee
            data = message.data
        }
    }
}
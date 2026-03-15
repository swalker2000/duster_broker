package com.duster.transport.data

import com.duster.database.data.DeliveryGuarantee
import com.duster.database.data.DeliveryStatus
import com.duster.database.data.Message
import com.duster.transport.data.dto.consumer.ConsumerMessageOutDto
import com.duster.transport.data.dto.producer.ProducerDeliveryStatusOutDto
import com.duster.transport.data.dto.producer.message.ProducerMessageInDto
import com.duster.transport.data.dto.producer.message.ProducerMessageOutDto
import org.springframework.stereotype.Component
import java.util.Date

@Component
class MessageConverter {

    /**
     * Получить сообщение сохраняемое в БД.
     */
    fun getMessage(
        producerMessageInDto: ProducerMessageInDto,
        deviseId: String,
        deliveredError : Boolean = false,
        createdDate : Date = Date(System.currentTimeMillis()),
        deliveredDate : Date? = null
        ): Message {
        val message = Message()

        message.deliveryStatus =  when(producerMessageInDto.believerGuarantee)
        {
            DeliveryGuarantee.NO->{
                DeliveryStatus.UNKNOWN
            }
            else -> {
                DeliveryStatus.NOT_DELIVERED
            }
        }
        message.producerDeviseId = producerMessageInDto.messageBirthCertificate?.producerDeviseId
        message.tmpId = producerMessageInDto.messageBirthCertificate?.tmpId

        message.deliveryGuarantee = producerMessageInDto.believerGuarantee
        message.command = producerMessageInDto.command
        message.data = producerMessageInDto.data
        message.deliveredError = deliveredError
        message.createdDate = createdDate
        message.deliveredDate = deliveredDate
        message.deviseId = deviseId
        return message
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

    fun getProducerMessageOutDto(message: Message): ProducerMessageOutDto {
        return ProducerMessageOutDto(message.id, message.tmpId, message.deliveryStatus)
    }

    fun getProducerDeliveryStatusOutDto(message: Message): ProducerDeliveryStatusOutDto {
        return ProducerDeliveryStatusOutDto(message.deliveryStatus)
    }
}
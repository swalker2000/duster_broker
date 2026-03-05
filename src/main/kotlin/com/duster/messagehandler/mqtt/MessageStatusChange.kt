package com.duster.messagehandler.mqtt

import com.duster.database.MainRepository
import com.duster.database.data.DeliveryStatus
import com.duster.database.data.Message
import com.duster.messagehandler.data.MessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Date

/**
 * Отвечает за смену статуса сообщения.
 *  - смена статуса в БД
 *  - уведомление producer
 */
@Service
class MessageStatusChange {

    @Autowired
    private lateinit var messageConverter : MessageConverter

    @Autowired
    private lateinit var mainRepository: MainRepository

    @Autowired
    private lateinit var producerMessagePublisher: com.duster.messagehandler.mqtt.publisher.ProducerMessagePublisher

    /**
     * Обновить статус доставки сообщения.
     * :TODO отправка статуса и изменение значения в БД должны быть паралельны.
     *  - обновляет статус в базе данных
     *  - сообщает producer о смене статуса
     */
    fun updateDeliveryStatus(
        id: Int,
        deliveryStatus: DeliveryStatus,
        deliveredDate: Date
    )
    {
        val message =mainRepository.updateDeliveryStatus(id, deliveryStatus,  deliveredDate)
        sendDeliveryStatusToProducerIfRequired(message)
    }

    /**
     * Обновить статус доставки сообщения если это требуется исходя из данных записанных в сообщение.
     *  - сообщает producer о смене статуса
     */
    fun sendDeliveryStatusToProducerIfRequired(
        message: Message,
    )
    {
        if(message.isProducerSubscribed()) {
            val getProducerMessageOutDto = messageConverter.getProducerMessageOutDto(message)
            producerMessagePublisher.publishMessageToProducer(getProducerMessageOutDto, message.producerDeviseId!!)
        }
    }
}
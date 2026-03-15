package com.duster.common

import com.duster.common.messagepublishinterface.ConsumerMessagePublishAction
import com.duster.common.messagepublishinterface.ProducerMessagePublishAction
import com.duster.database.MainRepository
import com.duster.database.data.DeliveryStatus
import com.duster.database.data.Message
import com.duster.transport.data.MessageConverter
import com.duster.transport.data.dto.producer.message.ProducerMessageOutDto
import com.duster.transport.mqtt.publisher.ProducerMessagePublisher
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Date

/**
 * Отвечает за смену статуса сообщения.
 *  - смена статуса в БД
 *  - уведомление producer
 */
@Service
class MessageStatusChangeHandler {

    private val logger = LoggerFactory.getLogger(MessageStatusChangeHandler::class.java)

    @Autowired
    private lateinit var messageConverter : MessageConverter

    @Autowired
    private lateinit var mainRepository: MainRepository

    private val producerMessagePublishActionList = mutableListOf<ProducerMessagePublishAction>()

    val subscribe = Subscribe()

    /**
     * Здесь подписываемся на события класса.
     */
    inner class Subscribe {

        fun addProducerMessagePublishAction(producerMessagePublishAction: ProducerMessagePublishAction) {
            producerMessagePublishActionList.add(producerMessagePublishAction)
        }
    }

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
     * Обновить статус доставки сообщения, если это требуется исходя из данных записанных в сообщение.
     *  - сообщает producer о смене статуса
     */
    fun sendDeliveryStatusToProducerIfRequired(
        message: Message,
    )
    {
        if(message.isProducerSubscribed()) {
            val getProducerMessageOutDto = messageConverter.getProducerMessageOutDto(message)
            publishMessageToProducerAction(message.producerDeviseId!!, getProducerMessageOutDto)
            //producerMessagePublisher.publishMessageToProducer(getProducerMessageOutDto, message.producerDeviseId!!)
        }
    }



    private fun publishMessageToProducerAction(deviseId: String, producerMessagePublishAction: ProducerMessageOutDto)
    {
        producerMessagePublishActionList.forEach {
            try {
                it.publishAction(deviseId, producerMessagePublishAction)
            }
            catch (e : Exception) {
                logger.error("Error in publishAction: ${e.stackTraceToString()}")
            }
        }
    }
}
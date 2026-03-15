package com.duster.common

import com.duster.common.messagepublishinterface.ConsumerMessagePublishAction
import com.duster.common.messagepublishinterface.ProducerMessagePublishAction
import com.duster.transport.data.dto.consumer.ConsumerMessageOutDto
import com.duster.transport.data.dto.producer.message.ProducerMessageOutDto
import org.slf4j.LoggerFactory

/**
 * Класс отвечает за публикацию сообщений.
 *  - способы публикации добавляются извне
 *  - способов публикации может быть несколько
 *  - при вызове методов publishMessageToProducerAction, publishMessageToConsumerAction сообщения публикуются всеми
 *     известными объекту способами
 */
object CommonPublisher {
    private val logger = LoggerFactory.getLogger(CommonPublisher::class.java)

    private val producerMessagePublishActionList = mutableListOf<ProducerMessagePublishAction>()
    private val consumerMessagePublishActionList = mutableListOf<ConsumerMessagePublishAction>()

    fun addConsumerMessagePublishAction(consumerMessagePublishAction: ConsumerMessagePublishAction) {
        consumerMessagePublishActionList.add(consumerMessagePublishAction)
    }
    fun addProducerMessagePublishAction(producerMessagePublishAction: ProducerMessagePublishAction) {
        producerMessagePublishActionList.add(producerMessagePublishAction)
    }

    fun publishMessageToProducer(deviseId: String, producerMessagePublishAction: ProducerMessageOutDto)
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

    fun publishMessageToConsumer(deviseId: String, consumerMessageOutDto: ConsumerMessageOutDto)
    {
        consumerMessagePublishActionList.forEach {
            try {
                it.publishAction(deviseId, consumerMessageOutDto)
            }
            catch (e : Exception) {
                logger.error("Error in publishAction: ${e.stackTraceToString()}")
            }
        }
    }
}
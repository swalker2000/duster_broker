package com.duster.mqtt

import com.duster.mqtt.message.dto.ConsumerMessageOutDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component

@Component
class ConsumerMessagePublisher(
    @Qualifier("outputChannel") private  val outputChannel: MessageChannel
) {

    private val logger = LoggerFactory.getLogger(ConsumerMessagePublisher::class.java)


    /**
     * Отправить сообщение подписчику.
     * @param consumerMessageOutDto сообщение
     * @param deviseId id устройства кому отправляем
     */
    fun publishMessageToConsumer(consumerMessageOutDto : ConsumerMessageOutDto, deviseId : String) {
        val outputTopic = "${MessageSource.CONSUMER.prefixInTopic}/request/${deviseId}"
        val consumerMessageOut = MessageBuilder.withPayload(consumerMessageOutDto)
            //.copyHeaders(headers)
            .setHeader("mqtt_topic", outputTopic)
            .build()
        logger.info("TD_CONSUMER [$deviseId] : $consumerMessageOut")
        outputChannel.send(consumerMessageOut)
    }
}
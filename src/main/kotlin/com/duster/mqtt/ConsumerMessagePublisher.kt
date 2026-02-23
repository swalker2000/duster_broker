package com.duster.mqtt

import com.duster.mqtt.message.dto.ConsumerMessageOutDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class ConsumerMessagePublisher(
    @Qualifier("outputChannel") private  val outputChannel: MessageChannel
) {

    private val logger = LoggerFactory.getLogger(ConsumerMessagePublisher::class.java)

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    /**
     * Отправить сообщение подписчику.
     * @param consumerMessageOutDto сообщение
     * @param deviseId id устройства кому отправляем
     */
    fun publishMessageToConsumer(consumerMessageOutDto : ConsumerMessageOutDto, deviseId : String) {
        val outputTopic = "${MessageSource.CONSUMER.prefixInTopic}/request/${deviseId}"
        val consumerMessageOutLine : String = objectMapper.writeValueAsString(consumerMessageOutDto)
        val consumerMessageOut = MessageBuilder.withPayload(consumerMessageOutLine)
            //.copyHeaders(headers)
            .setHeader("mqtt_topic", outputTopic)
            .build()

        logger.info("TD_CONSUMER [$deviseId] : $consumerMessageOutLine")
        outputChannel.send(consumerMessageOut)
    }
}
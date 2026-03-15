package com.duster.transport.mqtt.publisher

import com.duster.transport.data.dto.producer.message.ProducerMessageOutDto
import com.duster.transport.mqtt.MessageSource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class ProducerMessagePublisher(
    @Qualifier("outputChannel") private  val outputChannel: MessageChannel
) {

    private val logger = LoggerFactory.getLogger(ProducerMessagePublisher::class.java)

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    /**
     * Отправить информацию producer о статусе его сообщения.
     * @param producerMessageOutDto сообщение
     * @param producerDeviceId Id producer отправившего сообщение. Если у producer есть deviseId, то использовать нужно его.
     */
    fun publishMessageToProducer(producerMessageOutDto : ProducerMessageOutDto, producerDeviceId : String) {
        val outputTopic = "${MessageSource.PRODUCER.prefixInTopic}/response/${producerDeviceId}"
        val consumerMessageOutLine : String = objectMapper.writeValueAsString(producerMessageOutDto)
        val consumerMessageOut = MessageBuilder.withPayload(consumerMessageOutLine)
            //.copyHeaders(headers)
            .setHeader("mqtt_topic", outputTopic)
            .build()

        logger.info("TD_PRODUCER [$producerDeviceId] : $consumerMessageOutLine")
        outputChannel.send(consumerMessageOut)
    }
}
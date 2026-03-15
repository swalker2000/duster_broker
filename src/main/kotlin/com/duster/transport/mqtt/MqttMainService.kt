package com.duster.transport.mqtt

import com.duster.common.CommonMessageService
import com.duster.transport.data.dto.consumer.ConsumerMessageInDto
import com.duster.transport.data.dto.producer.message.ProducerMessageInDto
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHeaders
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

/*
'producer/request

*/

/**
 * Обработчик входящих сообщений по mqtt.
 *  - получает по MQTT команду на защищенную передачу сообщения в топике 'producer/request/{deviceId}'.
 *     Сообщение имеет тип JSON формата ProducerMessageInDto
 *  - Преобразует сообщение в формат Message
 *  - Сохраняет полученное сообщение в БД флаг delivered false, получает id сообщения сохраненного в БД
 *  - Из Message получает ConsumerMessageOutDto
 *  - ConsumerMessageOutDto передает дальше в топике 'consumer/request/{deviceId}'
 *  - Если enum DeliveryGuarantee не NO ожидаем ConsumerMessageInDto в топике 'consumer/response/{deviceId}'
 *  - После получения ответа меняем флаг delivered на true
 *
 * Возврат статуса сообщения producer:
 *  - producer в сообщение ProducerMessageInDto выставляет:
 *    - tmpId (временный id сообщения) не равный 0.  При 0, отсутсвии поля и null считается, что мы не ждем ответа.
 *    - producerDeviceId : id producer другими словами id устройства (deviceId) которое сгенерировало сообщение.
 *    (подробнее смотрите классы Message и ProducerMessageInDto)
 *    - producer получает сообщение в топике producer/response/{producerDeviceId}.
 *    Сообщение имеет формат ProducerMessageOutDto. В котором сообщению присваивается постоянный id. Так же присутсвует
 *    временный tmpId созданный producer
 */
@Service
class MqttMainService() {

    private val logger = LoggerFactory.getLogger(MqttMainService::class.java)

    @Autowired
    private lateinit var consumerMessagePublisher: com.duster.transport.mqtt.publisher.ConsumerMessagePublisher

    @Autowired
    private lateinit var commonMessageService: CommonMessageService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @PostConstruct
    private fun postConstruct() {
        //:todo подумать о реактивном стеке и переносе этого в consumerMessagePublisher
        //информируем commonMessageService о том как отправлять сообщения по MQTT
        commonMessageService.subscribe.addConsumerMessagePublishAction{deviseId, consumerMessageOutDto ->
            consumerMessagePublisher.publishMessageToConsumer(consumerMessageOutDto, deviseId)
        }
    }

    @ServiceActivator(inputChannel = "inputChannel")
    fun handleMessage(message: Message<String>) {
        logger.info("New message received:")

        val topic = message.headers["mqtt_receivedTopic"] as String // Входной топик
        val deviseId = getDeviceIdFromTopic(topic)
        val messageSource : MessageSource? = getMessageSourceFromTopic(topic)
        val headers : MessageHeaders = message.headers
        //logger.trace("RD [$topic] : $messageIn")

        when (messageSource) {
            MessageSource.PRODUCER -> {
                handlerProducerMessage(message, deviseId, headers)
            }
            MessageSource.CONSUMER -> {
                handlerConsumerMessage(message, deviseId, headers)
            }
            else -> {
                logger.error("Unknown message source [$topic]")
            }
        }
    }


    private fun handlerProducerMessage(message: Message<String>, deviseId : String, headers : MessageHeaders)
    {
        try {
            val producerMessageIn: ProducerMessageInDto = objectMapper.readValue(
                message.payload,
                ProducerMessageInDto::class.java
            )
            logger.info("RD_PRODUCER [$deviseId] : ${message.payload}")
            commonMessageService.newProducerMessageIn(producerMessageIn, deviseId)
        }
        catch (e : Exception) {
            logger.error(e.stackTraceToString())
        }
    }

    private fun handlerConsumerMessage(message: Message<String>, deviseId : String, headers : MessageHeaders)
    {
        val consumerMessageInDto = objectMapper.readValue(message.payload, ConsumerMessageInDto::class.java)
        logger.info("RD_CONSUMER [$deviseId] : $message.payload")
        commonMessageService.newConsumerMessageIn(consumerMessageInDto)

    }

    private fun getMessageSourceFromTopic(topic: String): MessageSource? {
        val prefix = topic.split('/').first()
        return MessageSource.entries.firstOrNull{it.prefixInTopic == prefix}
    }

    private fun getDeviceIdFromTopic(topic: String): String {
        return topic.split('/').last()
    }
}


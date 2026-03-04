package com.duster.mqtt

import com.duster.database.MainRepository
import com.duster.database.data.DeliveryGuarantee
import com.duster.database.data.DeliveryStatus
import com.duster.mqtt.cash.MessageSendTimeCash
import com.duster.mqtt.message.MessageConverter
import com.duster.mqtt.message.dto.consumer.ConsumerMessageInDto
import com.duster.mqtt.message.dto.consumer.ConsumerMessageOutDto
import com.duster.mqtt.message.dto.producer.ProducerMessageInDto
import com.duster.mqtt.publisher.ConsumerMessagePublisher
import com.duster.mqtt.publisher.ProducerMessagePublisher
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageHeaders
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.util.Date

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
 * TODO: Возврат статуса сообщения producer:
 *  - producer в сообщение ProducerMessageInDto выставляет:
 *    - tmpId (временный id сообщения) не равный 0.  При 0, отсутсвии поля и null считается, что мы не ждем ответа.
 *    - producerDeviceId : id producer другими словами id устройства (deviceId) которое сгенерировало сообщение.
 *    (подробнее смотрите классы Message и ProducerMessageInDto)
 *    - producer получает сообщение в топике producer/response/{producerDeviceId}.
 *    Сообщение имеет формат ProducerMessageOutDto. В котором сообщению присваивается постоянный id. Так же присутсвует
 *    временный tmpId созданный producer
 */
@Service
class MqttMessageHandler() {



    private val logger = LoggerFactory.getLogger(MqttMessageHandler::class.java)

    /**
     * Периодичность, с которой мы отсылаем сообщения на 1 устройство.
     * (если слать слишком часто устройство может подвиснуть)
     */
    @Value("\${common.sendMessagePeriod}")
    private  var sendMessagePeriod: Long = -1L

    @Value(("\${common.consumerTimeout}"))
    private var consumerTimeout : Long = -1

    @Autowired
    private lateinit var messageConverter : MessageConverter

    @Autowired
    private lateinit var mainRepository: MainRepository

    @Autowired
    private lateinit var consumerMessagePublisher: ConsumerMessagePublisher

    @Autowired
    private lateinit var messageSendTimeCash: MessageSendTimeCash

    @Autowired
    private lateinit var messageStatusChange: MessageStatusChange

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Qualifier("outputChannel")
    private lateinit var outputChannel: MessageChannel


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
            val producerMessageIn : ProducerMessageInDto = objectMapper.readValue(
                message.payload,
                ProducerMessageInDto::class.java
            )
            logger.info("RD_PRODUCER [$deviseId] : ${message.payload}")

            var message = messageConverter.getMessage(producerMessageIn, deviseId)
            val existsNotDeliveredMessages = mainRepository.existsByDeviseIdAndDeliveredFalse(deviseId)
            //WARN : если сообщение имеет DeliveryGuarantee ONLY_LAST, всем сообщения с данной командой для данного устройства
            //отправленные до этого будет проставлен статус доставки CANCELLED
            message = mainRepository.saveNewMessage(message)//нам очень важен id присваиваемый БД
            messageStatusChange.sendDeliveryStatusToProducerIfRequired(message)//:TODO сделать паралельно остальной обработке
            val consumerMessageOutDto: ConsumerMessageOutDto = messageConverter.getConsumerMessageOutDto(message)
            //Проверяем можно ли данному устройству отправить сейчас сообщение исходя из времени последней отправки?
            //Защита от ddos.
            val messageSendTimeCashAvailable =
                messageSendTimeCash.updateForDeviseIfAvailable(deviseId, sendMessagePeriod)
            if (messageSendTimeCashAvailable && !existsNotDeliveredMessages) {
                consumerMessagePublisher.publishMessageToConsumer(consumerMessageOutDto, deviseId)
                //если гарантия доставки отсутсвует, проставляем метку, что сообщение доставлено
                if (message.deliveryGuarantee == DeliveryGuarantee.NO)
                    mainRepository.updateDeliveryStatus(message.id, DeliveryStatus.UNKNOWN, Date(System.currentTimeMillis()))
            } else {
                logger.warn("Can`t send message to [$deviseId] immediately.")
                if (!messageSendTimeCashAvailable)
                    logger.warn("   - messageSendTimeCashAvailable is false")
                if (existsNotDeliveredMessages)
                    logger.warn("   - existsNotDeliveredMessages is true")
            }
        }
        catch (e : Exception) {
            logger.error(e.stackTraceToString())
        }
    }

    private fun handlerConsumerMessage(message: Message<String>, deviseId : String, headers : MessageHeaders)
    {
        val consumerMessageInDto = objectMapper.readValue(
            message.payload,
            ConsumerMessageInDto::class.java
        )
        logger.info("RD_CONSUMER [$deviseId] : $message.payload")
        messageStatusChange.updateDeliveryStatus(consumerMessageInDto.id, DeliveryStatus.DELIVERED,  Date(System.currentTimeMillis()))
    }

    private fun getMessageSourceFromTopic(topic: String): MessageSource? {
        val prefix = topic.split('/').first()
        return MessageSource.entries.firstOrNull{it.prefixInTopic == prefix}
    }

    private fun getDeviceIdFromTopic(topic: String): String {
        return topic.split('/').last()
    }
}


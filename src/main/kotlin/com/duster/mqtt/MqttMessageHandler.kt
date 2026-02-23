package com.duster.mqtt

import com.duster.database.MainRepository
import com.duster.database.data.DeliveryGuarantee
import com.duster.mqtt.cash.MessageSendTimeCash
import com.duster.mqtt.message.MessageConverter
import com.duster.mqtt.message.dto.ConsumerMessageInDto
import com.duster.mqtt.message.dto.ConsumerMessageOutDto
import com.duster.mqtt.message.dto.ProducerMessageInDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageHeaders
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.util.Date

/**
 * Обработчик входящих сообщений по mqtt.
 *  - получает по MQTT команду на защищенную передачу сообщения в топике 'producer/request/{deviceId}'.
 *     Сообщение имеет тип JSON формата CommandMessageInDto
 *  - Преобразует сообщение в формат ProtectedMessage
 *  - Сохраняет полученное сообщение в БД флаг delivered false, получает id сообщения сохраненного в БД
 *  - Из ProtectedMessage получает MessageOutDto
 *  - MessageOutDto передает дальше в топике 'consumer/request/{deviceId}'
 *  - Если enum DeliveryGuarantee не NO ожидаем MessageInDto в топике 'consumer/response/{deviceId}'
 *  - После получения ответа меняем флаг delivered на true
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
    private lateinit var objectMapper: ObjectMapper

    @Qualifier("outputChannel")
    private lateinit var outputChannel: MessageChannel
    

    //@PostConstruct
    fun run()
    {
        Thread{
            Thread.sleep(10_000)
        consumerMessagePublisher.publishMessageToConsumer(ConsumerMessageOutDto(), "deviseId")
        }.start()
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
                val messageIn : ProducerMessageInDto = objectMapper.readValue(
                    message.payload,
                    ProducerMessageInDto::class.java
                )
                handlerProducerMessage(messageIn, deviseId, headers)
            }
            MessageSource.CONSUMER -> {
                val messageIn = objectMapper.readValue(
                    message.payload,
                    ConsumerMessageInDto::class.java
                )
                handlerConsumerMessage(messageIn, deviseId, headers)
            }
            else -> {
                logger.error("Unknown message source [$topic]")
            }
        }
    }


    private fun handlerProducerMessage(producerMessageIn : ProducerMessageInDto, deviseId : String, headers : MessageHeaders)
    {
        try {
            logger.info("RD_PRODUCER [$deviseId] : $producerMessageIn")
            var message = messageConverter.getMessage(producerMessageIn, deviseId)
            val existsNotDeliveredMessages = mainRepository.existsByDeviseIdAndDeliveredFalse(deviseId)
            message = mainRepository.saveMessage(message)//нам очень важен id присваиваемый БД
            val consumerMessageOutDto: ConsumerMessageOutDto = messageConverter.getConsumerMessageOutDto(message)
            //Проверяем можно ли данному устройству отправить сейчас сообщение исходя из времени последней отправки?
            //Защита от ddos.
            val messageSendTimeCashAvailable =
                messageSendTimeCash.updateForDeviseIfAvailable(deviseId, sendMessagePeriod)
            if (messageSendTimeCashAvailable && !existsNotDeliveredMessages) {
                consumerMessagePublisher.publishMessageToConsumer(consumerMessageOutDto, deviseId)
                //если гарантия доставки отсутсвует, проставляем метку, что сообщение доставлено
                if (message.deliveryGuarantee == DeliveryGuarantee.NO)
                    mainRepository.updateDeliveryStatus(message.id, true, Date(System.currentTimeMillis()))
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

    private fun handlerConsumerMessage(consumerMessageInDto : ConsumerMessageInDto, deviseId : String, headers : MessageHeaders)
    {
        logger.info("RD_CONSUMER [$deviseId] : $consumerMessageInDto")
        mainRepository.updateDeliveryStatus(consumerMessageInDto.id, true,  Date(System.currentTimeMillis()))
    }

    private fun getMessageSourceFromTopic(topic: String): MessageSource? {
        val prefix = topic.split('/').first()
        return MessageSource.entries.firstOrNull{it.prefixInTopic == prefix}
    }

    private fun getDeviceIdFromTopic(topic: String): String {
        return topic.split('/').last()
    }
}


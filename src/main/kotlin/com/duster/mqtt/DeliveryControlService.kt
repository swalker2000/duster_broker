package com.duster.mqtt

import com.duster.database.MainRepository
import com.duster.database.data.Message

import com.duster.mqtt.message.MessageConverter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.support.MessageBuilder
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono.delay
import java.time.Duration
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds

/**
 * Раз в checkNotDeliveredTimeout миллисекунд проверяет сообщения в БД на которые не был получен ответ в течении timeout и
 * производит переотправку.
 */
@Service
class DeliveryControlService {

    private val logger = LoggerFactory.getLogger(DeliveryControlService::class.java)

    /**
     * Считаем, что если за этот период времени нам не ответил наш клиент, значит сообщение потерялось.
     */
    @Value("\${common.mqttWaitResponseTimeout}")
    private  var mqttWaitResponseTimeout = -1L

    /**
     * Периодичность с которой мы отсылаем сообщения на 1 устройство.
     * (если слать слишком часто устройство может подвиснуть)
     */
    @Value("\${common.sendMessagePeriod}")
    private  var sendMessagePeriod: Long = -1L

    @Autowired
    private lateinit var messageConverter : MessageConverter

    @Autowired
    private lateinit var mainRepository: MainRepository

    @Autowired
    private lateinit var consumerMessagePublisher: ConsumerMessagePublisher


    // Запускается каждую минуту (фиксированная задержка между окончанием предыдущего и началом следующего выполнения)
    @Scheduled(fixedRateString = "\${common.checkNotDeliveredTimeout}") // 60000 мс = 1 минута
    fun checkAndSend() {
        val searchBefore = Date(System.currentTimeMillis()-mqttWaitResponseTimeout)
        val messageList = mainRepository.findNotDeliveredMessages(searchBefore)
        val groupedMessages = messageList
            .groupBy { message-> message.deviseId }
            .map{(deviseId, messages)-> messages.sortedBy { it.createdDate }}
        runBlocking {
            for (group in groupedMessages) {
                launch {
                    publishMessagePacket(group)
                }
            }
        }//runBlocking
    }

    private suspend fun publishMessagePacket(messageList: List<Message>) {
        for (message in messageList) {
            val consumerMessageOutDto = messageConverter.getConsumerMessageOutDto(message)
            consumerMessagePublisher.publishMessageToConsumer(consumerMessageOutDto, message.deviseId)
            delay(sendMessagePeriod.milliseconds)
        }
    }


}
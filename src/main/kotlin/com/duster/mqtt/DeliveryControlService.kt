package com.duster.mqtt

import com.duster.database.MainRepository
import com.duster.database.data.Message
import com.duster.mqtt.cash.MessageSendTimeCash

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

    @Autowired
    private lateinit var messageSendTimeCash: MessageSendTimeCash


    /**
     * Находит в базе все не доставленные сообщения и пытается их доставить.
     * (так же проставляет флаг ошибки отправки)
     */
    @Scheduled(fixedDelayString = "\${common.checkNotDeliveredTimeout}")
    fun checkAndSend() {
        logger.info("Run checkAndSend method:")
        val searchBefore = Date(System.currentTimeMillis()-mqttWaitResponseTimeout)
        //ищем все сообщения, на которые не дождались ответа
        val messageList = mainRepository.findNotDeliveredMessages(searchBefore)
        //группируем по id устройства
        val groupedMessages :  List<List<Message>> = messageList
            .groupBy { message-> message.deviseId }
            .map{(deviseId, messages)-> messages.sortedBy { it.createdDate }}
        logger.info("    - found ${messageList.size} not delivered messages for ${groupedMessages.size} devises")
        runBlocking {
            //проставляем флаг ошибка отправки на все найденные сообщения
            launch {
                messageList
                    .filter { message -> message.deliveredError }
                    .forEach { message -> mainRepository.updateDeliveryError(message.id, true)}
            }
            //производим доотправку
            for (group : List<Message> in groupedMessages) {
                //для каждого id все сообщения собраны в list group
                launch {
                    //Отправляем все сообщения на устройство с выбранным id
                    // (в group все сообщения адресованы одному устройству).
                    //Нельзя вывалить все сообщения разом на одно устройство оно умрет, поэтому мы отсылаем их через
                    //временной интервал (см. метод publishMessagePacket).
                    //Чтобы процесс шел быстрее, отправка сообщений на разные устройства происходит паралельно.
                    publishMessagePacket(group)
                }
            }
        }//runBlocking
        logger.info("CheckAndSend method stop.")
    }

    /**
     * Отправить группу сообщений.
     *  - предполагается, что все сообщения отправляются на одно устройство
     *  - в избежания перегрузки устройства все сообщения отправляются через временной интервал.
     */
    private suspend fun publishMessagePacket(messageList: List<Message>) {
        for (message in messageList) {
            val consumerMessageOutDto = messageConverter.getConsumerMessageOutDto(message)
            //:TODO убрать костыльную блокировку умножением времени на 2
            //:TODO подумать нужна ли здесь блокировка
            messageSendTimeCash.updateForDevise(message.deviseId, sendMessagePeriod*2)
            consumerMessagePublisher.publishMessageToConsumer(consumerMessageOutDto, message.deviseId)
            delay(sendMessagePeriod.milliseconds)
            val isDeliveredOptional  = mainRepository.findDeliveredById(message.id)
            //если такого сообщения в базе нет, то это какая, то злая ошибка, пишем в лог
            if(!isDeliveredOptional.isPresent) {
                logger.error("    - message ${message.id} is for devise '${message.deviseId}' is not found")
            }
            //проверяем доставили ли сообщение
            else if(!mainRepository.findDeliveredById(message.id).get())
            {
                //если не доставили прерываем доставку сообщений для данного устройства
                logger.warn("    - message '${message.id}' is for devise '${message.deviseId}' not delivered. Break deliver for this devise.")
                return
            }
        }
    }
}
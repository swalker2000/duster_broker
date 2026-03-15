package com.duster.common

import com.duster.common.messagepublishinterface.ConsumerMessagePublishAction
import com.duster.common.messagepublishinterface.ProducerMessagePublishAction
import com.duster.database.MainRepository
import com.duster.database.data.DeliveryGuarantee
import com.duster.database.data.DeliveryStatus
import com.duster.transport.data.MessageConverter
import com.duster.transport.data.dto.consumer.ConsumerMessageInDto
import com.duster.transport.data.dto.consumer.ConsumerMessageOutDto
import com.duster.transport.data.dto.producer.ProducerDeliveryStatusOutDto
import com.duster.transport.data.dto.producer.message.ProducerMessageInDto
import com.duster.transport.data.dto.producer.message.ProducerMessageOutDto
import com.duster.transport.mqtt.cash.MessageSendTimeCash
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import java.util.Optional

/**
 * Общий сервис по работе с сообщениями без привязки к протоколу по которому мы их (сообщения) получили.
 */
@Service
class CommonMessageService {

    class CommonMessageServiceException(message: String) : RuntimeException(message)

    private val logger = LoggerFactory.getLogger(CommonMessageService::class.java)

    @Autowired
    private lateinit var mainRepository: MainRepository

    @Autowired
    private lateinit var messageSendTimeCash: MessageSendTimeCash

    //@Autowired
    //private lateinit var consumerMessagePublisher: com.duster.messagehandler.mqtt.publisher.ConsumerMessagePublisher

    @Autowired
    private lateinit var messageConverter : MessageConverter

    @Autowired
    private lateinit var messageStatusChangeHandler: MessageStatusChangeHandler


    /**
     * Периодичность, с которой мы отсылаем сообщения на 1 устройство.
     * (если слать слишком часто устройство может подвиснуть)
     */
    @Value("\${common.sendMessagePeriod}")
    private  var sendMessagePeriod: Long = -1L

    private val consumerMessagePublishActionList = mutableListOf<ConsumerMessagePublishAction>()


    /**
     * Здесь подписываемся на события класса.
     */
    inner class Subscribe {

        fun addConsumerMessagePublishAction(consumerMessagePublishAction: ConsumerMessagePublishAction) {
            consumerMessagePublishActionList.add(consumerMessagePublishAction)
        }
        fun addProducerMessagePublishAction(producerMessagePublishAction: ProducerMessagePublishAction) {
            messageStatusChangeHandler.subscribe.addProducerMessagePublishAction(producerMessagePublishAction)
        }
    }

    /**
     * Здесь подписываемся на события класса.
     */
    val subscribe = Subscribe()




    /**
     * Обработчик события создания нового сообщения от producer к consumer
     * @return сообщение, содержащее id присвоенное сообщению
     */
    fun newProducerMessageIn(
        producerMessageIn : ProducerMessageInDto,
        deviseId : String
    ) : Optional<ProducerMessageOutDto>
    {
        try{
        var message = messageConverter.getMessage(producerMessageIn, deviseId)
        val existsNotDeliveredMessages = mainRepository.existsByDeviseIdAndDeliveredFalse(deviseId)
        //WARN : если сообщение имеет DeliveryGuarantee ONLY_LAST, всем сообщения с данной командой для данного устройства
        //отправленные до этого будет проставлен статус доставки CANCELLED
        message = mainRepository.saveNewMessage(message)//нам очень важен id присваиваемый БД
        messageStatusChangeHandler.sendDeliveryStatusToProducerIfRequired(message)//:TODO сделать паралельно остальной обработке
        val consumerMessageOutDto: ConsumerMessageOutDto = messageConverter.getConsumerMessageOutDto(message)
        //Проверяем можно ли данному устройству отправить сейчас сообщение исходя из времени последней отправки?
        //Защита от ddos.
        val messageSendTimeCashAvailable =
            messageSendTimeCash.updateForDeviseIfAvailable(deviseId, sendMessagePeriod)
        if (messageSendTimeCashAvailable && !existsNotDeliveredMessages) {
            publishMessageToConsumerAction(deviseId, consumerMessageOutDto)
            //если гарантия доставки отсутсвует, проставляем метку, что сообщение доставлено
            if (message.deliveryGuarantee == DeliveryGuarantee.NO)
                mainRepository.updateDeliveryStatus(message.id, DeliveryStatus.UNKNOWN,
                    Date(System.currentTimeMillis())
                )
        } else {
            logger.warn("Can`t send message to [$deviseId] immediately.")
            if (!messageSendTimeCashAvailable)
                logger.warn("   - messageSendTimeCashAvailable is false")
            if (existsNotDeliveredMessages)
                logger.warn("   - existsNotDeliveredMessages is true")
        }
            return Optional.of(messageConverter.getProducerMessageOutDto(message))
    }
    catch (e : Exception) {
        logger.error(e.stackTraceToString())
        return Optional.empty()
    }
    }

    fun newConsumerMessageIn(consumerMessageInDto : ConsumerMessageInDto)
    {
        if (consumerMessageInDto.deliveryStatus!!.canReceiveFromConsumer) {
            messageStatusChangeHandler.updateDeliveryStatus(
                consumerMessageInDto.id,
                consumerMessageInDto.deliveryStatus!!,
                Date(System.currentTimeMillis())
            )
        }
        else{
            logger.error("Can' receive status ${consumerMessageInDto.deliveryStatus} from consumer!")
        }
    }


    fun getOldestMessageToConsumer(deviceId: String): Optional<ConsumerMessageOutDto> {
        return mainRepository.findOldestNotDeliveredMessageForDevise(deviceId)
            .map { messageConverter.getConsumerMessageOutDto(it) }
    }


    fun getDeliveryStatusStatus(messageId: Int) : Optional<ProducerDeliveryStatusOutDto>
    {
        return mainRepository.findDeliveredById(messageId)
            .map { ProducerDeliveryStatusOutDto(it) }
    }

    private fun publishMessageToConsumerAction(deviseId: String, consumerMessageOutDto: ConsumerMessageOutDto)
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
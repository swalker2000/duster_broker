package com.duster.messagehandler.rest

import com.duster.messagehandler.CommonMessageService
import com.duster.messagehandler.data.dto.consumer.ConsumerMessageOutDto
import com.duster.messagehandler.data.dto.producer.ProducerDeliveryStatusOutDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class RestServiceMessageHandler {

    @Autowired
    private lateinit var commonMessageService: CommonMessageService

    /**
     * Получить самое старшее не доставленное сообщение для onsumer
     */
    @GetMapping("/consumer/getLastMessage/{deviceId}")
    fun getOldestMessageToConsumer(@PathVariable deviceId: String) : List<ConsumerMessageOutDto>
    {
        val messageOptional = commonMessageService.getOldestMessageToConsumer(deviceId)
        return if(messageOptional.isPresent)
            listOf(messageOptional.get())
        else emptyList()
    }

    @GetMapping("/producer/getMessageStatus/{messageId}")
    fun getDeliveryStatusStatus(@PathVariable messageId: Int): ProducerDeliveryStatusOutDto {
        return commonMessageService.getDeliveryStatusStatus(messageId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found for id: $messageId")
        }
    }
}
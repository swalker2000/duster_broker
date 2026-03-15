package com.duster.transport.rest

import com.duster.common.CommonMessageService
import com.duster.transport.data.dto.consumer.ConsumerMessageOutDto
import com.duster.transport.data.dto.producer.ProducerDeliveryStatusOutDto
import io.swagger.v3.oas.annotations.Operation
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


    @Operation(summary = "Get oldest message (from consumer).")
    @GetMapping("/consumer/getLastMessage/{deviceId}")
    fun getOldestMessageToConsumer(@PathVariable deviceId: String) : List<ConsumerMessageOutDto>
    {
        val messageOptional = commonMessageService.getOldestMessageToConsumer(deviceId)
        return if(messageOptional.isPresent)
            listOf(messageOptional.get())
        else emptyList()
    }

    @Operation(summary = "Get delivery status of message (from producer).")
    @GetMapping("/producer/getMessageStatus/{messageId}")
    fun getDeliveryStatusStatus(@PathVariable messageId: Int): ProducerDeliveryStatusOutDto {
        return commonMessageService.getDeliveryStatusStatus(messageId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found for id: $messageId")
        }
    }
}
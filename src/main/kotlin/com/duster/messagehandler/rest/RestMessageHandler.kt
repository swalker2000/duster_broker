package com.duster.messagehandler.rest

import com.duster.messagehandler.CommonMessageService
import com.duster.messagehandler.data.dto.consumer.ConsumerMessageInDto
import com.duster.messagehandler.data.dto.producer.message.ProducerMessageInDto
import com.duster.messagehandler.data.dto.producer.message.ProducerMessageOutDto
import io.swagger.v3.oas.annotations.Operation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class RestMessageHandler {

    private val logger = LoggerFactory.getLogger(RestMessageHandler::class.java)

    @Autowired
    private lateinit var commonMessageService: CommonMessageService

    @Operation(summary = "Publish new message (from producer).")
    @PutMapping("/producer/request/{deviceId}")
    fun newProducerMessageIn(
        @PathVariable deviceId: String,               // deviceId из URL
        @RequestBody  producerMessageIn: ProducerMessageInDto  // JSON в теле
    ) : ProducerMessageOutDto {
            logger.info("newProducerMessageIn [$deviceId]")
            return commonMessageService.newProducerMessageIn(producerMessageIn, deviceId).orElseThrow {
                ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error create message to: $deviceId")
            }
    }


    @Operation(summary = "Set message delivery status (from consumer).")
    @PostMapping("/consumer/request/{deviceId}")
    fun newConsumerMessageIn(
        @PathVariable deviceId: String,               // deviceId из URL
        @RequestBody  consumerMessageInDto: ConsumerMessageInDto  // JSON в теле
    ) {
        logger.info("newConsumerMessageIn [$deviceId]")
        commonMessageService.newConsumerMessageIn(consumerMessageInDto)
    }

}
package com.duster.duster_protocol.transport.client.producer.tcp

import com.duster.duster_protocol.messagefactory.bytearray.generate.ByteArrayGeneratorWhis
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.BrokerMessageReceivedFromProducerParser
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.BrokerProducerLoginResultParser
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.BrokerReturnMessageStatusToProducerParser
import com.duster.duster_protocol.transport.client.Client
import com.duster.duster_protocol.transport.client.LoginFailedException
import com.duster.transport.data.dto.producer.ProducerDeliveryStatusOutDto
import com.duster.transport.data.dto.producer.message.ProducerMessageInDto
import com.duster.transport.data.dto.producer.message.ProducerMessageOutDto

class ProducerTcp(
    deviseId: String,
    url: String,
    port: Int,
    password: String = ""
) : Client(deviseId, url, port, password) {

    /**
     * Продюсер запрашивает статус сообщения.
     */
    fun askMessageStatus(messageId: Long): ProducerDeliveryStatusOutDto {
        return makeTransaction {
            writeFrame(ByteArrayGeneratorWhis.FromProducer.askMessageStatus(messageId))
            val parsed = BrokerReturnMessageStatusToProducerParser.parse(readFrame())
                ?: error("invalid message status frame")
            ProducerDeliveryStatusOutDto(parsed.deliveryStatus)
        }
    }

    /**
     * Продюсер отправляет сообщение с полезной нагрузкой.
     */
    fun sendMessage(message: ProducerMessageInDto): ProducerMessageOutDto {
        return makeTransaction {
            writeFrame(ByteArrayGeneratorWhis.FromProducer.sendMessage(message))
            BrokerMessageReceivedFromProducerParser.parse(readFrame())
                ?: error("invalid send-message ack frame")
        }
    }

    override fun connect() {
        openSocket()
        writeFrame(ByteArrayGeneratorWhis.FromProducer.login(deviseId, password))
        val result = BrokerProducerLoginResultParser.parse(readFrame())
            ?: error("invalid producer login result")
        if (!result.ok) {
            closeSocket()
            throw LoginFailedException(result)
        }
    }

    override fun disconnect() {
        closeSocket()
    }
}

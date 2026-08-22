package com.duster.duster_protocol.transport.client.consumer.tcp

import com.duster.duster_protocol.messagefactory.bytearray.generate.ByteArrayGeneratorWhis
import com.duster.duster_protocol.messagefactory.bytearray.parse.MessageDetector
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.BrokerConsumerLoginResultParser
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.BrokerSendMessageToConsumerParser
import com.duster.duster_protocol.messagefactory.transport.constant.DbpMessageType
import com.duster.duster_protocol.transport.client.Client
import com.duster.duster_protocol.transport.client.LoginFailedException
import com.duster.transport.data.dto.consumer.ConsumerMessageInDto
import com.duster.transport.data.dto.consumer.ConsumerMessageOutDto
import javax.net.ssl.SSLSocketFactory

class ConsumerTcp(
    deviseId: String,
    url: String,
    port: Int,
    password: String = "",
    useTls: Boolean = false,
    insecureTls: Boolean = true,
    sslSocketFactory: SSLSocketFactory? = null
) : Client(deviseId, url, port, password, useTls, insecureTls, sslSocketFactory) {

    private val detector = MessageDetector()

    /**
     * Запрашивает у брокера наиболее старое доступное ему сообщение.
     * @return если доступных сообщений нет возвращает null
     */
    fun giveMeMessage(): ConsumerMessageOutDto? {
        return makeTransaction {
            writeFrame(ByteArrayGeneratorWhis.FromConsumer.giveMeMessage())
            val frame = readFrame()
            when (detector.detect(frame)) {
                DbpMessageType.BROKER_DONT_HAVE_MESSAGE_FOR_CONSUMER -> null
                DbpMessageType.BROKER_SEND_MESSAGE_TO_CONSUMER ->
                    BrokerSendMessageToConsumerParser.parse(frame)
                        ?: error("invalid consumer message frame")
                else -> error("unexpected reply to CONSUMER_ASK_MESSAGE")
            }
        }
    }

    /**
     * Консьюмер сообщает брокеру, что статус сообщения поменялся.
     * (к примеру сообщение было получено, либо выполнено)
     */
    fun messageStatusChanged(message: ConsumerMessageInDto) {
        makeTransaction<Unit> {
            writeFrame(ByteArrayGeneratorWhis.FromConsumer.messageStatusChanged(message))
        }
    }

    override fun connect() {
        openSocket()
        writeFrame(ByteArrayGeneratorWhis.FromConsumer.login(deviseId, password))
        val result = BrokerConsumerLoginResultParser.parse(readFrame())
            ?: error("invalid consumer login result")
        if (!result.ok) {
            closeSocket()
            throw LoginFailedException(result)
        }
    }

    override fun disconnect() {
        closeSocket()
    }
}

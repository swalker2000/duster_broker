package com.duster.duster_protocol.transport.brocker.connection.toproducer

import com.duster.duster_protocol.messagefactory.bytearray.generate.ByteArrayGeneratorWhis
import com.duster.duster_protocol.messagefactory.bytearray.parse.MessageDetector
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.ProducerAskMessageStatusParser
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.ProducerSendMessageParser
import com.duster.duster_protocol.messagefactory.transport.constant.DbpMessageType
import com.duster.duster_protocol.transport.brocker.connection.Connection
import com.duster.duster_protocol.transport.brocker.connection.toproducer.handlers.OnAskMessageStatus
import com.duster.duster_protocol.transport.brocker.connection.toproducer.handlers.OnSendMessage
import com.duster.duster_protocol.transport.io.DbpFrameIo
import com.duster.transport.data.dto.producer.message.MessageBirthCertificate

class ConnectionToProducer(deviseId: String) : Connection(deviseId) {

    private val detector = MessageDetector()

    internal var onAskMessageStatusHandler: OnAskMessageStatus? = null
    internal var onSendMessageHandler: OnSendMessage? = null

    fun onAskMessageStatus(handler: OnAskMessageStatus) {
        onAskMessageStatusHandler = handler
    }

    fun onSendMessage(handler: OnSendMessage) {
        onSendMessageHandler = handler
    }

    override fun run() {
        requireHandlers(
            "onAskMessageStatus" to onAskMessageStatusHandler,
            "onSendMessage" to onSendMessageHandler,
        )
        val onAsk = onAskMessageStatusHandler!!
        val onSend = onSendMessageHandler!!
        while (true) {
            val frame = DbpFrameIo.read(input) ?: return
            when (detector.detect(frame)) {
                DbpMessageType.PRODUCER_SEND_MESSAGE -> {
                    val message = ProducerSendMessageParser.parse(frame) ?: continue
                    if (message.messageBirthCertificate?.producerDeviseId.isNullOrBlank()) {
                        val tmpId = message.messageBirthCertificate?.tmpId ?: 0L
                        message.messageBirthCertificate = MessageBirthCertificate(tmpId, deviseId)
                    }
                    val ack = onSend.onSendMessage(deviseId, message)
                    DbpFrameIo.write(output, ByteArrayGeneratorWhis.Broker.ToProducer.messageOut(ack))
                }
                DbpMessageType.PRODUCER_ASK_MESSAGE_STATUS -> {
                    val messageId = ProducerAskMessageStatusParser.parse(frame) ?: continue
                    val status = onAsk.onAskMessageStatus(deviseId, messageId)
                    DbpFrameIo.write(
                        output,
                        ByteArrayGeneratorWhis.Broker.ToProducer.sendMessageStatus(messageId, status)
                    )
                }
                else -> return
            }
        }
    }
}

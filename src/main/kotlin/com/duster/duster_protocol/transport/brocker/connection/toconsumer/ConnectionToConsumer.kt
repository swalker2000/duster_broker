package com.duster.duster_protocol.transport.brocker.connection.toconsumer

import com.duster.duster_protocol.messagefactory.bytearray.generate.ByteArrayGeneratorWhis
import com.duster.duster_protocol.messagefactory.bytearray.parse.MessageDetector
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.ConsumerMessageReceivedParser
import com.duster.duster_protocol.messagefactory.transport.constant.DbpMessageType
import com.duster.duster_protocol.transport.brocker.connection.Connection
import com.duster.duster_protocol.transport.brocker.connection.toconsumer.handlers.OnConsumerAskMessages
import com.duster.duster_protocol.transport.brocker.connection.toconsumer.handlers.OnMessageStatusChanged
import com.duster.duster_protocol.transport.io.DbpFrameIo

class ConnectionToConsumer(deviseId: String) : Connection(deviseId) {

    private val detector = MessageDetector()

    internal var onConsumerAskMessagesHandler: OnConsumerAskMessages? = null
    internal var onMessageStatusChangedHandler: OnMessageStatusChanged? = null

    fun onConsumerAskMessages(handler: OnConsumerAskMessages) {
        onConsumerAskMessagesHandler = handler
    }

    fun onMessageStatusChanged(handler: OnMessageStatusChanged) {
        onMessageStatusChangedHandler = handler
    }

    override fun run() {
        requireHandlers(
            "onConsumerAskMessages" to onConsumerAskMessagesHandler,
            "onMessageStatusChanged" to onMessageStatusChangedHandler,
        )
        val onAsk = onConsumerAskMessagesHandler!!
        val onStatus = onMessageStatusChangedHandler!!
        while (true) {
            val frame = DbpFrameIo.read(input) ?: return
            when (detector.detect(frame)) {
                DbpMessageType.CONSUMER_ASK_MESSAGE -> {
                    val message = onAsk.onConsumerAskMessages(deviseId)
                    val response = if (message == null) {
                        ByteArrayGeneratorWhis.Broker.ToConsumer.dontHaveMessage()
                    } else {
                        ByteArrayGeneratorWhis.Broker.ToConsumer.messageOut(message)
                    }
                    DbpFrameIo.write(output, response)
                }
                DbpMessageType.CONSUMER_MESSAGE_STATUS_CHANDGED -> {
                    val message = ConsumerMessageReceivedParser.parse(frame) ?: continue
                    onStatus.onMessageStatusChanged(deviseId, message)
                }
                else -> return
            }
        }
    }
}

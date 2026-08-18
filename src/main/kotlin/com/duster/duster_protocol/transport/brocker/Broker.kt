package com.duster.duster_protocol.transport.brocker

import com.duster.duster_protocol.transport.brocker.connection.toconsumer.handlers.OnConsumerAskMessages
import com.duster.duster_protocol.transport.brocker.connection.toconsumer.handlers.OnMessageStatusChanged
import com.duster.duster_protocol.transport.brocker.connection.toproducer.handlers.OnAskMessageStatus
import com.duster.duster_protocol.transport.brocker.connection.toproducer.handlers.OnSendMessage

class Broker {

    val connectionToProducer = ConnectionToProducerBuilder()

    val connectionToConsumer = ConnectionToConsumerBuilder()


    inner class ConnectionToProducerBuilder{
        fun onAskMessageStatus(handler: OnAskMessageStatus)
        {

        }

        fun onSendMessage(handler: OnSendMessage)
        {

        }

    }

    inner class ConnectionToConsumerBuilder{
        fun onConsumerAskMessages(handler : OnConsumerAskMessages)
        {

        }

        fun onMessageStatusChanged(handler : OnMessageStatusChanged)
        {

        }
    }









}
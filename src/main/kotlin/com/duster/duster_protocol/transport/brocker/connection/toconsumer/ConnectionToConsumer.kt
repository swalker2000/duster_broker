package com.duster.duster_protocol.transport.brocker.connection.toconsumer

import com.duster.duster_protocol.transport.brocker.connection.Connection
import com.duster.duster_protocol.transport.brocker.connection.toconsumer.handlers.OnConsumerAskMessages
import com.duster.duster_protocol.transport.brocker.connection.toconsumer.handlers.OnMessageStatusChanged

class ConnectionToConsumer(deviseId: String) : Connection(deviseId)  {

    fun onConsumerAskMessages(handler : OnConsumerAskMessages)
    {

    }

    fun onMessageStatusChanged(handler : OnMessageStatusChanged)
    {

    }

    override fun run() {
        TODO("Not yet implemented")
    }

}
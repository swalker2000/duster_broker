package com.duster.duster_protocol.transport.brocker.connection.toproducer

import com.duster.duster_protocol.transport.brocker.connection.Connection
import com.duster.duster_protocol.transport.brocker.connection.toproducer.handlers.OnAskMessageStatus
import com.duster.duster_protocol.transport.brocker.connection.toproducer.handlers.OnSendMessage

class ConnectionToProducer(deviseId: String) : Connection(deviseId) {
    override fun run() {
        TODO("Not yet implemented")
    }

    fun onAskMessageStatus(handler: OnAskMessageStatus)
    {

    }

    fun onSendMessage(handler: OnSendMessage)
    {

    }
}
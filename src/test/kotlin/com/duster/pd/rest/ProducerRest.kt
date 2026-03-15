package com.duster.pd.rest

import com.duster.transport.data.dto.producer.message.ProducerMessageInDto
import com.duster.pd.Producer

class ProducerRest (
brokerUrl: String,
override val deviceId: String = "0"
) : Producer {
    override fun publish(
        consumerDeviseId: String,
        message: ProducerMessageInDto,
        onMessageStatusChange: Producer.OnMessageStatusChange?
    ) {
        TODO("Not yet implemented")
    }

    override fun connect() {
        TODO("Not yet implemented")
    }

    override fun disconnect() {
        TODO("Not yet implemented")
    }
}
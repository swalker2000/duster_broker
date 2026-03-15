package com.duster.pd.rest

import com.duster.transport.data.dto.consumer.ConsumerMessageInDto
import com.duster.pd.Consumer

class ConsumerRest(brokerUrl: String, deviseId: String) : Consumer(deviseId) {
    override fun sendResponse(response: ConsumerMessageInDto) {
        TODO("Not yet implemented")
    }

    override fun connect() {
        TODO("Not yet implemented")
    }

    override fun disconnect() {
        TODO("Not yet implemented")
    }
}
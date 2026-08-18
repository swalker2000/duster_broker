package com.duster.duster_protocol.transport.client.producer.tcp

import com.duster.duster_protocol.transport.client.Client
import com.duster.transport.data.dto.producer.ProducerDeliveryStatusOutDto
import com.duster.transport.data.dto.producer.message.ProducerMessageInDto

class ProducerTcp(
    deviseId: String,
    url: String,
    port: Int
) : Client(deviseId, url, port) {

    /**
     * Продюсер запрашивает статус сообщения.
     */
    fun askMessageStatus(messageId: Long) : ProducerDeliveryStatusOutDto
    {
       return makeTransaction {
           //вот здесь реализуй
       }
    }

    /**
     * Продюсер отправляет сообщение с полезной нагрузкой.
     */
    fun sendMessage(message: ProducerMessageInDto): List<Int> {
        return makeTransaction {
            //вот здесь реализуй
        }
    }


    override fun connect() {
        TODO("Not yet implemented")
    }

    override fun disconnect() {
        TODO("Not yet implemented")
    }
}
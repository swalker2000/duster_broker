package com.duster.duster_protocol.transport.client.consumer.tcp

import com.duster.duster_protocol.transport.client.Client
import com.duster.transport.data.dto.consumer.ConsumerMessageInDto
import com.duster.transport.data.dto.consumer.ConsumerMessageOutDto

class ConsumerTcp(
    deviseId: String,
    url: String,
    port: Int
) : Client(deviseId, url, port) {






    /**
     * Запрашивает у брокера наиболее старое доступное ему сообщение.
     * @return если доступных сообщений нет возвращает null
     */
    fun giveMeMessage() : ConsumerMessageOutDto?
    {
        return makeTransaction {
            //вот здесь реализуй
        }
    }

    /**
     * Консьюмер сообщает брокеру, что статус сообщения поменялся.
     * (к примеру сообщение было получено, либо выполнено)
     */
    fun messageStatusChanged(message: ConsumerMessageInDto){
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
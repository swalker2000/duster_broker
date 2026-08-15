package com.duster.duster_protocol.messagefactory.generators

import com.duster.database.data.message.DeliveryStatus
import com.duster.duster_protocol.messagefactory.ByteArrayGenerator
import com.duster.duster_protocol.messagefactory.DbpMessageType
import com.duster.transport.data.dto.consumer.ConsumerMessageInDto

/**
 *  Генератор сообщений от консьюмера, нам брокеру.
 */
object ConsumerInByteArrayGenerator : ByteArrayGenerator<ConsumerMessageInDto>(DbpMessageType.MESSAGE_RECEIVED) {

    /** id (8 байт) + deliveryStatus (1 байт) — фиксированный размер */
    override val MIN_PAYLOAD_SIZE: Int = 9

    /**
     * Преобразует объект ConsumerMessageInDto в массив байт который в дальнейшем будет завернут в еще один слой и передан по протоколу duster_broker.
     *  - ВНИМАНИЕ: транспортные байты (стартовый, стоповый, экран) передаются не здесь! Здесь просто преобразуем объект сообщения в массив байт.
     */
    override fun generatePayload(message: ConsumerMessageInDto): List<Int> {
        val idArray = getBytes(message.id)
        val payload: MutableList<Int> = mutableListOf()
        payload.addAll(idArray)
        payload.add(message.deliveryStatus!!.ordinal)
        return payload
    }

    /**
     * Преобразует массив байт полезной нагрузки ответа consumer в ConsumerMessageInDto.
     * @return null если payload некорректный.
     */
    override fun parsePayload(payload: List<Int>): ConsumerMessageInDto? {
        if (payload.size != MIN_PAYLOAD_SIZE) {
            return null
        }
        val id = getLong(payload, 0)
        val statusOrdinal = payload[8]
        val statuses = DeliveryStatus.entries
        if (statusOrdinal !in statuses.indices) {
            return null
        }
        val status = statuses[statusOrdinal]
        if (!status.canReceiveFromConsumer) {
            return null
        }

        return ConsumerMessageInDto(id = id).apply {
            deliveryStatus = status
        }
    }
}

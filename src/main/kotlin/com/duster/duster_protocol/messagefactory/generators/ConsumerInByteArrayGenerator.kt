package com.duster.duster_protocol.messagefactory.generators

import com.duster.database.data.message.DeliveryStatus
import com.duster.duster_protocol.messagefactory.ByteArrayGenerator
import com.duster.duster_protocol.messagefactory.DbpMessageType
import com.duster.transport.data.dto.consumer.ConsumerMessageInDto
import java.util.Optional

/**
 *  Генератор сообщений от консьюмера, нам брокеру.
 */
object ConsumerInByteArrayGenerator : ByteArrayGenerator<ConsumerMessageInDto>(DbpMessageType.MESSAGE_RECEIVED) {

    /** id (8 байт) + deliveryStatus (1 байт) */
    private const val IN_PAYLOAD_SIZE: Int = 9

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
     * Преобразует массив байт ответа consumer в ConsumerMessageInDto.
     * Кадр: START | TYPE | escape(id[8] + deliveryStatus[1]) | CRC16_HI | CRC16_LO | STOP
     * @return Optional.empty() если массив байт не корректный и его невозможно превратить в сообщение.
     */
    override fun parseByteArray(message: List<Char>): Optional<ConsumerMessageInDto> {
        val payload = extractPayload(message) ?: return Optional.empty()

        if (payload.size != IN_PAYLOAD_SIZE) {
            return Optional.empty()
        }

        val id = getLong(payload, 0)
        val statusOrdinal = payload[8]
        val statuses = DeliveryStatus.entries
        if (statusOrdinal !in statuses.indices) {
            return Optional.empty()
        }
        val status = statuses[statusOrdinal]
        if (!status.canReceiveFromConsumer) {
            return Optional.empty()
        }

        return Optional.of(
            ConsumerMessageInDto(id = id).apply {
                deliveryStatus = status
            }
        )
    }
}

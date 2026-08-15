package com.duster.duster_protocol.messagefactory.generators

import com.duster.database.data.message.DeliveryGuarantee
import com.duster.duster_protocol.messagefactory.ByteArrayGenerator
import com.duster.duster_protocol.messagefactory.DbpMessageType
import com.duster.transport.data.dto.consumer.ConsumerMessageOutDto
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.Optional

/**
 *  Генератор сообщений от брокера, нам консьюмеру.
 *
 * Payload: id[8] + timestamp[8] + commandLen[2 LE] + command[N] + believerGuarantee[1] + dataJson[M]
 */
object ConsumerOutByteArrayGenerator : ByteArrayGenerator<ConsumerMessageOutDto>(DbpMessageType.TAKE_MESSAGE) {

    private val objectMapper = jacksonObjectMapper()

    /** id(8) + ts(8) + cmdLen(2) + guarantee(1); command и data переменной длины */
    private const val MIN_PAYLOAD_SIZE: Int = 8 + 8 + 2 + 1

    /**
     * Преобразует объект ConsumerMessageOutDto в массив байт который в дальнейшем будет завернут в еще один слой и передан по протоколу duster_broker.
     *  - ВНИМАНИЕ: транспортные байты (стартовый, стоповый, экран) передаются не здесь! Здесь просто преобразуем объект сообщения в массив байт.
     */
    override fun generatePayload(message: ConsumerMessageOutDto): List<Int> {
        val idArray = getBytes(message.id)
        val currentTimestampArray = getBytes(message.currentTimestamp)
        val commandArray: List<Int> = message.command.map { it.code and 0xFF }
        val dataJson = message.data?.let { objectMapper.writeValueAsString(it) } ?: "{}"
        val dataArray = dataJson.toByteArray(Charsets.UTF_8).map { it.toInt() and 0xFF }

        val payload: MutableList<Int> = mutableListOf()
        payload.addAll(idArray)
        payload.addAll(currentTimestampArray)
        payload.addAll(getBytes2(commandArray.size))
        payload.addAll(commandArray)
        payload.add(message.believerGuarantee.ordinal)
        payload.addAll(dataArray)

        return payload
    }

    /**
     * Преобразует массив байт брокера в ConsumerMessageOutDto.
     * Кадр: START | TYPE | escape(payload) | CRC16_HI | CRC16_LO | STOP
     * @return Optional.empty() если массив байт не корректный и его невозможно превратить в сообщение.
     */
    override fun parseByteArray(message: List<Char>): Optional<ConsumerMessageOutDto> {
        val payload = extractPayload(message) ?: return Optional.empty()
        if (payload.size < MIN_PAYLOAD_SIZE) {
            return Optional.empty()
        }

        val id = getLong(payload, 0)
        val currentTimestamp = getLong(payload, 8)
        val commandLen = getUnsignedShort(payload, 16)
        val commandEnd = 18 + commandLen
        if (commandEnd + 1 > payload.size) {
            return Optional.empty()
        }

        val command = payload.subList(18, commandEnd)
            .map { (it and 0xFF).toChar() }
            .joinToString("")

        val guaranteeOrdinal = payload[commandEnd]
        val guarantees = DeliveryGuarantee.entries
        if (guaranteeOrdinal !in guarantees.indices) {
            return Optional.empty()
        }

        val dataBytes = payload.subList(commandEnd + 1, payload.size)
            .map { (it and 0xFF).toByte() }
            .toByteArray()
        val dataJson = String(dataBytes, Charsets.UTF_8)
        val data: Map<String, Any> = try {
            objectMapper.readValue(dataJson)
        } catch (_: Exception) {
            return Optional.empty()
        }

        return Optional.of(
            ConsumerMessageOutDto(id = id).apply {
                this.currentTimestamp = currentTimestamp
                this.command = command
                this.believerGuarantee = guarantees[guaranteeOrdinal]
                this.data = data
            }
        )
    }
}

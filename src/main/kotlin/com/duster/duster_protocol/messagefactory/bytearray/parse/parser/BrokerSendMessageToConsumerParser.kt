package com.duster.duster_protocol.messagefactory.bytearray.parse.parser

import com.duster.database.data.message.DeliveryGuarantee
import com.duster.duster_protocol.messagefactory.transport.constant.DbpMessageType
import com.duster.transport.data.dto.consumer.ConsumerMessageOutDto
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * Парсер [DbpMessageType.BROKER_SEND_MESSAGE_TO_CONSUMER].
 * Payload: id[8] + timestamp[8] + commandLen[2 LE] + command[N] + believerGuarantee[1] + dataJson[M].
 */
object BrokerSendMessageToConsumerParser : AbstractParser<ConsumerMessageOutDto>() {

    override val dbpMessageType: DbpMessageType = DbpMessageType.BROKER_SEND_MESSAGE_TO_CONSUMER
    override val minPayloadSize: Int = 8 + 8 + 2 + 1

    private val objectMapper = jacksonObjectMapper()

    override fun parsePayload(payload: List<Int>): ConsumerMessageOutDto? {
        val id = getLong(payload, 0)
        val currentTimestamp = getLong(payload, 8)
        val commandLen = getUnsignedShort(payload, 16)
        val commandEnd = 18 + commandLen
        if (commandEnd + 1 > payload.size) {
            return null
        }
        val command = payload.subList(18, commandEnd)
            .map { (it and 0xFF).toChar() }
            .joinToString("")
        val guaranteeOrdinal = payload[commandEnd]
        val guarantees = DeliveryGuarantee.entries
        if (guaranteeOrdinal !in guarantees.indices) {
            return null
        }
        val data = parseDataJson(payload.subList(commandEnd + 1, payload.size)) ?: return null
        return ConsumerMessageOutDto(id = id).apply {
            this.currentTimestamp = currentTimestamp
            this.command = command
            this.believerGuarantee = guarantees[guaranteeOrdinal]
            this.data = data
        }
    }

    private fun parseDataJson(dataBytes: List<Int>): Map<String, Any>? {
        val dataJson = String(dataBytes.map { (it and 0xFF).toByte() }.toByteArray(), Charsets.UTF_8)
        return try {
            objectMapper.readValue(dataJson)
        } catch (_: Exception) {
            null
        }
    }
}

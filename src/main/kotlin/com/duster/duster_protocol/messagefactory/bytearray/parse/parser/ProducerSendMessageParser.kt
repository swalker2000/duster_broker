package com.duster.duster_protocol.messagefactory.bytearray.parse.parser

import com.duster.database.data.message.DeliveryGuarantee
import com.duster.duster_protocol.messagefactory.transport.constant.DbpMessageType
import com.duster.transport.data.dto.producer.message.MessageBirthCertificate
import com.duster.transport.data.dto.producer.message.ProducerMessageInDto
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * Парсер [DbpMessageType.PRODUCER_SEND_MESSAGE].
 * Payload: tmpId[8] + commandLen[2 LE] + command[N] + believerGuarantee[1] + dataJson[M].
 */
object ProducerSendMessageParser : AbstractParser<ProducerMessageInDto>() {

    override val dbpMessageType: DbpMessageType = DbpMessageType.PRODUCER_SEND_MESSAGE
    override val minPayloadSize: Int = 8 + 2 + 1

    private val objectMapper = jacksonObjectMapper()

    override fun parsePayload(payload: List<Int>): ProducerMessageInDto? {
        val tmpId = getLong(payload, 0)
        val commandLen = getUnsignedShort(payload, 8)
        val commandEnd = 10 + commandLen
        if (commandEnd + 1 > payload.size) {
            return null
        }
        val command = payload.subList(10, commandEnd)
            .map { (it and 0xFF).toChar() }
            .joinToString("")
        val guaranteeOrdinal = payload[commandEnd]
        val guarantees = DeliveryGuarantee.entries
        if (guaranteeOrdinal !in guarantees.indices) {
            return null
        }
        val dataJson = String(
            payload.subList(commandEnd + 1, payload.size).map { (it and 0xFF).toByte() }.toByteArray(),
            Charsets.UTF_8
        )
        val data: Map<String, Any> = try {
            objectMapper.readValue(dataJson)
        } catch (_: Exception) {
            return null
        }
        return ProducerMessageInDto().apply {
            messageBirthCertificate = MessageBirthCertificate(tmpId = tmpId, producerDeviseId = "")
            believerGuarantee = guarantees[guaranteeOrdinal]
            this.command = command
            this.data = data
        }
    }
}

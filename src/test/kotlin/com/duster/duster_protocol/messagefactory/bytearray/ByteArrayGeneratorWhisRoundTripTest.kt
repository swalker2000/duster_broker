package com.duster.duster_protocol.messagefactory.bytearray

import com.duster.database.data.client.Role
import com.duster.database.data.message.DeliveryGuarantee
import com.duster.database.data.message.DeliveryStatus
import com.duster.duster_protocol.messagefactory.bytearray.generate.ByteArrayGeneratorWhis
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.BrokerConsumerLoginResultParser
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.BrokerMessageReceivedFromProducerParser
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.BrokerProducerLoginResultParser
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.BrokerReturnMessageStatusToProducerParser
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.BrokerSendMessageToConsumerParser
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.ConsumerLoginParser
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.ConsumerMessageReceivedParser
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.ProducerAskMessageStatusParser
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.ProducerLoginParser
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.ProducerSendMessageParser
import com.duster.duster_protocol.messagefactory.transport.constant.DbpMessageType
import com.duster.duster_protocol.messagefactory.transport.constant.StandardBytes
import com.duster.transport.data.dto.consumer.ConsumerMessageInDto
import com.duster.transport.data.dto.consumer.ConsumerMessageOutDto
import com.duster.transport.data.dto.producer.ProducerDeliveryStatusOutDto
import com.duster.transport.data.dto.producer.message.MessageBirthCertificate
import com.duster.transport.data.dto.producer.message.ProducerMessageInDto
import com.duster.transport.data.dto.producer.message.ProducerMessageOutDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ByteArrayGeneratorWhisRoundTripTest {

    @Test
    fun consumerMessageIn_roundTrip() {
        val original = ConsumerMessageInDto(id = 42).apply {
            deliveryStatus = DeliveryStatus.COMPLETED
        }
        val parsed = ConsumerMessageReceivedParser.parse(ByteArrayGeneratorWhis.FromConsumer.messageIn(original))
        assertNotNull(parsed)
        assertEquals(original.id, parsed!!.id)
        assertEquals(original.deliveryStatus, parsed.deliveryStatus)
    }

    @Test
    fun consumerMessageIn_acceptsAllConsumerStatuses() {
        listOf(
            DeliveryStatus.COMPLETED_WITH_ERROR,
            DeliveryStatus.COMPLETED,
            DeliveryStatus.DELIVERED,
        ).forEach { status ->
            val dto = ConsumerMessageInDto(id = 7).apply { deliveryStatus = status }
            val parsed = ConsumerMessageReceivedParser.parse(ByteArrayGeneratorWhis.FromConsumer.messageIn(dto))
            assertEquals(status, parsed!!.deliveryStatus)
        }
    }

    @Test
    fun consumerMessageOut_roundTrip() {
        val original = sampleConsumerOut()
        val parsed = BrokerSendMessageToConsumerParser.parse(
            ByteArrayGeneratorWhis.Broker.ToConsumer.messageOut(original)
        )
        assertNotNull(parsed)
        assertEquals(original.id, parsed!!.id)
        assertEquals(original.currentTimestamp, parsed.currentTimestamp)
        assertEquals(original.command, parsed.command)
        assertEquals(original.believerGuarantee, parsed.believerGuarantee)
        assertEquals(1, (parsed.data!!["x"] as Number).toInt())
    }

    @Test
    fun consumerMessageOut_nullData_roundTripsAsEmptyJsonObject() {
        val original = sampleConsumerOut().apply { data = null }
        val parsed = BrokerSendMessageToConsumerParser.parse(
            ByteArrayGeneratorWhis.Broker.ToConsumer.messageOut(original)
        )
        assertNotNull(parsed)
        assertTrue(parsed!!.data!!.isEmpty())
    }

    @Test
    fun consumerMessageOut_emptyCommand() {
        val original = sampleConsumerOut().apply { command = "" }
        val parsed = BrokerSendMessageToConsumerParser.parse(
            ByteArrayGeneratorWhis.Broker.ToConsumer.messageOut(original)
        )
        assertEquals("", parsed!!.command)
    }

    @Test
    fun consumerMessageOut_escapesSpecialBytes() {
        val message = sampleConsumerOut().apply {
            id = 0
            currentTimestamp = 1
            command = "\u0004"
        }
        val frame = ByteArrayGeneratorWhis.Broker.ToConsumer.messageOut(message)
        val escaped = frame.subList(2, frame.size - 3)
        assertEquals(listOf(StandardBytes.MIRROR, StandardBytes.START_BYTE), escaped.subList(0, 2))
        val parsed = BrokerSendMessageToConsumerParser.parse(frame)
        assertEquals(0, parsed!!.id)
        assertEquals(1, parsed.currentTimestamp)
        assertEquals("\u0004", parsed.command)
    }

    @Test
    fun consumerLogin_roundTrip() {
        val parsed = ConsumerLoginParser.parse(
            ByteArrayGeneratorWhis.FromConsumer.login("sensor-1", "secret")
        )
        assertEquals("sensor-1", parsed!!.deviseId)
        assertEquals("secret", parsed.password)
    }

    @Test
    fun producerLogin_roundTrip() {
        val parsed = ProducerLoginParser.parse(
            ByteArrayGeneratorWhis.FromProducer.login("producer-1", "pwd")
        )
        assertEquals("producer-1", parsed!!.deviseId)
        assertEquals("pwd", parsed.password)
    }

    @Test
    fun brokerConsumerLoginResult_ok_roundTrip() {
        val parsed = BrokerConsumerLoginResultParser.parse(
            ByteArrayGeneratorWhis.Broker.ToConsumer.loginResult(
                ok = true,
                deviseId = "sensor-1",
                role = Role.DEVISE,
                accessToken = "jwt-token"
            )
        )
        assertTrue(parsed!!.ok)
        assertEquals("sensor-1", parsed.deviseId)
        assertEquals(Role.DEVISE, parsed.role)
        assertEquals("jwt-token", parsed.accessToken)
    }

    @Test
    fun brokerProducerLoginResult_failOmitsToken() {
        val parsed = BrokerProducerLoginResultParser.parse(
            ByteArrayGeneratorWhis.Broker.ToProducer.loginResult(ok = false, role = Role.MAN)
        )
        assertFalse(parsed!!.ok)
        assertEquals("", parsed.accessToken)
        assertEquals(Role.MAN, parsed.role)
    }

    @Test
    fun producerMessageIn_roundTrip() {
        val original = ProducerMessageInDto().apply {
            messageBirthCertificate = MessageBirthCertificate(tmpId = 99, producerDeviseId = "p1")
            believerGuarantee = DeliveryGuarantee.ONLY_LAST
            command = "digitalWrite"
            data = mapOf("pinNumber" to 13, "pinValue" to true)
        }
        val parsed = ProducerSendMessageParser.parse(ByteArrayGeneratorWhis.FromProducer.messageIn(original))
        assertNotNull(parsed)
        assertEquals(99, parsed!!.messageBirthCertificate!!.tmpId)
        assertEquals(original.command, parsed.command)
        assertEquals(original.believerGuarantee, parsed.believerGuarantee)
        assertEquals(13, (parsed.data!!["pinNumber"] as Number).toInt())
        assertEquals(true, parsed.data!!["pinValue"])
    }

    @Test
    fun producerMessageOut_roundTrip() {
        val original = ProducerMessageOutDto(id = 5, tmpId = 11, deliveryStatus = DeliveryStatus.NOT_DELIVERED)
        val parsed = BrokerMessageReceivedFromProducerParser.parse(
            ByteArrayGeneratorWhis.Broker.ToProducer.messageOut(original)
        )
        assertEquals(5, parsed!!.id)
        assertEquals(11L, parsed.tmpId)
        assertEquals(DeliveryStatus.NOT_DELIVERED, parsed.deliveryStatus)
    }

    @Test
    fun producerAskMessageStatus_roundTrip() {
        val parsed = ProducerAskMessageStatusParser.parse(
            ByteArrayGeneratorWhis.FromProducer.producerAskMessageStatus(123456789L)
        )
        assertEquals(123456789L, parsed)
    }

    @Test
    fun brokerSendMessageStatus_roundTrip() {
        val parsed = BrokerReturnMessageStatusToProducerParser.parse(
            ByteArrayGeneratorWhis.Broker.ToProducer.sendMessageStatus(
                77L,
                ProducerDeliveryStatusOutDto(DeliveryStatus.DELIVERED)
            )
        )
        assertEquals(77L, parsed!!.messageId)
        assertEquals(DeliveryStatus.DELIVERED, parsed.deliveryStatus)
    }

    @Test
    fun consumerMessageReceived_rejectsStatusesNotFromConsumer() {
        listOf(
            DeliveryStatus.NOT_DELIVERED,
            DeliveryStatus.CANCELLED,
            DeliveryStatus.UNKNOWN,
        ).forEach { status ->
            val dto = ConsumerMessageInDto(id = 1).apply { deliveryStatus = status }
            assertNull(ConsumerMessageReceivedParser.parse(ByteArrayGeneratorWhis.FromConsumer.messageIn(dto)))
        }
    }

    @Test
    fun parser_rejectsWrongType() {
        val frame = ByteArrayGeneratorWhis.FromConsumer.messageIn(sampleConsumerIn()).toMutableList()
        frame[1] = DbpMessageType.BROKER_SEND_MESSAGE_TO_CONSUMER.code
        assertNull(ConsumerMessageReceivedParser.parse(frame))
    }

    @Test
    fun parser_rejectsBadCrc() {
        val frame = ByteArrayGeneratorWhis.Broker.ToConsumer.messageOut(sampleConsumerOut()).toMutableList()
        val crcIndex = frame.size - 3
        frame[crcIndex] = frame[crcIndex] xor 0xFF
        assertNull(BrokerSendMessageToConsumerParser.parse(frame))
    }

    @Test
    fun parser_rejectsMissingStartOrStop() {
        val valid = ByteArrayGeneratorWhis.FromProducer.login("a", "b").toMutableList()
        val noStart = valid.toMutableList().also { it[0] = 2 }
        val noStop = valid.toMutableList().also { it[it.lastIndex] = 2 }
        assertNull(ProducerLoginParser.parse(noStart))
        assertNull(ProducerLoginParser.parse(noStop))
    }

    @Test
    fun parser_rejectsTooShortFrame() {
        assertNull(
            ConsumerMessageReceivedParser.parse(
                listOf(StandardBytes.START_BYTE, StandardBytes.STOP_BYTE)
            )
        )
    }

    private fun sampleConsumerIn(): ConsumerMessageInDto =
        ConsumerMessageInDto(id = 42).apply { deliveryStatus = DeliveryStatus.DELIVERED }

    private fun sampleConsumerOut(): ConsumerMessageOutDto =
        ConsumerMessageOutDto(id = 100).apply {
            currentTimestamp = 1_700_000_000_000L
            command = "PING"
            believerGuarantee = DeliveryGuarantee.RECEIPT_CONFIRMATION
            data = mapOf("x" to 1)
        }
}

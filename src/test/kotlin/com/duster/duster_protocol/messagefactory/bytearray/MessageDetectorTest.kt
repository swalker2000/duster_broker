package com.duster.duster_protocol.messagefactory.bytearray

import com.duster.database.data.client.Role
import com.duster.database.data.message.DeliveryGuarantee
import com.duster.database.data.message.DeliveryStatus
import com.duster.duster_protocol.messagefactory.bytearray.generate.ByteArrayGeneratorWhis
import com.duster.duster_protocol.messagefactory.bytearray.parse.MessageDetector
import com.duster.duster_protocol.messagefactory.bytearray.parse.dto.LoginCredentials
import com.duster.duster_protocol.messagefactory.bytearray.parse.dto.LoginResult
import com.duster.duster_protocol.messagefactory.bytearray.parse.dto.ProducerDeliveryStatusFrame
import com.duster.duster_protocol.messagefactory.transport.constant.DbpMessageType
import com.duster.duster_protocol.messagefactory.transport.constant.StandardBytes
import com.duster.transport.data.dto.consumer.ConsumerMessageInDto
import com.duster.transport.data.dto.consumer.ConsumerMessageOutDto
import com.duster.transport.data.dto.producer.ProducerDeliveryStatusOutDto
import com.duster.transport.data.dto.producer.message.MessageBirthCertificate
import com.duster.transport.data.dto.producer.message.ProducerMessageInDto
import com.duster.transport.data.dto.producer.message.ProducerMessageOutDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MessageDetectorTest {

    private val detector = MessageDetector()

    @Test
    fun detect_serviceFramesWithoutPayload() {
        assertEquals(
            DbpMessageType.CONSUMER_ASK_MESSAGE,
            detector.detect(ByteArrayGeneratorWhis.FromConsumer.askMessage())
        )
        assertEquals(
            DbpMessageType.BROKER_DONT_HAVE_MESSAGE_FOR_CONSUMER,
            detector.detect(ByteArrayGeneratorWhis.Broker.ToConsumer.dontHaveMessage())
        )
    }

    @Test
    fun parse_serviceFramesReturnType() {
        assertEquals(
            DbpMessageType.CONSUMER_ASK_MESSAGE,
            detector.parse(ByteArrayGeneratorWhis.FromConsumer.askMessage())
        )
        assertEquals(
            DbpMessageType.BROKER_DONT_HAVE_MESSAGE_FOR_CONSUMER,
            detector.parse(ByteArrayGeneratorWhis.Broker.ToConsumer.dontHaveMessage())
        )
    }

    @Test
    fun detect_and_parse_allPayloadFrames() {
        val consumerIn = ConsumerMessageInDto(id = 3).apply { deliveryStatus = DeliveryStatus.DELIVERED }
        val consumerOut = ConsumerMessageOutDto(id = 4).apply {
            currentTimestamp = 10
            command = "X"
            believerGuarantee = DeliveryGuarantee.NO
            data = mapOf("k" to "v")
        }
        val producerIn = ProducerMessageInDto().apply {
            messageBirthCertificate = MessageBirthCertificate(1, "p")
            command = "c"
            data = emptyMap()
        }
        val producerOut = ProducerMessageOutDto(8, 9, DeliveryStatus.COMPLETED)

        val cases = listOf(
            ByteArrayGeneratorWhis.FromConsumer.messageIn(consumerIn) to DbpMessageType.CONSUMER_MESSAGE_RECEIVED,
            ByteArrayGeneratorWhis.Broker.ToConsumer.messageOut(consumerOut) to DbpMessageType.BROKER_SEND_MESSAGE_TO_CONSUMER,
            ByteArrayGeneratorWhis.FromConsumer.login("d", "p") to DbpMessageType.CONSUMER_LOGIN,
            ByteArrayGeneratorWhis.Broker.ToConsumer.loginResult(true, "d", Role.DEVISE, "t") to DbpMessageType.BROKER_CONSUMER_LOGIN_RESULT,
            ByteArrayGeneratorWhis.FromProducer.messageIn(producerIn) to DbpMessageType.PRODUCER_SEND_MESSAGE,
            ByteArrayGeneratorWhis.Broker.ToProducer.messageOut(producerOut) to DbpMessageType.BROKER_MESSAGE_RECEIVED_FROM_PRODUCER,
            ByteArrayGeneratorWhis.FromProducer.producerAskMessageStatus(5) to DbpMessageType.PRODUCER_ASK_MESSAGE_STATUS,
            ByteArrayGeneratorWhis.Broker.ToProducer.sendMessageStatus(
                5,
                ProducerDeliveryStatusOutDto(DeliveryStatus.CANCELLED)
            ) to DbpMessageType.BROKER_RETURN_MESSAGE_STATUS_TO_PRODUCER,
            ByteArrayGeneratorWhis.FromProducer.login("d", "p") to DbpMessageType.PRODUCER_LOGIN,
            ByteArrayGeneratorWhis.Broker.ToProducer.loginResult(false) to DbpMessageType.BROKER_PRODUCER_LOGIN_RESULT,
        )

        for ((frame, expectedType) in cases) {
            assertEquals(expectedType, detector.detect(frame), "detect $expectedType")
            val parsed = detector.parse(frame)
            assertNotNull(parsed, "parse $expectedType")
            assertTrue(expectedType.parser != null)
            assertTrue(detector.parserFor(expectedType) === expectedType.parser)
        }
    }

    @Test
    fun parse_returnsTypedPayload() {
        val login = detector.parse(ByteArrayGeneratorWhis.FromConsumer.login("u", "s")) as LoginCredentials
        assertEquals("u", login.deviseId)

        val result = detector.parse(
            ByteArrayGeneratorWhis.Broker.ToConsumer.loginResult(true, "u", Role.MAN, "tok")
        ) as LoginResult
        assertEquals("tok", result.accessToken)

        val status = detector.parse(
            ByteArrayGeneratorWhis.Broker.ToProducer.sendMessageStatus(
                2L,
                ProducerDeliveryStatusOutDto(DeliveryStatus.COMPLETED)
            )
        ) as ProducerDeliveryStatusFrame
        assertEquals(2L, status.messageId)

        val asked = detector.parse(ByteArrayGeneratorWhis.FromProducer.producerAskMessageStatus(2L)) as Long
        assertEquals(2L, asked)
    }

    @Test
    fun detect_rejectsGarbage() {
        assertNull(detector.detect(emptyList()))
        assertNull(detector.detect(listOf(StandardBytes.START_BYTE, 0xFF, 0, 0, StandardBytes.STOP_BYTE)))
        val broken = ByteArrayGeneratorWhis.FromConsumer.askMessage().toMutableList()
        broken[broken.size - 3] = broken[broken.size - 3] xor 0xFF
        assertNull(detector.detect(broken))
    }

    @Test
    fun parser_isNullOnServiceTypes() {
        assertNull(DbpMessageType.CONSUMER_ASK_MESSAGE.parser)
        assertNull(DbpMessageType.BROKER_DONT_HAVE_MESSAGE_FOR_CONSUMER.parser)
        assertNull(detector.parserFor(DbpMessageType.CONSUMER_ASK_MESSAGE))
        assertNull(detector.parserFor(DbpMessageType.BROKER_DONT_HAVE_MESSAGE_FOR_CONSUMER))
    }
}

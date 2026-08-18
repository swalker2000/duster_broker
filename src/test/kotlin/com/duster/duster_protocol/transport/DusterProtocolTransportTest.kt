package com.duster.duster_protocol.transport

import com.duster.database.data.client.Role
import com.duster.database.data.message.DeliveryGuarantee
import com.duster.database.data.message.DeliveryStatus
import com.duster.duster_protocol.messagefactory.bytearray.parse.dto.LoginResult
import com.duster.duster_protocol.transport.brocker.Broker
import com.duster.duster_protocol.transport.brocker.connection.HandlersNotSetException
import com.duster.duster_protocol.transport.brocker.connection.toconsumer.ConnectionToConsumer
import com.duster.duster_protocol.transport.brocker.connection.toproducer.ConnectionToProducer
import com.duster.duster_protocol.transport.client.LoginFailedException
import com.duster.duster_protocol.transport.client.consumer.tcp.ConsumerTcp
import com.duster.duster_protocol.transport.client.producer.tcp.ProducerTcp
import com.duster.transport.data.dto.consumer.ConsumerMessageInDto
import com.duster.transport.data.dto.consumer.ConsumerMessageOutDto
import com.duster.transport.data.dto.producer.ProducerDeliveryStatusOutDto
import com.duster.transport.data.dto.producer.message.MessageBirthCertificate
import com.duster.transport.data.dto.producer.message.ProducerMessageInDto
import com.duster.transport.data.dto.producer.message.ProducerMessageOutDto
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DusterProtocolTransportTest {

    private var broker: Broker? = null

    @AfterEach
    fun tearDown() {
        broker?.stop()
        broker = null
    }

    @Test
    fun connectionRun_throwsIfHandlersMissing() {
        assertThrows(HandlersNotSetException::class.java) {
            ConnectionToProducer("p1").run()
        }
        assertThrows(HandlersNotSetException::class.java) {
            ConnectionToConsumer("c1").run()
        }
    }

    @Test
    fun producer_sendMessageAndAskStatus() {
        val broker = startBroker()
        broker.connectionToProducer.onSendMessage { deviseId, message ->
            assertEquals("producer-1", deviseId)
            assertEquals("digitalWrite", message.command)
            assertEquals(13, (message.data!!["pinNumber"] as Number).toInt())
            ProducerMessageOutDto(id = 501, tmpId = message.messageBirthCertificate?.tmpId, deliveryStatus = DeliveryStatus.NOT_DELIVERED)
        }
        broker.connectionToProducer.onAskMessageStatus { deviseId, messageId ->
            assertEquals("producer-1", deviseId)
            assertEquals(501L, messageId)
            ProducerDeliveryStatusOutDto(DeliveryStatus.DELIVERED)
        }

        val producer = ProducerTcp("producer-1", "127.0.0.1", broker.port, "secret")
        val sent = producer.sendMessage(
            ProducerMessageInDto().apply {
                messageBirthCertificate = MessageBirthCertificate(tmpId = 9, producerDeviseId = "")
                believerGuarantee = DeliveryGuarantee.RECEIPT_CONFIRMATION
                command = "digitalWrite"
                data = mapOf("pinNumber" to 13, "pinValue" to true)
            }
        )
        assertEquals(501L, sent.id)
        assertEquals(9L, sent.tmpId)
        assertEquals(DeliveryStatus.NOT_DELIVERED, sent.deliveryStatus)

        val status = producer.askMessageStatus(501L)
        assertEquals(DeliveryStatus.DELIVERED, status.deliveryStatus)
    }

    @Test
    fun consumer_giveMeMessage_andStatusChanged() {
        val broker = startBroker()
        val outgoing = ConsumerMessageOutDto(id = 42).apply {
            currentTimestamp = 1_700_000_000_000L
            command = "PING"
            believerGuarantee = DeliveryGuarantee.RECEIPT_CONFIRMATION
            data = mapOf("x" to 1)
        }
        val statuses = CopyOnWriteArrayList<Pair<String, Long>>()
        val statusChanged = CountDownLatch(1)
        broker.connectionToConsumer.onConsumerAskMessages { deviseId ->
            assertEquals("sensor-1", deviseId)
            outgoing
        }
        broker.connectionToConsumer.onMessageStatusChanged { deviseId, message ->
            statuses.add(deviseId to message.id)
            assertEquals(DeliveryStatus.COMPLETED, message.deliveryStatus)
            statusChanged.countDown()
        }

        val consumer = ConsumerTcp("sensor-1", "127.0.0.1", broker.port, "pwd")
        val received = consumer.giveMeMessage()
        assertEquals(42L, received!!.id)
        assertEquals("PING", received.command)
        assertEquals(1, (received.data!!["x"] as Number).toInt())

        consumer.messageStatusChanged(ConsumerMessageInDto(id = 42).apply {
            deliveryStatus = DeliveryStatus.COMPLETED
        })
        assertTrue(statusChanged.await(2, TimeUnit.SECONDS))
        assertEquals(listOf("sensor-1" to 42L), statuses)
    }

    @Test
    fun consumer_giveMeMessage_returnsNullWhenEmpty() {
        val broker = startBroker()
        broker.connectionToConsumer.onConsumerAskMessages { null }
        broker.connectionToConsumer.onMessageStatusChanged { _, _ -> }

        val consumer = ConsumerTcp("sensor-2", "127.0.0.1", broker.port)
        assertNull(consumer.giveMeMessage())
    }

    @Test
    fun login_rejectsInvalidPassword() {
        val broker = startBroker()
        broker.onLogin { credentials, _ ->
            LoginResult(
                ok = credentials.password == "ok",
                deviseId = credentials.deviseId,
                role = Role.DEVISE,
                accessToken = "jwt"
            )
        }
        broker.connectionToProducer.onSendMessage { _, _ ->
            ProducerMessageOutDto(1, 0, DeliveryStatus.NOT_DELIVERED)
        }
        broker.connectionToProducer.onAskMessageStatus { _, _ ->
            ProducerDeliveryStatusOutDto(DeliveryStatus.UNKNOWN)
        }

        val producer = ProducerTcp("p", "127.0.0.1", broker.port, "wrong")
        val ex = assertThrows(LoginFailedException::class.java) {
            producer.askMessageStatus(1)
        }
        assertTrue(!ex.result.ok)
    }

    @Test
    fun login_failsWhenProducerHandlersMissing() {
        val broker = Broker()
        this.broker = broker
        broker.start()
        val producer = ProducerTcp("p", "127.0.0.1", broker.port)
        assertThrows(LoginFailedException::class.java) {
            producer.sendMessage(ProducerMessageInDto().apply {
                command = "x"
                data = emptyMap()
            })
        }
    }

    @Test
    fun producer_fillsProducerDeviseIdOnCertificate() {
        val broker = startBroker()
        var captured: String? = null
        broker.connectionToProducer.onSendMessage { _, message ->
            captured = message.messageBirthCertificate?.producerDeviseId
            ProducerMessageOutDto(3, 1, DeliveryStatus.NOT_DELIVERED)
        }
        broker.connectionToProducer.onAskMessageStatus { _, _ ->
            ProducerDeliveryStatusOutDto(DeliveryStatus.UNKNOWN)
        }

        ProducerTcp("device-7", "127.0.0.1", broker.port).sendMessage(
            ProducerMessageInDto().apply {
                messageBirthCertificate = MessageBirthCertificate(1, "")
                command = "c"
                data = emptyMap()
            }
        )
        assertEquals("device-7", captured)
    }

    private fun startBroker(): Broker {
        val broker = Broker()
        this.broker = broker
        broker.connectionToProducer.onSendMessage { _, message ->
            ProducerMessageOutDto(1, message.messageBirthCertificate?.tmpId, DeliveryStatus.NOT_DELIVERED)
        }
        broker.connectionToProducer.onAskMessageStatus { _, _ ->
            ProducerDeliveryStatusOutDto(DeliveryStatus.UNKNOWN)
        }
        broker.connectionToConsumer.onConsumerAskMessages { null }
        broker.connectionToConsumer.onMessageStatusChanged { _, _ -> }
        broker.start()
        return broker
    }
}

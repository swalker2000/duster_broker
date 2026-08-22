package com.duster.duster_protocol.transport

import com.duster.database.data.message.DeliveryGuarantee
import com.duster.database.data.message.DeliveryStatus
import com.duster.duster_protocol.transport.brocker.Broker
import com.duster.duster_protocol.transport.client.LoginFailedException
import com.duster.duster_protocol.transport.client.consumer.tcp.ConsumerTcp
import com.duster.duster_protocol.transport.client.producer.tcp.ProducerTcp
import com.duster.duster_protocol.transport.ssl.DusterProtocolSsl
import com.duster.transport.data.dto.consumer.ConsumerMessageInDto
import com.duster.transport.data.dto.consumer.ConsumerMessageOutDto
import com.duster.transport.data.dto.producer.ProducerDeliveryStatusOutDto
import com.duster.transport.data.dto.producer.message.MessageBirthCertificate
import com.duster.transport.data.dto.producer.message.ProducerMessageInDto
import com.duster.transport.data.dto.producer.message.ProducerMessageOutDto
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLSocketFactory

class DusterProtocolTlsTransportTest {

    private var broker: Broker? = null

    @AfterEach
    fun tearDown() {
        broker?.stop()
        broker = null
    }

    @Test
    fun producer_sendMessageAndAskStatus_overTls() {
        val (broker, clientFactory) = startTlsBroker()
        broker.connectionToProducer.onSendMessage { deviseId, message ->
            assertEquals("producer-tls", deviseId)
            assertEquals("digitalWrite", message.command)
            ProducerMessageOutDto(id = 801, tmpId = message.messageBirthCertificate?.tmpId, deliveryStatus = DeliveryStatus.NOT_DELIVERED)
        }
        broker.connectionToProducer.onAskMessageStatus { deviseId, messageId ->
            assertEquals("producer-tls", deviseId)
            assertEquals(801L, messageId)
            ProducerDeliveryStatusOutDto(DeliveryStatus.DELIVERED)
        }

        val producer = tlsProducer("producer-tls", broker.tlsPort, clientFactory, "secret")
        val sent = producer.sendMessage(
            ProducerMessageInDto().apply {
                messageBirthCertificate = MessageBirthCertificate(tmpId = 4, producerDeviseId = "")
                believerGuarantee = DeliveryGuarantee.RECEIPT_CONFIRMATION
                command = "digitalWrite"
                data = mapOf("pinNumber" to 13, "pinValue" to true)
            }
        )
        assertEquals(801L, sent.id)
        assertEquals(4L, sent.tmpId)
        assertEquals(DeliveryStatus.NOT_DELIVERED, sent.deliveryStatus)
        assertEquals(DeliveryStatus.DELIVERED, producer.askMessageStatus(801L).deliveryStatus)
    }

    @Test
    fun consumer_giveMeMessage_andStatusChanged_overTls() {
        val (broker, clientFactory) = startTlsBroker()
        val outgoing = ConsumerMessageOutDto(id = 77).apply {
            currentTimestamp = 1_700_000_000_000L
            command = "PING"
            believerGuarantee = DeliveryGuarantee.RECEIPT_CONFIRMATION
            data = mapOf("x" to 2)
        }
        val statuses = CopyOnWriteArrayList<Pair<String, Long>>()
        val statusChanged = CountDownLatch(1)
        broker.connectionToConsumer.onConsumerAskMessages { deviseId ->
            assertEquals("sensor-tls", deviseId)
            outgoing
        }
        broker.connectionToConsumer.onMessageStatusChanged { deviseId, message ->
            statuses.add(deviseId to message.id)
            assertEquals(DeliveryStatus.COMPLETED, message.deliveryStatus)
            statusChanged.countDown()
        }

        val consumer = tlsConsumer("sensor-tls", broker.tlsPort, clientFactory, "pwd")
        val received = consumer.giveMeMessage()
        assertEquals(77L, received!!.id)
        assertEquals("PING", received.command)

        consumer.messageStatusChanged(ConsumerMessageInDto(id = 77).apply {
            deliveryStatus = DeliveryStatus.COMPLETED
        })
        assertTrue(statusChanged.await(2, TimeUnit.SECONDS))
        assertEquals(listOf("sensor-tls" to 77L), statuses)
    }

    @Test
    fun login_rejectsInvalidPassword_overTls() {
        val (broker, clientFactory) = startTlsBroker()
        broker.onLogin { credentials, _ ->
            com.duster.duster_protocol.messagefactory.bytearray.parse.dto.LoginResult(
                ok = credentials.password == "ok",
                deviseId = credentials.deviseId,
                role = com.duster.database.data.client.Role.DEVISE,
                accessToken = "jwt"
            )
        }

        val producer = tlsProducer("p", broker.tlsPort, clientFactory, "wrong")
        val ex = assertThrows(LoginFailedException::class.java) {
            producer.askMessageStatus(1)
        }
        assertTrue(!ex.result.ok)
    }

    @Test
    fun plain_and_tls_listeners_work_together() {
        val keyStore = DusterProtocolSsl.selfSignedKeyStore()
        val password = DusterProtocolSsl.DEFAULT_KEYSTORE_PASSWORD.toCharArray()
        val broker = Broker(
            bindPort = 0,
            enablePlain = true,
            tlsBindPort = 0,
            sslContext = DusterProtocolSsl.serverContext(keyStore, password)
        )
        this.broker = broker
        wireDefaultHandlers(broker)
        broker.start()

        val clientFactory = DusterProtocolSsl.trustingServer(keyStore).socketFactory
        val plainProducer = ProducerTcp("plain-p", "127.0.0.1", broker.port)
        val tlsProducer = tlsProducer("tls-p", broker.tlsPort, clientFactory)

        val plainAck = plainProducer.sendMessage(ProducerMessageInDto().apply {
            command = "plain"
            data = emptyMap()
        })
        val tlsAck = tlsProducer.sendMessage(ProducerMessageInDto().apply {
            command = "tls"
            data = emptyMap()
        })
        assertEquals(DeliveryStatus.NOT_DELIVERED, plainAck.deliveryStatus)
        assertEquals(DeliveryStatus.NOT_DELIVERED, tlsAck.deliveryStatus)
    }

    @Test
    fun plain_client_cannot_speak_tls_port() {
        val (broker, _) = startTlsBroker()
        val producer = ProducerTcp("p", "127.0.0.1", broker.tlsPort)
        assertThrows(Exception::class.java) {
            producer.sendMessage(ProducerMessageInDto().apply {
                command = "x"
                data = emptyMap()
            })
        }
    }

    @Test
    fun untrusted_tls_client_is_rejected() {
        val (broker, _) = startTlsBroker()
        val producer = ProducerTcp(
            "p",
            "127.0.0.1",
            broker.tlsPort,
            useTls = true,
            insecureTls = false
        )
        val ex = assertThrows(Exception::class.java) {
            producer.sendMessage(ProducerMessageInDto().apply {
                command = "x"
                data = emptyMap()
            })
        }
        assertTrue(
            generateSequence<Throwable>(ex) { it.cause }.any { it is SSLHandshakeException },
            "expected SSLHandshakeException, got $ex"
        )
    }

    private fun startTlsBroker(): Pair<Broker, SSLSocketFactory> {
        val keyStore = DusterProtocolSsl.selfSignedKeyStore()
        val password = DusterProtocolSsl.DEFAULT_KEYSTORE_PASSWORD.toCharArray()
        val broker = Broker(
            enablePlain = false,
            tlsBindPort = 0,
            sslContext = DusterProtocolSsl.serverContext(keyStore, password)
        )
        this.broker = broker
        wireDefaultHandlers(broker)
        broker.start()
        return broker to DusterProtocolSsl.trustingServer(keyStore).socketFactory
    }

    private fun wireDefaultHandlers(broker: Broker) {
        broker.connectionToProducer.onSendMessage { _, message ->
            ProducerMessageOutDto(1, message.messageBirthCertificate?.tmpId, DeliveryStatus.NOT_DELIVERED)
        }
        broker.connectionToProducer.onAskMessageStatus { _, _ ->
            ProducerDeliveryStatusOutDto(DeliveryStatus.UNKNOWN)
        }
        broker.connectionToConsumer.onConsumerAskMessages { null }
        broker.connectionToConsumer.onMessageStatusChanged { _, _ -> }
    }

    private fun tlsProducer(
        deviseId: String,
        port: Int,
        factory: SSLSocketFactory,
        password: String = ""
    ) = ProducerTcp(
        deviseId,
        "127.0.0.1",
        port,
        password,
        useTls = true,
        insecureTls = false,
        sslSocketFactory = factory
    )

    private fun tlsConsumer(
        deviseId: String,
        port: Int,
        factory: SSLSocketFactory,
        password: String = ""
    ) = ConsumerTcp(
        deviseId,
        "127.0.0.1",
        port,
        password,
        useTls = true,
        insecureTls = false,
        sslSocketFactory = factory
    )
}

package com.duster

import com.duster.database.data.DeliveryGuarantee
import com.duster.database.data.DeliveryStatus
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import org.springframework.beans.factory.annotation.Autowired
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

@SpringBootTest
class SmokeMqttTest {

    private val om = jacksonObjectMapper()

    @Autowired
    private lateinit var env: Environment

    @Test
    fun `smoke mqtt send and receive (README_RUS)`() {
        assertTrue(env.activeProfiles.contains("test"), "Должен быть активен профиль test. activeProfiles=${env.activeProfiles.joinToString(",")}")

        val brokerUrl = System.getProperty("mqtt.broker.url")
        assertNotNull(brokerUrl, "mqtt.broker.url должен быть выставлен тестовым окружением")
        val brokerUrlFromSpring = env.getProperty("mqtt.broker.url")
        assertNotNull(brokerUrlFromSpring, "Spring должен видеть свойство mqtt.broker.url")

        val deviceId = "device123"
        val producerDeviceId = "0"
        val tmpId = 3

        val topicProducerRequest = "producer/request/$deviceId"
        val topicConsumerRequest = "consumer/request/$deviceId"
        val topicConsumerResponse = "consumer/response/$deviceId"
        val topicProducerResponse = "producer/response/$producerDeviceId"

        val latchNotDelivered = CountDownLatch(1)
        val latchConsumerRequest = CountDownLatch(1)
        val latchDelivered = CountDownLatch(1)
        val latchPing = CountDownLatch(1)

        val receivedConsumerOut = AtomicReference<JsonNode>()
        val receivedProducerNotDelivered = AtomicReference<JsonNode>()
        val receivedProducerDelivered = AtomicReference<JsonNode>()
        val trace = ConcurrentLinkedQueue<String>()

        val client = MqttClient(brokerUrl, "smoke-${UUID.randomUUID()}")
        client.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) = Unit

            override fun messageArrived(topic: String, message: MqttMessage) {
                val payload = message.payload.toString(StandardCharsets.UTF_8)
                trace.add("IN $topic $payload")
                if (topic == "smoke/ping") {
                    latchPing.countDown()
                    return
                }
                val json = runCatching { om.readTree(payload) }.getOrNull() ?: return

                when (topic) {
                    topicConsumerRequest -> {
                        receivedConsumerOut.compareAndSet(null, json)
                        latchConsumerRequest.countDown()
                    }
                    topicProducerResponse -> {
                        val status = json.get("deliveryStatus")?.asText()
                        when (status) {
                            DeliveryStatus.NOT_DELIVERED.name -> {
                                receivedProducerNotDelivered.compareAndSet(null, json)
                                latchNotDelivered.countDown()
                            }
                            DeliveryStatus.DELIVERED.name -> {
                                receivedProducerDelivered.compareAndSet(null, json)
                                latchDelivered.countDown()
                            }
                        }
                    }
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit
        })

        val options = MqttConnectOptions().apply {
            isCleanSession = true
            connectionTimeout = 5
            keepAliveInterval = 5
            isAutomaticReconnect = true
        }

        client.connect(options)
        try {
            client.subscribe(arrayOf("smoke/ping", topicConsumerRequest, topicProducerResponse), intArrayOf(1, 1, 1))

            client.publish(
                "smoke/ping",
                "ping".toByteArray(StandardCharsets.UTF_8),
                0,
                false
            )
            assertTrue(
                latchPing.await(3, TimeUnit.SECONDS),
                "Не смогли проверить соединение с брокером ($brokerUrl): не получили ping. Trace: ${trace.joinToString(" | ")}"
            )

            // Даем Spring Integration MQTT время подключиться/подписаться к брокеру.
            Thread.sleep(500)

            val producerMessageInJson = om.createObjectNode().apply {
                put("believerGuarantee", DeliveryGuarantee.RECEIPT_CONFIRMATION.name)
                put("command", "digitalWrite")
                set<ObjectNode>("messageBirthCertificate", om.createObjectNode().apply {
                    put("tmpId", tmpId)
                    put("producerDeviseId", producerDeviceId)
                })
                set<ObjectNode>("data", om.createObjectNode().apply {
                    put("pinNumber", 13)
                    put("pinValue", true)
                })
            }

            client.publish(
                topicProducerRequest,
                producerMessageInJson.toString().toByteArray(StandardCharsets.UTF_8),
                1,
                false
            )

            val deadlineNs = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
            while (System.nanoTime() < deadlineNs &&
                (latchNotDelivered.count > 0L || latchConsumerRequest.count > 0L)
            ) {
                latchNotDelivered.await(100, TimeUnit.MILLISECONDS)
                latchConsumerRequest.await(100, TimeUnit.MILLISECONDS)
            }
            assertTrue(
                latchNotDelivered.count == 0L,
                "Не получили NOT_DELIVERED в $topicProducerResponse. Получено: ${trace.joinToString(" | ")}"
            )
            assertTrue(
                latchConsumerRequest.count == 0L,
                "Не получили сообщение для consumer в $topicConsumerRequest. Получено: ${trace.joinToString(" | ")}"
            )

            val consumerOut = receivedConsumerOut.get()
            val messageId = consumerOut.get("id")?.asInt()
            assertNotNull(messageId, "В consumer/request должен быть id")

            val producerNotDelivered = receivedProducerNotDelivered.get()
            assertEquals(tmpId, producerNotDelivered.get("tmpId")?.asInt())
            assertEquals(DeliveryStatus.NOT_DELIVERED.name, producerNotDelivered.get("deliveryStatus")?.asText())

            val consumerResponseJson = om.createObjectNode().apply {
                put("id", messageId!!)
                // deliveryStatus опционален; если не передать, сервис считает DELIVERED.
                put("deliveryStatus", DeliveryStatus.DELIVERED.name)
            }

            client.publish(
                topicConsumerResponse,
                consumerResponseJson.toString().toByteArray(StandardCharsets.UTF_8),
                1,
                false
            )

            assertTrue(
                latchDelivered.await(10, TimeUnit.SECONDS),
                "Не получили DELIVERED в $topicProducerResponse"
            )

            val producerDelivered = receivedProducerDelivered.get()
            assertEquals(messageId, producerDelivered.get("id")?.asInt())
            assertEquals(tmpId, producerDelivered.get("tmpId")?.asInt())
            assertEquals(DeliveryStatus.DELIVERED.name, producerDelivered.get("deliveryStatus")?.asText())
        } finally {
            runCatching { client.disconnect() }
            runCatching { client.close() }
        }
    }
}


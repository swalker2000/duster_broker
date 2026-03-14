package com.duster.pd

import com.duster.database.data.DeliveryGuarantee
import com.duster.database.data.DeliveryStatus
import com.duster.messagehandler.data.dto.consumer.ConsumerMessageInDto
import com.duster.messagehandler.data.dto.producer.message.MessageBirthCertificate
import com.duster.messagehandler.data.dto.producer.message.ProducerMessageInDto
import com.duster.pd.mqtt.ConsumerMqtt
import com.duster.pd.mqtt.ProducerMqtt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import kotlin.test.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SpringBootTest
class SmokeTestPd {

    @Autowired
    private lateinit var env: Environment

    private fun brokerUrl(): String {
        assertTrue(env.activeProfiles.contains("test"), "Должен быть активен профиль test")
        return System.getProperty("mqtt.broker.url")
            ?: env.getProperty("mqtt.broker.url")
            ?: throw IllegalStateException("mqtt.broker.url должен быть выставлен тестовым окружением")
    }

    /**
     * Передача данных из producer в consumer без подписки.
     * Producer не передаёт messageBirthCertificate — сервис не шлёт уведомления в producer/response.
     */
    @Test
    fun fromProducerToConsumerNoSubscribeTest() {
        val url = brokerUrl()
        val deviceId = "pd-device-no-sub-${System.currentTimeMillis()}"

        val consumer = ConsumerMqtt(url, deviceId)
        val producer = ProducerMqtt(url, deviceId)

        val latchMessage = CountDownLatch(1)
        var receivedMessage: com.duster.messagehandler.data.dto.consumer.ConsumerMessageOutDto? = null

        consumer.subscribeNewMessage { msg ->
            receivedMessage = msg
            latchMessage.countDown()
        }

        consumer.connect()
        producer.connect()
        try {
            Thread.sleep(500)

            val message = ProducerMessageInDto().apply {
                believerGuarantee = DeliveryGuarantee.RECEIPT_CONFIRMATION
                command = "digitalWrite"
                data = mapOf("pinNumber" to 13, "pinValue" to true)
            }

            producer.publish(message, null)

            assertTrue(
                latchMessage.await(10, TimeUnit.SECONDS),
                "Consumer должен получить сообщение"
            )

            assertNotNull(receivedMessage)
            val msg = receivedMessage!!
            assertEquals("digitalWrite", msg.command)
            assertEquals(13, (msg.data?.get("pinNumber") as? Number)?.toInt())
            assertEquals(true, msg.data?.get("pinValue"))

            val messageId = msg.id.toInt()
            consumer.sendResponse(ConsumerMessageInDto(id = messageId).apply {
                deliveryStatus = DeliveryStatus.DELIVERED
            })
        } finally {
            consumer.disconnect()
            producer.disconnect()
        }
    }

    /**
     * Передача данных из producer в consumer с подпиской.
     * 1. Сообщение отправлено (consumer получил)
     * 2. Сообщение доставлено (producer получил DELIVERED)
     * 3. Сообщение обработано (producer получил COMPLETED)
     */
    @Test
    fun fromProducerToConsumerWhisSubscribeTest() {
        val url = brokerUrl()
        val deviceId = "pd-device-with-sub-${System.currentTimeMillis()}"
        val tmpId = 42

        val consumer = ConsumerMqtt(url, deviceId)
        val producer = ProducerMqtt(url, deviceId, producerDeviceId = "0")

        val latchMessage = CountDownLatch(1)
        val latchDelivered = CountDownLatch(1)
        val latchCompleted = CountDownLatch(1)
        var receivedMessage: com.duster.messagehandler.data.dto.consumer.ConsumerMessageOutDto? = null

        consumer.subscribeNewMessage { msg ->
            receivedMessage = msg
            latchMessage.countDown()
        }

        consumer.connect()
        producer.connect()
        try {
            Thread.sleep(500)

            val message = ProducerMessageInDto().apply {
                believerGuarantee = DeliveryGuarantee.RECEIPT_CONFIRMATION
                command = "digitalWrite"
                messageBirthCertificate = MessageBirthCertificate(tmpId, "0")
                data = mapOf("pinNumber" to 13, "pinValue" to true)
            }

            producer.publish(message, object : Producer.OnMessageStatusChange {
                override fun newStatusEvent(dto: com.duster.messagehandler.data.dto.producer.message.ProducerMessageOutDto) {
                    when (dto.deliveryStatus) {
                        DeliveryStatus.DELIVERED -> latchDelivered.countDown()
                        DeliveryStatus.COMPLETED -> latchCompleted.countDown()
                        else -> {}
                    }
                }
            })
            assertTrue(latchMessage.await(10, TimeUnit.SECONDS), "1. Сообщение должно быть отправлено")
            val msg = receivedMessage!!
            val messageId = msg.id.toInt()

            consumer.sendResponse(ConsumerMessageInDto(id = messageId).apply {
                deliveryStatus = DeliveryStatus.DELIVERED
            })

            assertTrue(latchDelivered.await(10, TimeUnit.SECONDS), "2. Сообщение должно быть доставлено")

            consumer.sendResponse(ConsumerMessageInDto(id = messageId).apply {
                deliveryStatus = DeliveryStatus.COMPLETED
            })

            assertTrue(latchCompleted.await(10, TimeUnit.SECONDS), "3. Сообщение должно быть обработано")
        } finally {
            consumer.disconnect()
            producer.disconnect()
        }
    }
}

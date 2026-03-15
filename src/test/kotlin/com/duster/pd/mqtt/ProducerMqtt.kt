package com.duster.pd.mqtt

import com.duster.database.data.DeliveryStatus
import com.duster.messagehandler.data.dto.producer.message.ProducerMessageInDto
import com.duster.messagehandler.data.dto.producer.message.ProducerMessageOutDto
import com.duster.pd.Producer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.nio.charset.StandardCharsets

class ProducerMqtt(
    brokerUrl: String,
    override val deviceId: String = "0"
) : Producer {

    private val om = jacksonObjectMapper()
    private val client = MqttClient(brokerUrl, "producer-${deviceId}-${System.currentTimeMillis()}")

    private var statusChangeHandler: Producer.OnMessageStatusChange? = null

    init {
        client.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) = Unit

            override fun messageArrived(topic: String, message: MqttMessage) {
                if (topic != "producer/response/$deviceId") return
                val payload = message.payload.toString(StandardCharsets.UTF_8)
                runCatching {
                    val json = om.readTree(payload)
                    val id = json.get("id")?.asInt() ?: return@runCatching
                    val tmpId = json.get("tmpId")?.asInt()
                    val statusStr = json.get("deliveryStatus")?.asText() ?: return@runCatching
                    val status = DeliveryStatus.valueOf(statusStr)
                    statusChangeHandler?.newStatusEvent(ProducerMessageOutDto(id, tmpId, status))
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit
        })
    }

    override fun connect() {
        if (!client.isConnected) {
            client.connect(MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 5
                keepAliveInterval = 5
                isAutomaticReconnect = true
            })
            client.subscribe("producer/response/$deviceId", 1)
        }
    }

    override fun disconnect() {
        runCatching { client.disconnect() }
        runCatching { client.close() }
    }

    override fun publish(consumerDeviseId: String, message: ProducerMessageInDto, onMessageStatusChange: Producer.OnMessageStatusChange?) {
        statusChangeHandler = onMessageStatusChange
        val topic = "producer/request/$consumerDeviseId"
        val payload = om.writeValueAsString(message)
        client.publish(topic, payload.toByteArray(StandardCharsets.UTF_8), 1, false)
    }
}

package com.duster.pd.mqtt

import com.duster.transport.data.dto.consumer.ConsumerMessageInDto
import com.duster.transport.data.dto.consumer.ConsumerMessageOutDto
import com.duster.pd.Consumer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.nio.charset.StandardCharsets

class ConsumerMqtt(brokerUrl: String, deviseId: String) : Consumer(deviseId) {

    private val om = jacksonObjectMapper()
    private val client = MqttClient(brokerUrl, "consumer-$deviseId-${System.currentTimeMillis()}")

    init {
        client.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) = Unit

            override fun messageArrived(topic: String, message: MqttMessage) {
                if (topic != "consumer/request/$deviseId") return
                val payload = message.payload.toString(StandardCharsets.UTF_8)
                runCatching {
                    val dto = om.readValue(payload, ConsumerMessageOutDto::class.java)
                    onNewMessage(dto)
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
            client.subscribe("consumer/request/$deviseId", 1)
        }
    }

    override fun disconnect() {
        runCatching { client.disconnect() }
        runCatching { client.close() }
    }

    override fun sendResponse(response: ConsumerMessageInDto) {
        val topic = "consumer/response/$deviseId"
        val payload = om.writeValueAsString(response)
        client.publish(topic, payload.toByteArray(StandardCharsets.UTF_8), 1, false)
    }
}

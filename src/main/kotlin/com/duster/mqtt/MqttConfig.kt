package com.duster.mqtt

import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.channel.DirectChannel
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory
import org.springframework.integration.mqtt.core.MqttPahoClientFactory
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageHandler

@Configuration
class MqttConfig {

    @Value("\${mqtt.broker.url}")
    private lateinit var brokerUrl: String

    @Value("\${mqtt.username:}")
    private lateinit var username: String

    @Value("\${mqtt.password:}")
    private lateinit var password: String

    @Value("\${mqtt.qos:1}")
    private var qos: Int = 1

    @Bean
    fun mqttClientFactory(): MqttPahoClientFactory {
        val factory = DefaultMqttPahoClientFactory()
        val options = MqttConnectOptions()
        options.serverURIs = arrayOf(brokerUrl)
        if (username.isNotBlank()) {
            options.userName = username
        }
        if (password.isNotBlank()) {
            options.password = password.toCharArray()
        }
        options.isCleanSession = true
        factory.connectionOptions = options
        return factory
    }


    /**
     * Входной канал (из inbound в обработчик)
     */
    @Bean
    fun inputChannel(): MessageChannel {
        return DirectChannel()
    }

    /**
     * Выходной канал (из обработчика в outbound)
     */
    @Bean
    fun outputChannel(): MessageChannel {
        return DirectChannel()
    }

    /**
     * Inbound-адаптер: подписка на входной топик
     */
    @Bean
    fun mqttInboundAdapter(clientFactory: MqttPahoClientFactory, inputChannel: MessageChannel): MqttPahoMessageDrivenChannelAdapter {
        val adapter = MqttPahoMessageDrivenChannelAdapter("inbound-client-${System.currentTimeMillis()}", clientFactory, "input/topic")
        adapter.outputChannel = inputChannel
        adapter.setConverter(DefaultPahoMessageConverter())
        adapter.setQos(qos)
        return adapter
    }


    /**
     * Outbound-адаптер: отправка в выходной топик
     */
    @Bean
    @org.springframework.integration.annotation.ServiceActivator(inputChannel = "outputChannel")
    fun mqttOutboundHandler(clientFactory: MqttPahoClientFactory): MessageHandler {
        val handler = MqttPahoMessageHandler("outbound-client-${System.currentTimeMillis()}", clientFactory)
        handler.setAsync(true)
        handler.setDefaultTopic("output/topic")
        handler.setDefaultQos(qos)
        return handler
    }
}
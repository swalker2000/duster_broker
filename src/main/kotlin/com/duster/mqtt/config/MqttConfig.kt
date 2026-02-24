package com.duster.mqtt.config

import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.integration.channel.DirectChannel
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory
import org.springframework.integration.mqtt.core.MqttPahoClientFactory
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageHandler
import org.springframework.messaging.converter.JacksonJsonMessageConverter
import java.security.SecureRandom
import javax.net.ssl.SSLContext
import kotlin.math.log

@Configuration
class MqttConfig {

    @Value("\${mqtt.broker.url}")
    private lateinit var brokerUrl: String

    @Value("\${mqtt.broker.username:}")
    private lateinit var username: String

    @Value("\${mqtt.broker.password:}")
    private lateinit var password: String

    @Value("\${mqtt.qos:1}")
    private var qos: Int = 1

    @Value("\${mqtt.ssl.insecure:false}")   // ← берём из yaml, по умолчанию false
    private var insecureSsl: Boolean = false

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
        // ─────────────────────────────── TLS + отключение проверки ───────────────────────────────
        if (brokerUrl.startsWith("ssl://")) {
            try {
                if (insecureSsl) {

                    // Полностью отключаем проверку сертификата (Validate certificate: false)
                    val sslContext = SSLContext.getInstance("TLS")
                    sslContext.init(null, arrayOf(TrustAllTrustManager), SecureRandom())
                    options.socketFactory = sslContext.socketFactory

                    // Отключаем проверку hostname (часто тоже нужно)
                    options.isHttpsHostnameVerificationEnabled = false
                }
                // Если insecure = false → здесь можно добавить нормальный TrustManager с CA, но пока не трогаем
            } catch (e: Exception) {
                throw RuntimeException("SSL settings error", e)
            }
        }
        // ────────────────────────────────────────────────────────────────────────────────────────
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
        val adapter = MqttPahoMessageDrivenChannelAdapter(
            "inbound-client-${System.currentTimeMillis()}",
            clientFactory
        )
        adapter.addTopic("producer/request/#")
        adapter.addTopic("consumer/response/#")
        adapter.setOutputChannel(inputChannel)
        adapter.setConverter(DefaultPahoMessageConverter())
        adapter.setQos(qos)
        return adapter
    }


    /**
     * Outbound-адаптер: отправка в выходной топик
     */
    @Bean
    @ServiceActivator(inputChannel = "outputChannel")
    fun mqttOutboundHandler(clientFactory: MqttPahoClientFactory): MessageHandler {
        val handler = MqttPahoMessageHandler("outbound-client-${System.currentTimeMillis()}", clientFactory)
        handler.setAsync(true)
        handler.setDefaultTopic("output/topic")
        handler.setDefaultQos(qos)
        return handler
    }
}
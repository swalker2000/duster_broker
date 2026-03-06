package com.duster.testsupport

import io.moquette.broker.Server
import io.moquette.broker.config.IConfig
import io.moquette.broker.config.MemoryConfig
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean

object TestEnvironment {
    private val started = AtomicBoolean(false)
    private var server: Server? = null
    private const val MQTT_PORT = 18884

    @Synchronized
    fun startIfNeeded() {
        if (!started.compareAndSet(false, true)) return

        System.setProperty("spring.profiles.active", mergeProfiles(System.getProperty("spring.profiles.active"), "test"))
        System.setProperty("mqtt.broker.url", "tcp://127.0.0.1:$MQTT_PORT")
        System.setProperty("mqtt.broker.username", "")
        System.setProperty("mqtt.broker.password", "")
        System.setProperty("mqtt.ssl.insecure", "true")

        val props = Properties().apply {
            put(IConfig.HOST_PROPERTY_NAME, "127.0.0.1")
            put(IConfig.PORT_PROPERTY_NAME, MQTT_PORT.toString())
            put(IConfig.ALLOW_ANONYMOUS_PROPERTY_NAME, "true")
            put(IConfig.PERSISTENCE_ENABLED_PROPERTY_NAME, "false")
        }

        val broker = Server()
        try {
            broker.startServer(MemoryConfig(props))
        } catch (e: Exception) {
            throw IllegalStateException("Не удалось поднять test MQTT broker на порту $MQTT_PORT (возможно порт занят).", e)
        }
        server = broker
    }

    @Synchronized
    fun stop() {
        runCatching { server?.stopServer() }
        server = null
    }

    private fun mergeProfiles(existing: String?, required: String): String {
        val set = existing
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toMutableSet()
            ?: mutableSetOf()
        set.add(required)
        return set.joinToString(",")
    }
}


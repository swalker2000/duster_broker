package com.duster.testsupport

import io.moquette.broker.Server
import io.moquette.broker.config.IConfig
import io.moquette.broker.config.MemoryConfig
import java.net.ServerSocket
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean

object TestEnvironment {
    private val started = AtomicBoolean(false)
    private var server: Server? = null

    @Synchronized
    fun startIfNeeded() {
        if (!started.compareAndSet(false, true)) return

        val port = findFreePort()

        System.setProperty("spring.profiles.active", mergeProfiles(System.getProperty("spring.profiles.active"), "test"))
        System.setProperty("mqtt.broker.url", "tcp://127.0.0.1:$port")
        System.setProperty("mqtt.broker.username", "")
        System.setProperty("mqtt.broker.password", "")
        System.setProperty("mqtt.ssl.insecure", "true")

        val props = Properties().apply {
            put(IConfig.HOST_PROPERTY_NAME, "127.0.0.1")
            put(IConfig.PORT_PROPERTY_NAME, port.toString())
            put(IConfig.ALLOW_ANONYMOUS_PROPERTY_NAME, "true")
            put(IConfig.PERSISTENCE_ENABLED_PROPERTY_NAME, "false")
        }

        val broker = Server()
        broker.startServer(MemoryConfig(props))
        server = broker
    }

    @Synchronized
    fun stop() {
        runCatching { server?.stopServer() }
        server = null
    }

    private fun findFreePort(): Int =
        ServerSocket(0).use { it.localPort }

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


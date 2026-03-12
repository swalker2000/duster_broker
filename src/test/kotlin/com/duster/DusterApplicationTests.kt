package com.duster

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@SpringBootTest
@Testcontainers
class DusterApplicationTests {

    companion object {
        @Container
        @JvmStatic
        val mqttContainer: GenericContainer<*> = GenericContainer(
            DockerImageName.parse("eclipse-mosquitto:2.0")
        ).withExposedPorts(1883)

        @JvmStatic
        @DynamicPropertySource
        fun registerMqttProperties(registry: DynamicPropertyRegistry) {
            registry.add("mqtt.broker.url") {
                "tcp://${mqttContainer.host}:${mqttContainer.getMappedPort(1883)}"
            }
            registry.add("mqtt.broker.username") { "" }
            registry.add("mqtt.broker.password") { "" }
            registry.add("mqtt.ssl.insecure") { false }
        }
    }

    @Test
    fun contextLoads() {
    }
}

package com.duster.transport.duster

import com.duster.common.CommonMessageService
import com.duster.database.ClientRepository
import com.duster.database.data.client.Role
import com.duster.duster_protocol.messagefactory.bytearray.parse.dto.LoginCredentials
import com.duster.duster_protocol.messagefactory.bytearray.parse.dto.LoginResult
import com.duster.duster_protocol.transport.brocker.Broker
import com.duster.security.AppSecurityProperties
import com.duster.security.ClientPasswords
import com.duster.security.JwtService
import com.duster.transport.data.dto.producer.message.ProducerMessageInDto
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

/**
 * TCP-обработчик протокола duster_broker (аналог [com.duster.transport.rest.RestMessageHandler]
 * и [com.duster.transport.rest.RestServiceMessageHandler]).
 *
 * Продюсер логинится с `deviseId` целевого consumer (как `{deviceId}` в REST-пути)
 * и отправляет [ProducerMessageInDto]. Консьюмер забирает самое старое сообщение
 * и сообщает смену статуса.
 */
@Service
class DusterTcpMessageHandler(
    private val commonMessageService: CommonMessageService,
    private val appSecurityProperties: AppSecurityProperties,
    private val clientRepository: ClientRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    @Value("\${duster.protocol.tcp.port:9091}") private val bindPort: Int
) {

    private val logger = LoggerFactory.getLogger(DusterTcpMessageHandler::class.java)

    private lateinit var broker: Broker

    val port: Int
        get() = broker.port

    @PostConstruct
    fun start() {
        val server = Broker(bindPort)
        server.onLogin { credentials, _ -> authenticate(credentials) }
        server.connectionToProducer.onSendMessage { deviceId, message ->
            logger.info("newProducerMessageIn [$deviceId]")
            commonMessageService.newProducerMessageIn(prepareProducerMessage(message), deviceId).orElseThrow {
                ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error create message to: $deviceId")
            }
        }
        server.connectionToProducer.onAskMessageStatus { _, messageId ->
            commonMessageService.getDeliveryStatusStatus(messageId).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found for id: $messageId")
            }
        }
        server.connectionToConsumer.onConsumerAskMessages { deviceId ->
            commonMessageService.getOldestMessageToConsumer(deviceId).orElse(null)
        }
        server.connectionToConsumer.onMessageStatusChanged { deviceId, message ->
            logger.info("newConsumerMessageIn [$deviceId]")
            commonMessageService.newConsumerMessageIn(message)
        }
        server.start()
        broker = server
        logger.info("Duster TCP protocol listening on port {}", port)
    }

    @PreDestroy
    fun stop() {
        if (::broker.isInitialized) {
            broker.stop()
        }
    }

    private fun authenticate(credentials: LoginCredentials): LoginResult {
        if (appSecurityProperties.permitAll) {
            return LoginResult(
                ok = true,
                deviseId = credentials.deviseId,
                role = Role.DEVISE,
                accessToken = ""
            )
        }
        val deviseId = credentials.deviseId.trim()
        if (deviseId.isBlank() || credentials.password.isBlank()) {
            return LoginResult(ok = false, deviseId = deviseId, role = Role.DEVISE, accessToken = "")
        }
        val client = clientRepository.findByDeviseId(deviseId)
            ?: return LoginResult(ok = false, deviseId = deviseId, role = Role.DEVISE, accessToken = "")
        if (!ClientPasswords.matches(credentials.password, client.password, passwordEncoder)) {
            return LoginResult(ok = false, deviseId = deviseId, role = Role.DEVISE, accessToken = "")
        }
        return LoginResult(
            ok = true,
            deviseId = client.deviseId,
            role = client.role,
            accessToken = jwtService.createToken(client.deviseId, client.role)
        )
    }

    /**
     * Парсер всегда кладёт certificate с `tmpId=0` и пустым producer id, если поля не было.
     * Для CommonMessageService это должно оставаться «без подписки».
     */
    private fun prepareProducerMessage(message: ProducerMessageInDto): ProducerMessageInDto {
        val cert = message.messageBirthCertificate ?: return message
        if (cert.tmpId == 0L && cert.producerDeviseId.isBlank()) {
            message.messageBirthCertificate = null
        }
        return message
    }
}

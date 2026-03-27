package com.duster.pd.rest

import com.duster.database.data.message.DeliveryStatus
import com.duster.transport.data.dto.producer.ProducerDeliveryStatusOutDto
import com.duster.transport.data.dto.producer.message.ProducerMessageInDto
import com.duster.transport.data.dto.producer.message.ProducerMessageOutDto
import com.duster.pd.Producer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

/**
 * Producer поверх HTTP REST API брокера (аналог [com.duster.pd.mqtt.ProducerMqtt]).
 *
 * @param brokerUrl базовый URL сервиса, например `http://127.0.0.1:8080` (без завершающего `/`)
 * @param bearerToken если задан, добавляется заголовок `Authorization: Bearer …` (профиль с security)
 */
class ProducerRest(
    brokerUrl: String,
    override val deviceId: String = "0",
    private val bearerToken: String? = null
) : Producer {

    private val root = brokerUrl.trimEnd('/')
    private val om = jacksonObjectMapper()
    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private val statusChangeHandlerRef = AtomicReference<Producer.OnMessageStatusChange?>(null)
    private val pollThreadRef = AtomicReference<Thread?>(null)

    override fun connect() {
        // У HTTP нет постоянного соединения — метод для совместимости с [Producer].
    }

    override fun disconnect() {
        pollThreadRef.getAndSet(null)?.interrupt()
        statusChangeHandlerRef.set(null)
    }

    override fun publish(
        consumerDeviseId: String,
        message: ProducerMessageInDto,
        onMessageStatusChange: Producer.OnMessageStatusChange?
    ) {
        statusChangeHandlerRef.set(onMessageStatusChange)
        pollThreadRef.getAndSet(null)?.interrupt()

        val uri = URI.create("$root/producer/request/${encodePath(consumerDeviseId)}")
        val body = om.writeValueAsString(message)
        val request = requestBuilder(uri)
            .PUT(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException("producer/request failed: ${response.statusCode()} ${response.body()}")
        }
        val initial: ProducerMessageOutDto = om.readValue(response.body())

        val cert = message.messageBirthCertificate
        if (onMessageStatusChange != null && cert != null) {
            val poller = Thread(
                {
                    pollMessageStatus(initial.id, cert.tmpId, initial.deliveryStatus)
                },
                "producer-rest-status-${initial.id}"
            )
            poller.isDaemon = true
            pollThreadRef.set(poller)
            poller.start()
        }
    }

    private fun pollMessageStatus(messageId: Int, tmpId: Int?, initialStatus: DeliveryStatus) {
        var last = initialStatus
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(150)
                val dto = fetchDeliveryStatus(messageId) ?: continue
                val status = dto.deliveryStatus
                if (status != last) {
                    last = status
                    statusChangeHandlerRef.get()?.newStatusEvent(
                        ProducerMessageOutDto(messageId, tmpId, status)
                    )
                }
                if (isTerminalForPolling(status)) break
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun fetchDeliveryStatus(messageId: Int): ProducerDeliveryStatusOutDto? {
        val uri = URI.create("$root/producer/getMessageStatus/$messageId")
        val request = requestBuilder(uri).GET().build()
        val response = runCatching {
            http.send(request, HttpResponse.BodyHandlers.ofString())
        }.getOrElse { return null }
        if (response.statusCode() == 404) return null
        if (response.statusCode() !in 200..299) return null
        return runCatching { om.readValue<ProducerDeliveryStatusOutDto>(response.body()) }.getOrNull()
    }

    private fun requestBuilder(uri: URI): HttpRequest.Builder {
        val b = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
        bearerToken?.takeIf { it.isNotBlank() }?.let { b.header("Authorization", "Bearer $it") }
        return b
    }

    private fun isTerminalForPolling(status: DeliveryStatus): Boolean =
        when (status) {
            DeliveryStatus.COMPLETED,
            DeliveryStatus.COMPLETED_WITH_ERROR,
            DeliveryStatus.CANCELLED -> true
            else -> false
        }

    private companion object {
        fun encodePath(segment: String): String =
            java.net.URLEncoder.encode(segment, Charsets.UTF_8).replace("+", "%20")
    }
}

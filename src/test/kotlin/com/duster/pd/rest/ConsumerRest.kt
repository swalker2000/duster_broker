package com.duster.pd.rest

import com.duster.transport.data.dto.consumer.ConsumerMessageInDto
import com.duster.transport.data.dto.consumer.ConsumerMessageOutDto
import com.duster.pd.Consumer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Consumer поверх HTTP REST API брокера (аналог [com.duster.pd.mqtt.ConsumerMqtt]).
 *
 * [connect] запускает фоновый опрос `GET /consumer/getLastMessage/{deviceId}`; при появлении нового
 * сообщения (новый id) вызываются подписчики [subscribeNewMessage].
 *
 * @param brokerUrl базовый URL сервиса, например `http://127.0.0.1:8080`
 * @param bearerToken если задан, добавляется заголовок `Authorization: Bearer …`
 */
class ConsumerRest(
    brokerUrl: String,
    deviseId: String,
    private val bearerToken: String? = null,
    private val pollIntervalMs: Long = 200L
) : Consumer(deviseId) {

    private val root = brokerUrl.trimEnd('/')
    private val om = jacksonObjectMapper()
    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    @Volatile
    private var active: Boolean = false

    private var pollThread: Thread? = null
    private var lastNotifiedId: Long? = null

    override fun connect() {
        if (active) return
        lastNotifiedId = null
        active = true
        val thread = Thread({ pollLoop() }, "consumer-rest-poll-$deviseId")
        thread.isDaemon = true
        pollThread = thread
        thread.start()
    }

    override fun disconnect() {
        active = false
        pollThread?.interrupt()
        pollThread?.join(3000)
        pollThread = null
    }

    private fun pollLoop() {
        try {
            while (active && !Thread.currentThread().isInterrupted()) {
                runCatching { pollOnce() }
                Thread.sleep(pollIntervalMs)
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun pollOnce() {
        val uri = URI.create("$root/consumer/getLastMessage/${encodePath(deviseId)}")
        val request = requestBuilder(uri).GET().build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) return
        val list: List<ConsumerMessageOutDto> = om.readValue(response.body())
        val msg = list.firstOrNull() ?: return
        val id = msg.id
        if (lastNotifiedId != null && id == lastNotifiedId) return
        lastNotifiedId = id
        onNewMessage(msg)
    }

    override fun sendResponse(response: ConsumerMessageInDto) {
        val uri = URI.create("$root/consumer/request/${encodePath(deviseId)}")
        val body = om.writeValueAsString(response)
        val request = requestBuilder(uri)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val httpResponse = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (httpResponse.statusCode() !in 200..299) {
            throw IllegalStateException("consumer/request failed: ${httpResponse.statusCode()} ${httpResponse.body()}")
        }
    }

    private fun requestBuilder(uri: URI): HttpRequest.Builder {
        val b = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
        bearerToken?.takeIf { it.isNotBlank() }?.let { b.header("Authorization", "Bearer $it") }
        return b
    }

    private companion object {
        fun encodePath(segment: String): String =
            java.net.URLEncoder.encode(segment, Charsets.UTF_8).replace("+", "%20")
    }
}

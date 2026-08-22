package com.duster.pd.dustertcp

import com.duster.database.data.message.DeliveryStatus
import com.duster.duster_protocol.transport.client.producer.tcp.ProducerTcp
import com.duster.pd.Producer
import com.duster.transport.data.dto.producer.ProducerDeliveryStatusOutDto
import com.duster.transport.data.dto.producer.message.ProducerMessageInDto
import com.duster.transport.data.dto.producer.message.ProducerMessageOutDto
import java.util.concurrent.atomic.AtomicReference

/**
 * Producer поверх TCP-протокола duster_broker (аналог [com.duster.pd.rest.ProducerRest]).
 *
 * Публикация идёт на `{consumerDeviseId}`: клиент логинится этим id (как `{deviceId}` в REST-пути)
 * и отправляет кадр `PRODUCER_SEND_MESSAGE`. Смена статуса опрашивается через `PRODUCER_ASK_MESSAGE_STATUS`.
 *
 * @param host адрес брокера, например `127.0.0.1`
 * @param port порт [com.duster.transport.duster.DusterTcpMessageHandler]
 */
class ProducerDusterTcp(
    private val host: String,
    private val port: Int,
    override val deviceId: String = "0",
    private val password: String = "",
    private val useTls: Boolean = false
) : Producer {

    private val statusChangeHandlerRef = AtomicReference<Producer.OnMessageStatusChange?>(null)
    private val pollThreadRef = AtomicReference<Thread?>(null)

    override fun connect() {
        // У DBP нет постоянной сессии в PD-клиенте — каждое действие открывает свой коннект.
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

        val initial = producerClient(consumerDeviseId).sendMessage(message)
        val cert = message.messageBirthCertificate
        if (onMessageStatusChange != null && cert != null) {
            val poller = Thread.ofVirtual()
                .name("producer-duster-tcp-status-${initial.id}")
                .start { pollMessageStatus(initial.id, cert.tmpId, initial.deliveryStatus) }
            pollThreadRef.set(poller)
        }
    }

    private fun pollMessageStatus(messageId: Long, tmpId: Long?, initialStatus: DeliveryStatus) {
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

    private fun fetchDeliveryStatus(messageId: Long): ProducerDeliveryStatusOutDto? =
        runCatching { producerClient(deviceId).askMessageStatus(messageId) }.getOrNull()

    private fun producerClient(loginAs: String): ProducerTcp =
        ProducerTcp(loginAs, host, port, password, useTls = useTls)

    private fun isTerminalForPolling(status: DeliveryStatus): Boolean =
        when (status) {
            DeliveryStatus.COMPLETED,
            DeliveryStatus.COMPLETED_WITH_ERROR,
            DeliveryStatus.CANCELLED -> true
            else -> false
        }
}

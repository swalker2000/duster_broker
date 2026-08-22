package com.duster.pd.dustertcp

import com.duster.duster_protocol.transport.client.consumer.tcp.ConsumerTcp
import com.duster.pd.Consumer
import com.duster.transport.data.dto.consumer.ConsumerMessageInDto

/**
 * Consumer поверх TCP-протокола duster_broker (аналог [com.duster.pd.rest.ConsumerRest]).
 *
 * [connect] запускает фоновый опрос `CONSUMER_ASK_MESSAGE`; при появлении нового сообщения
 * (новый id) вызываются подписчики [subscribeNewMessage].
 */
class ConsumerDusterTcp(
    host: String,
    port: Int,
    deviseId: String,
    password: String = "",
    private val pollIntervalMs: Long = 200L,
    useTls: Boolean = false
) : Consumer(deviseId) {

    private val tcp = ConsumerTcp(deviseId, host, port, password, useTls = useTls)

    @Volatile
    private var active: Boolean = false

    private var pollThread: Thread? = null
    private var lastNotifiedId: Long? = null

    override fun connect() {
        if (active) return
        lastNotifiedId = null
        active = true
        pollThread = Thread.ofVirtual()
            .name("consumer-duster-tcp-poll-$deviseId")
            .start { pollLoop() }
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
        val msg = tcp.giveMeMessage() ?: return
        val id = msg.id
        if (lastNotifiedId != null && id == lastNotifiedId) return
        lastNotifiedId = id
        onNewMessage(msg)
    }

    override fun sendResponse(response: ConsumerMessageInDto) {
        tcp.messageStatusChanged(response)
    }
}

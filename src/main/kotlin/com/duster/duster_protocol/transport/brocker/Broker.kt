package com.duster.duster_protocol.transport.brocker

import com.duster.database.data.client.Role
import com.duster.duster_protocol.messagefactory.bytearray.generate.ByteArrayGeneratorWhis
import com.duster.duster_protocol.messagefactory.bytearray.parse.MessageDetector
import com.duster.duster_protocol.messagefactory.bytearray.parse.dto.LoginCredentials
import com.duster.duster_protocol.messagefactory.bytearray.parse.dto.LoginResult
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.ConsumerLoginParser
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.ProducerLoginParser
import com.duster.duster_protocol.messagefactory.transport.constant.DbpMessageType
import com.duster.duster_protocol.transport.brocker.connection.toconsumer.ConnectionToConsumer
import com.duster.duster_protocol.transport.brocker.connection.toconsumer.handlers.OnConsumerAskMessages
import com.duster.duster_protocol.transport.brocker.connection.toconsumer.handlers.OnMessageStatusChanged
import com.duster.duster_protocol.transport.brocker.connection.toproducer.ConnectionToProducer
import com.duster.duster_protocol.transport.brocker.connection.toproducer.handlers.OnAskMessageStatus
import com.duster.duster_protocol.transport.brocker.connection.toproducer.handlers.OnSendMessage
import com.duster.duster_protocol.transport.io.DbpFrameIo
import org.slf4j.LoggerFactory
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket

class Broker(
    private val bindPort: Int = 0,
    private val enablePlain: Boolean = true,
    private val tlsBindPort: Int = 0,
    private val sslContext: SSLContext? = null
) {

    fun interface OnLogin {
        fun onLogin(credentials: LoginCredentials, producer: Boolean): LoginResult
    }

    private val logger = LoggerFactory.getLogger(Broker::class.java)
    private val detector = MessageDetector()
    private val running = AtomicBoolean(false)

    private val executor = Executors.newThreadPerTaskExecutor(
        Thread.ofVirtual().name("duster-dbp-", 0).factory()
    )

    @Volatile
    private var plainServerSocket: ServerSocket? = null

    @Volatile
    private var tlsServerSocket: ServerSocket? = null

    @Volatile
    private var loginHandler: OnLogin = OnLogin { credentials, _ ->
        LoginResult(
            ok = true,
            deviseId = credentials.deviseId,
            role = Role.DEVISE,
            accessToken = ""
        )
    }

    val connectionToProducer = ConnectionToProducerBuilder()
    val connectionToConsumer = ConnectionToConsumerBuilder()

    val port: Int
        get() = plainServerSocket?.localPort ?: error("plain TCP listener is not started")

    val tlsPort: Int
        get() = tlsServerSocket?.localPort ?: error("TLS listener is not started")

    fun onLogin(handler: OnLogin) {
        loginHandler = handler
    }

    fun start() {
        val enableTls = sslContext != null
        check(enablePlain || enableTls) { "at least one of plain TCP or TLS listeners must be enabled" }
        check(running.compareAndSet(false, true)) { "broker already started" }
        try {
            if (enablePlain) {
                val server = ServerSocket(bindPort)
                plainServerSocket = server
                executor.execute { acceptLoop(server) }
            }
            if (enableTls) {
                val server = sslContext!!.serverSocketFactory.createServerSocket(tlsBindPort) as SSLServerSocket
                server.needClientAuth = false
                server.enabledProtocols = arrayOf("TLSv1.2", "TLSv1.3")
                tlsServerSocket = server
                executor.execute { acceptLoop(server) }
            }
        } catch (ex: Exception) {
            running.set(false)
            runCatching { plainServerSocket?.close() }
            runCatching { tlsServerSocket?.close() }
            plainServerSocket = null
            tlsServerSocket = null
            throw ex
        }
    }

    fun stop() {
        running.set(false)
        runCatching { plainServerSocket?.close() }
        runCatching { tlsServerSocket?.close() }
        plainServerSocket = null
        tlsServerSocket = null
        executor.shutdownNow()
        executor.awaitTermination(2, TimeUnit.SECONDS)
    }

    private fun acceptLoop(server: ServerSocket) {
        while (running.get()) {
            val client = try {
                server.accept()
            } catch (_: SocketException) {
                break
            }
            executor.execute { handleClient(client) }
        }
    }

    private fun handleClient(socket: Socket) {
        socket.soTimeout = 10_000
        socket.use { client ->
            try {
                if (client is SSLSocket) {
                    client.startHandshake()
                }
                val input = client.getInputStream()
                val output = client.getOutputStream()
                val loginFrame = DbpFrameIo.read(input) ?: return
                when (detector.detect(loginFrame)) {
                    DbpMessageType.PRODUCER_LOGIN -> {
                        val credentials = ProducerLoginParser.parse(loginFrame) ?: return
                        val authenticated = loginHandler.onLogin(credentials, true)
                        val result = if (authenticated.ok && !connectionToProducer.handlersReady()) {
                            authenticated.copy(ok = false, accessToken = "")
                        } else {
                            authenticated
                        }
                        DbpFrameIo.write(
                            output,
                            ByteArrayGeneratorWhis.Broker.ToProducer.loginResult(
                                result.ok,
                                result.deviseId,
                                result.role,
                                result.accessToken
                            )
                        )
                        if (!result.ok) {
                            return
                        }
                        val connection = ConnectionToProducer(result.deviseId.ifBlank { credentials.deviseId })
                        connection.attach(input, output)
                        connectionToProducer.applyTo(connection)
                        connection.run()
                    }
                    DbpMessageType.CONSUMER_LOGIN -> {
                        val credentials = ConsumerLoginParser.parse(loginFrame) ?: return
                        val authenticated = loginHandler.onLogin(credentials, false)
                        val result = if (authenticated.ok && !connectionToConsumer.handlersReady()) {
                            authenticated.copy(ok = false, accessToken = "")
                        } else {
                            authenticated
                        }
                        DbpFrameIo.write(
                            output,
                            ByteArrayGeneratorWhis.Broker.ToConsumer.loginResult(
                                result.ok,
                                result.deviseId,
                                result.role,
                                result.accessToken
                            )
                        )
                        if (!result.ok) {
                            return
                        }
                        val connection = ConnectionToConsumer(result.deviseId.ifBlank { credentials.deviseId })
                        connection.attach(input, output)
                        connectionToConsumer.applyTo(connection)
                        connection.run()
                    }
                    else -> return
                }
            } catch (ex: Exception) {
                logger.debug("DBP client session ended: {}", ex.message)
            }
        }
    }

    inner class ConnectionToProducerBuilder {
        internal var onAskMessageStatusHandler: OnAskMessageStatus? = null
        internal var onSendMessageHandler: OnSendMessage? = null

        fun onAskMessageStatus(handler: OnAskMessageStatus) {
            onAskMessageStatusHandler = handler
        }

        fun onSendMessage(handler: OnSendMessage) {
            onSendMessageHandler = handler
        }

        internal fun handlersReady(): Boolean =
            onAskMessageStatusHandler != null && onSendMessageHandler != null

        internal fun applyTo(connection: ConnectionToProducer) {
            onAskMessageStatusHandler?.let { connection.onAskMessageStatus(it) }
            onSendMessageHandler?.let { connection.onSendMessage(it) }
        }
    }

    inner class ConnectionToConsumerBuilder {
        internal var onConsumerAskMessagesHandler: OnConsumerAskMessages? = null
        internal var onMessageStatusChangedHandler: OnMessageStatusChanged? = null

        fun onConsumerAskMessages(handler: OnConsumerAskMessages) {
            onConsumerAskMessagesHandler = handler
        }

        fun onMessageStatusChanged(handler: OnMessageStatusChanged) {
            onMessageStatusChangedHandler = handler
        }

        internal fun handlersReady(): Boolean =
            onConsumerAskMessagesHandler != null && onMessageStatusChangedHandler != null

        internal fun applyTo(connection: ConnectionToConsumer) {
            onConsumerAskMessagesHandler?.let { connection.onConsumerAskMessages(it) }
            onMessageStatusChangedHandler?.let { connection.onMessageStatusChanged(it) }
        }
    }
}

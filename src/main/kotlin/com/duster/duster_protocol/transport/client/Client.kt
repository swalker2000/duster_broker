package com.duster.duster_protocol.transport.client

import com.duster.duster_protocol.transport.io.DbpFrameIo
import com.duster.duster_protocol.transport.ssl.DusterProtocolSsl
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Callable
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

abstract class Client(
    val deviseId: String,
    protected val url: String,
    protected val port: Int,
    protected val password: String = "",
    protected val useTls: Boolean = false,
    protected val insecureTls: Boolean = true,
    protected val sslSocketFactory: SSLSocketFactory? = null
) {

    private var socket: Socket? = null

    protected val input: InputStream
        get() = socket?.getInputStream() ?: error("not connected")

    protected val output: OutputStream
        get() = socket?.getOutputStream() ?: error("not connected")

    /**
     * Перед каждым обменом сообщениями с брокером происходит коннект.
     *  - Здесь клиент авторизуется
     */
    protected abstract fun connect()

    /**
     * После каждого сеанса обмена происходит дисконект.
     */
    protected abstract fun disconnect()

    protected fun openSocket() {
        val s: Socket = if (useTls) {
            val factory = sslSocketFactory
                ?: if (insecureTls) DusterProtocolSsl.insecureClientSocketFactory()
                else SSLSocketFactory.getDefault()
            val ssl = factory.createSocket() as SSLSocket
            ssl.enabledProtocols = arrayOf("TLSv1.2", "TLSv1.3")
            if (insecureTls) {
                ssl.sslParameters = ssl.sslParameters.apply {
                    endpointIdentificationAlgorithm = null
                }
            }
            ssl.connect(InetSocketAddress(url, port), 5_000)
            ssl.startHandshake()
            ssl
        } else {
            val plain = Socket()
            plain.connect(InetSocketAddress(url, port), 5_000)
            plain
        }
        s.soTimeout = 10_000
        socket = s
    }

    protected fun closeSocket() {
        runCatching { socket?.close() }
        socket = null
    }

    protected fun writeFrame(frame: List<Int>) {
        DbpFrameIo.write(output, frame)
    }

    protected fun readFrame(): List<Int> =
        DbpFrameIo.read(input) ?: error("broker closed the connection")

    protected fun <T> makeTransaction(action: Callable<T>): T {
        connect()
        try {
            return action.call()
        } finally {
            disconnect()
        }
    }
}

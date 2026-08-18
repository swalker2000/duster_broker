package com.duster.duster_protocol.transport.client

import com.duster.duster_protocol.transport.io.DbpFrameIo
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Callable

abstract class Client(
    val deviseId: String,
    protected val url: String,
    protected val port: Int,
    protected val password: String = ""
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
        val s = Socket()
        s.connect(InetSocketAddress(url, port), 5_000)
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

package com.duster.duster_protocol.transport.client

import java.util.concurrent.Callable

abstract class Client(val deviseId: String,
                      private val url: String,
                      private val port: Int) {

    /**
     * Перед каждым обменом сообщениями с брокером происходит коннект.
     *  - Здесь клиент авторизуется
     */
    protected abstract fun connect()

    /**
     * После каждого сеанса обмена происходит дисконект.
     */
    protected abstract fun disconnect()

    protected fun <T>makeTransaction(action : Callable<T>) : T {
        connect()
        val result = action.call()
        disconnect()
        return result
    }

}
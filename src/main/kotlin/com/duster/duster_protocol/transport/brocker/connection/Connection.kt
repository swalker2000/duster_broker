package com.duster.duster_protocol.transport.brocker.connection

import java.io.InputStream
import java.io.OutputStream

abstract class Connection(val deviseId: String) {

    internal lateinit var input: InputStream
    internal lateinit var output: OutputStream

    internal fun attach(input: InputStream, output: OutputStream) {
        this.input = input
        this.output = output
    }

    /**
     * Проверяет, что все хендлеры выставлены, в противном случае кидает exception.
     */
    abstract fun run()

    protected fun requireHandlers(vararg named: Pair<String, Any?>) {
        val missing = named.filter { it.second == null }.map { it.first }
        if (missing.isNotEmpty()) {
            throw HandlersNotSetException(missing)
        }
    }
}

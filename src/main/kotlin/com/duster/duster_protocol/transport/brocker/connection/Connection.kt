package com.duster.duster_protocol.transport.brocker.connection

abstract class Connection(val deviseId: String) {

    /**
     * Проверяет, что все хендлеры выставлены, в противном случае кидает exception.
     */
    abstract fun run()
}
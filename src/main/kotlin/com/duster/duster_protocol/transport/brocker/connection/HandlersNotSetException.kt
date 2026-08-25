package com.duster.duster_protocol.transport.brocker.connection

class HandlersNotSetException(missing: Collection<String>) :
    IllegalStateException("Handlers not set: ${missing.joinToString()}")

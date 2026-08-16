package com.duster.duster_protocol.messagefactory.generators.service

import com.duster.duster_protocol.messagefactory.DbpMessageType
import com.duster.duster_protocol.messagefactory.generators.service.simple.SimpleByteArrayGenerator

/**
 * Генерирует сервисные сообщения. Такие как готовность принять новое сообщение или сообщение о том, что новых сообщений нет.
 *
 */
object ServiceByteArrayGenerator {

    fun brokerDontHaveMessageForConsumer(): List<Int> {
        return SimpleByteArrayGenerator.generate(DbpMessageType.BROKER_DONT_HAVE_MESSAGE_FOR_CONSUMER)
    }

}
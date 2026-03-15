package com.duster.transport.mqtt

/**
 * Источник от которого пришло сообщение.
 */
enum class MessageSource(val prefixInTopic : String)
{
    PRODUCER("producer"),
    CONSUMER("consumer"),
}
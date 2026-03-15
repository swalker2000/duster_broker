package com.duster.pd

import com.duster.messagehandler.data.dto.consumer.ConsumerMessageInDto
import com.duster.messagehandler.data.dto.consumer.ConsumerMessageOutDto
import org.slf4j.LoggerFactory

abstract class Consumer(val deviseId: String) {

    private val logger = LoggerFactory.getLogger(Consumer::class.java)

    private val onNewMessageList = mutableListOf<OnNewMessage>()

    /**
     * Хендлер прихода нового сообщения (от брокера в consumer/request).
     */
    fun interface OnNewMessage {
        fun newMessageEvent(message: ConsumerMessageOutDto)
    }

    /**
     * Подписка на приход новых сообщений.
     */
    fun subscribeNewMessage(onNewMessage: OnNewMessage) {
        onNewMessageList.add(onNewMessage)
    }

    protected fun onNewMessage(message: ConsumerMessageOutDto) {
        onNewMessageList.forEach {
            try {
                it.newMessageEvent(message)
            } catch (e: Exception) {
                logger.error("OnNewMessage error:")
                logger.error(e.stackTraceToString())
            }
        }
    }

    /**
     * Отправить ответ брокеру (подтверждение доставки, статус выполнения и т.д.)
     */
    abstract fun sendResponse(response: ConsumerMessageInDto)

    abstract fun connect()

    abstract fun disconnect()
}
package com.duster.duster_protocol.messagefactory.transport.constant

/**
 * Типы сообщений специфичные для протокола duster_broker
 * Используется следующий нейминг:
 *  'КтоОтправил_ТипСообщения'
 *  если сообщение отправляет продюсер
 *  'КтоОтправил_ТипСообщения_КтоПолучил'
 * @param code код типа сообщения, передаваемый в бинарном протоколе. (1 байт)
 */
enum class DbpMessageType(val code: Int) {

    //********BROKER-CONSUMER************

    /**
     * Консьюмер запрашивает у брокера, есть ли доступные для него сообщения.
     */
    CONSUMER_ASK_MESSAGE(0xC1),

    /**
     *  Брокер передает консьюмеру сообщение.
     *  (забирай сообщение)
     */
    BROKER_SEND_MESSAGE_TO_CONSUMER(0xC2),

    /**
     *  Брокер сообщает консьюмеру, что у него нет сообщений.
     */
    BROKER_DONT_HAVE_MESSAGE_FOR_CONSUMER(0xC3),

    /**
     *  Консьюмер сообщает брокеру, что сообщение им получено.
     */
    CONSUMER_MESSAGE_RECEIVED(0xC4),


    //********BROKER-PRODUCER************

    /**
     * Продюсер отправляет сообщение с полезной нагрузкой.
     */
    PRODUCER_SEND_MESSAGE(0xB1),

    /**
     *  Брокер сообщает продюсеру, что сообщение им получено.
     */
    BROKER_MESSAGE_RECEIVED_FROM_PRODUCER(0xB2),

    /**
     * Продюсер запрашивает статус сообщения.
     */
    PRODUCER_ASK_MESSAGE_STATUS(0xB3),

    /**
     *  Брокер сообщает продюсеру, статус его сообщения.
     */
    BROKER_RETURN_MESSAGE_STATUS_TO_PRODUCER(0xB4),
}
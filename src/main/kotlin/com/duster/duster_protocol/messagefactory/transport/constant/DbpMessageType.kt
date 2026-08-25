package com.duster.duster_protocol.messagefactory.transport.constant

import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.AbstractParser
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.BrokerConsumerLoginResultParser
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.BrokerMessageReceivedFromProducerParser
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.BrokerProducerLoginResultParser
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.BrokerReturnMessageStatusToProducerParser
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.BrokerSendMessageToConsumerParser
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.ConsumerLoginParser
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.ConsumerMessageReceivedParser
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.ProducerAskMessageStatusParser
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.ProducerLoginParser
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.ProducerSendMessageParser

/**
 * Типы сообщений специфичные для протокола duster_broker
 * Используется следующий нейминг:
 *  'КтоОтправил_ТипСообщения'
 *  если сообщение отправляет продюсер
 *  'КтоОтправил_ТипСообщения_КтоПолучил'
 * @param code код типа сообщения, передаваемый в бинарном протоколе. (1 байт)
 * @param parserProvider парсер полезной нагрузки; `null`, если у кадра нет аргументов.
 *        Передаётся лямбдой, чтобы не зациклить инициализацию enum ↔ object-парсер.
 */
enum class DbpMessageType(
    val code: Int,
    private val parserProvider: () -> AbstractParser<*>? = { null }
) {

    //********BROKER-CONSUMER************

    /**
     * Консьюмер запрашивает у брокера, есть ли доступные для него сообщения.
     */
    CONSUMER_ASK_MESSAGE(0xC1),

    /**
     *  Брокер передает консьюмеру сообщение.
     *  (забирай сообщение)
     */
    BROKER_SEND_MESSAGE_TO_CONSUMER(0xC2, { BrokerSendMessageToConsumerParser }),

    /**
     *  Брокер сообщает консьюмеру, что у него нет сообщений.
     */
    BROKER_DONT_HAVE_MESSAGE_FOR_CONSUMER(0xC3),

    /**
     * Консьюмер сообщает брокеру, что статус сообщения поменялся.
     * (к примеру сообщение было получено, либо выполнено)
     */
    CONSUMER_MESSAGE_STATUS_CHANDGED(0xC4, { ConsumerMessageReceivedParser }),

    /**
     * Консъюмер логинится (аналог REST `POST /auth/login`).
     * Payload: deviseIdLen[2 LE] + deviseId UTF-8 + passwordLen[2 LE] + password UTF-8.
     */
    CONSUMER_LOGIN(0xC5, { ConsumerLoginParser }),

    /**
     * Брокер отвечает консьюмеру на логин (аналог JSON ответа `/auth/login` / 401).
     * Payload: ok[1] + roleOrdinal[1] + deviseIdLen[2 LE] + deviseId UTF-8 + tokenLen[2 LE] + accessToken UTF-8.
     */
    BROKER_CONSUMER_LOGIN_RESULT(0xC6, { BrokerConsumerLoginResultParser }),


    //********BROKER-PRODUCER************

    /**
     * Продюсер отправляет сообщение с полезной нагрузкой.
     */
    PRODUCER_SEND_MESSAGE(0xB1, { ProducerSendMessageParser }),

    /**
     *  Брокер сообщает продюсеру, что сообщение им получено.
     */
    BROKER_MESSAGE_RECEIVED_FROM_PRODUCER(0xB2, { BrokerMessageReceivedFromProducerParser }),

    /**
     * Продюсер запрашивает статус сообщения.
     */
    PRODUCER_ASK_MESSAGE_STATUS(0xB3, { ProducerAskMessageStatusParser }),

    /**
     *  Брокер сообщает продюсеру, статус его сообщения.
     */
    BROKER_RETURN_MESSAGE_STATUS_TO_PRODUCER(0xB4, { BrokerReturnMessageStatusToProducerParser }),


    /**
     * Продюсер логинится (аналог REST `POST /auth/login`).
     * Payload: deviseIdLen[2 LE] + deviseId UTF-8 + passwordLen[2 LE] + password UTF-8.
     */
    PRODUCER_LOGIN(0xB5, { ProducerLoginParser }),

    /**
     * Брокер отвечает продюсеру на логин (аналог JSON ответа `/auth/login` / 401).
     * Payload: ok[1] + roleOrdinal[1] + deviseIdLen[2 LE] + deviseId UTF-8 + tokenLen[2 LE] + accessToken UTF-8.
     */
    BROKER_PRODUCER_LOGIN_RESULT(0xB6, { BrokerProducerLoginResultParser }),
    ;

    /** Парсер кадра или `null`, если полезная нагрузка не разбирается. */
    val parser: AbstractParser<*>? get() = parserProvider()
}

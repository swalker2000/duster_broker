package com.duster.duster_protocol.messagefactory.bytearray.parse

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
import com.duster.duster_protocol.messagefactory.transport.constant.DbpMessageType

class MessageDetector {

    /**
     * Определяет тип сообщения на основе массива байт.
     * @return тип входящего сообщения. null если определить не удается.
     */
    fun detect(byteArray: List<Int>): DbpMessageType? {
        return AbstractParser.detectType(byteArray)
    }

    /**
     * Парсер полезной нагрузки для типа, у которого она есть.
     * Сервисные кадры без аргументов ([DbpMessageType.CONSUMER_ASK_MESSAGE],
     * [DbpMessageType.BROKER_DONT_HAVE_MESSAGE_FOR_CONSUMER]) парсера не имеют.
     */
    fun parserFor(type: DbpMessageType): AbstractParser<*>? {
        return when (type) {
            DbpMessageType.BROKER_SEND_MESSAGE_TO_CONSUMER -> BrokerSendMessageToConsumerParser
            DbpMessageType.CONSUMER_MESSAGE_RECEIVED -> ConsumerMessageReceivedParser
            DbpMessageType.CONSUMER_LOGIN -> ConsumerLoginParser
            DbpMessageType.BROKER_CONSUMER_LOGIN_RESULT -> BrokerConsumerLoginResultParser
            DbpMessageType.PRODUCER_SEND_MESSAGE -> ProducerSendMessageParser
            DbpMessageType.BROKER_MESSAGE_RECEIVED_FROM_PRODUCER -> BrokerMessageReceivedFromProducerParser
            DbpMessageType.PRODUCER_ASK_MESSAGE_STATUS -> ProducerAskMessageStatusParser
            DbpMessageType.BROKER_RETURN_MESSAGE_STATUS_TO_PRODUCER -> BrokerReturnMessageStatusToProducerParser
            DbpMessageType.PRODUCER_LOGIN -> ProducerLoginParser
            DbpMessageType.BROKER_PRODUCER_LOGIN_RESULT -> BrokerProducerLoginResultParser
            DbpMessageType.CONSUMER_ASK_MESSAGE,
            DbpMessageType.BROKER_DONT_HAVE_MESSAGE_FOR_CONSUMER -> null
        }
    }

    /**
     * Определяет тип и разбирает кадр.
     * @return объект сообщения, [DbpMessageType] для кадров без полезной нагрузки, null если кадр некорректен.
     */
    fun parse(byteArray: List<Int>): Any? {
        val type = detect(byteArray) ?: return null
        val parser = parserFor(type) ?: return type
        return parser.parse(byteArray)
    }
}

package com.duster.duster_protocol.messagefactory.bytearray.parse

import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.AbstractParser
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
     * Парсер полезной нагрузки для типа, у которого она есть ([DbpMessageType.parser]).
     */
    fun parserFor(type: DbpMessageType): AbstractParser<*>? = type.parser

    /**
     * Определяет тип и разбирает кадр.
     * @return объект сообщения, [DbpMessageType] для кадров без полезной нагрузки, null если кадр некорректен.
     */
    fun parse(byteArray: List<Int>): Any? {
        val type = detect(byteArray) ?: return null
        val parser = type.parser ?: return type
        return parser.parse(byteArray)
    }
}

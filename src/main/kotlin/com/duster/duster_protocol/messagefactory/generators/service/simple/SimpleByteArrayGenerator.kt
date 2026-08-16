package com.duster.duster_protocol.messagefactory.generators.service.simple

import com.duster.duster_protocol.messagefactory.CrcCounter
import com.duster.duster_protocol.messagefactory.DbpMessageType
import com.duster.duster_protocol.messagefactory.StandardBytes

/**
 * Предназначен для генерации и парсинга простых сообщений состоящих из типа, стартового и стопового байта.
 */
object SimpleByteArrayGenerator {

    fun generate(dbpMessageType: DbpMessageType): List<Int> {
        val byteArray = mutableListOf<Int>(StandardBytes.START_BYTE, dbpMessageType.code, StandardBytes.STOP_BYTE)
        byteArray.addAll(CrcCounter.countCrc16(listOf(dbpMessageType.code)))
        return  byteArray
    }
}
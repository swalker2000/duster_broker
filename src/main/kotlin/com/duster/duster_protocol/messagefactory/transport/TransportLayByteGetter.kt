package com.duster.duster_protocol.messagefactory.transport

import com.duster.duster_protocol.messagefactory.transport.constant.DbpMessageType
import com.duster.duster_protocol.messagefactory.transport.constant.StandardBytes

/**
 *  Инструмент для получения массива байт передаваемых по сети из полезной нагрузки.
 */
class TransportLayByteGetter {

    /**
     * Получить байты передаваемые по сети из 'полезной нагрузки'
     * @param dbpMessageType тип сообщения
     * @param payload полезная нагрузка
     */
    fun getTransmitDateFromPayload(dbpMessageType : DbpMessageType, payload: List<Int> = listOf()): List<Int> {
        val transmitDate: MutableList<Int> = mutableListOf()
        transmitDate.add(StandardBytes.START_BYTE)
        transmitDate.add(dbpMessageType.code)
        transmitDate.addAll(escapeBytes(payload))
        transmitDate.addAll(CrcCounter.countCrc16(payload))
        transmitDate.add(StandardBytes.STOP_BYTE)
        return transmitDate
    }

    /**
     * Экранирует байты полезной нагрузки:
     * перед startByte, stopByte и самим mirror вставляется mirror.
     */
    private fun escapeBytes(bytes: List<Int>): List<Int> {
        val result = mutableListOf<Int>()
        for (byte in bytes) {
            if (byte == StandardBytes.START_BYTE || byte == StandardBytes.STOP_BYTE || byte == StandardBytes.MIRROR) {
                result.add(StandardBytes.MIRROR)
            }
            result.add(byte)
        }
        return result
    }
}
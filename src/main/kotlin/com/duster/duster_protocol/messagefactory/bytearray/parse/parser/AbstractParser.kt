package com.duster.duster_protocol.messagefactory.bytearray.parse.parser

import com.duster.duster_protocol.messagefactory.transport.CrcCounter
import com.duster.duster_protocol.messagefactory.transport.constant.DbpMessageType
import com.duster.duster_protocol.messagefactory.transport.constant.StandardBytes

/**
 * Парсер одного типа сообщения протокола duster_broker.
 * Транспортный кадр: START | TYPE | escape(payload) | CRC16_HI | CRC16_LO | STOP.
 */
abstract class AbstractParser<T> {

    protected abstract val dbpMessageType: DbpMessageType

    /** Минимальный размер полезной нагрузки. */
    protected abstract val minPayloadSize: Int

    /**
     * Разбирает транспортный кадр в объект T.
     * @return null если кадр битый / чужой TYPE / неверный CRC / некорректный payload.
     */
    fun parse(byteArray: List<Int>): T? {
        val payload = extractPayload(byteArray, dbpMessageType) ?: return null
        if (payload.size < minPayloadSize) {
            return null
        }
        return parsePayload(payload)
    }

    /**
     * Парсит уже извлечённую полезную нагрузку (без транспортных байтов).
     */
    protected abstract fun parsePayload(payload: List<Int>): T?

    protected fun getLong(bytes: List<Int>, offset: Int): Long {
        var value = 0L
        for (i in 0 until 8) {
            value = value or ((bytes[offset + i].toLong() and 0xFF) shl (8 * i))
        }
        return value
    }

    protected fun getUnsignedShort(bytes: List<Int>, offset: Int): Int =
        (bytes[offset] and 0xFF) or ((bytes[offset + 1] and 0xFF) shl 8)

    /**
     * Length-prefixed UTF-8 (2 байта LE + данные).
     * @return строка и смещение после неё, или null если кадр обрезан.
     */
    protected fun readUtf8(payload: List<Int>, offset: Int): Pair<String, Int>? {
        if (offset + 2 > payload.size) {
            return null
        }
        val len = getUnsignedShort(payload, offset)
        val start = offset + 2
        val end = start + len
        if (end > payload.size) {
            return null
        }
        val bytes = payload.subList(start, end).map { (it and 0xFF).toByte() }.toByteArray()
        return String(bytes, Charsets.UTF_8) to end
    }

    companion object {

        /**
         * Определяет тип по валидному кадру (START/STOP, известный TYPE, CRC).
         */
        fun detectType(byteArray: List<Int>): DbpMessageType? {
            val normalized = normalize(byteArray) ?: return null
            val type = DbpMessageType.entries.firstOrNull { it.code == normalized[1] } ?: return null
            extractPayload(normalized, type) ?: return null
            return type
        }

        fun extractPayload(message: List<Int>, expectedType: DbpMessageType): List<Int>? {
            val bytes = normalize(message) ?: return null
            if (bytes[1] != expectedType.code) {
                return null
            }
            val middle = bytes.subList(2, bytes.size - 1)
            if (middle.size < 2) {
                return null
            }
            val crcReceived = middle.subList(middle.size - 2, middle.size).toList()
            val escapedPayload = middle.subList(0, middle.size - 2)
            val payload = unescapeBytes(escapedPayload) ?: return null
            if (CrcCounter.countCrc16(payload) != crcReceived) {
                return null
            }
            return payload
        }

        private fun normalize(message: List<Int>): List<Int>? {
            val bytes = message.map { it and 0xFF }
            if (bytes.size < 1 + 1 + 2 + 1) {
                return null
            }
            if (bytes.first() != StandardBytes.START_BYTE || bytes.last() != StandardBytes.STOP_BYTE) {
                return null
            }
            return bytes
        }

        private fun unescapeBytes(bytes: List<Int>): List<Int>? {
            val result = mutableListOf<Int>()
            var i = 0
            while (i < bytes.size) {
                val byte = bytes[i]
                if (byte == StandardBytes.MIRROR) {
                    if (i + 1 >= bytes.size) {
                        return null
                    }
                    result.add(bytes[i + 1])
                    i += 2
                } else {
                    if (byte == StandardBytes.START_BYTE || byte == StandardBytes.STOP_BYTE) {
                        return null
                    }
                    result.add(byte)
                    i++
                }
            }
            return result
        }
    }
}

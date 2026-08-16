package com.duster.duster_protocol.messagefactory.generators.common

import com.duster.duster_protocol.messagefactory.transport.CrcCounter
import com.duster.duster_protocol.messagefactory.transport.TransportLayByteGetter
import com.duster.duster_protocol.messagefactory.transport.constant.DbpMessageType
import com.duster.duster_protocol.messagefactory.transport.constant.StandardBytes

/**
 * Генератор/парсер для одного конкретного типа <T> сообщений в соответсвии с протоколом duster_broker.
 *  - предназначен для работы с сообщениями имеющими полезную нагрузку. Не с сервисными сообщениями.
 * @param dbpMessageType тип сообщений, с которыми работает данный генератор.
 */
abstract class CommonByteArrayGenerator<T : Any>(private val dbpMessageType: DbpMessageType) {


    private val transportLayByteGetter = TransportLayByteGetter()

    /** Минимальный размер полезной нагрузки (у переменных сообщений фактический size может быть больше). */
    protected abstract val MIN_PAYLOAD_SIZE: Int


    /**
     * Преобразует объект T в массив байт который в дальнейшем будет завернут в еще один слой и передан по протоколу duster_broker.
     *  - ВНИМАНИЕ: транспортные байты (стартовый, стоповый, экран) передаются не здесь! Здесь просто преобразуем объект сообщения в массив байт.
     */
    protected abstract fun generatePayload(message: T) : List<Int>

    /**
     * Парсит массив байт полезной нагрузки (без транспортных байтов) сообщения в объект T.
     *  - ВНИМАНИЕ: транспортные байты (стартовый, стоповый, экран) парсятся не здесь! Здесь просто преобразуем массив байт в объект.
     * @return null если payload некорректный.
     */
    protected abstract fun parsePayload(payload: List<Int>) : T?


    /**
     * Преобразует объект T в массив байт передаваемых по протоколу duster_broker.
     */
    fun generateByteArray(message: T) : List<Int>
    {
        val payload = generatePayload(message)
        return transportLayByteGetter.getTransmitDateFromPayload(dbpMessageType, payload)
    }

    /**
     * Преобразует массив байт ответа объект.
     * @return null если массив байт не корректный и его невозможно превратить в сообщение.
     */
    fun parseByteArray(message: List<Char>) : T?
    {
        val payload = extractPayload(message) ?: return null

        if (payload.size < MIN_PAYLOAD_SIZE) {
            return null
        }
        return parsePayload(payload)
    }


    /**
     * Разбирает транспортный кадр: START | TYPE | escape(payload) | CRC16_HI | CRC16_LO | STOP.
     * @return payload или null, если кадр битый / чужой TYPE / неверный CRC.
     */
    protected fun extractPayload(message: List<Char>): List<Int>? {
        val bytes = message.map { it.code and 0xFF }
        // START + TYPE + CRC(2) + STOP (payload может быть пустым)
        if (bytes.size < 1 + 1 + 2 + 1) {
            return null
        }
        if (bytes.first() != StandardBytes.START_BYTE || bytes.last() != StandardBytes.STOP_BYTE) {
            return null
        }
        if (bytes[1] != dbpMessageType.code) {
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


    /**
     * Снимает экранирование. null — если кадр битый (висячий MIRROR или голый START/STOP).
     */
    protected fun unescapeBytes(bytes: List<Int>): List<Int>? {
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


    protected fun getBytes(value : Long) : List<Int>
    {
        return  listOf(
            (value and 0xFF).toInt(),
            ((value shr 8) and 0xFF).toInt(),
            ((value shr 16) and 0xFF).toInt(),
            ((value shr 24) and 0xFF).toInt(),
            ((value shr 32) and 0xFF).toInt(),
            ((value shr 40) and 0xFF).toInt(),
            ((value shr 48) and 0xFF).toInt(),
            ((value shr 56) and 0xFF).toInt(),
        )
    }


    protected fun getLong(bytes: List<Int>, offset: Int): Long {
        var value = 0L
        for (i in 0 until 8) {
            value = value or ((bytes[offset + i].toLong() and 0xFF) shl (8 * i))
        }
        return value
    }

    /** Little-endian unsigned short (2 байта). */
    protected fun getBytes2(value: Int): List<Int> =
        listOf(value and 0xFF, (value shr 8) and 0xFF)

    protected fun getUnsignedShort(bytes: List<Int>, offset: Int): Int =
        (bytes[offset] and 0xFF) or ((bytes[offset + 1] and 0xFF) shl 8)

}
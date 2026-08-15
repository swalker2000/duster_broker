package com.duster.duster_protocol.messagefactory

import java.util.Optional

/**
 * Генератор/парсер для одного конкретного типа <T> сообщений в соответсвии с протоколом duster_broker.
 * @param dbpMessageType тип сообщений, с которыми работает данный генератор.
 */
abstract class ByteArrayGenerator<T>(private val dbpMessageType: DbpMessageType) {


    /**
     * Преобразует объект T  в массив байт который в дальнейшем будет завернут в еще один слой и передан по протоколу duster_broker.
     *  - ВНИМАНИЕ: транспортные байты (стартовый, стоповый, экран) передаются не здесь! Здесь просто преобразуем объект сообщения в массив байт.
     */
    protected abstract fun generatePayload(message: T) : List<Int>


    /**
     * Преобразует объект T в массив байт передаваемых по протоколу duster_broker.
     */
    fun generateByteArray(message: T) : List<Int>
    {
        val payload = generatePayload(message)
        return getTransmitDateFromPayload(payload)
    }

    /**
     * Преобразует массив байт ответа объект.
     * @return Optional.empty() если массив байт не корректный и его невозможно превратить в сообщение.
     */
    abstract fun parseByteArray(message: List<Char>) : Optional<T>





    private fun getTransmitDateFromPayload(payload: List<Int>): List<Int> {
        val transmitDate: MutableList<Int> = mutableListOf()
        transmitDate.add(StandardBytes.START_BYTE)
        transmitDate.add(dbpMessageType.code)
        transmitDate.addAll(escapeBytes(payload))
        transmitDate.addAll(CrcCounter.countCrc16(payload))
        transmitDate.add(StandardBytes.STOP_BYTE)
        return transmitDate
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
     * Экранирует байты полезной нагрузки:
     * перед startByte, stopByte и самим mirrow вставляется mirrow.
     */
    protected fun escapeBytes(bytes: List<Int>): List<Int> {
        val result = mutableListOf<Int>()
        for (byte in bytes) {
            if (byte == StandardBytes.START_BYTE || byte == StandardBytes.STOP_BYTE || byte == StandardBytes.MIRROR) {
                result.add(StandardBytes.MIRROR)
            }
            result.add(byte)
        }
        return result
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
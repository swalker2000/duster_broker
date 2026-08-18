package com.duster.duster_protocol.transport.io

import com.duster.duster_protocol.messagefactory.bytearray.parse.MessageDetector
import com.duster.duster_protocol.messagefactory.transport.constant.StandardBytes
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Чтение и запись транспортных кадров:
 * START | TYPE | escape(payload) | CRC16 | STOP.
 *
 * CRC не экранируется, поэтому стоп-байт может встретиться внутри кадра.
 * Кадр считается завершённым, когда буфер начинается с START, заканчивается STOP
 * и проходит проверку [MessageDetector.detect].
 */
object DbpFrameIo {

    const val MAX_FRAME_BYTES: Int = 1024 * 1024

    private val detector = MessageDetector()

    fun write(output: OutputStream, frame: List<Int>) {
        val bytes = ByteArray(frame.size)
        for (i in frame.indices) {
            bytes[i] = (frame[i] and 0xFF).toByte()
        }
        output.write(bytes)
        output.flush()
    }

    /**
     * @return кадр или `null`, если поток закрыт до первого байта.
     */
    fun read(input: InputStream): List<Int>? {
        val buffer = ArrayList<Int>(64)
        while (true) {
            val b = input.read()
            if (b < 0) {
                if (buffer.isEmpty()) {
                    return null
                }
                throw EOFException("incomplete DBP frame")
            }
            buffer.add(b and 0xFF)
            if (buffer.size > MAX_FRAME_BYTES) {
                throw IOException("DBP frame exceeds $MAX_FRAME_BYTES bytes")
            }
            dropLeadingGarbage(buffer)
            if (buffer.size >= 5 &&
                buffer.first() == StandardBytes.START_BYTE &&
                buffer.last() == StandardBytes.STOP_BYTE &&
                detector.detect(buffer) != null
            ) {
                return buffer.toList()
            }
        }
    }

    private fun dropLeadingGarbage(buffer: ArrayList<Int>) {
        if (buffer.isEmpty() || buffer[0] == StandardBytes.START_BYTE) {
            return
        }
        val start = buffer.indexOf(StandardBytes.START_BYTE)
        if (start < 0) {
            buffer.clear()
        } else if (start > 0) {
            buffer.subList(0, start).clear()
        }
    }
}

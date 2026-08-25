package com.duster.duster_protocol.transport.io

import com.duster.duster_protocol.messagefactory.bytearray.generate.ByteArrayGeneratorWhis
import com.duster.duster_protocol.messagefactory.bytearray.parse.parser.ProducerLoginParser
import com.duster.duster_protocol.messagefactory.transport.constant.StandardBytes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class DbpFrameIoTest {

    @Test
    fun writeThenRead_roundTrip() {
        val frame = ByteArrayGeneratorWhis.FromProducer.login("dev", "pwd")
        val out = ByteArrayOutputStream()
        DbpFrameIo.write(out, frame)
        val read = DbpFrameIo.read(ByteArrayInputStream(out.toByteArray()))
        assertEquals("dev", ProducerLoginParser.parse(read!!)!!.deviseId)
    }

    @Test
    fun read_skipsLeadingGarbage() {
        val frame = ByteArrayGeneratorWhis.FromConsumer.giveMeMessage()
        val bytes = byteArrayOf(9, 8, 7) + frame.map { (it and 0xFF).toByte() }.toByteArray()
        val read = DbpFrameIo.read(ByteArrayInputStream(bytes))
        assertEquals(frame, read)
    }

    @Test
    fun read_emptyStreamReturnsNull() {
        assertNull(DbpFrameIo.read(ByteArrayInputStream(ByteArray(0))))
    }

    @Test
    fun read_doesNotStopOnCrcEqualToStopByte() {
        val frame = ByteArrayGeneratorWhis.Broker.ToConsumer.dontHaveMessage()
        val out = ByteArrayOutputStream()
        DbpFrameIo.write(out, frame)
        val bytes = out.toByteArray()
        assertEquals(StandardBytes.STOP_BYTE.toByte(), bytes.last())
        assertEquals(frame, DbpFrameIo.read(ByteArrayInputStream(bytes)))
    }
}

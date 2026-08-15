package com.duster.duster_protocol.messagefactory.generators

import com.duster.database.data.message.DeliveryGuarantee
import com.duster.duster_protocol.messagefactory.DbpMessageType
import com.duster.duster_protocol.messagefactory.StandardBytes
import com.duster.transport.data.dto.consumer.ConsumerMessageOutDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConsumerOutByteArrayGeneratorTest {

    @Test
    fun generate_frameHasStartTypeCrcAndStop() {
        val frame = ConsumerOutByteArrayGenerator.generateByteArray(sampleOut())

        assertEquals(StandardBytes.START_BYTE, frame.first())
        assertEquals(DbpMessageType.TAKE_MESSAGE.code, frame[1])
        assertEquals(StandardBytes.STOP_BYTE, frame.last())
    }

    @Test
    fun generate_and_parse_roundTrip() {
        val original = sampleOut()

        val parsed = ConsumerOutByteArrayGenerator.parseByteArray(
            ConsumerOutByteArrayGenerator.generateByteArray(original).map { it.toChar() }
        )

        assertTrue(parsed.isPresent)
        assertEquals(original.id, parsed.get().id)
        assertEquals(original.currentTimestamp, parsed.get().currentTimestamp)
        assertEquals(original.command, parsed.get().command)
        assertEquals(original.believerGuarantee, parsed.get().believerGuarantee)
        assertEquals(original.data!!["x"], (parsed.get().data!!["x"] as Number).toInt())
    }

    @Test
    fun generate_nullData_roundTripsAsEmptyJsonObject() {
        val original = sampleOut().apply { data = null }

        val parsed = ConsumerOutByteArrayGenerator.parseByteArray(
            ConsumerOutByteArrayGenerator.generateByteArray(original).map { it.toChar() }
        )

        assertTrue(parsed.isPresent)
        assertTrue(parsed.get().data!!.isEmpty())
    }

    @Test
    fun generate_escapesStartStopAndMirrorInsidePayload() {
        val message = sampleOut().apply {
            id = 0 // все байты id == START
            currentTimestamp = 1 // младший байт ts == STOP
            command = "\u0004" // MIRROR
        }
        val frame = ConsumerOutByteArrayGenerator.generateByteArray(message)
        // START | TYPE | escaped...
        val escaped = frame.subList(2, frame.size - 3)

        // каждый из 8 байт id=0 экранируется → (MIRROR, START) * 8
        assertEquals(listOf(StandardBytes.MIRROR, StandardBytes.START_BYTE), escaped.subList(0, 2))
        val tsOffset = 8 * 2
        assertEquals(
            listOf(StandardBytes.MIRROR, StandardBytes.STOP_BYTE),
            escaped.subList(tsOffset, tsOffset + 2)
        )
        // после ts (тоже 8 байт, первый экранирован) идёт commandLen=1 → тоже STOP
        val cmdLenOffset = tsOffset + 2 + 7 * 2 // (MIRROR,STOP) + 7×(MIRROR,0)
        assertEquals(
            listOf(StandardBytes.MIRROR, StandardBytes.STOP_BYTE),
            escaped.subList(cmdLenOffset, cmdLenOffset + 2)
        )
    }

    @Test
    fun parse_handlesEmptyCommand() {
        val original = sampleOut().apply { command = "" }

        val parsed = ConsumerOutByteArrayGenerator.parseByteArray(
            ConsumerOutByteArrayGenerator.generateByteArray(original).map { it.toChar() }
        )

        assertTrue(parsed.isPresent)
        assertEquals("", parsed.get().command)
    }

    @Test
    fun parse_rejectsWrongMessageType() {
        val frame = ConsumerOutByteArrayGenerator.generateByteArray(sampleOut()).toMutableList()
        frame[1] = DbpMessageType.MESSAGE_RECEIVED.code

        assertTrue(
            ConsumerOutByteArrayGenerator.parseByteArray(frame.map { it.toChar() }).isEmpty
        )
    }

    @Test
    fun parse_rejectsBadCrc() {
        val frame = ConsumerOutByteArrayGenerator.generateByteArray(sampleOut()).toMutableList()
        val crcIndex = frame.size - 3
        frame[crcIndex] = frame[crcIndex] xor 0xFF

        assertTrue(
            ConsumerOutByteArrayGenerator.parseByteArray(frame.map { it.toChar() }).isEmpty
        )
    }

    @Test
    fun parse_rejectsTooShortFrame() {
        assertTrue(
            ConsumerOutByteArrayGenerator.parseByteArray(
                listOf(StandardBytes.START_BYTE.toChar(), StandardBytes.STOP_BYTE.toChar())
            ).isEmpty
        )
    }

    @Test
    fun parse_rejectsMissingStartOrStop() {
        val valid = ConsumerOutByteArrayGenerator.generateByteArray(sampleOut()).map { it.toChar() }

        val noStart = valid.toMutableList().also { it[0] = 2.toChar() }
        val noStop = valid.toMutableList().also { it[it.lastIndex] = 2.toChar() }

        assertTrue(ConsumerOutByteArrayGenerator.parseByteArray(noStart).isEmpty)
        assertTrue(ConsumerOutByteArrayGenerator.parseByteArray(noStop).isEmpty)
    }

    private fun sampleOut(): ConsumerMessageOutDto =
        ConsumerMessageOutDto(id = 100).apply {
            currentTimestamp = 1_700_000_000_000L
            command = "PING"
            believerGuarantee = DeliveryGuarantee.RECEIPT_CONFIRMATION
            data = mapOf("x" to 1)
        }
}

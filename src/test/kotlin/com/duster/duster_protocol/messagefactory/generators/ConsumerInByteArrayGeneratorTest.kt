package com.duster.duster_protocol.messagefactory.generators

import com.duster.database.data.message.DeliveryStatus
import com.duster.duster_protocol.messagefactory.CrcCounter
import com.duster.duster_protocol.messagefactory.DbpMessageType
import com.duster.duster_protocol.messagefactory.StandardBytes
import com.duster.transport.data.dto.consumer.ConsumerMessageInDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ConsumerInByteArrayGeneratorTest {

    @Test
    fun generate_frameHasStartTypeCrcAndStop() {
        val frame = ConsumerInByteArrayGenerator.generateByteArray(sampleIn())

        assertEquals(StandardBytes.START_BYTE, frame.first())
        assertEquals(DbpMessageType.MESSAGE_RECEIVED.code, frame[1])
        assertEquals(StandardBytes.STOP_BYTE, frame.last())
    }

    @Test
    fun generate_and_parse_roundTrip() {
        val original = sampleIn()

        val parsed = ConsumerInByteArrayGenerator.parseByteArray(
            ConsumerInByteArrayGenerator.generateByteArray(original).map { it.toChar() }
        )

        assertNotNull(parsed)
        assertEquals(original.id, parsed!!.id)
        assertEquals(original.deliveryStatus, parsed.deliveryStatus)
    }

    @Test
    fun parse_acceptsAllConsumerStatuses() {
        val accepted = listOf(
            DeliveryStatus.COMPLETED_WITH_ERROR,
            DeliveryStatus.COMPLETED,
            DeliveryStatus.DELIVERED,
        )

        for (status in accepted) {
            val result = ConsumerInByteArrayGenerator.parseByteArray(buildInFrame(7, status))
            assertNotNull(result, "expected present for $status")
            assertEquals(status, result!!.deliveryStatus)
        }
    }

    @Test
    fun parse_rejectsStatusesNotAllowedFromConsumer() {
        val rejected = listOf(
            DeliveryStatus.NOT_DELIVERED,
            DeliveryStatus.CANCELLED,
            DeliveryStatus.UNKNOWN,
        )

        for (status in rejected) {
            val result = ConsumerInByteArrayGenerator.parseByteArray(buildInFrame(7, status))
            assertNull(result, "expected null for $status")
        }
    }

    @Test
    fun parse_handlesEscapedSpecialBytesInId() {
        val result = ConsumerInByteArrayGenerator.parseByteArray(
            buildInFrame(0, DeliveryStatus.COMPLETED)
        )

        assertNotNull(result)
        assertEquals(0, result!!.id)
        assertEquals(DeliveryStatus.COMPLETED, result.deliveryStatus)
    }

    @Test
    fun parse_rejectsBadCrc() {
        val frame = buildInFrame(1, DeliveryStatus.DELIVERED).toMutableList()
        val crcIndex = frame.size - 3
        frame[crcIndex] = ((frame[crcIndex].code xor 0xFF) and 0xFF).toChar()

        assertNull(ConsumerInByteArrayGenerator.parseByteArray(frame))
    }

    @Test
    fun parse_rejectsWrongMessageType() {
        val frame = buildInFrame(1, DeliveryStatus.DELIVERED).toMutableList()
        frame[1] = DbpMessageType.TAKE_MESSAGE.code.toChar()

        assertNull(ConsumerInByteArrayGenerator.parseByteArray(frame))
    }

    @Test
    fun parse_rejectsMissingStartOrStop() {
        val valid = buildInFrame(1, DeliveryStatus.DELIVERED)

        val noStart = valid.toMutableList().also { it[0] = 2.toChar() }
        val noStop = valid.toMutableList().also { it[it.lastIndex] = 2.toChar() }

        assertNull(ConsumerInByteArrayGenerator.parseByteArray(noStart))
        assertNull(ConsumerInByteArrayGenerator.parseByteArray(noStop))
    }

    @Test
    fun parse_rejectsTooShortFrame() {
        assertNull(
            ConsumerInByteArrayGenerator.parseByteArray(
                listOf(StandardBytes.START_BYTE.toChar(), StandardBytes.STOP_BYTE.toChar())
            )
        )
    }

    @Test
    fun parse_rejectsDanglingMirror() {
        val payload = longBytes(1) + DeliveryStatus.DELIVERED.ordinal
        val brokenEscaped = escape(payload) + StandardBytes.MIRROR
        val frame = (
            listOf(StandardBytes.START_BYTE, DbpMessageType.MESSAGE_RECEIVED.code) +
                brokenEscaped +
                CrcCounter.countCrc16(payload) +
                listOf(StandardBytes.STOP_BYTE)
            ).map { it.toChar() }

        assertNull(ConsumerInByteArrayGenerator.parseByteArray(frame))
    }

    private fun sampleIn(): ConsumerMessageInDto =
        ConsumerMessageInDto(id = 42).apply {
            deliveryStatus = DeliveryStatus.DELIVERED
        }

    private fun buildInFrame(id: Long, status: DeliveryStatus): List<Char> {
        val payload = longBytes(id) + status.ordinal
        return (
            listOf(StandardBytes.START_BYTE, DbpMessageType.MESSAGE_RECEIVED.code) +
                escape(payload) +
                CrcCounter.countCrc16(payload) +
                listOf(StandardBytes.STOP_BYTE)
            ).map { it.toChar() }
    }

    private fun longBytes(value: Long): List<Int> =
        (0 until 8).map { ((value shr (8 * it)) and 0xFF).toInt() }

    private fun escape(bytes: List<Int>): List<Int> {
        val result = mutableListOf<Int>()
        for (byte in bytes) {
            if (byte == StandardBytes.START_BYTE ||
                byte == StandardBytes.STOP_BYTE ||
                byte == StandardBytes.MIRROR
            ) {
                result.add(StandardBytes.MIRROR)
            }
            result.add(byte)
        }
        return result
    }
}

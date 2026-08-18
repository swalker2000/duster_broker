package com.duster.duster_protocol.messagefactory.generators.service

import com.duster.duster_protocol.messagefactory.transport.TransportLayByteGetter
import com.duster.duster_protocol.messagefactory.transport.constant.DbpMessageType
import com.duster.transport.data.dto.consumer.ConsumerMessageInDto
import com.duster.transport.data.dto.consumer.ConsumerMessageOutDto
import com.duster.transport.data.dto.producer.ProducerDeliveryStatusOutDto
import com.duster.transport.data.dto.producer.message.ProducerMessageInDto
import com.duster.transport.data.dto.producer.message.ProducerMessageOutDto
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

/**
 * Генерирует сервисные сообщения. Такие, как готовность принять новое сообщение или сообщение о том, что новых сообщений нет.
 *
 */
object ByteArrayGeneratorWhis {

    private val transportLayByteGetter = TransportLayByteGetter()


    private val objectMapper = jacksonObjectMapper()


    object Broker{
        object ToConsumer{
            /**
             *  Брокер сообщает консьюмеру, что у него нет сообщений.
             */
            fun dontHaveMessage(): List<Int> {
                return  transportLayByteGetter.getTransmitDateFromPayload(DbpMessageType.BROKER_DONT_HAVE_MESSAGE_FOR_CONSUMER)
            }

            /**
             * Брокер передает консьюмеру сообщение. (забирай сообщение)
             */
            fun messageOut(message: ConsumerMessageOutDto): List<Int> {
                val idArray = getBytes(message.id)
                val currentTimestampArray = getBytes(message.currentTimestamp)
                val commandArray: List<Int> = message.command.map { it.code and 0xFF }
                val dataJson = message.data?.let { objectMapper.writeValueAsString(it) } ?: "{}"
                val dataArray = dataJson.toByteArray(Charsets.UTF_8).map { it.toInt() and 0xFF }

                val payload: MutableList<Int> = mutableListOf()
                payload.addAll(idArray)
                payload.addAll(currentTimestampArray)
                payload.addAll(getBytes2(commandArray.size))
                payload.addAll(commandArray)
                payload.add(message.believerGuarantee.ordinal)
                payload.addAll(dataArray)
                return transportLayByteGetter.getTransmitDateFromPayload(DbpMessageType.BROKER_SEND_MESSAGE_TO_CONSUMER, payload)
            }
        }

        object ToProducer{

            fun sendMessageStatus(messageId : Long, producerDeliveryStatusOutDto : ProducerDeliveryStatusOutDto) : List<Int>
            {
                val idArray = getBytes(messageId)
                val payload: MutableList<Int> = mutableListOf()
                payload.add(producerDeliveryStatusOutDto.deliveryStatus.ordinal)
                payload.addAll(idArray)
                return transportLayByteGetter.getTransmitDateFromPayload(
                    DbpMessageType.BROKER_RETURN_MESSAGE_STATUS_TO_PRODUCER,
                    payload
                )
            }

            /**
             * Брокер сообщает продюсеру, что сообщение им получено.
             */
            fun messageOut(message: ProducerMessageOutDto): List<Int> {
                val idArray = getBytes(message.id)
                val tmpIdArray = getBytes(message.tmpId ?: 0)

                val payload: MutableList<Int> = mutableListOf()
                payload.addAll(idArray)
                payload.addAll(tmpIdArray)
                payload.add(message.deliveryStatus.ordinal)
                return transportLayByteGetter.getTransmitDateFromPayload(
                    DbpMessageType.BROKER_MESSAGE_RECEIVED_FROM_PRODUCER,
                    payload
                )
            }
        }
    }

    object FromConsumer
    {
        /**
         * Консьюмер сообщает брокеру, что сообщение им получено.
         */
        fun messageIn(message: ConsumerMessageInDto): List<Int> {
            val idArray = getBytes(message.id)
            val payload: MutableList<Int> = mutableListOf()
            payload.addAll(idArray)
            payload.add(message.deliveryStatus!!.ordinal)
            return transportLayByteGetter.getTransmitDateFromPayload(DbpMessageType.CONSUMER_MESSAGE_RECEIVED, payload)
        }

        /**
         * Консьюмер запрашивает у брокера, есть ли доступные для него сообщения.
         */
        fun askMessage() : List<Int>
        {
            return  transportLayByteGetter.getTransmitDateFromPayload(DbpMessageType.CONSUMER_ASK_MESSAGE)
        }
    }

    object FromProducer {//ProducerDeliveryStatusOutDto

        /**
         * Продюсер запрашивает статус сообщения.
         */
        fun producerAskMessageStatus(messageId: Long) : List<Int>
        {
            val idArray = getBytes(messageId)
            val payload: MutableList<Int> = mutableListOf()
            payload.addAll(idArray)
            return transportLayByteGetter.getTransmitDateFromPayload(DbpMessageType.PRODUCER_ASK_MESSAGE_STATUS, payload)

        }

        /**
         * Продюсер отправляет сообщение с полезной нагрузкой.
         */
        fun messageIn(message: ProducerMessageInDto): List<Int> {
            val tmpIdArray = getBytes(message.messageBirthCertificate?.tmpId ?: 0)
            val commandArray: List<Int> = message.command.map { it.code and 0xFF }
            val dataJson = message.data?.let { objectMapper.writeValueAsString(it) } ?: "{}"
            val dataArray = dataJson.toByteArray(Charsets.UTF_8).map { it.toInt() and 0xFF }

            val payload: MutableList<Int> = mutableListOf()
            payload.addAll(tmpIdArray)
            payload.addAll(getBytes2(commandArray.size))
            payload.addAll(commandArray)
            payload.add(message.believerGuarantee.ordinal)
            payload.addAll(dataArray)
            return transportLayByteGetter.getTransmitDateFromPayload(DbpMessageType.PRODUCER_SEND_MESSAGE, payload)
        }





    }

    private fun getBytes(value : Long) : List<Int>
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

    /** Little-endian unsigned short (2 байта). */
    private fun getBytes2(value: Int): List<Int> =
        listOf(value and 0xFF, (value shr 8) and 0xFF)

}
package com.duster.duster_protocol.messagefactory.bytearray.generate

import com.duster.database.data.client.Role
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
             * Ответ на логин консьюмера: успех с JWT или отказ (как `POST /auth/login` / 401).
             */
            fun loginResult(
                ok: Boolean,
                deviseId: String = "",
                role: Role = Role.DEVISE,
                accessToken: String = ""
            ): List<Int> {
                return transportLayByteGetter.getTransmitDateFromPayload(
                    DbpMessageType.BROKER_CONSUMER_LOGIN_RESULT,
                    loginResultPayload(ok, deviseId, role, accessToken)
                )
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
             * Ответ на логин продюсера: успех с JWT или отказ (как `POST /auth/login` / 401).
             */
            fun loginResult(
                ok: Boolean,
                deviseId: String = "",
                role: Role = Role.DEVISE,
                accessToken: String = ""
            ): List<Int> {
                return transportLayByteGetter.getTransmitDateFromPayload(
                    DbpMessageType.BROKER_PRODUCER_LOGIN_RESULT,
                    loginResultPayload(ok, deviseId, role, accessToken)
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
         * Консьюмер сообщает брокеру, что статус сообщения поменялся.
         * (к примеру сообщение было получено, либо выполнено)
         */
        fun messageStatusChanged(message: ConsumerMessageInDto): List<Int> {
            val idArray = getBytes(message.id)
            val payload: MutableList<Int> = mutableListOf()
            payload.addAll(idArray)
            payload.add(message.deliveryStatus!!.ordinal)
            return transportLayByteGetter.getTransmitDateFromPayload(DbpMessageType.CONSUMER_MESSAGE_STATUS_CHANDGED, payload)
        }

        /**
         * Консьюмер запрашивает у брокера, есть ли доступные для него сообщения.
         */
        fun giveMeMessage() : List<Int>
        {
            return  transportLayByteGetter.getTransmitDateFromPayload(DbpMessageType.CONSUMER_ASK_MESSAGE)
        }

        /**
         * Консьюмер логинится: `deviseId` + пароль (как тело REST `POST /auth/login`).
         * Устройство должно уже существовать в [com.duster.database.data.client.Client] (создание — `POST /admin/api/clients`).
         */
        fun login(deviseId: String, password: String): List<Int> {
            return transportLayByteGetter.getTransmitDateFromPayload(
                DbpMessageType.CONSUMER_LOGIN,
                loginPayload(deviseId, password)
            )
        }
    }

    object FromProducer {//ProducerDeliveryStatusOutDto

        /**
         * Продюсер запрашивает статус сообщения.
         */
        fun askMessageStatus(messageId: Long) : List<Int>
        {
            val idArray = getBytes(messageId)
            val payload: MutableList<Int> = mutableListOf()
            payload.addAll(idArray)
            return transportLayByteGetter.getTransmitDateFromPayload(DbpMessageType.PRODUCER_ASK_MESSAGE_STATUS, payload)

        }

        /**
         * Продюсер отправляет сообщение с полезной нагрузкой.
         */
        fun sendMessage(message: ProducerMessageInDto): List<Int> {
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

        /**
         * Продюсер логинится: `deviseId` + пароль (как тело REST `POST /auth/login`).
         * Устройство должно уже существовать в [com.duster.database.data.client.Client] (создание — `POST /admin/api/clients`).
         */
        fun login(deviseId: String, password: String): List<Int> {
            return transportLayByteGetter.getTransmitDateFromPayload(
                DbpMessageType.PRODUCER_LOGIN,
                loginPayload(deviseId, password)
            )
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

    private fun utf8WithLen(value: String): List<Int> {
        val bytes = value.toByteArray(Charsets.UTF_8).map { it.toInt() and 0xFF }
        return getBytes2(bytes.size) + bytes
    }

    private fun loginPayload(deviseId: String, password: String): List<Int> {
        val payload: MutableList<Int> = mutableListOf()
        payload.addAll(utf8WithLen(deviseId))
        payload.addAll(utf8WithLen(password))
        return payload
    }

    private fun loginResultPayload(ok: Boolean, deviseId: String, role: Role, accessToken: String): List<Int> {
        val payload: MutableList<Int> = mutableListOf()
        payload.add(if (ok) 1 else 0)
        payload.add(role.ordinal)
        payload.addAll(utf8WithLen(deviseId))
        payload.addAll(utf8WithLen(if (ok) accessToken else ""))
        return payload
    }

}
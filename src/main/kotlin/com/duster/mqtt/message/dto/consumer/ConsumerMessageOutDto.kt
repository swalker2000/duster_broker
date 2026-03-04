package com.duster.mqtt.message.dto.consumer

import com.duster.database.data.DeliveryGuarantee
import com.duster.mqtt.message.dto.MessageOutDto

/**
 * Сообщение отправляемое от брокера consumer.
 * @param id присваивается брокером (в нашей конкретной реализации базой Postgres)
 */
class ConsumerMessageOutDto(
    var id: Long = 0,

) : MessageOutDto {
    /**
     * Текущий таймстамп, чтобы устройства могли синхронизировать по нему время.
     */
    var currentTimestamp: Long = 0


    /**
     * Команда передаваемая в сообщении.
     *  - для всех одинаковых значений command рекомендуется делать одинаковую структуру поля data.
     */
    var command : String = ""


    /**
     * Ожидаем, что устройство в ответ пришлет какие-то данные.
     */
    var believerGuarantee : DeliveryGuarantee = DeliveryGuarantee.RECEIPT_CONFIRMATION


    /**
     * Дополнительные данные в формате JSON.
     */
    var data: Map<String, Any>? = null
}
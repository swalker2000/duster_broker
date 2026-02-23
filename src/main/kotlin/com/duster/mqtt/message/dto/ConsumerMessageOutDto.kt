package com.duster.mqtt.message.dto

import com.duster.database.data.DeliveryGuarantee

/**
 * Сообщение отправляемое от брокера клиенту.
 * @param id присваивается брокером (в нашей конкретной реализации базой Postgres)
 */
class ConsumerMessageOutDto(
    var id: Long = 0,

) {
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
    var believerGuarantee : DeliveryGuarantee = DeliveryGuarantee.NO


    /**
     * Дополнительные данные в формате JSON.
     */
    var data: Map<String, Any>? = null
}
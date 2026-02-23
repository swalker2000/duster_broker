package com.duster.mqtt.message.dto


/*
  {"id" : 1,  "command" : "abce", "data" : null }
 */


/**
 * Защищенный ответ от клиента к брокеру.
 * @param id сообщения ProtectedMessageOutDto на которое мы отвечаем
 */
class ConsumerMessageInDto (
    var id: Int = 0,
    ) : MessageInDto {

    /**
     * Команда передаваемая в сообщении.
     *  - для всех одинаковых значений command рекомендуется делать одинаковую структуру поля data.
     */
    var command : String = ""


    /**
     * Дополнительные данные в формате JSON.
     */
    var data: Map<String, Any>? = null
}
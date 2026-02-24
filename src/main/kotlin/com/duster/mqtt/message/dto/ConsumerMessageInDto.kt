package com.duster.mqtt.message.dto

/*
    Example :
  {"id" : 1}
 */

/**
 * Защищенный ответ от клиента к брокеру.
 * @param id сообщения ProtectedMessageOutDto на которое мы отвечаем
 */
class ConsumerMessageInDto (
    var id: Int = 0,
    ) : MessageInDto
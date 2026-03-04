package com.duster.mqtt.message.dto.consumer

import com.duster.mqtt.message.dto.MessageInDto

/*
    Example :
  {"id" : 1}
 */



/**
 * Ответ от consumer producer о том, что сообщение было принято.
 * @param id сообщения Message на которое мы отвечаем
 */
class ConsumerMessageInDto (
    var id: Int = 0,
    ) : MessageInDto
package com.duster.messagehandler.data.dto.producer.message

import com.duster.database.data.DeliveryGuarantee
import com.duster.messagehandler.data.dto.InDto

/**
 * Сообщение от producer к брокеру описывающее сообщение для consumer.
 *  - адрес consumer передается в топике
 */
class ProducerMessageInDto : InDto {


    var messageBirthCertificate : MessageBirthCertificate? =null

    /**
     * Ожидаем, что устройство в ответ пришлет какие-то данные.
     */
    var believerGuarantee : DeliveryGuarantee = DeliveryGuarantee.RECEIPT_CONFIRMATION

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


/*
    Examples:
    producer/request/{deviceId}


  {
    "believerGuarantee": "RECEIPT_CONFIRMATION",
    "command": "digitalWrite",
    "data": {
      "pinNumber": 13,
      "pinValue" : true
    }
  }

  {
    "believerGuarantee": "RECEIPT_CONFIRMATION",
    "command": "blink",
    "messageBirthCertificate" : {
        "tmpId" : 3,
        "producerDeviseId" : "0"
    },
    "data": {
      "pinNumber": 13,
      "period" : 1000,
      "count" : 5
    }
  }
 */

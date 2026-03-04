package com.duster.mqtt.message.dto.producer

import com.duster.database.data.DeliveryGuarantee
import com.duster.mqtt.message.dto.MessageInDto
import jakarta.persistence.Column
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

/*
    Examples:
    producer/request/{deviceId}
  {
    "believerGuarantee": "RECEIPT_CONFIRMATION",
    "command": "pinModeOutput",
    "data": {
      "pinNumber": "13"
    }
  }

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
    "command": "digitalWrite",
    "messageBirthCertificate" : {
        "tmpId" : 3,
        "producerDeviseId" : "0"
    },
    "data": {
      "pinNumber": 13,
      "pinValue" : true
    }
  }
 */


/**
 * Сообщение от producer к брокеру описывающее сообщение для consumer.
 *  - адрес consumer передается в топике
 */
class ProducerMessageInDto : MessageInDto {


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
package com.duster.mqtt.message.dto

import com.duster.database.data.DeliveryGuarantee
import jakarta.persistence.Column
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

class ProducerMessageInDto : MessageInDto {

    /**
     * Ожидаем, что устройство в ответ пришлет какие-то данные.
     */
    @Enumerated(EnumType.ORDINAL)
    var believerGuarantee : DeliveryGuarantee = DeliveryGuarantee.NO

    /**
     * Команда передаваемая в сообщении.
     *  - для всех одинаковых значений command рекомендуется делать одинаковую структуру поля data.
     */
    @Column(nullable = false)
    var command : String = ""


    /**
     * Дополнительные данные в формате JSON.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    var data: Map<String, Any>? = null
}
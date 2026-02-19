package com.duster.database.data

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.*


/**
 * Описывает исходящее сообщение отправляемое IOT клиенту.
 */
@Table
@Entity
class Message {


    /**
     * Уникальный ID конкретного сообщения. Id сообщения передается вместе с сообщением.
     */
    @Id
    @GeneratedValue
    var id : Int = 0

    /**
     * Топик по которому должно быть отправлено сообщение
     */
    @Column(nullable = false)
    var topic : String? = ""

    /**
     *  Время создания задачи.
     */
    @Column
    var createdDate : Date = Date(System.currentTimeMillis())



    /**
     *  Время последней (не важно удачной или нет) оправки задачи.
     */
    @Column
    var deliveredDate : Date? = null

    /**
     * Сообщение доставлено.
     */
    @Column
    var delivered : Boolean = false


    /**
     * True - только если была не успешная попытка отправки сообщения и сообщения еще не доставлено.
     */
    @Column
    var deliveredError : Boolean = false

    /**
     * Ожидаем, что устройство в ответ пришлет какие-то данные.
     */
    @Enumerated(EnumType.ORDINAL)
    var deliveryGuarantee : DeliveryGuarantee = DeliveryGuarantee.NO

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
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

    //------------Данные получателя (consumer)-----------

    /**
     * Id устройства которому мы шлем сообщение.
     */
    @Column(nullable = false)
    var deviseId : String = ""

    //------------Данные отправителя (producer)-----------
    //:TODO убрать в отдельную таблицу и сделать связь oneToOne

    /**
     * Временный id который producer присваивает сообщению.
     *  - нужен только если мы хотим подписаться на изменение статуса сообщения, если не хотим, этот параметр не передаем
     * В ответном сообщении мы возвращаем этот tmpId и постоянный id сообщения, что бы producer мог их сматчить и уже
     * ориентироваться на постоянный id при получении статуса доставки.
     * WARN : брокер не проверяет его уникальность.
     */
    @Column
    var tmpId : Int? = null

    /**
     * Id producer отправившего сообщение. Если у producer есть deviseId, то использовать нужно его.
     * - нужен только если мы хотим подписаться на изменение статуса сообщения, если не хотим, этот параметр не передаем
     * - если со стороны сервера у нас монолит, то хорошим тоно является producerDeviseId равный 0
     */
    @Column
    var producerDeviseId : String? = null



    /**
     * Подписался ли producer на получение уведомлений о данном сообщении.
     */
    fun isProducerSubscribed(): Boolean {
        return producerDeviseId != null && tmpId != null
    }



    //----------------------Даты-------------------------

    /**
     *  Время создания задачи.
     */
    @Column
    var createdDate : Date = Date(System.currentTimeMillis())


    /**
     *  Время оправки задачи.
     *  (время когда мы получили ответ от устройства, что сообщение доставлено)
     */
    @Column
    var deliveredDate : Date? = null


    //----------------------Статусы классификации-------------------------

    /**
     * Сообщение доставлено.
     */
    @Enumerated(EnumType.ORDINAL)
    var deliveryStatus : DeliveryStatus = DeliveryStatus.NOT_DELIVERED


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


    //----------------------Полезная нагрузка-------------------------

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
package com.duster.database.data.savedmessages

import com.duster.database.data.client.Client
import com.duster.database.data.message.DeliveryGuarantee
import com.duster.database.data.message.DeliveryStatus
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.*


/**?
 * Сообщение которое пользователь сохранил, что бы отправлять другим пользователям.
 */
@Table
@Entity
class SavedMessage {


    @Id
    @GeneratedValue
    var id : Int = 0

    //------------Данные получателя (consumer)-----------

    /**
     * Клиент которому адресовано сообщение.
     */
    @ManyToOne
    var client : Client? = null


    //----------------------Статусы классификации-------------------------


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
package com.duster.transport.data.dto.producer.message

import com.duster.database.data.DeliveryStatus
import com.duster.transport.data.dto.OutDto

/**
 * Сообщение от брокера к producer с описанием статуса отправленного producer сообщения.
 * @param id сообщения Message статус которого мы хотим передать. (данный id генерируется базой данных)
 * @param tmpId временный id, который присвоил producer сообщению.
 * @param deliveryStatus статус доставки сообщения отправленного producer
 */
class ProducerMessageOutDto(
    var id: Int,
    var tmpId : Int?,
    var deliveryStatus: DeliveryStatus
) : OutDto
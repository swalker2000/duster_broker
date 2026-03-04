package com.duster.mqtt.message.dto.producer

import com.duster.database.data.DeliveryStatus
import com.duster.mqtt.message.dto.MessageOutDto

/**
 * Сообщение от брокера к producer с описанием статуса отправленного producer сообщения.
 * @param id сообщения Message статус которого мы хотим передать. (данный id генерируется базой данных)
 * @param tmpId временный id, который присвоил producer сообщению.
 * @param deliveryStatus статус доставки сообщения отправленного producer
 */
class ProducerMessageOutDto(
    var id: Int,
    var tmpId : Int,
    var deliveryStatus: DeliveryStatus
) : MessageOutDto
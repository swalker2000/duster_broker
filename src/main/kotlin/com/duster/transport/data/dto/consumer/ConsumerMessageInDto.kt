package com.duster.transport.data.dto.consumer

import com.duster.database.data.DeliveryStatus
import com.duster.transport.data.dto.InDto

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
    ) : InDto
{
    /**
     * Статус доставки сообщения.
     *  - Если consumer возвращает ответ без этого поля считаем что сообщение доставлено (DeliveryStatus.DELIVERED).
     */
    var deliveryStatus : DeliveryStatus? = null
    get() = if (field == null) DeliveryStatus.DELIVERED else field
}
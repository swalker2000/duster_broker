package com.duster.transport.data.dto.producer

import com.duster.database.data.DeliveryStatus
import com.duster.transport.data.dto.OutDto

/**
 * Сообщение описывающее статус доставки Message к consumer.
 */
data class ProducerDeliveryStatusOutDto( val deliveryStatus : DeliveryStatus) : OutDto
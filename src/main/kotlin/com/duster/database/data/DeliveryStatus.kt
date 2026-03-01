package com.duster.database.data

/**
 * Статус доставки сообщения.
 */
enum class DeliveryStatus {
    /**
     * Сообщение доставлено.
     */
    DELIVERED,

    /**
     * Сообщение не доставлено.
     */
    NOT_DELIVERED,

    /**
     * Доставка отменена.
     */
    CANCELLED,

    /**
     * Статус доставки не известен.
     */
    UNKNOWN
}
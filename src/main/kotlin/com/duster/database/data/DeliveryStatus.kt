package com.duster.database.data

/**
 * Статус доставки сообщения.
 * @param canReceiveFromConsumer данный статус может быть получен от consumer.
 */
enum class DeliveryStatus(val canReceiveFromConsumer: Boolean) {

    /**
     * Задача, отправленная в сообщении, не выполнена или выполнена с ошибкой.
     * (высылается только после DELIVERED)
     */
    COMPLETED_WITH_ERROR(true),

    /**
     * Задача отправленная в сообщении выполнена.
     * (высылается только после DELIVERED)
     */
    COMPLETED(true),

    /**
     * Сообщение доставлено.
     */
    DELIVERED(true),

    /**
     * Сообщение не доставлено.
     */
    NOT_DELIVERED(false),

    /**
     * Доставка отменена.
     */
    CANCELLED(false),

    /**
     * Статус доставки не известен.
     */
    UNKNOWN(false)
}
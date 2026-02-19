package com.duster.database.data

/**
 * Временная политика отправки сообщения.
 */
enum class TimePolicy {
    /**
     * Отправить прямо сейчас.
     */
    CURRENT,

    /**
     * Временная задержка при отправке сообщения.
     */
    DELAY
}
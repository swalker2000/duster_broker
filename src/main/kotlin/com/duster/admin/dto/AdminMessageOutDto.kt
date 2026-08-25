package com.duster.admin.dto

/**
 * Сводка сообщения для админ-интерфейса (без внутренних деталей JPA).
 */
data class AdminMessageOutDto(
    val id: Long,
    val deviseId: String,
    val command: String,
    val deliveryStatus: String,
    val deliveryGuarantee: String,
    val createdDate: Long,
    val deliveredDate: Long?,
    val tmpId: Long?,
    val producerDeviseId: String?,
    val deliveredError: Boolean,
    val data: Map<String, Any>?
)
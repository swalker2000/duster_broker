package com.duster.admin.dto

/**
 * Сохраняемый шаблон сообщения для админ-интерфейса.
 */
data class AdminSavedMessageOutDto(
    val id: Int,
    val clientId: Int?,
    val deviseId: String?,
    val description: String,
    val command: String,
    val deliveryGuarantee: String,
    val data: Map<String, Any>?
)
package com.duster.admin

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

data class AdminSavedMessageCreateInDto(
    val clientId: Int,
    val description: String = "",
    val command: String = "",
    val deliveryGuarantee: String? = null,
    val data: Map<String, Any>? = null
)

data class AdminSavedMessageSendInDto(
    val deviseId: String = ""
)

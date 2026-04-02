package com.duster.admin.dto

data class AdminSavedMessageCreateInDto(
    val clientId: Int,
    val description: String = "",
    val command: String = "",
    val deliveryGuarantee: String? = null,
    val data: Map<String, Any>? = null
)

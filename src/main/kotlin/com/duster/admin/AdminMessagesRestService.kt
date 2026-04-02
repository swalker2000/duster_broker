package com.duster.admin

import com.duster.admin.dto.AdminMessageOutDto
import com.duster.database.MessageRepository
import com.duster.database.data.message.Message
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/api/messages")
class AdminMessagesRestService(
    private val messageRepository: MessageRepository
) {

    @GetMapping
    fun list(
        @RequestParam(required = false) deviseId: String?,
        @RequestParam(defaultValue = "200") limit: Int
    ): List<AdminMessageOutDto> {
        val size = limit.coerceIn(1, 500)
        val pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdDate"))
        val page = if (deviseId.isNullOrBlank()) {
            messageRepository.findAllByOrderByCreatedDateDesc(pageable)
        } else {
            messageRepository.findAllByDeviseIdOrderByCreatedDateDesc(deviseId.trim(), pageable)
        }
        return page.content.map { it.toAdminDto() }
    }

    private fun Message.toAdminDto(): AdminMessageOutDto =
        AdminMessageOutDto(
            id = id,
            deviseId = deviseId,
            command = command,
            deliveryStatus = deliveryStatus.name,
            deliveryGuarantee = deliveryGuarantee.name,
            createdDate = createdDate.time,
            deliveredDate = deliveredDate?.time,
            tmpId = tmpId,
            producerDeviseId = producerDeviseId,
            deliveredError = deliveredError,
            data = data
        )
}

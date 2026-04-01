package com.duster.admin

import com.duster.common.CommonMessageService
import com.duster.database.ClientRepository
import com.duster.database.SavedMessageRepository
import com.duster.database.data.client.Role
import com.duster.database.data.savedmessages.SavedMessage
import com.duster.database.data.message.DeliveryGuarantee
import com.duster.transport.data.dto.producer.message.ProducerMessageInDto
import com.duster.transport.data.dto.producer.message.ProducerMessageOutDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/admin/api/saved-messages")
class AdminSavedMessagesRestService(
    private val savedMessageRepository: SavedMessageRepository,
    private val clientRepository: ClientRepository,
    private val commonMessageService: CommonMessageService
) {

    @GetMapping
    fun list(
        @RequestParam(required = false) clientId: Int?,
        @RequestParam(required = false) deviseId: String?
    ): List<AdminSavedMessageOutDto> {
        val list = when {
            clientId != null ->
                savedMessageRepository.findAllByClient_IdOrderByIdDesc(clientId)
            !deviseId.isNullOrBlank() ->
                savedMessageRepository.findAllByClient_DeviseIdOrderByIdDesc(deviseId.trim())
            else ->
                savedMessageRepository.findAllByOrderByIdDesc()
        }
        return list.map { it.toDto() }
    }

    @GetMapping("/{id}")
    fun getOne(@PathVariable id: Int): AdminSavedMessageOutDto {
        val sm = savedMessageRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "SavedMessage id=$id not found")
        }
        return sm.toDto()
    }

    @PostMapping
    fun create(@RequestBody body: AdminSavedMessageCreateInDto): ResponseEntity<AdminSavedMessageOutDto> {
        val desc = body.description.trim()
        val cmd = body.command.trim()
        if (desc.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "description is required")
        }
        if (cmd.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "command is required")
        }
        val client = clientRepository.findById(body.clientId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Client id=${body.clientId} not found")
        }
        if (client.role != Role.DEVISE) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Saved templates can only be bound to clients with role DEVISE"
            )
        }
        val guarantee = parseDeliveryGuarantee(body.deliveryGuarantee)
        val entity = SavedMessage().apply {
            this.client = client
            description = desc
            command = cmd
            deliveryGuarantee = guarantee
            data = body.data
        }
        val saved = savedMessageRepository.save(entity)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toDto())
    }

    @DeleteMapping("/{id}")
    fun remove(@PathVariable id: Int): ResponseEntity<Void> {
        if (!savedMessageRepository.existsById(id)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "SavedMessage id=$id not found")
        }
        savedMessageRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/send")
    fun sendToDevice(
        @PathVariable id: Int,
        @RequestBody body: AdminSavedMessageSendInDto
    ): ProducerMessageOutDto {
        val target = body.deviseId.trim()
        if (target.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "deviseId is required")
        }
        val sm = savedMessageRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "SavedMessage id=$id not found")
        }
        val dto = ProducerMessageInDto().apply {
            believerGuarantee = sm.deliveryGuarantee
            command = sm.command
            data = sm.data
        }
        return commonMessageService.newProducerMessageIn(dto, target).orElseThrow {
            ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to send saved message to: $target"
            )
        }
    }

    private fun SavedMessage.toDto(): AdminSavedMessageOutDto =
        AdminSavedMessageOutDto(
            id = id,
            clientId = client?.id,
            deviseId = client?.deviseId,
            description = description,
            command = command,
            deliveryGuarantee = deliveryGuarantee.name,
            data = data
        )

    private fun parseDeliveryGuarantee(raw: String?): DeliveryGuarantee {
        if (raw.isNullOrBlank()) return DeliveryGuarantee.NO
        return try {
            DeliveryGuarantee.valueOf(raw.trim())
        } catch (_: IllegalArgumentException) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid deliveryGuarantee: $raw"
            )
        }
    }
}

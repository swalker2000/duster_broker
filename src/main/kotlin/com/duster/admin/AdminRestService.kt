package com.duster.admin

import com.duster.database.ClientRepository
import com.duster.database.data.client.Client
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/admin/api/clients")
class AdminRestService(
    private val clientRepository: ClientRepository
) {

    @GetMapping
    fun getAll(): List<Client> = clientRepository.findAll()

    @GetMapping("/{id}")
    fun getOne(@PathVariable id: Int): Client =
        clientRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Client id=$id not found")
        }

    @PostMapping
    fun create(@RequestBody client: Client): ResponseEntity<Client> {
        if (client.deviseId.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "deviseId is required")
        }
        if (clientRepository.existsByDeviseId(client.deviseId)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "deviseId already exists: ${client.deviseId}")
        }
        client.id = 0
        val saved = clientRepository.save(client)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved)
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Int, @RequestBody body: Client): Client {
        val existing = clientRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Client id=$id not found")
        }
        if (body.deviseId.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "deviseId is required")
        }
        if (clientRepository.existsByDeviseIdAndIdNot(body.deviseId, id)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "deviseId already exists: ${body.deviseId}")
        }
        existing.deviseId = body.deviseId
        existing.password = body.password
        existing.role = body.role
        existing.description = body.description
        return clientRepository.save(existing)
    }

    @DeleteMapping("/{id}")
    fun remove(@PathVariable id: Int): ResponseEntity<Void> {
        if (!clientRepository.existsById(id)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Client id=$id not found")
        }
        clientRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }
}

package com.duster.database

import com.duster.database.data.client.Client
import com.duster.database.data.client.Role
import org.springframework.data.jpa.repository.JpaRepository

interface ClientRepository : JpaRepository<Client, Int> {
    fun findByDeviseId(deviseId: String): Client?
    fun existsByDeviseId(deviseId: String): Boolean
    fun existsByDeviseIdAndIdNot(deviseId: String, id: Int): Boolean
    fun existsByRole(role: Role): Boolean
}
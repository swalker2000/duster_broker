package com.duster.database

import com.duster.database.data.client.Client
import org.springframework.data.jpa.repository.JpaRepository

interface ClientRepository : JpaRepository<Client, Int> {
    fun existsByDeviseId(deviseId: String): Boolean
    fun existsByDeviseIdAndIdNot(deviseId: String, id: Int): Boolean
}

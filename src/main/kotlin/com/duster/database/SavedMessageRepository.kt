package com.duster.database

import com.duster.database.data.savedmessages.SavedMessage
import org.springframework.data.jpa.repository.JpaRepository

interface SavedMessageRepository : JpaRepository<SavedMessage, Int> {
    fun findAllByOrderByIdDesc(): List<SavedMessage>
    fun findAllByClient_IdOrderByIdDesc(clientId: Int): List<SavedMessage>
    fun findAllByClient_DeviseIdOrderByIdDesc(deviseId: String): List<SavedMessage>
}

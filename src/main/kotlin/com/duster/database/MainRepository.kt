package com.duster.database

import com.duster.database.data.Message
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Date
import java.util.function.Consumer

@Repository
class MainRepository {

    @Autowired
    private lateinit var messageRepository: MessageRepository

    /**
     * Сохранить новое сообщение.
     */
    fun saveMessage(message: Message) : Message {
        return messageRepository.save(message)
    }

    /**
     * Обновить статус доставки сообщения.
     */
    fun updateDeliveryStatus(
        id: Int,
        delivered: Boolean,
        deliveredError: Boolean,
        deliveredDate: Date
    ): Int
    {
        return messageRepository.updateDeliveryStatus(id, delivered, deliveredError, deliveredDate)
    }
}
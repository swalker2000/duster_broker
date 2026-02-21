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
     * Найти все не доставленные созданные раньше createDate.
     *  - вывод отсортирован по дате создания (по возрастанию).
     *  @param searchBefore дата до которой выполняется поиск
     */
    fun findNotDeliveredMessages(
        searchBefore: Date
    ): List<Message>
    {
        return messageRepository.findAllByDeliveredAndCreatedDateLessThanOrderByCreatedDateAsc(false, searchBefore)
    }

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
        deliveredDate: Date
    ): Int
    {
        return messageRepository.updateDeliveryStatus(id, delivered,  deliveredDate)
    }

    /**
     * Обновить флаг ошибки доставки для сообщения с заданным id.
     * @return количество обновленных данных в базе.
     */
    fun updateDeliveryError(
        id: Int,
        deliveredError: Boolean
    ): Int
    {
        return messageRepository.updateDeliveryError(id, deliveredError)
    }
}
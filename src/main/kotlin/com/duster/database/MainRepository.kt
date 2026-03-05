package com.duster.database

import com.duster.database.data.DeliveryGuarantee
import com.duster.database.data.DeliveryStatus
import com.duster.database.data.Message
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.Date
import java.util.Optional

@Repository
class MainRepository {

    @Autowired
    private lateinit var messageRepository: MessageRepository

    /**
     * Получить самое старое не доставленное сообщение для устройства.
     */
    fun findOldestNotDeliveredMessageForDevise(
        deviseId: String,
    ) : Optional<Message>
    {
        return messageRepository.findFirstByDeviseIdAndDeliveryStatusOrderByCreatedDateAsc(deviseId, DeliveryStatus.NOT_DELIVERED)
    }

    /**
     * Получить статус доставки сообщения по его id.
     * @param id идентификатор сообщения
     * @return статус доставки, или null если сообщение с таким id не найдено
     */
    fun findDeliveredById(id: Int): Optional<DeliveryStatus> {
        return messageRepository.findDeliveredById(id)
    }

    /**
     * Проверяет, существует ли хотя бы одно не доставленное сообщение для данного deviceId.
     * @param deviseId идентификатор устройства
     * @return true, если есть хотя бы одно сообщение с delivered = false для указанного deviseId
     */
    fun existsByDeviseIdAndDeliveredFalse(deviseId: String): Boolean
    {
        return messageRepository.existsByDeviseIdAndDeliveryStatus(deviseId, DeliveryStatus.NOT_DELIVERED)
    }

    /**
     * Найти все не доставленные созданные раньше createDate.
     *  - вывод отсортирован по дате создания (по возрастанию).
     *  @param searchBefore дата до которой выполняется поиск
     */
    fun findNotDeliveredMessages(
        searchBefore: Date
    ): List<Message>
    {
        return messageRepository.findAllByDeliveryStatusAndCreatedDateLessThanOrderByCreatedDateAsc(DeliveryStatus.NOT_DELIVERED, searchBefore)
    }

    /**
     * Сохранить новое сообщение.
     * WARN : если сообщение имеет DeliveryGuarantee ONLY_LAST, всем сообщения с данной командой для данного устройства
     * отправленные до этого будет проставлен статус доставки CANCELLED
     */
    @Transactional
    fun saveNewMessage(message: Message) : Message {
        if(message.deliveryGuarantee==DeliveryGuarantee.ONLY_LAST)
            messageRepository.updateDeliveryStatus(message.command, message.deviseId, DeliveryStatus.CANCELLED)
        return messageRepository.save(message)
    }


    /**
     * Обновить статус доставки сообщения.
     * @return наше сообщение с обновленным статусом.
     */
    @Transactional
    fun updateDeliveryStatus(
        id: Int,
        deliveryStatus: DeliveryStatus,
        deliveredDate: Date
    )  : Message
    {
        messageRepository.updateDeliveryStatus(id, deliveryStatus,  deliveredDate)
        return messageRepository.findById(id).get()
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
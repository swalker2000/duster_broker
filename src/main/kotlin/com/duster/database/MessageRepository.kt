package com.duster.database

import com.duster.database.data.Message
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.util.Date
import java.util.Optional

interface MessageRepository: JpaRepository<Message, Int> {




    /**
     * Проверяет, существует ли хотя бы одно не доставленное сообщение для данного deviceId.
     * @param deviseId идентификатор устройства
     * @return true, если есть хотя бы одно сообщение с delivered = false для указанного deviseId
     */
    fun existsByDeviseIdAndDeliveredFalse(deviseId: String): Boolean


    /**
     * Получить статус доставки сообщения по его id.
     * @param id идентификатор сообщения
     * @return true если сообщение доставлено, false если нет, или null если сообщение с таким id не найдено
     */
    @Query("SELECT m.delivered FROM Message m WHERE m.id = :id")
    fun findDeliveredById(@Param("id") id: Int): Optional<Boolean>

    /**
     * Найти все сообщения по значению флага delivered, и созданные раньше createDate.
     *  - вывод отсортирован по дате создания (по возрастанию).
     *  @param delivered было ли сообщение доставлено
     *  @param searchBefore дата до которой выполняется поиск
     */
    fun findAllByDeliveredAndCreatedDateLessThanOrderByCreatedDateAsc(
        delivered: Boolean,
        searchBefore: Date
    ): List<Message>


    /**
     * Обновить статус доставки для сообщения с заданным id.
     * Устанавливает поля delivered и sendDate.
     * @return количество обновленных данных в базе.
     */
    @Transactional
    @Modifying
    @Query(
        "UPDATE Message m SET m.delivered = :delivered, " +
                "m.deliveredDate = :deliveredDate WHERE m.id = :id")
    fun updateDeliveryStatus(
        @Param("id") id: Int,
        @Param("delivered") delivered: Boolean,
        @Param("deliveredDate") deliveredDate: Date
    ): Int

    /**
     * Обновить флаг ошибки доставки для сообщения с заданным id.
     * @return количество обновленных данных в базе.
     */
    @Transactional
    @Modifying
    @Query(
        "UPDATE Message m SET  m.deliveredError = :deliveredError WHERE m.id = :id")
    fun updateDeliveryError(
        @Param("id") id: Int,
        @Param("deliveredError") deliveredError: Boolean
    ): Int
}
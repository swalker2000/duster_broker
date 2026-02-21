package com.duster.database

import com.duster.database.data.Message
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Date

interface MessageRepository: JpaRepository<Message, Int> {
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
     * Устанавливает поля delivered, deliveredError и sendDate.
     * @return количество обновленных данных в базе.
     */
    @Deprecated("Usee updateDeliveryError to update deliveredError")
    @Modifying
    @Query(
        "UPDATE Message m SET m.delivered = :delivered, m.deliveredError = :deliveredError, " +
            "m.deliveredDate = :deliveredDate WHERE m.id = :id")
    fun updateDeliveryStatus(
        @Param("id") id: Int,
        @Param("delivered") delivered: Boolean,
        @Param("deliveredDate") deliveredDate: Date,
        @Param("deliveredError") deliveredError: Boolean
    ): Int

    /**
     * Обновить статус доставки для сообщения с заданным id.
     * Устанавливает поля delivered и sendDate.
     * @return количество обновленных данных в базе.
     */
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
    @Modifying
    @Query(
        "UPDATE Message m SET  m.deliveredError = :deliveredError WHERE m.id = :id")
    fun updateDeliveryError(
        @Param("id") id: Int,
        @Param("deliveredError") deliveredError: Boolean
    ): Int
}
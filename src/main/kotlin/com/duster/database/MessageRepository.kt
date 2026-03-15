package com.duster.database

import com.duster.database.data.DeliveryStatus
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
     * Проверяет, существует ли хотя бы одно не доставленное сообщение для данного deviceId. с данным статусом доставки.
     * @param deviseId идентификатор устройства
     * @param deliveryStatus статус доставки
     * @return true, если есть хотя бы одно сообщение с delivered = false для указанного deviseId
     */
    fun existsByDeviseIdAndDeliveryStatus(deviseId: String,deliveryStatus: DeliveryStatus): Boolean


    /**
     * Получить статус доставки сообщения по его id.
     * @param id идентификатор сообщения
     * @return true если сообщение доставлено, false если нет, или null если сообщение с таким id не найдено
     */
    @Query("SELECT m.deliveryStatus FROM Message m WHERE m.id = :id")
    fun findDeliveredById(@Param("id") id: Int): Optional<DeliveryStatus>

    /**
     * Найти все сообщения по значению флага delivered, и созданные раньше createDate.
     *  - вывод отсортирован по дате создания (по возрастанию).
     *  @param deliveredStatus статус доставки сообщения
     *  @param searchBefore дата до которой выполняется поиск
     */
    fun findAllByDeliveryStatusAndCreatedDateLessThanOrderByCreatedDateAsc(
        deliveredStatus: DeliveryStatus,
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
        "UPDATE Message m SET m.deliveryStatus = :deliveryStatus, " +
                "m.deliveredDate = :deliveredDate WHERE m.id = :id")
    fun updateDeliveryStatus(
        @Param("id") id: Int,
        @Param("deliveryStatus") deliveryStatus: DeliveryStatus,
        @Param("deliveredDate") deliveredDate: Date
    ): Int

    /**
     * Обновить статус доставки для сообщений.
     * Устанавливает поля deliveryStatus.
     * @param command обновления коснутся только сообщений с этой командой
     * @param deviseId обновления коснутся только сообщений с данным deviseId
     * @return количество обновленных данных в базе.
     */
    @Transactional
    @Modifying
    @Query(
        "UPDATE Message m SET m.deliveryStatus = :deliveryStatus " +
                "WHERE m.command = :command AND " +
                "m.deviseId = :deviseId AND " +
                "m.deliveryStatus = com.duster.database.data.DeliveryStatus.NOT_DELIVERED ")
    fun updateDeliveryStatus(
        @Param("command") command: String,
        @Param("deviseId") deviseId: String,
        @Param("deliveryStatus") deliveryStatus: DeliveryStatus
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

    /**
     * Находит самое старое (с наименьшей датой создания) сообщение для указанного устройства и статуса доставки.
     *
     * @param deviseId идентификатор устройства-получателя
     * @param deliveryStatus статус доставки сообщения
     * @return Optional с найденным сообщением, или пустой Optional, если сообщение не найдено
     */
    fun findFirstByDeviseIdAndDeliveryStatusOrderByCreatedDateAsc(
        deviseId: String,
        deliveryStatus: DeliveryStatus
    ): Optional<Message>


}
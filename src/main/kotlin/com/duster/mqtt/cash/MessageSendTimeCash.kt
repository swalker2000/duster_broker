package com.duster.mqtt.cash

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

/**
 * Кэш хранящий время последнего отправленного сообщения для каждого устройства.
 *  - используется для проверки возможности отправки на устройство сообщения прямо сейчас не вызывая ddos атаки.
 */
@Component
class MessageSendTimeCash {

    private val logger = LoggerFactory.getLogger(MessageSendTimeCash::class.java)

    /**
     * Как далеко мы смотрим в прошлое для очистки кеша.
     */
    @Value("\${common.messageSendTimeCash.collectorRunPeriod}")
    private  val collectorRemovePeriod : Long = -1


    private val deviseIdToDate = ConcurrentHashMap<String, Date>()

    /**
     * Сохранить в кэш, что на это устройство сейчас отправляется сообщение.
     * @param deviseId id устройства
     * @param period период с которым можно на это устройство отправлять это сообщение не вызывая ddos
     */
    fun updateForDevise(deviseId : String, period : Long) {
        deviseIdToDate[deviseId] = Date(System.currentTimeMillis()+period)
    }

    /**
     * Можно ли данному устройству отправить сейчас сообщение?
     * @param deviseId id устройства
     * @return можно ли данному устройству отправить сейчас сообщение
     */
    private fun isAvailable(deviseId : String) : Boolean {
        return if(deviseIdToDate[deviseId] != null) {
            System.currentTimeMillis() - deviseIdToDate[deviseId]!!.time > 0
        } else true
    }

    /**
     * Сохранить в кэш, что на это устройство сейчас отправляется сообщение если сейчас отправить можно.
     * @param deviseId id устройства
     * @param period период с которым можно на это устройство отправлять это сообщение не вызывая ddos
     * @return можно ли было отправить на устройство сообщение до вызова метода.
     */
    @Synchronized
    fun updateForDeviseIfAvailable(deviseId : String, period : Long) : Boolean
    {
        if(isAvailable(deviseId)) {
            updateForDevise(deviseId, period)
            return true
        }
        else return false
    }

    @Scheduled(fixedRateString = "\${common.messageSendTimeCash.collectorRunPeriod}")
    private fun collector()
    {
        logger.info("Run collector.")
        val threshold = System.currentTimeMillis() - collectorRemovePeriod
        deviseIdToDate.entries.removeIf { it.value.time < threshold }
    }
}
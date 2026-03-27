package com.duster.database.data.client

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * Описывает подключаемое устройство или мясного пользователя его логины и пароли.
 */
@Table
@Entity
class Client {
    /**
     * Уникальный ID конкретного устройства. Не используется в dto классах.
     */
    @Id
    @GeneratedValue
    var id : Int = 0

    /**
     * Имя устройства оно же Id устройства которому мы шлем сообщение. По нему выполняем авторизацию.
     */
    @Column(nullable = false, unique = true)
    var deviseId : String = ""

    /**
     * Пароль или хеш пароля по которому устройство авторизуется.
     */
    @Column(nullable = false)
    var password : String = ""

    /**
     * Роль пользователя.
     */
    @Column(nullable = false)
    var role = Role.DEVISE


    /**
     * Текстовое описание.
     * (что за устройство)
     */
    @Column(nullable = false)
    var description = ""
}
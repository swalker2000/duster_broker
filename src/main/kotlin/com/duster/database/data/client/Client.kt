package com.duster.database.data.client

import com.duster.database.data.savedmessages.SavedMessage
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
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
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
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

    /**
     * Список сохраненных сообщений для клиента.
     * Не сериализуется в JSON — иначе цикл Client ↔ SavedMessage даёт превышение глубины вложенности.
     */
    @JsonIgnore
    @OneToMany(mappedBy = "client", cascade = [(CascadeType.ALL)])
    var savedMessage : List<SavedMessage> = emptyList()
}
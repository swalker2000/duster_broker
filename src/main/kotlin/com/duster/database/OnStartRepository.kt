package com.duster.database

import com.duster.database.data.client.Client
import com.duster.database.data.client.Role
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

/**
 * При старте приложения гарантирует наличие хотя бы одного клиента с ролью [Role.MAN]:
 * если таких нет и логин [DEFAULT_LOGIN] свободен — создаётся запись с паролем [DEFAULT_PASSWORD] (bcrypt).
 */
@Component
class OnStartRepository(
    private val clientRepository: ClientRepository,
    private val passwordEncoder: PasswordEncoder
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(OnStartRepository::class.java)

    override fun run(args: ApplicationArguments) {
        if (clientRepository.existsByRole(Role.MAN)) {
            return
        }
        if (clientRepository.existsByDeviseId(DEFAULT_LOGIN)) {
            log.warn(
                "Клиентов с ролью MAN нет, но deviseId '{}' уже занят — создайте пользователя MAN вручную",
                DEFAULT_LOGIN
            )
            return
        }
        val client = Client().apply {
            deviseId = DEFAULT_LOGIN
            password = passwordEncoder.encode(DEFAULT_PASSWORD)!!
            role = Role.MAN
            description = "Автоматически созданный администратор (смените пароль)"
        }
        clientRepository.save(client)
        log.warn(
            "Создан пользователь MAN: deviseId='{}', пароль='{}' — смените пароль в продакшене",
            DEFAULT_LOGIN,
            DEFAULT_PASSWORD
        )
    }

    companion object {
        private const val DEFAULT_LOGIN = "admin"
        private const val DEFAULT_PASSWORD = "admin"
    }
}

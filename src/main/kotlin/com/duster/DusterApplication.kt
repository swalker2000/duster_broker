package com.duster

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class DusterApplication

/*
swagger ip : http://localhost:8080/swagger-ui.html
*/

fun main(args: Array<String>) {
    runApplication<DusterApplication>(*args)
}

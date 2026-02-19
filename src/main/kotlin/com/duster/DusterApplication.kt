package com.duster

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DusterApplication

fun main(args: Array<String>) {
    runApplication<DusterApplication>(*args)
}

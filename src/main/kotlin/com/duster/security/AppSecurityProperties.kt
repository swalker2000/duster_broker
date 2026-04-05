package com.duster.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.security")
class AppSecurityProperties {
    var permitAll: Boolean = false

    /** IP или CIDR; эндпоинты producer/consumer REST с этих адресов без JWT. */
    var trustedRestIps: List<String> = emptyList()

    /** Доп. список через запятую (удобно для env в Docker: TRUSTED_REST_IPS). */
    var trustedRestIpsCsv: String = ""

    fun resolvedTrustedRestIps(): List<String> {
        val fromCsv = trustedRestIpsCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return (trustedRestIps + fromCsv).distinct()
    }
}

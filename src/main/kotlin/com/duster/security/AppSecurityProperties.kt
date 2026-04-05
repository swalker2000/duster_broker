package com.duster.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.security")
class AppSecurityProperties {
    var permitAll: Boolean = false

    /**
     * IP/CIDR, с которых эндпоинты /producer/** и /consumer/** доступны без JWT.
     */
    var restAuthWhitelistIps: List<String> = emptyList()
}

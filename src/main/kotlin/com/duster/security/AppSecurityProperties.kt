package com.duster.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.security")
class AppSecurityProperties {
    var permitAll: Boolean = false
}

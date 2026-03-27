package com.duster.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.jwt")
class JwtProperties {
    var secret: String = ""
    var expirationMs: Long = 86400000
}

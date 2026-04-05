package com.duster.security

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.web.util.matcher.IpAddressMatcher
import org.springframework.security.web.util.matcher.RequestMatcher

/** Запросы к REST producer и consumer с IP из белого списка; CIDR см. [IpAddressMatcher]. */
class RestTrustedIpRequestMatcher(
    trustedIpPatterns: List<String>
) : RequestMatcher {

    private val ipMatchers: List<IpAddressMatcher> = trustedIpPatterns
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { IpAddressMatcher(it) }

    override fun matches(request: HttpServletRequest): Boolean {
        if (ipMatchers.isEmpty()) return false
        if (!isProducerOrConsumerPath(request)) return false
        val clientIp = resolveClientIp(request) ?: return false
        return ipMatchers.any { it.matches(clientIp) }
    }

    private fun isProducerOrConsumerPath(request: HttpServletRequest): Boolean {
        val path = (request.requestURI ?: return false)
            .removePrefix(request.contextPath ?: "")
            .substringBefore('?')
        return path == "/producer" || path.startsWith("/producer/") ||
            path == "/consumer" || path.startsWith("/consumer/")
    }

    private fun resolveClientIp(request: HttpServletRequest): String? {
        val forwarded = request.getHeader("X-Forwarded-For")
        if (!forwarded.isNullOrBlank()) {
            val first = forwarded.split(",").first().trim()
            if (first.isNotEmpty()) return first
        }
        val realIp = request.getHeader("X-Real-IP")?.trim()
        if (!realIp.isNullOrBlank()) return realIp
        return request.remoteAddr?.takeIf { it.isNotBlank() }
    }
}

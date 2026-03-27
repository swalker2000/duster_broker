package com.duster.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (!header.isNullOrBlank() && header.startsWith("Bearer ")) {
            val token = header.substring(7).trim()
            if (token.isNotEmpty()) {
                try {
                    val claims = jwtService.parseToken(token)
                    val authorities = listOf(SimpleGrantedAuthority("ROLE_${claims.role}"))
                    val auth = UsernamePasswordAuthenticationToken(claims.deviseId, null, authorities)
                    auth.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = auth
                } catch (e: Exception) {
                    log.debug("JWT rejected: {}", e.message)
                    SecurityContextHolder.clearContext()
                }
            }
        }
        filterChain.doFilter(request, response)
    }
}

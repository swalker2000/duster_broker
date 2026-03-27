package com.duster.security

import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties::class, AppSecurityProperties::class)
class SecurityConfiguration(
    private val jwtService: JwtService,
    private val appSecurityProperties: AppSecurityProperties
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun jwtAuthenticationFilter(): JwtAuthenticationFilter = JwtAuthenticationFilter(jwtService)

    @Bean
    fun securityFilterChain(http: HttpSecurity, jwtAuthenticationFilter: JwtAuthenticationFilter): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                if (appSecurityProperties.permitAll) {
                    auth.anyRequest().permitAll()
                } else {
                    auth.requestMatchers("/auth/login").permitAll()
                    auth.requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                    ).permitAll()
                    auth.requestMatchers("/admin/api/**").hasRole("MAN")
                    auth.requestMatchers("/admin/**").permitAll()
                    auth.requestMatchers("/producer/**", "/consumer/**").authenticated()
                    auth.anyRequest().authenticated()
                }
            }
            .exceptionHandling {
                it.authenticationEntryPoint { _, response, _ ->
                    response.status = HttpServletResponse.SC_UNAUTHORIZED
                    response.characterEncoding = "UTF-8"
                    response.contentType = "application/json;charset=UTF-8"
                    response.writer.write("""{"error":"Unauthorized"}""")
                }
                it.accessDeniedHandler { _, response, _ ->
                    response.status = HttpServletResponse.SC_FORBIDDEN
                    response.characterEncoding = "UTF-8"
                    response.contentType = "application/json;charset=UTF-8"
                    response.writer.write("""{"error":"Forbidden"}""")
                }
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}

package com.romafrihet.server.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig(private val securityErrorHandlers: SecurityErrorHandlers) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain = http
        .csrf { it.disable() }
        .httpBasic { it.disable() }
        .formLogin { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests {
            it
                .requestMatchers(*PUBLIC_PATHS)
                .permitAll()
                .anyRequest()
                .authenticated()
        }.oauth2ResourceServer {
            it.jwt { }
            it.authenticationEntryPoint(securityErrorHandlers)
            it.accessDeniedHandler(securityErrorHandlers)
        }.exceptionHandling {
            it.authenticationEntryPoint(securityErrorHandlers)
            it.accessDeniedHandler(securityErrorHandlers)
        }.build()

    companion object {
        private val PUBLIC_PATHS =
            arrayOf(
                "/actuator/health",
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/v3/api-docs/**",
            )
    }
}

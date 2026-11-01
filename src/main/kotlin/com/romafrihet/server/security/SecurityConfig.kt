package com.romafrihet.server.security

import com.romafrihet.server.common.web.ApiPaths
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val securityErrorHandlers: SecurityErrorHandlers,
    private val jwtAuthenticationConverter: SupabaseJwtAuthenticationConverter,
) {
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
                .requestMatchers(HttpMethod.GET, ApiPaths.SCHOOLS_V1)
                .permitAll()
                .requestMatchers("${ApiPaths.TEACHER_V1}/**")
                .hasRole("TEACHER")
                .anyRequest()
                .authenticated()
        }.oauth2ResourceServer {
            it.jwt { jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter) }
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

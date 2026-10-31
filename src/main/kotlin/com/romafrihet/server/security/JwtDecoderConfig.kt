package com.romafrihet.server.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import javax.crypto.spec.SecretKeySpec

/**
 * Supabase Auth가 발급한 액세스 토큰을 검증한다.
 * - SUPABASE_JWT_SECRET 이 있으면 HS256(레거시 공유 secret)
 * - 없으면 {SUPABASE_URL}/auth/v1/.well-known/jwks.json 의 공개키(ES256/RS256)
 */
@Configuration
class JwtDecoderConfig(private val properties: SecurityProperties) {
    @Bean
    fun jwtDecoder(): JwtDecoder {
        val decoder =
            if (properties.jwtSecret.isNotBlank()) {
                NimbusJwtDecoder
                    .withSecretKey(SecretKeySpec(properties.jwtSecret.toByteArray(), "HmacSHA256"))
                    .macAlgorithm(MacAlgorithm.HS256)
                    .build()
            } else {
                NimbusJwtDecoder
                    .withJwkSetUri("${properties.supabaseUrl.trimEnd('/')}/auth/v1/.well-known/jwks.json")
                    .jwsAlgorithms {
                        it.add(SignatureAlgorithm.ES256)
                        it.add(SignatureAlgorithm.RS256)
                    }.build()
            }
        decoder.setJwtValidator(
            DelegatingOAuth2TokenValidator(JwtValidators.createDefault(), audienceValidator()),
        )
        return decoder
    }

    private fun audienceValidator(): OAuth2TokenValidator<Jwt> = OAuth2TokenValidator { jwt ->
        if (jwt.audience.orEmpty().contains(properties.audience)) {
            OAuth2TokenValidatorResult.success()
        } else {
            OAuth2TokenValidatorResult.failure(
                OAuth2Error("invalid_token", "aud가 ${properties.audience}가 아닙니다.", null),
            )
        }
    }
}

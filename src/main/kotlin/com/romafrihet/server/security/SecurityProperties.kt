package com.romafrihet.server.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.security")
data class SecurityProperties(
    /** Supabase 프로젝트 URL (예: https://<ref>.supabase.co, 로컬은 http://127.0.0.1:54321) */
    val supabaseUrl: String,
    /** 레거시 HS256 JWT secret. 비어 있으면 JWKS(비대칭 키)로 검증한다. */
    val jwtSecret: String = "",
    /** Supabase가 로그인 사용자 토큰에 넣는 aud 값 */
    val audience: String = "authenticated",
)

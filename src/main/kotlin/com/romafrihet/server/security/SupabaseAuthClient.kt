package com.romafrihet.server.security

import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.util.UUID

/**
 * Supabase Auth 관리자 API 클라이언트다. 비밀번호는 Supabase가 갖고 있어 서버가 직접 바꾸지 않는다.
 * 트랜잭션 밖에서만 호출한다.
 */
@Component
class SupabaseAuthClient(properties: SupabaseAuthProperties, builder: RestClient.Builder) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val client =
        builder
            .baseUrl("${properties.supabaseUrl.trimEnd('/')}/auth/v1")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ${properties.serviceRoleKey}")
            .defaultHeader("apikey", properties.serviceRoleKey)
            .build()

    /** 비밀번호를 바꾸면 Supabase가 그 사용자의 refresh 토큰도 무효화한다. */
    fun updatePassword(userId: UUID, password: String) {
        try {
            client
                .put()
                .uri("/admin/users/{id}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("password" to password))
                .retrieve()
                .toBodilessEntity()
        } catch (e: RestClientException) {
            logger.error("Supabase 비밀번호 변경 실패 user={}", userId, e)
            throw BusinessException(ErrorCode.AUTH_PROVIDER_UNAVAILABLE)
        }
    }
}

package com.romafrihet.server.security

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
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
    fun updatePassword(userId: UUID, password: String) = update(userId, mapOf("password" to password), "비밀번호 변경")

    /**
     * app_metadata.role을 바꾼다. null이면 키를 지운다(GoTrue는 app_metadata를 키 단위로 합치고 null 값은 지운다).
     * 이미 받은 토큰에는 예전 역할이 남으므로, 사용자가 세션을 갱신해야 바뀐 역할이 보인다.
     */
    fun updateRole(userId: UUID, role: String?) =
        update(userId, mapOf("app_metadata" to mapOf(ROLE_KEY to role)), "역할 변경")

    /** 정지하면 로그인과 토큰 갱신이 막힌다. 이미 받은 access 토큰은 만료될 때까지 쓸 수 있다. */
    fun updateBan(userId: UUID, banned: Boolean) =
        update(userId, mapOf("ban_duration" to if (banned) BAN_FOREVER else BAN_NONE), "정지 변경")

    fun findUser(userId: UUID): SupabaseUser = try {
        client
            .get()
            .uri("/admin/users/{id}", userId)
            .retrieve()
            .body(SupabaseUser::class.java)
            ?: throw BusinessException(ErrorCode.AUTH_PROVIDER_UNAVAILABLE)
    } catch (_: HttpClientErrorException.NotFound) {
        throw BusinessException(ErrorCode.NOT_FOUND)
    } catch (e: RestClientException) {
        logger.error("Supabase 사용자 조회 실패 user={}", userId, e)
        throw BusinessException(ErrorCode.AUTH_PROVIDER_UNAVAILABLE)
    }

    private fun update(userId: UUID, body: Map<String, Any?>, action: String) {
        try {
            client
                .put()
                .uri("/admin/users/{id}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity()
        } catch (_: HttpClientErrorException.NotFound) {
            throw BusinessException(ErrorCode.NOT_FOUND)
        } catch (e: RestClientException) {
            logger.error("Supabase {} 실패 user={}", action, userId, e)
            throw BusinessException(ErrorCode.AUTH_PROVIDER_UNAVAILABLE)
        }
    }

    private companion object {
        const val ROLE_KEY = "role"

        /** GoTrue는 영구 정지 값을 따로 두지 않아 100년으로 준다. */
        const val BAN_FOREVER = "876000h"
        const val BAN_NONE = "none"
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class SupabaseUser(val email: String? = null)

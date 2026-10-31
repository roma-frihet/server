package com.romafrihet.server.common.web

import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

/**
 * 인증 정보는 이 헬퍼로만 꺼낸다. Controller에서 SecurityContextHolder 직접 접근 금지.
 * Supabase Auth의 사용자 식별자(JWT sub)는 UUID다.
 */
interface BaseController {
    fun getUserId(): UUID = getUserIdOrNull() ?: throw BusinessException(ErrorCode.UNAUTHORIZED)

    fun getUserIdOrNull(): UUID? {
        val jwt = SecurityContextHolder.getContext().authentication?.principal as? Jwt ?: return null
        return runCatching { UUID.fromString(jwt.subject) }.getOrNull()
    }
}

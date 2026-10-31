package com.romafrihet.server.security

import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.toProblemDetail
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

/** 인증/인가 실패도 ProblemDetail 형태로 응답한다. */
@Component
class SecurityErrorHandlers(private val jsonMapper: JsonMapper) :
    AuthenticationEntryPoint,
    AccessDeniedHandler {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) = write(response, ErrorCode.UNAUTHORIZED)

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) = write(response, ErrorCode.FORBIDDEN)

    private fun write(response: HttpServletResponse, errorCode: ErrorCode) {
        response.status = errorCode.httpStatus.value()
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        jsonMapper.writeValue(response.writer, errorCode.toProblemDetail())
    }
}

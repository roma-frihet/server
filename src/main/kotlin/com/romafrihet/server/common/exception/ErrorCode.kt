package com.romafrihet.server.common.exception

import org.springframework.http.HttpStatus

/**
 * 도메인별 접두어로 묶어서 추가한다. 한 번 배포된 code 값은 바꾸지 않는다.
 *
 * | 접두어 | 도메인 |
 * |---|---|
 * | C  | 공통 |
 * | AU | 인증/인가 |
 */
enum class ErrorCode(val httpStatus: HttpStatus, val code: String, val message: String) {
    // Common
    INVALID_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 값이 입력되었습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "C002", "해당 데이터를 찾을 수 없습니다."),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "C003", "요청 본문을 읽을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C004", "지원하지 않는 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C999", "일시적인 오류가 발생했습니다."),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AU001", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AU002", "권한이 없습니다."),
}

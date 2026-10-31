package com.romafrihet.server.common.exception

import org.springframework.http.ProblemDetail

/** 프론트엔드는 메시지 대신 `code` 값으로 오류를 구분한다. */
fun ErrorCode.toProblemDetail(detail: String = message): ProblemDetail =
    ProblemDetail.forStatusAndDetail(httpStatus, detail).apply {
        title = name
        setProperty("code", code)
    }

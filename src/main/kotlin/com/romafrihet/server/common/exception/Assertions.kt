package com.romafrihet.server.common.exception

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/** 서비스 사전조건 검증. 실패하면 BusinessException을 던진다. */
@OptIn(ExperimentalContracts::class)
inline fun assertCheck(condition: Boolean, errorCode: () -> ErrorCode) {
    contract { returns() implies condition }
    if (!condition) throw BusinessException(errorCode())
}

@OptIn(ExperimentalContracts::class)
inline fun <T : Any> assertCheckNotNull(value: T?, errorCode: () -> ErrorCode): T {
    contract { returns() implies (value != null) }
    return value ?: throw BusinessException(errorCode())
}

package com.romafrihet.server.common.exception

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class AssertionsTest {
    @Test
    fun `assertCheck - 조건이 거짓이면 BusinessException을 던진다`() {
        val e = assertThrows<BusinessException> { assertCheck(false) { ErrorCode.INVALID_VALUE } }
        assertEquals(ErrorCode.INVALID_VALUE, e.errorCode)
    }

    @Test
    fun `assertCheckNotNull - 값이 있으면 그대로 반환한다`() {
        val value: String? = "ok"
        assertEquals("ok", assertCheckNotNull(value) { ErrorCode.NOT_FOUND })
    }

    @Test
    fun `assertCheckNotNull - null이면 BusinessException을 던진다`() {
        val e = assertThrows<BusinessException> { assertCheckNotNull(null as String?) { ErrorCode.NOT_FOUND } }
        assertEquals(ErrorCode.NOT_FOUND, e.errorCode)
    }
}

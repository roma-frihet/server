package com.romafrihet.server.common.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.assertj.MockMvcTester
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

class GlobalExceptionHandlerTest {
    @RestController
    private class ThrowingController {
        @GetMapping("/business")
        fun business(): Unit = throw BusinessException(ErrorCode.NOT_FOUND)

        @GetMapping("/unexpected")
        fun unexpected(): Unit = throw IllegalStateException("내부 정보")
    }

    private val mvc =
        MockMvcTester.create(
            MockMvcBuilders
                .standaloneSetup(ThrowingController())
                .setControllerAdvice(GlobalExceptionHandler())
                .build(),
        )

    @Test
    fun `비즈니스 예외는 ErrorCode의 상태와 코드로 응답한다`() {
        assertThat(mvc.get().uri("/business"))
            .hasStatus(404)
            .bodyText()
            .contains("\"code\":\"C002\"")
    }

    @Test
    fun `예상하지 못한 예외는 내부 메시지를 노출하지 않는다`() {
        assertThat(mvc.get().uri("/unexpected"))
            .hasStatus(500)
            .bodyText()
            .contains("\"code\":\"C999\"")
            .doesNotContain("내부 정보")
    }
}

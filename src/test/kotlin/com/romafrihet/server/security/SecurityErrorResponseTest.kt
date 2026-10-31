package com.romafrihet.server.security

import com.ninjasquad.springmockk.MockkBean
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.web.servlet.assertj.MockMvcTester

@WebMvcTest(properties = ["app.security.supabase-url=http://localhost:54321"])
@Import(SecurityConfig::class, SecurityErrorHandlers::class)
class SecurityErrorResponseTest(@Autowired private val mvc: MockMvcTester) {
    @MockkBean
    private lateinit var jwtDecoder: JwtDecoder

    @Test
    fun `토큰 없는 요청은 401과 오류 코드로 응답한다`() {
        assertThat(mvc.get().uri("/api/v1/anything"))
            .hasStatus(401)
            .bodyText()
            .contains("\"code\":\"AU001\"")
    }
}

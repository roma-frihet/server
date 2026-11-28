package com.romafrihet.server.security

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.web.servlet.assertj.MockMvcTester
import java.time.Instant
import java.util.UUID

@WebMvcTest(
    controllers = [EmptyTestController::class],
    properties = ["app.security.supabase-url=http://localhost:54321"],
)
@Import(SecurityConfig::class, SecurityErrorHandlers::class, SupabaseJwtAuthenticationConverter::class)
class AdminAuthorizationTest(@Autowired private val mvc: MockMvcTester) {
    @MockkBean
    private lateinit var jwtDecoder: JwtDecoder

    @Test
    fun `app_metadata 역할이 admin이면 관리자 API를 통과한다`() {
        tokenWith(mapOf("app_metadata" to mapOf("role" to "admin")))

        assertThat(request("/api/v1/admin/anything")).hasStatus(404)
    }

    @Test
    fun `교사는 관리자 API에서 403을 받는다`() {
        tokenWith(mapOf("app_metadata" to mapOf("role" to "teacher")))

        assertThat(request("/api/v1/admin/anything")).hasStatus(403).bodyText().contains("\"code\":\"AU002\"")
    }

    @Test
    fun `관리자도 교사 API는 쓰지 못한다`() {
        tokenWith(mapOf("app_metadata" to mapOf("role" to "admin")))

        assertThat(request("/api/v1/teacher/anything")).hasStatus(403)
    }

    @Test
    fun `user_metadata의 admin은 권한으로 인정하지 않는다`() {
        tokenWith(mapOf("user_metadata" to mapOf("role" to "admin")))

        assertThat(request("/api/v1/admin/anything")).hasStatus(403)
    }

    @Test
    fun `역할이 없는 로그인 사용자도 교사 가입 신청 API에는 닿는다`() {
        tokenWith(mapOf("app_metadata" to emptyMap<String, Any>()))

        assertThat(request("/api/v1/teacher-applications/me")).hasStatus(404)
    }

    private fun tokenWith(claims: Map<String, Any>) {
        every { jwtDecoder.decode(TOKEN) } returns
            Jwt
                .withTokenValue(TOKEN)
                .header("alg", "HS256")
                .subject(UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claims { it.putAll(claims) }
                .build()
    }

    private fun request(path: String) = mvc.get().uri(path).header(HttpHeaders.AUTHORIZATION, "Bearer $TOKEN")

    private companion object {
        const val TOKEN = "token"
    }
}

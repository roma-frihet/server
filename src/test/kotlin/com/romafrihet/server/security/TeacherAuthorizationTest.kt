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
class TeacherAuthorizationTest(@Autowired private val mvc: MockMvcTester) {
    @MockkBean
    private lateinit var jwtDecoder: JwtDecoder

    @Test
    fun `app_metadata 역할이 teacher면 교사 API를 통과한다`() {
        every { jwtDecoder.decode(TOKEN) } returns jwt(mapOf("app_metadata" to mapOf("role" to "teacher")))

        assertThat(requestTeacherApi()).hasStatus(404)
    }

    @Test
    fun `역할이 없는 사용자는 교사 API에서 403을 받는다`() {
        every { jwtDecoder.decode(TOKEN) } returns jwt(mapOf("app_metadata" to emptyMap<String, Any>()))

        assertThat(requestTeacherApi())
            .hasStatus(403)
            .bodyText()
            .contains("\"code\":\"AU002\"")
    }

    @Test
    fun `user_metadata의 역할은 권한으로 인정하지 않는다`() {
        every { jwtDecoder.decode(TOKEN) } returns jwt(mapOf("user_metadata" to mapOf("role" to "teacher")))

        assertThat(requestTeacherApi()).hasStatus(403)
    }

    private fun requestTeacherApi() = mvc
        .get()
        .uri("/api/v1/teacher/anything")
        .header(HttpHeaders.AUTHORIZATION, "Bearer $TOKEN")

    private fun jwt(claims: Map<String, Any>): Jwt = Jwt
        .withTokenValue(TOKEN)
        .header("alg", "HS256")
        .subject(UUID.randomUUID().toString())
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60))
        .claims { it.putAll(claims) }
        .build()

    private companion object {
        const val TOKEN = "token"
    }
}

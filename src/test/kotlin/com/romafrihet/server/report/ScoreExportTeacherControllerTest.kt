package com.romafrihet.server.report

import com.ninjasquad.springmockk.MockkBean
import com.romafrihet.server.report.dto.ScoreExportDto
import com.romafrihet.server.security.SecurityConfig
import com.romafrihet.server.security.SecurityErrorHandlers
import com.romafrihet.server.security.SupabaseJwtAuthenticationConverter
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
    controllers = [ScoreExportTeacherController::class],
    properties = ["app.security.supabase-url=http://localhost:54321"],
)
@Import(SecurityConfig::class, SecurityErrorHandlers::class, SupabaseJwtAuthenticationConverter::class)
class ScoreExportTeacherControllerTest(@Autowired private val mvc: MockMvcTester) {
    @MockkBean
    private lateinit var scoreExportService: ScoreExportService

    @MockkBean
    private lateinit var jwtDecoder: JwtDecoder

    @Test
    fun `CSV를 한글 파일 이름과 함께 첨부 파일로 내린다`() {
        every { jwtDecoder.decode(TOKEN) } returns teacherJwt()
        val content = CsvTable(listOf("이름"), listOf(listOf("홍길동"))).toBytes()
        every { scoreExportService.export(any()) } returns ScoreExportDto("2학년_1반_과학_학생별_차시점수.csv", content)

        val result = mvc.get().uri("/api/v1/teacher/courses/10/scores/export?sheet=STUDENT_SCORES").header(
            HttpHeaders.AUTHORIZATION,
            "Bearer $TOKEN",
        )

        assertThat(result).hasStatusOk()
        assertThat(result).headers().hasValue(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8")
        assertThat(result).headers().hasHeaderSatisfying(HttpHeaders.CONTENT_DISPOSITION) { values ->
            assertThat(values.single()).startsWith("attachment;").contains("filename*=UTF-8''2%ED%95%99%EB%85%84")
        }
        assertThat(result).body().isEqualTo(content)
    }

    @Test
    fun `시트를 정하지 않으면 400이다`() {
        every { jwtDecoder.decode(TOKEN) } returns teacherJwt()

        assertThat(
            mvc.get().uri(
                "/api/v1/teacher/courses/10/scores/export",
            ).header(HttpHeaders.AUTHORIZATION, "Bearer $TOKEN"),
        ).hasStatus(400)
    }

    private fun teacherJwt(): Jwt = Jwt
        .withTokenValue(TOKEN)
        .header("alg", "HS256")
        .subject(UUID.randomUUID().toString())
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60))
        .claims { it["app_metadata"] = mapOf("role" to "teacher") }
        .build()

    private companion object {
        const val TOKEN = "token"
    }
}

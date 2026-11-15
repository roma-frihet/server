package com.romafrihet.server.subject

import com.ninjasquad.springmockk.MockkBean
import com.romafrihet.server.security.SecurityConfig
import com.romafrihet.server.security.SecurityErrorHandlers
import com.romafrihet.server.security.SupabaseJwtAuthenticationConverter
import com.romafrihet.server.subject.dto.SubjectDto
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
    controllers = [SubjectTeacherController::class],
    properties = ["app.security.supabase-url=http://localhost:54321"],
)
@Import(SecurityConfig::class, SecurityErrorHandlers::class, SupabaseJwtAuthenticationConverter::class)
class SubjectTeacherControllerTest(@Autowired private val mvc: MockMvcTester) {
    @MockkBean
    private lateinit var subjectService: SubjectService

    @MockkBean
    private lateinit var jwtDecoder: JwtDecoder

    @Test
    fun `학교급을 주지 않으면 중학교 과목을 내려준다`() {
        every { jwtDecoder.decode(TOKEN) } returns teacherJwt()
        every { subjectService.findAll(SchoolLevel.MIDDLE) } returns listOf(SCIENCE)

        assertThat(mvc.get().uri(PATH).header(HttpHeaders.AUTHORIZATION, "Bearer $TOKEN"))
            .hasStatusOk()
            .bodyText()
            .contains("과학")
    }

    @Test
    fun `학교급으로 거른다`() {
        every { jwtDecoder.decode(TOKEN) } returns teacherJwt()
        every { subjectService.findAll(SchoolLevel.HIGH) } returns emptyList()

        assertThat(
            mvc.get().uri(PATH).param("schoolLevel", "HIGH").header(HttpHeaders.AUTHORIZATION, "Bearer $TOKEN"),
        ).hasStatusOk()
            .bodyText()
            .isEqualTo("[]")
    }

    @Test
    fun `없는 학교급은 400으로 막는다`() {
        every { jwtDecoder.decode(TOKEN) } returns teacherJwt()

        assertThat(
            mvc.get().uri(PATH).param("schoolLevel", "ELEMENTARY").header(HttpHeaders.AUTHORIZATION, "Bearer $TOKEN"),
        ).hasStatus(400)
    }

    @Test
    fun `교사가 아니면 과목 목록을 볼 수 없다`() {
        assertThat(mvc.get().uri(PATH)).hasStatus(401)
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
        const val PATH = "/api/v1/teacher/subjects"
        val SCIENCE = SubjectDto(id = 6L, schoolLevel = SchoolLevel.MIDDLE, subjectGroup = "과학", name = "과학")
    }
}

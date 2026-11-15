package com.romafrihet.server.classroom

import com.ninjasquad.springmockk.MockkBean
import com.romafrihet.server.classroom.dto.ClassroomDto
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
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.web.servlet.assertj.MockMvcTester
import java.time.Instant
import java.util.UUID

@WebMvcTest(
    controllers = [ClassroomTeacherController::class],
    properties = ["app.security.supabase-url=http://localhost:54321"],
)
@Import(SecurityConfig::class, SecurityErrorHandlers::class, SupabaseJwtAuthenticationConverter::class)
class ClassroomTeacherControllerTest(@Autowired private val mvc: MockMvcTester) {
    @MockkBean
    private lateinit var classroomService: ClassroomService

    @MockkBean
    private lateinit var classroomMembershipService: ClassroomMembershipService

    @MockkBean
    private lateinit var jwtDecoder: JwtDecoder

    @Test
    fun `반은 번호로 만들고 수업 정보까지 돌려준다`() {
        every { jwtDecoder.decode(TOKEN) } returns teacherJwt()
        every { classroomService.create(any()) } returns CLASSROOM

        assertThat(post("""{"courseId":10,"classNo":3}"""))
            .hasStatus(201)
            .bodyText()
            .contains("\"classNo\":3", "\"grade\":2", "\"subjectName\":\"과학\"")
    }

    @Test
    fun `반 번호가 범위를 벗어나면 막는다`() {
        every { jwtDecoder.decode(TOKEN) } returns teacherJwt()

        assertThat(post("""{"courseId":10,"classNo":0}""")).hasStatus(400)
        assertThat(post("""{"courseId":10,"classNo":21}""")).hasStatus(400)
    }

    @Test
    fun `반 번호가 없으면 막는다`() {
        every { jwtDecoder.decode(TOKEN) } returns teacherJwt()

        assertThat(post("""{"courseId":10}""")).hasStatus(400)
    }

    private fun post(body: String) = mvc
        .post()
        .uri("/api/v1/teacher/classrooms")
        .header(HttpHeaders.AUTHORIZATION, "Bearer $TOKEN")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)

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
        val CLASSROOM = ClassroomDto(
            id = 1L,
            courseId = 10L,
            grade = 2,
            subjectName = "과학",
            classNo = 3,
            joinCode = null,
            joinCodeExpiresAt = null,
            studentCount = 0,
        )
    }
}

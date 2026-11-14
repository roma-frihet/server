package com.romafrihet.server.school

import com.ninjasquad.springmockk.MockkBean
import com.romafrihet.server.school.dto.SchoolDto
import com.romafrihet.server.security.SecurityConfig
import com.romafrihet.server.security.SecurityErrorHandlers
import com.romafrihet.server.security.SupabaseJwtAuthenticationConverter
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.web.servlet.assertj.MockMvcTester

@WebMvcTest(
    controllers = [SchoolController::class],
    properties = ["app.security.supabase-url=http://localhost:54321"],
)
@Import(SecurityConfig::class, SecurityErrorHandlers::class, SupabaseJwtAuthenticationConverter::class)
class SchoolControllerTest(@Autowired private val mvc: MockMvcTester) {
    @MockkBean
    private lateinit var schoolService: SchoolService

    @MockkBean
    private lateinit var jwtDecoder: JwtDecoder

    @Test
    fun `가입 전이라 토큰 없이 학교를 찾는다`() {
        every { schoolService.findAll("다원") } returns listOf(SchoolDto(1L, "다원중학교"))

        assertThat(mvc.get().uri("/api/v1/schools").param("keyword", " 다원 "))
            .hasStatusOk()
            .bodyText()
            .contains("다원중학교")
    }

    @Test
    fun `학교 등록은 공개하지 않는다`() {
        assertThat(mvc.post().uri("/api/v1/schools")).hasStatus(401)
    }
}

package com.romafrihet.server.lesson

import com.ninjasquad.springmockk.MockkBean
import com.romafrihet.server.lesson.dto.AttemptDto
import com.romafrihet.server.lesson.dto.CurrentQuestionDto
import com.romafrihet.server.lesson.dto.FinalizedAnswerDto
import com.romafrihet.server.lesson.dto.QuestionDto
import com.romafrihet.server.lesson.dto.QuestionOptionDto
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
    controllers = [AttemptStudentController::class],
    properties = ["app.security.supabase-url=http://localhost:54321"],
)
@Import(SecurityConfig::class, SecurityErrorHandlers::class, SupabaseJwtAuthenticationConverter::class)
class AttemptStudentControllerTest(@Autowired private val mvc: MockMvcTester) {
    @MockkBean
    private lateinit var attemptService: AttemptService

    @MockkBean
    private lateinit var jwtDecoder: JwtDecoder

    @Test
    fun `푸는 중인 문제에는 정답 여부를 담지 않는다`() {
        every { jwtDecoder.decode(TOKEN) } returns studentJwt()
        every { attemptService.start(any()) } returns inQuestion()

        assertThat(post("$PATH/questions/0/start"))
            .hasStatusOk()
            .bodyText()
            .contains("\"remainingMillis\":21430", "엽록체", "미토콘드리아")
            .doesNotContain("isCorrect", "해설입니다", "correctOptionId")
    }

    @Test
    fun `확정된 문제에만 정답과 해설을 담는다`() {
        every { jwtDecoder.decode(TOKEN) } returns studentJwt()
        every { attemptService.submit(any()) } returns finished()

        assertThat(post("$PATH/questions/0/submit", """{"optionId":21}"""))
            .hasStatusOk()
            .bodyText()
            .contains("\"result\":\"RIGHT\"", "\"correctOptionId\":21", "해설입니다", "\"correctCount\":1")
    }

    @Test
    fun `임시 저장은 PUT으로 받는다`() {
        every { jwtDecoder.decode(TOKEN) } returns studentJwt()
        every { attemptService.saveDraft(any()) } returns inQuestion()

        assertThat(
            mvc
                .put()
                .uri("$PATH/questions/0/draft")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $TOKEN")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"optionId":21}"""),
        ).hasStatusOk()
    }

    @Test
    fun `토큰이 없으면 응시할 수 없다`() {
        assertThat(mvc.get().uri(PATH)).hasStatus(401)
    }

    private fun post(uri: String, body: String? = null) = mvc
        .post()
        .uri(uri)
        .header(HttpHeaders.AUTHORIZATION, "Bearer $TOKEN")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body ?: "")

    private fun studentJwt(): Jwt = Jwt
        .withTokenValue(TOKEN)
        .header("alg", "HS256")
        .subject(UUID.randomUUID().toString())
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60))
        .claims { it["app_metadata"] = emptyMap<String, Any>() }
        .build()

    private fun inQuestion() = AttemptDto(
        phase = AttemptPhase.IN_QUESTION,
        total = 2,
        nextIndex = 0,
        nextTimeLimitSec = 30,
        correctCount = null,
        current = CurrentQuestionDto(
            questionIndex = 0,
            question = question(),
            remainingMillis = 21_430,
            draftOptionId = null,
            draftText = null,
        ),
        last = null,
    )

    private fun finished() = AttemptDto(
        phase = AttemptPhase.DONE,
        total = 1,
        nextIndex = 1,
        nextTimeLimitSec = null,
        correctCount = 1,
        current = null,
        last = FinalizedAnswerDto(
            questionIndex = 0,
            result = QuestionResult.RIGHT,
            byTimeout = false,
            selectedOptionId = 21,
            answerText = null,
            correctOptionId = 21,
            correctAnswer = "엽록체",
            explanation = "해설입니다",
        ),
    )

    private fun question() = QuestionDto(
        id = 10,
        lessonNumber = 1,
        type = QuestionType.MULTIPLE_CHOICE,
        content = "광합성이 일어나는 곳은?",
        imageId = null,
        imageUrl = null,
        timeLimitSec = 30,
        explanation = "해설입니다",
        displayOrder = 0,
        options = listOf(
            QuestionOptionDto(21, 0, "엽록체", null, null, isCorrect = true),
            QuestionOptionDto(22, 1, "미토콘드리아", null, null, isCorrect = false),
        ),
    )

    private companion object {
        const val TOKEN = "token"
        const val PATH = "/api/v1/classrooms/1/lessons/1/attempt"
    }
}

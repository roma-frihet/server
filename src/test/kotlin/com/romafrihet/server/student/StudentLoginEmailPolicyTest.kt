package com.romafrihet.server.student

import com.romafrihet.server.student.command.StudentProfileCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class StudentLoginEmailPolicyTest {
    private val policy = StudentLoginEmailPolicy(StudentLoginProperties(loginEmailDomain = "students.test"))

    @Test
    fun `학교와 학번을 담아 이메일 모양으로 만든다`() {
        assertThat(policy.of(command(schoolId = 7, grade = 2, classNumber = 1, seatNumber = 1)))
            .isEqualTo("s7-20101@students.test")
    }

    /** 학교가 빠지면 다른 학교의 같은 학번과 겹친다. */
    @Test
    fun `학교가 다르면 학번이 같아도 다른 계정이 된다`() {
        val first = policy.of(command(schoolId = 7, grade = 2, classNumber = 1, seatNumber = 1))
        val second = policy.of(command(schoolId = 8, grade = 2, classNumber = 1, seatNumber = 1))

        assertThat(first).isNotEqualTo(second)
    }

    @Test
    fun `반과 번호는 두 자리로 채운다`() {
        assertThat(policy.of(command(schoolId = 1, grade = 1, classNumber = 12, seatNumber = 3)))
            .isEqualTo("s1-11203@students.test")
    }

    @Test
    fun `GoTrue가 소문자로 저장하므로 대소문자는 무시한다`() {
        val command = command(schoolId = 7, grade = 2, classNumber = 1, seatNumber = 1)

        assertThat(policy.matches("S7-20101@STUDENTS.TEST", command)).isTrue()
    }

    @Test
    fun `학번이 다른 계정과 이메일이 없는 경우를 거른다`() {
        val command = command(schoolId = 7, grade = 2, classNumber = 1, seatNumber = 1)

        assertThat(policy.matches("s7-20102@students.test", command)).isFalse()
        assertThat(policy.matches("teacher@test.com", command)).isFalse()
        assertThat(policy.matches(null, command)).isFalse()
    }

    private fun command(schoolId: Long, grade: Int, classNumber: Int, seatNumber: Int) = StudentProfileCommand.Create(
        userId = UUID.randomUUID(),
        schoolId = schoolId,
        grade = grade,
        classNumber = classNumber,
        seatNumber = seatNumber,
        name = "홍길동",
        loginEmail = null,
    )
}

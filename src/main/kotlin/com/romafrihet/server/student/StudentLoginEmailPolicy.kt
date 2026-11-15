package com.romafrihet.server.student

import com.romafrihet.server.student.command.StudentProfileCommand
import org.springframework.stereotype.Component

/**
 * 학생은 학번으로 로그인한다. Supabase Auth가 이메일만 받으므로 프론트가 학번을 이 규칙대로
 * 이메일 모양으로 바꿔 가입시킨다. 서버는 가입에 끼지 않고, 가입 정보를 저장할 때 규칙만 확인한다.
 *
 * 학교를 함께 담는 이유는 학교가 다르면 학번이 겹치기 때문이다. 유일 제약
 * `uk_student_profiles_school_id_grade_class_number_seat_number`와 같은 네 값을 쓴다.
 */
@Component
class StudentLoginEmailPolicy(private val properties: StudentLoginProperties) {
    fun of(command: StudentProfileCommand.Create): String = "s%d-%d%02d%02d@%s".format(
        command.schoolId,
        command.grade,
        command.classNumber,
        command.seatNumber,
        properties.loginEmailDomain,
    )

    /** GoTrue가 이메일을 소문자로 저장하므로 대소문자는 무시한다. */
    fun matches(loginEmail: String?, command: StudentProfileCommand.Create): Boolean =
        loginEmail != null && loginEmail.equals(of(command), ignoreCase = true)
}

package com.romafrihet.server.classroom

import com.romafrihet.server.TeacherFlowFixture
import com.romafrihet.server.classroom.command.ClassroomCommand
import com.romafrihet.server.classroom.command.ClassroomMembershipCommand
import com.romafrihet.server.common.exception.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class ClassroomTeacherFlowTest : TeacherFlowFixture() {
    @Test
    fun `학교는 이름 일부로 찾는다`() {
        assertThat(schoolService.findAll("다원").map { it.name }).containsExactly("다원중학교")
        assertThat(schoolService.findAll("없는학교")).isEmpty()
    }

    @Test
    fun `코드는 수업과 반마다 따로 발급한다`() {
        val first = classroomService.create(ClassroomCommand.Create(teacherId, createCourse("수학"), 1)).id
        val second = classroomService.create(ClassroomCommand.Create(teacherId, createCourse("국어"), 1)).id

        val firstCode = classroomService.issueJoinCode(ClassroomCommand.IssueJoinCode(teacherId, first)).joinCode
        val secondCode = classroomService.issueJoinCode(ClassroomCommand.IssueJoinCode(teacherId, second)).joinCode

        assertThat(firstCode).isNotEqualTo(secondCode)
    }

    @Test
    fun `학생이 연결되면 학생 수가 늘고 코드를 끄면 비워진다`() {
        val classroomId = createClassroom()
        val issue = ClassroomCommand.IssueJoinCode(teacherId, classroomId)
        classroomService.issueJoinCode(issue)
        request(classroomId, studentId, "홍길동", "20101")

        assertThat(classroomService.findAll(teacherId).single().studentCount).isEqualTo(1)

        classroomService.closeJoinCode(issue)
        assertThat(classroomService.findAll(teacherId).single().joinCode).isNull()
    }

    @Test
    fun `학생을 내보내면 반에서 사라진다`() {
        val classroomId = createClassroom()
        val id = requireNotNull(request(classroomId, studentId, "홍길동", "20101").id)

        membershipService.removeStudent(
            ClassroomMembershipCommand.RemoveStudent(teacherId, classroomId, id),
        )

        assertThat(classroomService.findAll(teacherId).single().studentCount).isZero()
    }

    @Test
    fun `다른 교사는 학생을 내보낼 수 없다`() {
        val classroomId = createClassroom()
        val id = requireNotNull(request(classroomId, studentId, "홍길동", "20101").id)

        assertErrorCode(ErrorCode.NOT_FOUND) {
            membershipService.removeStudent(
                ClassroomMembershipCommand.RemoveStudent(UUID.randomUUID(), classroomId, id),
            )
        }
    }
}

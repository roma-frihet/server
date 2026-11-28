package com.romafrihet.server.admin

import com.romafrihet.server.admin.command.AdminCommand
import com.romafrihet.server.admin.dto.AdminStudentDto
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheck
import com.romafrihet.server.student.StudentPasswordService
import com.romafrihet.server.student.StudentProfileRepository
import com.romafrihet.server.student.dto.StudentProfileQueryDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

/** 클래스에 @Transactional을 두지 않는다. 비밀번호 초기화의 Supabase 호출이 트랜잭션 안으로 들어간다. */
@Service
class StudentAdminService(
    private val studentProfileRepository: StudentProfileRepository,
    private val accountSuspensionRepository: AccountSuspensionRepository,
    private val studentPasswordService: StudentPasswordService,
    transactionManager: PlatformTransactionManager,
) {
    private val readOnlyTemplate = TransactionTemplate(transactionManager).apply { isReadOnly = true }

    fun findAll(command: AdminCommand.StudentQuery): Page<AdminStudentDto> = requireNotNull(
        readOnlyTemplate.execute {
            val page =
                studentProfileRepository.queryProfiles(
                    StudentProfileQueryDto(
                        schoolId = command.schoolId,
                        grade = command.grade,
                        classNumber = command.classNumber,
                        keyword = command.keyword,
                    ),
                    PageRequest.of(command.page, command.size),
                )
            val suspended =
                accountSuspensionRepository.findAllByUserIdIn(page.content.map { it.userId }).map { it.userId }.toSet()
            page.map {
                AdminStudentDto(
                    userId = it.userId,
                    schoolId = it.schoolId,
                    schoolName = it.schoolName,
                    grade = it.grade,
                    classNumber = it.classNumber,
                    seatNumber = it.seatNumber,
                    studentNumber = "%d%02d%02d".format(it.grade, it.classNumber, it.seatNumber),
                    name = it.name,
                    mustChangePassword = it.mustChangePassword,
                    isSuspended = it.userId in suspended,
                    createdAt = it.createdAt,
                )
            }
        },
    )

    /** 교사 초기화와 같다. 임시 비밀번호를 한 번만 돌려주고 학생의 기존 세션을 끊는다. */
    fun resetPassword(studentUserId: UUID): String {
        val exists = requireNotNull(readOnlyTemplate.execute { studentProfileRepository.existsByUserId(studentUserId) })
        assertCheck(exists) { ErrorCode.NOT_FOUND }
        return studentPasswordService.reset(studentUserId)
    }
}

package com.romafrihet.server.lesson

import com.romafrihet.server.lesson.dto.LessonScoreQueryDto
import com.romafrihet.server.lesson.projection.LessonAttemptCountProjection
import com.romafrihet.server.lesson.projection.LessonScoreProjection
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface AttemptRepositoryExtension {
    fun queryScores(query: LessonScoreQueryDto, pageable: Pageable): Page<LessonScoreProjection>

    /**
     * 차시마다 그 수업의 모든 반을 합쳐 푼 적이 있는 학생 수다. 지우면 점수가 바뀌는 인원을
     * 교사에게 미리 알리는 데 쓴다. 차시마다 세면 차시 수만큼 쿼리가 나가므로 한 번에 묶는다.
     */
    fun queryAttemptedStudentCounts(courseId: Long): List<LessonAttemptCountProjection>
}

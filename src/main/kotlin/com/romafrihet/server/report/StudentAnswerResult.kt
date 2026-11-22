package com.romafrihet.server.report

/**
 * 교사가 보는 문제 하나의 상태다. 확정된 결과(QuestionResult)에 응답 행이 없거나 아직 푸는 중인 경우를 더했다.
 * 시작하지 않은 문제는 응답 행이 없어 교사가 결과를 고칠 수 없다.
 */
enum class StudentAnswerResult {
    RIGHT,
    WRONG,
    NONE,
    IN_PROGRESS,
    NOT_STARTED,
}

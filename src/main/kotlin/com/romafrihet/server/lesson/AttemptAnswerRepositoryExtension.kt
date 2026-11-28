package com.romafrihet.server.lesson

interface AttemptAnswerRepositoryExtension {
    /**
     * 문제 하나를 지울 때 그 문제의 응답을 치운다. 응답은 문제와 보기를 모두 참조하므로 둘보다 먼저 지워야 한다.
     * 학생 하나씩 돌면 인원수만큼 쿼리가 나가므로 한 번에 지운다.
     */
    fun deleteAllByQuestionId(questionId: Long)
}

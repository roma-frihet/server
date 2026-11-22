package com.romafrihet.server.report

import com.romafrihet.server.lesson.Question
import com.romafrihet.server.lesson.QuestionResult
import com.romafrihet.server.lesson.QuestionType
import java.util.Locale

/** 성적 CSV 칸에 들어갈 글자다. */
object ScoreExportCells {
    /** 화면의 평균처럼 소수 첫째 자리까지 쓴다. 정수로 내리면 3명 중 2명은 66으로 실제보다 낮게 보인다. */
    fun percent(right: Int, answered: Int): String = String.format(Locale.ROOT, "%.1f", right * PERCENT / answered)

    fun mark(result: QuestionResult?): String = when (result) {
        QuestionResult.RIGHT -> "O"
        QuestionResult.WRONG -> "X"
        QuestionResult.NONE, null -> ""
    }

    fun typeLabel(question: Question): String = when (question.type) {
        QuestionType.MULTIPLE_CHOICE -> "객관식"
        QuestionType.SHORT_ANSWER -> "주관식"
    }

    private const val PERCENT = 100.0
}

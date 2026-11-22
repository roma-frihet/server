package com.romafrihet.server.report

/** 내려받을 표의 종류다. 문제 단위 표는 차시를 하나 정해야 한다. */
enum class ScoreExportSheet(val label: String, val needsLesson: Boolean) {
    STUDENT_SCORES("학생별_차시점수", false),
    QUESTION_RATES("문제별_정답률", true),
    ANSWER_SHEET("학생별_정오표", true),
}

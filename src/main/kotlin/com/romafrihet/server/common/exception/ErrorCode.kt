package com.romafrihet.server.common.exception

import org.springframework.http.HttpStatus

/**
 * 도메인별 접두어로 묶어서 추가한다. 한 번 배포된 code 값은 바꾸지 않는다.
 *
 * | 접두어 | 도메인 |
 * |---|---|
 * | C  | 공통 |
 * | AU | 인증/인가 |
 * | CL | 반/학생 합류 |
 * | CO | 수업 |
 * | LS | 차시/문제 |
 * | SP | 학생 가입 정보 |
 * | ST | 파일 저장소 |
 */
enum class ErrorCode(val httpStatus: HttpStatus, val code: String, val message: String) {
    // Common
    INVALID_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 값이 입력되었습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "C002", "해당 데이터를 찾을 수 없습니다."),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "C003", "요청 본문을 읽을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C004", "지원하지 않는 HTTP 메서드입니다."),
    FILE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "C005", "파일 용량이 너무 큽니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C999", "일시적인 오류가 발생했습니다."),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AU001", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AU002", "권한이 없습니다."),

    // Classroom
    CLASSROOM_NAME_DUPLICATED(HttpStatus.CONFLICT, "CL001", "같은 이름의 반이 이미 있습니다."),
    JOIN_CODE_INVALID(HttpStatus.BAD_REQUEST, "CL002", "합류 코드가 없거나 만료되었습니다."),
    MEMBERSHIP_ALREADY_REQUESTED(HttpStatus.CONFLICT, "CL003", "이미 합류했거나 승인을 기다리는 반입니다."),
    MEMBERSHIP_NOT_PENDING(HttpStatus.CONFLICT, "CL004", "승인 대기 중인 요청이 아닙니다."),
    JOIN_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "CL005", "시간이 지난 합류 코드입니다."),
    CLASSROOM_HAS_RECORDS(HttpStatus.CONFLICT, "CL006", "학생이나 차시 기록이 있는 반은 삭제할 수 없습니다."),

    // Course
    COURSE_DUPLICATED(HttpStatus.CONFLICT, "CO001", "같은 학년과 과목의 수업이 이미 있습니다."),
    COURSE_HAS_RECORDS(HttpStatus.CONFLICT, "CO002", "반이나 문제가 있는 수업은 삭제할 수 없습니다."),

    // Lesson
    LESSON_ALREADY_OPEN(HttpStatus.CONFLICT, "LS001", "이 반에 이미 열린 차시가 있습니다."),
    LESSON_NOT_OPEN(HttpStatus.CONFLICT, "LS002", "열려 있는 차시가 아닙니다."),
    LESSON_HAS_NO_QUESTIONS(HttpStatus.CONFLICT, "LS003", "문제가 없는 차시는 열 수 없습니다."),
    LESSON_LOCKED_BY_OPENING(HttpStatus.CONFLICT, "LS004", "열렸거나 마감했거나 제출이 있는 차시의 문제는 변경할 수 없습니다."),
    QUESTION_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "LS005", "문제 글이나 이미지가 필요합니다."),
    QUESTION_OPTIONS_INVALID(HttpStatus.BAD_REQUEST, "LS006", "보기 또는 정답 구성이 올바르지 않습니다."),
    QUESTION_IMAGE_NOT_USABLE(HttpStatus.BAD_REQUEST, "LS007", "사용할 수 없는 이미지입니다."),
    QUESTION_IMAGE_INVALID(HttpStatus.BAD_REQUEST, "LS008", "PNG, JPEG, WebP 이미지만 올릴 수 있습니다."),
    LESSON_FINISHED(HttpStatus.CONFLICT, "LS009", "마감한 차시입니다."),
    SUBMISSION_ALREADY_EXISTS(HttpStatus.CONFLICT, "LS010", "이미 제출한 차시입니다."),
    SUBMISSION_ANSWER_INVALID(HttpStatus.BAD_REQUEST, "LS011", "이 차시의 문제나 보기가 아닌 답안입니다."),

    // Student profile
    STUDENT_PROFILE_ALREADY_EXISTS(HttpStatus.CONFLICT, "SP001", "이미 가입 정보를 저장했습니다."),
    STUDENT_NUMBER_DUPLICATED(HttpStatus.CONFLICT, "SP002", "이미 가입한 학번입니다."),
    STUDENT_PROFILE_REQUIRED(HttpStatus.CONFLICT, "SP003", "학생 가입 정보를 먼저 저장해 주세요."),

    // Storage
    STORAGE_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "ST001", "파일 저장소를 사용할 수 없습니다."),
}

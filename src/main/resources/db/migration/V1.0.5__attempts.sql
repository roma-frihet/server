-- 학생이 문제를 하나씩 푸는 모델이다.
-- 차시 전체를 한 번에 받던 lesson_submissions는 문제별 제한 시간과 교사의 결과 수정을
-- 담을 수 없어 교체한다. 옛 테이블은 전환이 끝난 뒤 다음 마이그레이션에서 지운다.
--
-- 점수는 저장하지 않는다. 교사가 결과를 고치면 값이 바뀌는데, 같은 숫자를 여러 화면이
-- 쓰기 때문에 저장해 두면 어긋난다. 읽을 때 final_result를 세서 만든다.

CREATE SEQUENCE attempts_seq INCREMENT BY 50;
CREATE SEQUENCE attempt_answers_seq INCREMENT BY 50;
CREATE SEQUENCE result_overrides_seq INCREMENT BY 50;

-- 한 학생이 한 반에서 한 차시를 푼 기록이다.
CREATE TABLE attempts (
    id            bigint       PRIMARY KEY,
    classroom_id  bigint       NOT NULL,
    lesson_number integer      NOT NULL,
    user_id       uuid         NOT NULL,
    phase         varchar(20)  NOT NULL,
    next_index    integer      NOT NULL,
    completed_at  timestamp(6),
    created_at    timestamp(6) NOT NULL,
    updated_at    timestamp(6) NOT NULL,
    CONSTRAINT uk_attempts_classroom_id_lesson_number_user_id
        UNIQUE (classroom_id, lesson_number, user_id),
    CONSTRAINT fk_attempts_classrooms FOREIGN KEY (classroom_id) REFERENCES classrooms (id),
    CONSTRAINT ck_attempts_phase CHECK (phase IN ('READY', 'IN_QUESTION', 'DONE')),
    CONSTRAINT ck_attempts_next_index CHECK (next_index >= 0)
);

CREATE INDEX idx_attempts_classroom_id_lesson_number ON attempts (classroom_id, lesson_number);

-- 문제 하나의 응답이다. 학생이 그 문제를 시작할 때 만들어지므로,
-- 시작하지 않은 문제는 행이 없다. 점수의 분모는 questions 쪽에서 센다.
CREATE TABLE attempt_answers (
    id             bigint       PRIMARY KEY,
    attempt_id     bigint       NOT NULL,
    question_id    bigint       NOT NULL,
    question_index integer      NOT NULL,
    time_limit_sec integer      NOT NULL,
    started_at     timestamp(6) NOT NULL,
    deadline       timestamp(6) NOT NULL,
    question_option_id bigint,
    answer_text    text,
    final_result   varchar(10)  NOT NULL,
    is_finalized   boolean      NOT NULL,
    by_timeout     boolean      NOT NULL,
    is_overridden  boolean      NOT NULL,
    finalized_at   timestamp(6),
    created_at     timestamp(6) NOT NULL,
    updated_at     timestamp(6) NOT NULL,
    CONSTRAINT uk_attempt_answers_attempt_id_question_index
        UNIQUE (attempt_id, question_index),
    CONSTRAINT fk_attempt_answers_attempts FOREIGN KEY (attempt_id) REFERENCES attempts (id),
    CONSTRAINT fk_attempt_answers_questions FOREIGN KEY (question_id) REFERENCES questions (id),
    CONSTRAINT fk_attempt_answers_question_options
        FOREIGN KEY (question_option_id) REFERENCES question_options (id),
    CONSTRAINT ck_attempt_answers_final_result CHECK (final_result IN ('RIGHT', 'WRONG', 'NONE')),
    CONSTRAINT ck_attempt_answers_question_index CHECK (question_index >= 0)
);

-- 교사가 결과를 고친 이력이다. 원래 결과를 지우지 않으려고 따로 쌓는다.
-- 집계는 이 테이블을 읽지 않고 attempt_answers.final_result만 센다.
CREATE TABLE result_overrides (
    id            bigint       PRIMARY KEY,
    attempt_answer_id bigint   NOT NULL,
    user_id       uuid         NOT NULL,
    before_result varchar(10)  NOT NULL,
    after_result  varchar(10)  NOT NULL,
    created_at    timestamp(6) NOT NULL,
    updated_at    timestamp(6) NOT NULL,
    CONSTRAINT fk_result_overrides_attempt_answers
        FOREIGN KEY (attempt_answer_id) REFERENCES attempt_answers (id),
    CONSTRAINT ck_result_overrides_before_result CHECK (before_result IN ('RIGHT', 'WRONG', 'NONE')),
    CONSTRAINT ck_result_overrides_after_result CHECK (after_result IN ('RIGHT', 'WRONG', 'NONE'))
);

CREATE INDEX idx_result_overrides_attempt_answer_id ON result_overrides (attempt_answer_id);

ALTER TABLE attempts ENABLE ROW LEVEL SECURITY;
ALTER TABLE attempt_answers ENABLE ROW LEVEL SECURITY;
ALTER TABLE result_overrides ENABLE ROW LEVEL SECURITY;

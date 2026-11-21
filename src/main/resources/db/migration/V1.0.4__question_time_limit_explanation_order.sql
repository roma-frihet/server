-- 문제마다 제한 시간과 해설을 두고, 차시 안에서 풀 순서를 고정한다.
-- 제한 시간은 학생이 그 문제를 시작한 순간부터 흐르고, 값이 없으면 30초다.

ALTER TABLE questions
    ADD COLUMN time_limit_sec integer NOT NULL DEFAULT 30,
    ADD COLUMN explanation    text,
    ADD COLUMN display_order  integer;

-- 기존 문제는 만든 순서를 그대로 풀이 순서로 쓴다.
UPDATE questions q
SET display_order = ordered.position
FROM (
    SELECT id,
           row_number() OVER (PARTITION BY course_id, lesson_number ORDER BY id) - 1 AS position
    FROM questions
) ordered
WHERE q.id = ordered.id;

ALTER TABLE questions ALTER COLUMN display_order SET NOT NULL;
ALTER TABLE questions ALTER COLUMN time_limit_sec DROP DEFAULT;

ALTER TABLE questions
    ADD CONSTRAINT ck_questions_time_limit_sec CHECK (time_limit_sec BETWEEN 1 AND 300);

-- 순서까지 포함한 유일 인덱스가 기존 (course_id, lesson_number) 인덱스를 대신한다.
DROP INDEX idx_questions_course_id_lesson_number;
CREATE UNIQUE INDEX uk_questions_course_id_lesson_number_display_order
    ON questions (course_id, lesson_number, display_order);

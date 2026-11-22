-- 교사가 반의 차시를 정한 시각에 열고 마감하도록 예약한다.
-- Cloud Run은 요청이 없으면 인스턴스가 0으로 줄어 스케줄러가 돌지 않으므로, 반의 차시 상태를 읽을 때
-- 시각이 지난 예약을 먼저 적용하고 지운다. 적용한 쪽(열기/마감)은 값을 비우고, 둘 다 비면 행을 지운다.

CREATE SEQUENCE lesson_schedules_seq INCREMENT BY 50;

CREATE TABLE lesson_schedules (
    id            bigint       PRIMARY KEY,
    classroom_id  bigint       NOT NULL,
    lesson_number integer      NOT NULL,
    open_at       timestamp(6),
    finish_at     timestamp(6),
    created_at    timestamp(6) NOT NULL,
    updated_at    timestamp(6) NOT NULL,
    CONSTRAINT uk_lesson_schedules_classroom_id_lesson_number UNIQUE (classroom_id, lesson_number),
    CONSTRAINT fk_lesson_schedules_classrooms FOREIGN KEY (classroom_id) REFERENCES classrooms (id),
    CONSTRAINT ck_lesson_schedules_lesson_number CHECK (lesson_number > 0),
    CONSTRAINT ck_lesson_schedules_has_time CHECK (open_at IS NOT NULL OR finish_at IS NOT NULL),
    CONSTRAINT ck_lesson_schedules_order CHECK (open_at IS NULL OR finish_at IS NULL OR finish_at > open_at)
);

ALTER TABLE lesson_schedules ENABLE ROW LEVEL SECURITY;

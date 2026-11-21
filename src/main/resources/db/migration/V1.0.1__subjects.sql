-- 과목을 자유 입력에서 교육과정 목록으로 바꾼다.
-- 2022 개정 교육과정(교육부 고시 제2022-33호) 기준이고, 중학교 전체와 고등학교 보통교과의
-- 공통·일반 선택까지 넣는다. 고교학점제의 진로 선택과 융합 선택은 반을 행정학급으로 보는
-- 지금 모델의 범위 밖이라 넣지 않는다. 교육과정이 개정되면 마이그레이션으로 행을 추가한다.

CREATE SEQUENCE subjects_seq INCREMENT BY 50;

CREATE TABLE subjects (
    id            bigint       PRIMARY KEY,
    school_level  varchar(10)  NOT NULL,
    subject_group varchar(30)  NOT NULL,
    name          varchar(50)  NOT NULL,
    display_order integer      NOT NULL,
    created_at    timestamp(6) NOT NULL,
    updated_at    timestamp(6) NOT NULL,
    CONSTRAINT uk_subjects_school_level_name UNIQUE (school_level, name),
    CONSTRAINT ck_subjects_school_level CHECK (school_level IN ('MIDDLE', 'HIGH'))
);

CREATE INDEX idx_subjects_school_level_display_order ON subjects (school_level, display_order);

ALTER TABLE subjects ENABLE ROW LEVEL SECURITY;

INSERT INTO subjects (id, school_level, subject_group, name, display_order, created_at, updated_at)
VALUES
    -- 중학교 교과(군)
    (nextval('subjects_seq'), 'MIDDLE', '국어', '국어', 1, now(), now()),
    (nextval('subjects_seq'), 'MIDDLE', '사회', '사회', 2, now(), now()),
    (nextval('subjects_seq'), 'MIDDLE', '사회', '역사', 3, now(), now()),
    (nextval('subjects_seq'), 'MIDDLE', '도덕', '도덕', 4, now(), now()),
    (nextval('subjects_seq'), 'MIDDLE', '수학', '수학', 5, now(), now()),
    (nextval('subjects_seq'), 'MIDDLE', '과학', '과학', 6, now(), now()),
    (nextval('subjects_seq'), 'MIDDLE', '기술·가정/정보', '기술·가정', 7, now(), now()),
    (nextval('subjects_seq'), 'MIDDLE', '기술·가정/정보', '정보', 8, now(), now()),
    (nextval('subjects_seq'), 'MIDDLE', '체육', '체육', 9, now(), now()),
    (nextval('subjects_seq'), 'MIDDLE', '예술', '음악', 10, now(), now()),
    (nextval('subjects_seq'), 'MIDDLE', '예술', '미술', 11, now(), now()),
    (nextval('subjects_seq'), 'MIDDLE', '영어', '영어', 12, now(), now()),
    -- 중학교 선택 교과
    (nextval('subjects_seq'), 'MIDDLE', '선택', '한문', 13, now(), now()),
    (nextval('subjects_seq'), 'MIDDLE', '선택', '환경', 14, now(), now()),
    (nextval('subjects_seq'), 'MIDDLE', '선택', '보건', 15, now(), now()),
    (nextval('subjects_seq'), 'MIDDLE', '선택', '진로와 직업', 16, now(), now()),
    (nextval('subjects_seq'), 'MIDDLE', '선택', '생활 독일어', 17, now(), now()),
    (nextval('subjects_seq'), 'MIDDLE', '선택', '생활 프랑스어', 18, now(), now()),
    (nextval('subjects_seq'), 'MIDDLE', '선택', '생활 스페인어', 19, now(), now()),
    (nextval('subjects_seq'), 'MIDDLE', '선택', '생활 중국어', 20, now(), now()),
    (nextval('subjects_seq'), 'MIDDLE', '선택', '생활 일본어', 21, now(), now()),
    (nextval('subjects_seq'), 'MIDDLE', '선택', '생활 러시아어', 22, now(), now()),
    (nextval('subjects_seq'), 'MIDDLE', '선택', '생활 아랍어', 23, now(), now()),
    (nextval('subjects_seq'), 'MIDDLE', '선택', '생활 베트남어', 24, now(), now()),
    -- 고등학교 공통 과목
    (nextval('subjects_seq'), 'HIGH', '국어', '공통국어1', 1, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '국어', '공통국어2', 2, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '수학', '공통수학1', 3, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '수학', '공통수학2', 4, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '수학', '기본수학1', 5, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '수학', '기본수학2', 6, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '영어', '공통영어1', 7, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '영어', '공통영어2', 8, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '영어', '기본영어1', 9, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '영어', '기본영어2', 10, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '사회', '통합사회1', 11, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '사회', '통합사회2', 12, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '사회', '한국사1', 13, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '사회', '한국사2', 14, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '과학', '통합과학1', 15, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '과학', '통합과학2', 16, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '과학', '과학탐구실험1', 17, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '과학', '과학탐구실험2', 18, now(), now()),
    -- 고등학교 일반 선택 과목
    (nextval('subjects_seq'), 'HIGH', '국어', '화법과 언어', 19, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '국어', '독서와 작문', 20, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '국어', '문학', 21, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '수학', '대수', 22, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '수학', '미적분Ⅰ', 23, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '수학', '확률과 통계', 24, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '영어', '영어Ⅰ', 25, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '영어', '영어Ⅱ', 26, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '영어', '영어 독해와 작문', 27, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '사회', '세계시민과 지리', 28, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '사회', '세계사', 29, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '사회', '사회와 문화', 30, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '사회', '현대사회와 윤리', 31, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '과학', '물리학', 32, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '과학', '화학', 33, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '과학', '생명과학', 34, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '과학', '지구과학', 35, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '체육', '체육1', 36, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '체육', '체육2', 37, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '예술', '음악', 38, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '예술', '미술', 39, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '예술', '연극', 40, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '기술·가정/정보', '기술·가정', 41, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '기술·가정/정보', '정보', 42, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '제2외국어/한문', '독일어', 43, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '제2외국어/한문', '프랑스어', 44, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '제2외국어/한문', '스페인어', 45, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '제2외국어/한문', '중국어', 46, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '제2외국어/한문', '일본어', 47, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '제2외국어/한문', '러시아어', 48, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '제2외국어/한문', '아랍어', 49, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '제2외국어/한문', '베트남어', 50, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '제2외국어/한문', '한문', 51, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '교양', '진로와 직업', 52, now(), now()),
    (nextval('subjects_seq'), 'HIGH', '교양', '생태와 환경', 53, now(), now());

-- 기존 자유 입력 과목을 중학교 과목으로 옮긴다. 이름이 맞지 않는 값이 있으면
-- SET NOT NULL에서 마이그레이션이 실패하므로 조용히 깨지지 않는다.
ALTER TABLE courses ADD COLUMN subject_id bigint;

UPDATE courses c
SET subject_id = s.id
FROM subjects s
WHERE s.school_level = 'MIDDLE'
  AND s.name = c.subject;

ALTER TABLE courses ALTER COLUMN subject_id SET NOT NULL;

ALTER TABLE courses DROP CONSTRAINT uk_courses_user_id_grade_subject;
ALTER TABLE courses DROP COLUMN subject;

ALTER TABLE courses
    ADD CONSTRAINT uk_courses_user_id_grade_subject_id UNIQUE (user_id, grade, subject_id),
    ADD CONSTRAINT fk_courses_subjects FOREIGN KEY (subject_id) REFERENCES subjects (id);

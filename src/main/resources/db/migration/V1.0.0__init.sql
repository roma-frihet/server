-- 릴리즈 전 전체 스키마다.
-- 사용자 정보는 Supabase Auth(auth.users)가 관리한다. 앱 테이블은 user_id(uuid) 값으로만 참조하고 FK를 걸지 않는다.
-- 서버만 접근하는 테이블이므로 RLS를 켜고 정책은 두지 않는다.

CREATE SEQUENCE schools_seq INCREMENT BY 50;
CREATE SEQUENCE courses_seq INCREMENT BY 50;
CREATE SEQUENCE student_profiles_seq INCREMENT BY 50;
CREATE SEQUENCE teacher_profiles_seq INCREMENT BY 50;
CREATE SEQUENCE classrooms_seq INCREMENT BY 50;
CREATE SEQUENCE classroom_memberships_seq INCREMENT BY 50;
CREATE SEQUENCE lessons_seq INCREMENT BY 50;
CREATE SEQUENCE lesson_openings_seq INCREMENT BY 50;
CREATE SEQUENCE question_images_seq INCREMENT BY 50;
CREATE SEQUENCE questions_seq INCREMENT BY 50;
CREATE SEQUENCE question_options_seq INCREMENT BY 50;
CREATE SEQUENCE lesson_submissions_seq INCREMENT BY 50;
CREATE SEQUENCE submission_answers_seq INCREMENT BY 50;

CREATE TABLE schools (
    id         bigint       PRIMARY KEY,
    name       varchar(100) NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    CONSTRAINT uk_schools_name UNIQUE (name)
);

INSERT INTO schools (id, name, created_at, updated_at)
VALUES (nextval('schools_seq'), '다원중학교', now(), now());

-- 수업은 교사 + 학년 + 과목 단위다. 차시와 문제는 수업에 속하고 같은 수업의 반들이 공유한다.
CREATE TABLE courses (
    id         bigint       PRIMARY KEY,
    user_id    uuid         NOT NULL,
    school_id  bigint       NOT NULL,
    grade      integer      NOT NULL,
    subject    varchar(50)  NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    CONSTRAINT uk_courses_user_id_grade_subject UNIQUE (user_id, grade, subject),
    CONSTRAINT fk_courses_schools FOREIGN KEY (school_id) REFERENCES schools (id),
    CONSTRAINT ck_courses_grade CHECK (grade > 0)
);

CREATE TABLE student_profiles (
    id           bigint       PRIMARY KEY,
    user_id      uuid         NOT NULL,
    school_id    bigint       NOT NULL,
    grade        integer      NOT NULL,
    class_number integer      NOT NULL,
    seat_number  integer      NOT NULL,
    name         varchar(50)  NOT NULL,
    created_at   timestamp(6) NOT NULL,
    updated_at   timestamp(6) NOT NULL,
    CONSTRAINT uk_student_profiles_user_id UNIQUE (user_id),
    CONSTRAINT uk_student_profiles_school_id_grade_class_number_seat_number
        UNIQUE (school_id, grade, class_number, seat_number),
    CONSTRAINT fk_student_profiles_schools FOREIGN KEY (school_id) REFERENCES schools (id),
    CONSTRAINT ck_student_profiles_grade CHECK (grade > 0),
    CONSTRAINT ck_student_profiles_class_number CHECK (class_number > 0),
    CONSTRAINT ck_student_profiles_seat_number CHECK (seat_number > 0)
);

CREATE TABLE teacher_profiles (
    id         bigint       PRIMARY KEY,
    user_id    uuid         NOT NULL,
    school_id  bigint       NOT NULL,
    name       varchar(50)  NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    CONSTRAINT uk_teacher_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_teacher_profiles_schools FOREIGN KEY (school_id) REFERENCES schools (id)
);

CREATE TABLE classrooms (
    id                   bigint       PRIMARY KEY,
    course_id            bigint       NOT NULL,
    name                 varchar(50)  NOT NULL,
    join_code            varchar(8),
    join_code_expires_at timestamp(6),
    created_at           timestamp(6) NOT NULL,
    updated_at           timestamp(6) NOT NULL,
    CONSTRAINT uk_classrooms_course_id_name UNIQUE (course_id, name),
    CONSTRAINT uk_classrooms_join_code UNIQUE (join_code),
    CONSTRAINT fk_classrooms_courses FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT ck_classrooms_join_code_expiry
        CHECK ((join_code IS NULL) = (join_code_expires_at IS NULL))
);

CREATE TABLE classroom_memberships (
    id             bigint       PRIMARY KEY,
    classroom_id   bigint       NOT NULL,
    user_id        uuid         NOT NULL,
    student_name   varchar(50)  NOT NULL,
    student_number varchar(20)  NOT NULL,
    status         varchar(20)  NOT NULL,
    created_at     timestamp(6) NOT NULL,
    updated_at     timestamp(6) NOT NULL,
    CONSTRAINT uk_classroom_memberships_classroom_id_user_id UNIQUE (classroom_id, user_id),
    CONSTRAINT fk_classroom_memberships_classrooms FOREIGN KEY (classroom_id) REFERENCES classrooms (id),
    CONSTRAINT ck_classroom_memberships_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_classroom_memberships_user_id_status ON classroom_memberships (user_id, status);

CREATE TABLE lessons (
    id            bigint       PRIMARY KEY,
    course_id     bigint       NOT NULL,
    lesson_number integer      NOT NULL,
    title         varchar(100) NOT NULL,
    created_at    timestamp(6) NOT NULL,
    updated_at    timestamp(6) NOT NULL,
    CONSTRAINT uk_lessons_course_id_lesson_number UNIQUE (course_id, lesson_number),
    CONSTRAINT fk_lessons_courses FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT ck_lessons_lesson_number CHECK (lesson_number > 0)
);

CREATE TABLE lesson_openings (
    id            bigint       PRIMARY KEY,
    classroom_id  bigint       NOT NULL,
    lesson_number integer      NOT NULL,
    opened_at     timestamp(6) NOT NULL,
    closed_at     timestamp(6),
    finished_at   timestamp(6),
    created_at    timestamp(6) NOT NULL,
    updated_at    timestamp(6) NOT NULL,
    CONSTRAINT uk_lesson_openings_classroom_id_lesson_number UNIQUE (classroom_id, lesson_number),
    CONSTRAINT fk_lesson_openings_classrooms FOREIGN KEY (classroom_id) REFERENCES classrooms (id),
    CONSTRAINT ck_lesson_openings_lesson_number CHECK (lesson_number > 0),
    CONSTRAINT ck_lesson_openings_finished_at_closed_at
        CHECK (finished_at IS NULL OR closed_at IS NOT NULL)
);

-- 반마다 동시에 열린 차시는 하나뿐이다.
CREATE UNIQUE INDEX uk_lesson_openings_classroom_id ON lesson_openings (classroom_id) WHERE closed_at IS NULL;

-- 문제와 보기가 참조하지 않는 행이 고아 이미지다.
CREATE TABLE question_images (
    id           bigint       PRIMARY KEY,
    user_id      uuid         NOT NULL,
    storage_path varchar(255) NOT NULL,
    content_type varchar(50)  NOT NULL,
    size_bytes   bigint       NOT NULL,
    created_at   timestamp(6) NOT NULL,
    updated_at   timestamp(6) NOT NULL,
    CONSTRAINT uk_question_images_storage_path UNIQUE (storage_path),
    CONSTRAINT ck_question_images_size_bytes CHECK (size_bytes >= 0)
);

CREATE TABLE questions (
    id                bigint       PRIMARY KEY,
    course_id         bigint       NOT NULL,
    lesson_number     integer      NOT NULL,
    type              varchar(20)  NOT NULL,
    content           text,
    question_image_id bigint,
    created_at        timestamp(6) NOT NULL,
    updated_at        timestamp(6) NOT NULL,
    CONSTRAINT fk_questions_courses FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT fk_questions_question_images FOREIGN KEY (question_image_id) REFERENCES question_images (id),
    CONSTRAINT ck_questions_lesson_number CHECK (lesson_number > 0),
    CONSTRAINT ck_questions_type CHECK (type IN ('MULTIPLE_CHOICE', 'SHORT_ANSWER')),
    CONSTRAINT ck_questions_content_or_image CHECK (content IS NOT NULL OR question_image_id IS NOT NULL)
);

CREATE INDEX idx_questions_course_id_lesson_number ON questions (course_id, lesson_number);
CREATE INDEX idx_questions_question_image_id ON questions (question_image_id);

CREATE TABLE question_options (
    id                bigint       PRIMARY KEY,
    question_id       bigint       NOT NULL,
    display_order     integer      NOT NULL,
    content           text,
    question_image_id bigint,
    is_correct        boolean      NOT NULL,
    created_at        timestamp(6) NOT NULL,
    updated_at        timestamp(6) NOT NULL,
    CONSTRAINT uk_question_options_question_id_display_order UNIQUE (question_id, display_order),
    CONSTRAINT fk_question_options_questions FOREIGN KEY (question_id) REFERENCES questions (id),
    CONSTRAINT fk_question_options_question_images FOREIGN KEY (question_image_id) REFERENCES question_images (id),
    CONSTRAINT ck_question_options_content_or_image CHECK (content IS NOT NULL OR question_image_id IS NOT NULL)
);

CREATE INDEX idx_question_options_question_image_id ON question_options (question_image_id);

CREATE TABLE lesson_submissions (
    id             bigint       PRIMARY KEY,
    classroom_id   bigint       NOT NULL,
    lesson_number  integer      NOT NULL,
    user_id        uuid         NOT NULL,
    correct_count  integer      NOT NULL,
    question_count integer      NOT NULL,
    created_at     timestamp(6) NOT NULL,
    updated_at     timestamp(6) NOT NULL,
    CONSTRAINT uk_lesson_submissions_classroom_id_lesson_number_user_id
        UNIQUE (classroom_id, lesson_number, user_id),
    CONSTRAINT fk_lesson_submissions_classrooms FOREIGN KEY (classroom_id) REFERENCES classrooms (id),
    CONSTRAINT ck_lesson_submissions_correct_count CHECK (correct_count BETWEEN 0 AND question_count)
);

CREATE TABLE submission_answers (
    id                   bigint       PRIMARY KEY,
    lesson_submission_id bigint       NOT NULL,
    question_id          bigint       NOT NULL,
    question_option_id   bigint,
    answer_text          text,
    is_correct           boolean      NOT NULL,
    created_at           timestamp(6) NOT NULL,
    updated_at           timestamp(6) NOT NULL,
    CONSTRAINT uk_submission_answers_lesson_submission_id_question_id UNIQUE (lesson_submission_id, question_id),
    CONSTRAINT fk_submission_answers_lesson_submissions
        FOREIGN KEY (lesson_submission_id) REFERENCES lesson_submissions (id),
    CONSTRAINT fk_submission_answers_questions FOREIGN KEY (question_id) REFERENCES questions (id),
    CONSTRAINT fk_submission_answers_question_options
        FOREIGN KEY (question_option_id) REFERENCES question_options (id)
);

ALTER TABLE schools ENABLE ROW LEVEL SECURITY;
ALTER TABLE courses ENABLE ROW LEVEL SECURITY;
ALTER TABLE student_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE teacher_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE classrooms ENABLE ROW LEVEL SECURITY;
ALTER TABLE classroom_memberships ENABLE ROW LEVEL SECURITY;
ALTER TABLE lessons ENABLE ROW LEVEL SECURITY;
ALTER TABLE lesson_openings ENABLE ROW LEVEL SECURITY;
ALTER TABLE question_images ENABLE ROW LEVEL SECURITY;
ALTER TABLE questions ENABLE ROW LEVEL SECURITY;
ALTER TABLE question_options ENABLE ROW LEVEL SECURITY;
ALTER TABLE lesson_submissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE submission_answers ENABLE ROW LEVEL SECURITY;

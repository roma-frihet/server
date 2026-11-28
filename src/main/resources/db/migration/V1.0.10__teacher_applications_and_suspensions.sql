-- 교사는 직접 가입을 신청하고 관리자가 승인한다. 승인하면 Supabase app_metadata.role에 teacher를 넣는다.
-- 사용자당 한 행이다. 거절된 사용자가 다시 신청하면 같은 행을 대기로 되돌린다.
-- 이메일은 신청할 때 토큰에서 옮겨 둔다. 관리자 목록에서 사람을 알아보는 용도다.
-- email_verified_at은 나중에 붙일 이메일 인증이 채운다. 학생이 받을 수 없는 주소로 가입하므로
-- Supabase의 Confirm email은 켤 수 없고, 교사 신청에만 따로 인증을 받는다.

CREATE SEQUENCE teacher_applications_seq INCREMENT BY 50;
CREATE SEQUENCE account_suspensions_seq INCREMENT BY 50;

CREATE TABLE teacher_applications (
    id            bigint       PRIMARY KEY,
    user_id       uuid         NOT NULL,
    email         varchar(255) NOT NULL,
    school_id     bigint       NOT NULL,
    name          varchar(50)  NOT NULL,
    status        varchar(20)  NOT NULL,
    reject_reason varchar(200),
    email_verified_at timestamp(6),
    reviewed_by   uuid,
    reviewed_at   timestamp(6),
    created_at    timestamp(6) NOT NULL,
    updated_at    timestamp(6) NOT NULL,
    CONSTRAINT uk_teacher_applications_user_id UNIQUE (user_id),
    CONSTRAINT fk_teacher_applications_schools FOREIGN KEY (school_id) REFERENCES schools (id),
    CONSTRAINT ck_teacher_applications_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'REVOKED'))
);

-- 관리자 화면은 상태로 거르고 최근 신청부터 본다.
CREATE INDEX idx_teacher_applications_status_created_at ON teacher_applications (status, created_at);

-- 관리자가 정지한 계정이다. 실제 차단은 Supabase ban이 하고, 이 표는 목록에 정지 여부를 보이려고 둔다.
CREATE TABLE account_suspensions (
    id           bigint       PRIMARY KEY,
    user_id      uuid         NOT NULL,
    reason       varchar(200),
    suspended_by uuid         NOT NULL,
    created_at   timestamp(6) NOT NULL,
    updated_at   timestamp(6) NOT NULL,
    CONSTRAINT uk_account_suspensions_user_id UNIQUE (user_id)
);

ALTER TABLE teacher_applications ENABLE ROW LEVEL SECURITY;
ALTER TABLE account_suspensions ENABLE ROW LEVEL SECURITY;

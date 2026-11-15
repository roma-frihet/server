-- 교사가 비밀번호를 초기화하면 임시 비밀번호가 되고, 학생이 새로 정할 때까지 표시해 둔다.
-- 비밀번호 자체는 Supabase Auth가 갖고 있고 서버는 이 플래그만 관리한다.

ALTER TABLE student_profiles
    ADD COLUMN must_change_password boolean NOT NULL DEFAULT false;

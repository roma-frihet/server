-- 교사 승인을 없앤다. 학생이 유효한 참여 코드를 넣으면 바로 연결된다.
-- 거절 상태로 남아 있던 요청은 연결된 적이 없으므로 지우고, 대기 상태는 그대로 연결로 본다.

DELETE FROM classroom_memberships WHERE status = 'REJECTED';

DROP INDEX idx_classroom_memberships_user_id_status;

ALTER TABLE classroom_memberships DROP CONSTRAINT ck_classroom_memberships_status;
ALTER TABLE classroom_memberships DROP COLUMN status;

CREATE INDEX idx_classroom_memberships_user_id ON classroom_memberships (user_id);

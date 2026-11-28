-- 교사가 푼 기록이 있는 문제를 지울 수 있게 되면서 attempt_answers를 question_id로 찾는 경로가 생겼다.
-- 이 테이블은 (학생 x 문제 x 차시)로 늘어나 이 스키마에서 가장 빨리 커지는데, 두 컬럼에 인덱스가 없어
-- 지울 때마다 풀스캔이 난다. 삭제 쿼리뿐 아니라 Postgres가 외래 키 참조를 확인할 때도 같은 스캔을 한다.
--
-- 고르지 않은 것
--   attempt_id        : uk_attempt_answers_attempt_id_question_index의 선두 컬럼이라 이미 쓰인다.
--                       따로 만들면 앞부분이 겹치는 중복 인덱스가 된다.
--   복합 (question_id, ...) : question_id 하나로 이미 충분히 좁혀지고, 뒤에 붙일 컬럼을 쓰는 쿼리가 없다.
--   부분 인덱스       : question_option_id는 주관식과 무응답에서만 NULL이라 객관식 위주인 지금은
--                       걸러지는 양이 적고, 외래 키 확인에 부분 인덱스가 쓰이는지는 플래너에 달려 있다.
--                       아끼는 공간보다 안 쓰일 위험이 크다.
--
-- CONCURRENTLY를 쓰지 않는다. Flyway가 마이그레이션을 트랜잭션으로 감싸는데 그 안에서는 쓸 수 없고,
-- 지금은 테이블이 비어 있어 즉시 끝난다. 데이터가 쌓인 뒤 인덱스를 더할 때는 트랜잭션을 꺼야 한다.

-- 문제 하나의 응답을 찾는다. 삭제와 외래 키 확인, 문제별 응답 분포 집계가 모두 쓴다.
-- 한 문제의 응답은 (반 수 x 반 인원)이라 전체 대비 아주 작아 선택도가 높다.
CREATE INDEX idx_attempt_answers_question_id ON attempt_answers (question_id);

-- 보기가 지워질 때 Postgres가 이 컬럼을 참조하는 행이 있는지 확인한다. 문제 하나를 지우면
-- 보기 수만큼 확인이 일어나므로 인덱스가 없으면 그만큼 풀스캔이 반복된다.
CREATE INDEX idx_attempt_answers_question_option_id ON attempt_answers (question_option_id);

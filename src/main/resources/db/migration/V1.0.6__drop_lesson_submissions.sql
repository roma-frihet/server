-- 차시 전체를 한 번에 받던 모델을 지운다. attempts / attempt_answers가 대신한다.
-- 개발 단계의 테스트 데이터뿐이라 옮기지 않고 버린다.

DROP TABLE submission_answers;
DROP TABLE lesson_submissions;

DROP SEQUENCE submission_answers_seq;
DROP SEQUENCE lesson_submissions_seq;

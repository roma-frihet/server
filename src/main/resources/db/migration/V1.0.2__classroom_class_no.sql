-- 반을 자유 입력 이름에서 번호로 바꾼다. 문자열이면 반이 10개를 넘을 때
-- "1반, 10반, 2반" 순으로 정렬이 어긋나고, 화면도 번호 선택으로 되어 있다.
-- 기존 이름에서 숫자만 뽑아 옮기고, 뽑히지 않는 값이 있으면 SET NOT NULL에서 실패한다.

ALTER TABLE classrooms ADD COLUMN class_no integer;

UPDATE classrooms
SET class_no = NULLIF(regexp_replace(name, '[^0-9]', '', 'g'), '')::integer;

ALTER TABLE classrooms ALTER COLUMN class_no SET NOT NULL;

ALTER TABLE classrooms DROP CONSTRAINT uk_classrooms_course_id_name;
ALTER TABLE classrooms DROP COLUMN name;

ALTER TABLE classrooms
    ADD CONSTRAINT uk_classrooms_course_id_class_no UNIQUE (course_id, class_no),
    ADD CONSTRAINT ck_classrooms_class_no CHECK (class_no BETWEEN 1 AND 20);

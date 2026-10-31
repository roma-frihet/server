-- 초기 마이그레이션
-- 이미 적용된 마이그레이션 파일은 수정하지 않는다. 변경은 새 버전 파일로 추가한다. (01-entity.md 1.3)
-- 사용자 정보는 Supabase Auth(auth.users)가 관리한다. 앱 테이블에서는 user_id(uuid)로 참조한다.
SELECT 1;

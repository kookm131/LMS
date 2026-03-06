-- 기본 관리자 계정 (없을 때만 생성)
INSERT INTO users (user_type, username, email, password, name, birth_date, gender, phone, role)
SELECT 'NORMAL', 'admin', 'admin@lms.local', '{noop}admin123', '관리자', '1990-01-01', 'MALE', '010-0000-0000', 'ADMIN'
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE username = 'admin'
);

-- 추천 강의 샘플 50개
-- 앱 시작 시 courses 테이블에 더미 데이터를 채움

TRUNCATE TABLE courses;

INSERT INTO courses (title, instructor_name, description, status)
VALUES
('스프링부트 핵심 입문 1', '김강사', '스프링부트 기초 개념과 프로젝트 시작 방법', 'PUBLISHED'),
('스프링부트 핵심 입문 2', '김강사', '의존성 관리와 실행 구조 이해', 'PUBLISHED'),
('스프링부트 핵심 입문 3', '김강사', 'Controller와 View 연결 실습', 'PUBLISHED'),
('스프링부트 핵심 입문 4', '김강사', 'REST API 설계 기본기', 'PUBLISHED'),
('스프링부트 핵심 입문 5', '김강사', '예외 처리와 검증 처리', 'PUBLISHED'),
('실전 SQL 튜닝 6', '박튜너', '인덱스와 실행 계획 보는 방법', 'PUBLISHED'),
('실전 SQL 튜닝 7', '박튜너', '느린 쿼리 개선 사례 분석', 'PUBLISHED'),
('실전 SQL 튜닝 8', '박튜너', '조인 최적화 핵심 포인트', 'PUBLISHED'),
('실전 SQL 튜닝 9', '박튜너', '대용량 테이블 조회 전략', 'PUBLISHED'),
('실전 SQL 튜닝 10', '박튜너', 'MySQL 튜닝 체크리스트', 'PUBLISHED'),
('JPA 실무 사용법 11', '이엔티티', '엔티티 설계와 연관관계 기초', 'PUBLISHED'),
('JPA 실무 사용법 12', '이엔티티', '영속성 컨텍스트 제대로 이해하기', 'PUBLISHED'),
('JPA 실무 사용법 13', '이엔티티', 'N+1 문제와 페치 전략', 'PUBLISHED'),
('JPA 실무 사용법 14', '이엔티티', '성능을 고려한 쿼리 작성', 'PUBLISHED'),
('JPA 실무 사용법 15', '이엔티티', '트랜잭션 경계 설계', 'PUBLISHED'),
('JdbcTemplate 마스터 16', '최쿼리', 'JdbcTemplate 기본 패턴', 'PUBLISHED'),
('JdbcTemplate 마스터 17', '최쿼리', 'RowMapper 실전 활용', 'PUBLISHED'),
('JdbcTemplate 마스터 18', '최쿼리', '배치 처리와 성능 최적화', 'PUBLISHED'),
('JdbcTemplate 마스터 19', '최쿼리', '트랜잭션과 예외 처리', 'PUBLISHED'),
('JdbcTemplate 마스터 20', '최쿼리', '실무 SQL 코드 구조화', 'PUBLISHED'),
('Thymeleaf 화면 개발 21', '오템플릿', 'Thymeleaf 기본 문법', 'PUBLISHED'),
('Thymeleaf 화면 개발 22', '오템플릿', '폼 바인딩과 검증 메세지', 'PUBLISHED'),
('Thymeleaf 화면 개발 23', '오템플릿', '레이아웃/프래그먼트 재사용', 'PUBLISHED'),
('Thymeleaf 화면 개발 24', '오템플릿', '리스트/페이징 화면 구성', 'PUBLISHED'),
('Thymeleaf 화면 개발 25', '오템플릿', '실무 UI 컴포넌트 분리', 'PUBLISHED'),
('Spring Security 기초 26', '정보안', '인증/인가 핵심 개념', 'PUBLISHED'),
('Spring Security 기초 27', '정보안', '로그인 흐름 구현', 'PUBLISHED'),
('Spring Security 기초 28', '정보안', '권한별 접근 제어', 'PUBLISHED'),
('Spring Security 기초 29', '정보안', '세션과 쿠키 보안', 'PUBLISHED'),
('Spring Security 기초 30', '정보안', '실무 보안 설정 팁', 'PUBLISHED'),
('Docker 배포 입문 31', '윤배포', '컨테이너 기본 개념', 'PUBLISHED'),
('Docker 배포 입문 32', '윤배포', 'Dockerfile 작성법', 'PUBLISHED'),
('Docker 배포 입문 33', '윤배포', 'Compose로 멀티 컨테이너 구성', 'PUBLISHED'),
('Docker 배포 입문 34', '윤배포', '운영 배포 체크리스트', 'PUBLISHED'),
('Docker 배포 입문 35', '윤배포', '장애 대응 기본기', 'PUBLISHED'),
('Git 협업 실무 36', '한협업', '브랜치 전략 한눈에 보기', 'PUBLISHED'),
('Git 협업 실무 37', '한협업', 'PR 리뷰 잘 받는 법', 'PUBLISHED'),
('Git 협업 실무 38', '한협업', '충돌 해결 실습', 'PUBLISHED'),
('Git 협업 실무 39', '한협업', '커밋 메세지 규칙', 'PUBLISHED'),
('Git 협업 실무 40', '한협업', '릴리즈 관리 실전', 'PUBLISHED'),
('생성형 AI 업무활용 41', '정자동화', '프롬프트 작성 기본', 'PUBLISHED'),
('생성형 AI 업무활용 42', '정자동화', '문서 자동화 사례', 'PUBLISHED'),
('생성형 AI 업무활용 43', '정자동화', '코드 보조 활용법', 'PUBLISHED'),
('생성형 AI 업무활용 44', '정자동화', '품질 검증 체크리스트', 'PUBLISHED'),
('생성형 AI 업무활용 45', '정자동화', '팀 생산성 향상 전략', 'PUBLISHED'),
('알고리즘 감각 키우기 46', '남알고', '문제 해결 사고법', 'PUBLISHED'),
('알고리즘 감각 키우기 47', '남알고', '자료구조 핵심 정리', 'PUBLISHED'),
('알고리즘 감각 키우기 48', '남알고', '코딩테스트 실전 패턴', 'PUBLISHED'),
('알고리즘 감각 키우기 49', '남알고', '시간복잡도 빠르게 판단', 'PUBLISHED'),
('알고리즘 감각 키우기 50', '남알고', '실수 줄이는 풀이 습관', 'PUBLISHED');

-- 카테고리/평점/수강생 더미 보정
UPDATE courses SET category = '백엔드' WHERE id BETWEEN 1 AND 10;
UPDATE courses SET category = '데이터' WHERE id BETWEEN 11 AND 20;
UPDATE courses SET category = '프론트엔드' WHERE id BETWEEN 21 AND 30;
UPDATE courses SET category = '인프라' WHERE id BETWEEN 31 AND 40;
UPDATE courses SET category = 'AI/자동화' WHERE id BETWEEN 41 AND 50;

UPDATE courses SET rating = 4.9, purchase_count = 1280, total_hours = 24, avg_hours = 3.8 WHERE id % 5 = 0;
UPDATE courses SET rating = 4.7, purchase_count = 920, total_hours = 20, avg_hours = 3.4 WHERE id % 5 = 1;
UPDATE courses SET rating = 4.5, purchase_count = 740, total_hours = 18, avg_hours = 3.1 WHERE id % 5 = 2;
UPDATE courses SET rating = 4.3, purchase_count = 560, total_hours = 16, avg_hours = 2.8 WHERE id % 5 = 3;
UPDATE courses SET rating = 4.1, purchase_count = 380, total_hours = 14, avg_hours = 2.5 WHERE id % 5 = 4;

-- 추가 더미 100개 (페이징 테스트용)
INSERT INTO courses (category, title, instructor_name, description, rating, purchase_count, status)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 100
)
SELECT
    CASE
        WHEN n % 5 = 0 THEN '백엔드'
        WHEN n % 5 = 1 THEN '데이터'
        WHEN n % 5 = 2 THEN '프론트엔드'
        WHEN n % 5 = 3 THEN '인프라'
        ELSE 'AI/자동화'
    END AS category,
    CONCAT('페이징 테스트 강의 ', n) AS title,
    CONCAT('테스트강사', n % 10) AS instructor_name,
    CONCAT('페이징 동작 확인용 더미 강의입니다. #', n) AS description,
    CASE
        WHEN n % 5 = 0 THEN 4.9
        WHEN n % 5 = 1 THEN 4.7
        WHEN n % 5 = 2 THEN 4.5
        WHEN n % 5 = 3 THEN 4.3
        ELSE 4.1
    END AS rating,
    100 + (n * 7) AS purchase_count,
    'PUBLISHED' AS status
FROM seq;

-- 콘텐츠 설명 텍스트(전체 강의)
UPDATE courses
SET content_text = CONCAT(
    title, ' 강의입니다. 실무에 바로 적용할 수 있는 핵심 개념과 예제를 중심으로 구성되어 있습니다. ',
    '초급부터 중급까지 단계적으로 학습할 수 있도록 설계되었고, 과제와 실습이 포함됩니다.'
);

-- 학습목차 더미(전체 강의): 강의별 5개 목차, 시간은 랜덤성 있게 생성
TRUNCATE TABLE lectures;
INSERT INTO lectures (course_id, title, content_type, duration_sec, sort_order)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 5
)
SELECT
    c.id AS course_id,
    CONCAT('학습목차 ', n) AS title,
    'VIDEO' AS content_type,
    (900 + ((c.id * 137 + n * 59) % 2401)) AS duration_sec,
    n AS sort_order
FROM courses c
CROSS JOIN seq;

-- 총 수강시간 / 회차당 평균시간 재계산
-- 회차당 평균시간 = (총 초 / 60) / 목차 수  => 분 단위
UPDATE courses c
JOIN (
    SELECT
        course_id,
        SUM(duration_sec) AS total_sec,
        COUNT(*) AS lecture_cnt
    FROM lectures
    GROUP BY course_id
) t ON c.id = t.course_id
SET
    c.total_hours = CEIL(t.total_sec / 3600),
    c.avg_hours = ROUND((t.total_sec / 60) / t.lecture_cnt, 1);

-- 공지사항 더미
DELETE FROM notices;
INSERT INTO notices (course_id, author_id, title, content)
VALUES
(NULL, 1, '2월 시스템 점검 안내', '2/28 02:00~04:00 점검 예정입니다.'),
(NULL, 1, '신규 강의 업데이트', '스프링 시리즈 강의가 추가되었습니다.'),
(NULL, 1, '학습 이벤트 오픈', '3월 출석 이벤트가 시작됩니다.'),
(NULL, 1, '모바일 화면 개선 공지', '모바일 UI가 개선되었습니다.'),
(NULL, 1, 'QnA 운영정책 변경', '욕설/비방 글은 삭제 처리됩니다.'),
(NULL, 1, '출석 인정 기준 안내', '출석 인정 기준이 일부 변경되었습니다.'),
(NULL, 1, '서버 안정화 작업 공지', '금일 야간 서버 안정화 작업이 예정되어 있습니다.'),
(NULL, 1, '수강신청 정책 업데이트', '수강 취소 가능 기간 정책이 업데이트되었습니다.'),
(NULL, 1, '학습자료 업로드 완료', '요청하신 보충 학습자료 업로드가 완료되었습니다.'),
(NULL, 1, '모의고사 일정 안내', '다음 주 모의고사 일정이 공지되었습니다.'),
(NULL, 1, '커뮤니티 이용 가이드', '커뮤니티 이용 가이드가 추가되었습니다.'),
(NULL, 1, '주간 점검 사전 안내', '매주 일요일 점검 시간이 적용됩니다.');

-- 공지사항 댓글 더미 (상세/페이징 테스트용)
DELETE FROM notice_comments;
INSERT INTO notice_comments (notice_id, user_id, content)
SELECT n.id, 1, c.content
FROM (SELECT id FROM notices WHERE title = '모의고사 일정 안내' LIMIT 1) n
JOIN (
    SELECT '모의고사 일정 확인했습니다.' AS content
    UNION ALL SELECT '시험 범위도 함께 공지 부탁드립니다.'
    UNION ALL SELECT '시간표와 겹치지 않아서 다행이네요.'
    UNION ALL SELECT '준비 자료가 있으면 공유 부탁드려요.'
    UNION ALL SELECT '응시 방법 안내도 기대하겠습니다.'
    UNION ALL SELECT '감사합니다. 일정 반영할게요.'
    UNION ALL SELECT '온라인 응시 가능한가요?'
    UNION ALL SELECT '시험 시간 변경 가능성도 있나요?'
    UNION ALL SELECT '학습 계획 세우는 데 도움이 됐어요.'
    UNION ALL SELECT '공지 빠르게 올려주셔서 감사합니다.'
    UNION ALL SELECT '추가 안내 나오면 다시 확인하겠습니다.'
) c;

-- 수강평 더미
TRUNCATE TABLE course_reviews;
INSERT INTO course_reviews (course_id, writer_name, rating, content)
VALUES
(1, '김수강', 5, '기초 정리가 깔끔해서 좋아요.'),
(2, '박학습', 4, '실무 사례가 많아서 도움 됐어요.'),
(3, '이프론트', 5, '레이아웃 분리 설명이 특히 좋았습니다.'),
(4, '정자동', 4, '바로 적용 가능한 팁이 많아요.'),
(5, '최개발', 5, '팀 협업할 때 꼭 필요한 내용입니다.');

-- 1번 강의 리뷰 추가(페이징 확인용)
INSERT INTO course_reviews (course_id, writer_name, rating, content)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 30
)
SELECT
    1 AS course_id,
    CONCAT('수강생', n) AS writer_name,
    CASE
        WHEN n % 5 = 0 THEN 5
        WHEN n % 5 = 1 THEN 4
        WHEN n % 5 = 2 THEN 5
        WHEN n % 5 = 3 THEN 4
        ELSE 3
    END AS rating,
    CONCAT('상세페이지 수강평 페이징 테스트 리뷰 ', n, '번입니다.') AS content
FROM seq;

-- '페이징 테스트 강의 100' 리뷰 10개 (강의ID를 제목으로 찾아서 삽입)
INSERT INTO course_reviews (course_id, writer_name, rating, content)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 10
)
SELECT
    c.id AS course_id,
    CONCAT('테스트리뷰어', n) AS writer_name,
    CASE
        WHEN n % 2 = 0 THEN 5
        ELSE 4
    END AS rating,
    CONCAT('페이징 테스트 강의 100 리뷰 ', n, '번입니다.') AS content
FROM seq
JOIN courses c ON c.title = '페이징 테스트 강의 100'
LIMIT 10;

-- QnA 더미 50개
DELETE FROM qna_questions;
INSERT INTO qna_questions (user_id, title, content, view_count)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 50
)
SELECT
    1 AS user_id,
    CONCAT('QnA 질문 ', LPAD(n, 2, '0'), '번 입니다') AS title,
    CONCAT('QnA 테스트용 더미 질문 내용입니다. 번호: ', n, '.') AS content,
    10 + (n * 3) AS view_count
FROM seq;

-- 자주 묻는 질문 상단 노출용: 조회수 높은 5개 보정
UPDATE qna_questions SET view_count = 1550 WHERE id = 50;
UPDATE qna_questions SET view_count = 1480 WHERE id = 49;
UPDATE qna_questions SET view_count = 1420 WHERE id = 48;
UPDATE qna_questions SET view_count = 1360 WHERE id = 47;
UPDATE qna_questions SET view_count = 1300 WHERE id = 46;

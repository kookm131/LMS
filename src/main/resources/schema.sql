CREATE DATABASE IF NOT EXISTS lms
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE lms;

-- 1) 사용자
-- 회원가입 데이터 보존을 위해 DROP 하지 않음
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_type ENUM('NORMAL','BUSINESS') NOT NULL DEFAULT 'NORMAL',
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    birth_date DATE NOT NULL,
    gender ENUM('MALE','FEMALE') NOT NULL,
    phone VARCHAR(20) NOT NULL,
    role ENUM('STUDENT','INSTRUCTOR','ADMIN') NOT NULL DEFAULT 'STUDENT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 1-1) 사업자 사용자 (별도 DB 테이블)
CREATE TABLE IF NOT EXISTS business_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    representative_name VARCHAR(100) NOT NULL,
    business_name VARCHAR(200) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 2) 강의
-- 초기 개발 단계라 스키마 불일치 방지를 위해 courses 테이블은 재생성
DROP TABLE IF EXISTS courses;
CREATE TABLE courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(50) NOT NULL DEFAULT '기타',
    title VARCHAR(200) NOT NULL,
    instructor_name VARCHAR(100) NOT NULL,
    description TEXT,
    content_text TEXT,
    created_by_username VARCHAR(50) NULL,
    total_hours INT NOT NULL DEFAULT 10,
    avg_hours DECIMAL(4,1) NOT NULL DEFAULT 3.5,
    rating DECIMAL(2,1) NOT NULL DEFAULT 4.5,
    purchase_count INT NOT NULL DEFAULT 0,
    status ENUM('DRAFT','PUBLISHED','CLOSED') NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 3) 수강신청
CREATE TABLE IF NOT EXISTS enrollments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    progress_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    enrolled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    UNIQUE KEY uq_enrollments_course_student (course_id, student_id)
);

-- 4) 강의 섹션
CREATE TABLE IF NOT EXISTS sections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    sort_order INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5) 강의 콘텐츠
DROP TABLE IF EXISTS lectures;
CREATE TABLE lectures (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    section_id BIGINT NULL,
    title VARCHAR(200) NOT NULL,
    content_type ENUM('VIDEO','TEXT','FILE') NOT NULL DEFAULT 'VIDEO',
    content_url VARCHAR(500) NULL,
    duration_sec INT NULL,
    sort_order INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 8) 퀴즈
CREATE TABLE IF NOT EXISTS quizzes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 9) 퀴즈 문항
CREATE TABLE IF NOT EXISTS quiz_questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quiz_id BIGINT NOT NULL,
    question_text TEXT NOT NULL,
    question_type ENUM('MCQ','SHORT') NOT NULL DEFAULT 'MCQ',
    points INT NOT NULL DEFAULT 10,
    sort_order INT NOT NULL DEFAULT 1
);

-- 10) 객관식 선택지
CREATE TABLE IF NOT EXISTS quiz_choices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    choice_text VARCHAR(500) NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 1
);

-- 11) 퀴즈 결과
CREATE TABLE IF NOT EXISTS quiz_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quiz_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    score INT NOT NULL DEFAULT 0,
    started_at DATETIME NULL,
    submitted_at DATETIME NULL,
    UNIQUE KEY uq_quiz_results_quiz_student (quiz_id, student_id)
);

-- 12) 공지사항
CREATE TABLE IF NOT EXISTS notices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NULL,
    author_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- 12-1) QnA
CREATE TABLE IF NOT EXISTS qna_questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    view_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 13) 수강평
DROP TABLE IF EXISTS course_reviews;
CREATE TABLE course_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    writer_name VARCHAR(100) NOT NULL,
    rating INT NOT NULL,
    content VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 14) 공지사항 댓글
CREATE TABLE IF NOT EXISTS notice_comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notice_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 (필요 시 추후 추가)


-- 15) 학습 노트 (사용자/강의/목차별 메모장)
CREATE TABLE IF NOT EXISTS study_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    lecture_id BIGINT NOT NULL,
    note_content TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_study_notes_user_course_lecture (user_id, course_id, lecture_id)
);


-- 16) 강의 단위 학습 메모 워크스페이스
CREATE TABLE IF NOT EXISTS study_course_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    note_content TEXT,
    summary_line1 VARCHAR(500),
    summary_line2 VARCHAR(500),
    summary_line3 VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_study_course_notes_user_course (user_id, course_id)
);

-- 17) 학습 질문 메모
CREATE TABLE IF NOT EXISTS study_questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    question_text VARCHAR(1000) NOT NULL,
    status ENUM('OPEN','DONE') NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

# CodePilot LMS 아키텍처 정의서 (현재 구현 기준)

## 1. 개요
본 문서는 현재까지 구현된 CodePilot LMS의 실제 구조를 기준으로 아키텍처를 정의한다.

시스템 목표:
- IT 학습 안내형 LMS 제공
- 일반 사용자(학습자)와 사업자 사용자(강의 운영자) 기능 분리
- 수강 흐름(탐색 → 신청 → 학습 → 피드백) 지원

---

## 2. 아키텍처 스타일
- **모놀리식 웹 애플리케이션**
- **서버 렌더링(SSR)** 기반
- 계층 구조:
  - Controller (요청/응답, 화면 바인딩)
  - Service (회원가입 등 비즈니스 로직)
  - Repository (JdbcTemplate SQL)
  - View (Thymeleaf 템플릿)

---

## 3. 기술 스택
- Backend: Java, Spring Boot
- Security: Spring Security (Form Login, OAuth2 구성 포함)
- Data Access: JdbcTemplate
- Database: MySQL
- Template/UI: Thymeleaf, HTML/CSS/JavaScript
- Build: Gradle

---

## 4. 핵심 도메인

### 4.1 사용자 도메인
- 일반 사용자: `users`
- 사업자 사용자: `business_users` (별도 테이블)
- 인증 시 일반/사업자 모두 로그인 가능

### 4.2 강의 도메인
- 강의: `courses`
- 목차/회차: `lectures`
- 수강신청: `enrollments`
- 수강평: `course_reviews`

### 4.3 커뮤니티 도메인
- 공지사항: `notices`
- 공지 댓글: `notice_comments`

### 4.4 학습/평가 도메인
- 과제: `assignments`, `submissions`
- 퀴즈: `quizzes`, `quiz_results`
- 학습 스케줄: `study_schedules`

---

## 5. 화면/기능 구조

### 5.1 공통 레이아웃
- 상단 메뉴 + 검색창
- 로그인 role에 따라 메뉴 분기
  - 일반: 학습하기
  - 사업자: 강의 관리

### 5.2 일반 사용자 흐름
1) 메인에서 강의 탐색
2) 수강신청 페이지 검색/필터
3) 강의 상세에서 수강신청
4) 수강신청내역에서 학습하기 진입
5) 성적/수강평/수강포기
6) 공지/댓글 이용

### 5.3 사업자 사용자 흐름
1) 사업자 가입/로그인
2) 강의 관리 메뉴 접근
3) 수강등록 페이지에서 강의/회차 등록

---

## 6. 주요 데이터 흐름

### 6.1 수강신청
- 입력: 사용자의 강의 신청
- 처리: `enrollments` insert, 중복 방지
- 후처리: `courses.purchase_count` 동기화

### 6.2 수강포기
- 입력: 학습 상세의 수강포기
- 처리: `enrollments` delete (본인 수강)
- 후처리: `purchase_count` 동기화

### 6.3 수강평
- 조회: 강의별 리뷰 목록 + 페이징
- 작성: 진도율 20% 이상에서만 허용
- 저장: `course_reviews`

### 6.4 공지 댓글
- 작성/수정/삭제 권한: 본인만
- 목록 페이징: 5개 단위

### 6.5 스케줄
- 요일/알람시간 설정 저장
- 요일별 표시 및 전체삭제 지원

---

## 7. 보안/권한
- Spring Security 기반 인증
- 비로그인 접근 허용 페이지와 인증 필요 페이지 분리
- 작성자 본인 검증 기능 적용(공지 댓글)
- role 기반 메뉴 분기(`isBusinessUser` 모델 속성)

---

## 8. 배포/실행 관점
- 단일 Spring Boot 프로세스 실행
- DB는 MySQL 연결
- 데이터 초기화 자동 실행 비활성(`spring.sql.init.mode=never`)
  - 재시작 시 사용자 작성 데이터 보존

---

## 9. 설계 결정 요약
1. ORM 대신 JdbcTemplate 사용: SQL 제어/학습 목적 최적화
2. 사업자 계정 별도 테이블 분리: 역할/데이터 분리 명확화
3. UI는 서버 렌더링 중심: 구현 복잡도 절감
4. 기능 확장은 Controller/Repository 단위 점진 확장 전략 사용

---

## 10. 향후 확장 포인트
- 시험 응시/채점 기능 완성
- 학습노트 저장 기능
- 사업자용 실제 관리 페이지(강의/수강생/출결/시험)
- 알림 연동(문자/푸시)
- 권한별 접근제어 고도화

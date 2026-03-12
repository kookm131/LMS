# LMS (Learning Management System)

Java + Spring Boot 기반의 LMS 웹 애플리케이션입니다.  
수강신청, 학습, 시험, 만족도, 커뮤니티, 챗봇, 관리자 대시보드 기능을 제공합니다.

---

## 1) 프로젝트 개요

본 프로젝트는 학습자/강사/관리자 역할을 지원하는 통합 LMS입니다.

- **학습자(STUDENT)**: 수강신청, 학습 진행, 시험 응시, 수강평/만족도 작성
- **강사(INSTRUCTOR)**: 강의 등록/수정, 시험/만족도 관리
- **관리자(ADMIN)**: 운영 통계 대시보드 확인

핵심 목표:
- 강의 탐색부터 학습 완료까지 하나의 흐름 제공
- 운영자가 서비스 상태를 숫자로 바로 확인할 수 있는 대시보드 제공
- OAuth(구글/네이버) 로그인 연동

---

## 2) 기술 스택

- **Backend**: Java 17, Spring Boot 4.x
- **Security**: Spring Security (Form Login + OAuth2 Login)
- **Template Engine**: Thymeleaf
- **DB**: MySQL 8.x
- **Data Access**: JdbcTemplate
- **Build Tool**: Gradle
- **Infra/Agent 연동**: OpenClaw Gateway (챗봇 질의 라우팅)

---

## 3) 프로젝트 구조

```text
src/
 ├─ main/
 │   ├─ java/com/example/LMS/
 │   │   ├─ home/              # 메인, 공지, QnA
 │   │   ├─ user/              # 회원가입/로그인/유저 저장소
 │   │   ├─ security/          # 보안 설정, UserDetails, OAuth upsert
 │   │   ├─ course/            # 강의/수강/시험/만족도/학습노트
 │   │   ├─ chatbot/           # 챗봇 API, 게이트웨이 연동
 │   │   └─ admin/             # 관리자 대시보드
 │   └─ resources/
 │       ├─ templates/         # thymeleaf 화면
 │       ├─ static/            # 정적 자원(css 등)
 │       ├─ application.yaml   # 실행 설정
 │       └─ schema.sql         # 스키마 정의
 └─ test/
```

---

## 4) 주요 기능

### 공통
- 회원가입/로그인(폼)
- OAuth 로그인(구글/네이버)
  - 로그인 시 `users` 테이블 기준 자동 등록/갱신(upsert)

### 수강/학습
- 강의 목록/검색/상세
- 수강 신청/취소
- 학습 목차 및 진도율
- 학습 노트/질문 관리
- 시험 응시/점수 반영
- 수강평/만족도 작성

### 커뮤니티
- 공지사항
- QnA 게시판/댓글(채택 포함)

### 챗봇
- 강의 페이지 내장형 챗봇 위젯
- `/chat/query` 기반 질의응답

### 관리자
- `/admin/dashboard` 통계 대시보드
  - KPI 카드 (가입자/수강신청/강의수/완료율/합격률)
  - 신규가입/수강신청 추이(일/월/연)
  - 카테고리 분포
  - 인기 강의 TOP10
  - 미채택 QnA, 저평점 강의 리스트

---

## 5) 데이터베이스 구조

주요 테이블(요약):

- `users` : 사용자 계정/권한(ADMIN, INSTRUCTOR, STUDENT)
- `business_users` : 강사 계정 보조 테이블
- `courses` : 강의 기본 정보
- `enrollments` : 수강 신청/진도
- `sections`, `lectures` : 강의 목차/강의 단위
- `exam_settings`, `exam_questions`, `exam_attempts` : 시험 설정/문항/응시 결과
- `quizzes`, `quiz_questions`, `quiz_choices`, `quiz_results` : 퀴즈
- `course_reviews` : 수강평
- `satisfaction_surveys` : 만족도
- `notices`, `notice_comments` : 공지/댓글
- `qna_questions`, `qna_comments` : QnA/댓글(채택)
- `study_notes`, `study_course_notes`, `study_questions`, `study_schedules` : 학습 보조 데이터

---

## 6) API 예시

### 인증/로그인
- `GET /login` : 로그인 페이지
- `GET /oauth2/authorization/google` : 구글 OAuth 시작
- `GET /oauth2/authorization/naver` : 네이버 OAuth 시작

### 수강/학습
- `POST /enrollments/{id}/apply` : 수강 신청
- `GET /study/courses/{courseId}` : 학습 페이지
- `POST /study/courses/{courseId}/exam/submit` : 시험 제출

### 챗봇
- `POST /chat/query`

요청 예시:
```json
{
  "question": "이 강의 핵심만 정리해줘",
  "userName": "홍길동",
  "ragContext": "강의 본문 텍스트"
}
```

응답 예시:
```json
{
  "answer": "핵심 내용은 ..."
}
```

### 관리자
- `GET /admin/dashboard?period=daily|monthly|yearly`

---

## 7) 실행 방법

### 1. 사전 준비
- Java 17+
- MySQL 8+
- DB 생성: `lms`

### 2. 설정 확인
`src/main/resources/application.yaml`에서 DB 접속 정보 확인

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3308/lms?...
    username: lms_user
    password: ********
```

### 3. OAuth 환경변수 설정 (권장)

```bash
export GOOGLE_CLIENT_ID="..."
export GOOGLE_CLIENT_SECRET="..."
export NAVER_CLIENT_ID="..."
export NAVER_CLIENT_SECRET="..."
```

영구 반영하려면 `~/.bashrc`에 추가 후:
```bash
source ~/.bashrc
```

### 4. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 5. 접속
- 앱: `http://localhost:8080`
- 관리자 대시보드: `http://localhost:8080/admin/dashboard`

---
1
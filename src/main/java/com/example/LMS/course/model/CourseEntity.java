package com.example.LMS.course.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * JPA 엔티티: 테이블 매핑 검증(validate) 용도
 *
 * - Repository는 JPA Repository를 사용하지 않고 JdbcTemplate만 사용합니다.
 * - 이 클래스는 앱 시작 시 courses 테이블 컬럼 구조가 올바른지 검증하는 역할입니다.
 */
@Entity
@Table(name = "courses")
public class CourseEntity {

    @Id
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "instructor_name", nullable = false, length = 100)
    private String instructorName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // JPA 기본 생성자
    protected CourseEntity() {
    }
}

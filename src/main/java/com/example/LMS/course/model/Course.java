package com.example.LMS.course.model;

import java.time.LocalDateTime;

/**
 * JdbcTemplate 조회 결과를 담는 도메인 모델
 */
public record Course(
        Long id,
        String title,
        String instructorName,
        String description,
        LocalDateTime createdAt
) {
}

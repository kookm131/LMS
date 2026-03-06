package com.example.LMS.course.model;

import java.time.LocalDateTime;

/**
 * 수강신청 목록 화면용 강의 모델
 */
public record CourseCatalogItem(
        Long id,
        String category,
        String title,
        String instructorName,
        String description,
        double rating,
        int purchaseCount,
        LocalDateTime createdAt
) {
}

package com.example.LMS.course.model;

import java.time.LocalDateTime;

public record CourseReviewItem(
        Long id,
        String writerName,
        double rating,
        String content,
        LocalDateTime createdAt
) {
}

package com.example.LMS.course.model;

import java.time.LocalDateTime;

public record CourseDetail(
        Long id,
        String category,
        String title,
        String content,
        String instructorName,
        int totalHours,
        double avgHours,
        double rating,
        LocalDateTime createdAt
) {
}

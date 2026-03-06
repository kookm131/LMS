package com.example.LMS.course.model;

public record CourseOutlineItem(
        Long lectureId,
        int orderNo,
        String title,
        int durationMin
) {
}

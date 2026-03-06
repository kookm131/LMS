package com.example.LMS.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 강의 등록 폼 데이터 객체
 */
public class CourseCreateForm {

    @NotBlank(message = "강의 제목은 필수입니다.")
    @Size(max = 200, message = "강의 제목은 200자 이하여야 합니다.")
    private String title;

    @NotBlank(message = "강사명은 필수입니다.")
    @Size(max = 100, message = "강사명은 100자 이하여야 합니다.")
    private String instructorName;

    @Size(max = 2000, message = "설명은 2000자 이하여야 합니다.")
    private String description;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getInstructorName() {
        return instructorName;
    }

    public void setInstructorName(String instructorName) {
        this.instructorName = instructorName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

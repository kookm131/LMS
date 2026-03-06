package com.example.LMS.course.service;

import com.example.LMS.course.model.*;
import com.example.LMS.course.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 비즈니스 로직 계층
 */
@Service
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    /**
     * 강의 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Course> getCourses() {
        return courseRepository.findAll();
    }

    /**
     * 강의 등록
     */
    @Transactional
    public void createCourse(String title, String instructorName, String description) {
        courseRepository.save(title, instructorName, description);
    }
}

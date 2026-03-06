package com.example.LMS.course.controller;

import com.example.LMS.course.dto.*;
import com.example.LMS.course.service.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 웹 요청을 처리하는 Controller
 *
 * 화면 흐름:
 * - GET  /courses      : 목록 + 등록 폼 화면
 * - POST /courses      : 등록 처리 후 목록으로 리다이렉트
 */
@Controller
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    // 사용처: 목록/상세 조회 기능 (GetMapping 기본 경로)
    @GetMapping
    public String courses(Model model) {
        // 폼 객체가 없다면 새로 생성
        if (!model.containsAttribute("courseCreateForm")) {
            model.addAttribute("courseCreateForm", new CourseCreateForm());
        }

        // 강의 목록 전달
        model.addAttribute("courses", courseService.getCourses());

        // templates/courses/list.html 렌더링
        return "courses/list";
    }

    // 사용처: 등록/처리 기능 (PostMapping 기본 경로)
    @PostMapping
    public String createCourse(
            @Valid @ModelAttribute CourseCreateForm courseCreateForm,
            BindingResult bindingResult,
            Model model
    ) {
        // 검증 오류가 있으면 같은 화면에 에러를 보여줌
        if (bindingResult.hasErrors()) {
            model.addAttribute("courses", courseService.getCourses());
            return "courses/list";
        }

        courseService.createCourse(
                courseCreateForm.getTitle(),
                courseCreateForm.getInstructorName(),
                courseCreateForm.getDescription()
        );

        // PRG(Post/Redirect/Get) 패턴 적용
        return "redirect:/courses";
    }
}

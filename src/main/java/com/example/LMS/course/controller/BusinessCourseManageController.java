package com.example.LMS.course.controller;

import com.example.LMS.course.repository.*;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/business/courses")
public class BusinessCourseManageController {

    private static final int PAGE_SIZE = 10;
    private static final int PAGE_GROUP_SIZE = 5;

    private final CourseRepository courseRepository;

    public BusinessCourseManageController(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    // 사용처: 목록/상세 조회 기능 (GetMapping 기본 경로)
    @GetMapping
    public String manageCourses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String category,
            Model model,
            Authentication authentication
    ) {
        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        boolean isBusinessUser = isAuthenticated && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_INSTRUCTOR".equals(a.getAuthority()));

        if (!isBusinessUser) {
            return "redirect:/enrollments";
        }

        String instructorUsername = authentication.getName();

        int currentPage = Math.max(page, 1);
        int offset = (currentPage - 1) * PAGE_SIZE;

        int totalCount = courseRepository.countPagedByInstructor(instructorUsername, category);
        int totalPages = (int) Math.ceil(totalCount / (double) PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
        if (currentPage > totalPages) {
            currentPage = totalPages;
            offset = (currentPage - 1) * PAGE_SIZE;
        }

        var courses = courseRepository.findPagedByInstructor(instructorUsername, PAGE_SIZE, offset, category);
        var categories = courseRepository.findCategoriesByInstructor(instructorUsername);

        int groupIndex = (currentPage - 1) / PAGE_GROUP_SIZE;
        int startPage = groupIndex * PAGE_GROUP_SIZE + 1;
        int endPage = Math.min(startPage + PAGE_GROUP_SIZE - 1, totalPages);

        model.addAttribute("isAuthenticated", true);
        model.addAttribute("courses", courses);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategory", category == null ? "" : category);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("hasPrevGroup", startPage > 1);
        model.addAttribute("hasNextGroup", endPage < totalPages);
        model.addAttribute("prevGroupPage", startPage - 1);
        model.addAttribute("nextGroupPage", endPage + 1);

        return "courses/business-manage";
    }
}

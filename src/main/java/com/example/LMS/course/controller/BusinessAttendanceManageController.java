package com.example.LMS.course.controller;

import com.example.LMS.course.repository.CourseRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/business/attendance")
public class BusinessAttendanceManageController {

    private final CourseRepository courseRepository;

    public BusinessAttendanceManageController(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @GetMapping
    public String attendanceManage(@RequestParam(required = false) Long courseId,
                                   Authentication authentication,
                                   Model model) {
        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        boolean isBusinessUser = isAuthenticated && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_INSTRUCTOR".equals(a.getAuthority()));

        if (!isBusinessUser) {
            return "redirect:/enrollments";
        }

        String instructorUsername = authentication.getName();
        var myCourses = courseRepository.findAllByInstructor(instructorUsername);

        model.addAttribute("isAuthenticated", true);
        model.addAttribute("myCourses", myCourses);
        model.addAttribute("selectedCourseId", courseId);

        return "courses/business-attendance-manage";
    }
}

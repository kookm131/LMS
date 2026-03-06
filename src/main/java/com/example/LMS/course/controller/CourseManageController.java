package com.example.LMS.course.controller;

import com.example.LMS.course.repository.*;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/courses/register")
public class CourseManageController {

    private final CourseManageRepository courseManageRepository;

    public CourseManageController(CourseManageRepository courseManageRepository) {
        this.courseManageRepository = courseManageRepository;
    }

    // 사용처: 목록/상세 조회 기능 (GetMapping 기본 경로)
    @GetMapping
    public String registerPage(@RequestParam(required = false) Long courseId, Model model, Authentication authentication) {
        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        boolean isBusinessUser = isAuthenticated && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_INSTRUCTOR".equals(a.getAuthority()));

        if (!isBusinessUser) {
            return "redirect:/enrollments";
        }

        model.addAttribute("isAuthenticated", isAuthenticated);

        if (courseId != null) {
            var course = courseManageRepository.findOwnedCourseForEdit(courseId, authentication.getName());
            if (course.isPresent()) {
                model.addAttribute("editMode", true);
                model.addAttribute("editCourse", course.get());
                model.addAttribute("outline", courseManageRepository.findOutline(courseId));
            } else {
                model.addAttribute("editMode", false);
                model.addAttribute("outline", java.util.List.of());
            }
        } else {
            model.addAttribute("editMode", false);
            model.addAttribute("outline", java.util.List.of());
        }

        return "courses/register";
    }

    // 사용처: 등록/처리 기능 (PostMapping 기본 경로)
    @PostMapping
    public String registerSubmit(
            @RequestParam(required = false) Long courseId,
            @RequestParam String title,
            @RequestParam(required = false) String category,
            @RequestParam String instructorName,
            @RequestParam String description,
            @RequestParam String contentText,
            @RequestParam(value = "outlineTitle", required = false) List<String> outlineTitles,
            @RequestParam(value = "outlineMinutes", required = false) List<Integer> outlineMinutes,
            Authentication authentication
    ) {
        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        boolean isBusinessUser = isAuthenticated && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_INSTRUCTOR".equals(a.getAuthority()));

        if (!isBusinessUser) {
            return "redirect:/login";
        }

        if (title == null || title.isBlank() || instructorName == null || instructorName.isBlank()) {
            return "redirect:/courses/register";
        }

        String creatorUsername = authentication.getName();

        if (courseId != null && courseManageRepository.isOwnedBy(courseId, creatorUsername)) {
            courseManageRepository.updateCourseByOwner(courseId, creatorUsername, category, title.trim(), instructorName.trim(), description, contentText);
            courseManageRepository.replaceLectures(courseId, outlineTitles, outlineMinutes);
            return "redirect:/courses/" + courseId;
        }

        Long newCourseId = courseManageRepository.saveCourse(category, title.trim(), instructorName.trim(), description, contentText, creatorUsername);
        courseManageRepository.saveLectures(newCourseId, outlineTitles, outlineMinutes);

        return "redirect:/courses/" + newCourseId;
    }
}

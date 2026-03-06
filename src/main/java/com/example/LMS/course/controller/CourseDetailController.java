package com.example.LMS.course.controller;

import com.example.LMS.course.model.*;
import com.example.LMS.course.repository.*;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class CourseDetailController {

    private static final int REVIEW_PAGE_SIZE = 5;
    private static final int REVIEW_PAGE_GROUP = 5;

    private final CourseRepository courseRepository;
    private final CourseManageRepository courseManageRepository;

    public CourseDetailController(CourseRepository courseRepository, CourseManageRepository courseManageRepository) {
        this.courseRepository = courseRepository;
        this.courseManageRepository = courseManageRepository;
    }

    // 사용처: 목록/상세 조회 기능 (GetMapping /courses/{courseId})
    @GetMapping("/courses/{courseId}")
    public String detail(@PathVariable Long courseId,
                         @RequestParam(defaultValue = "1") int reviewPage,
                         @RequestParam(defaultValue = "false") boolean alreadyEnrolled,
                         @RequestParam(defaultValue = "false") boolean notOwnerDelete,
                         Model model,
                         Authentication authentication) {

        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        CourseDetail detail = courseRepository.findDetail(courseId).orElse(null);
        if (detail == null) {
            return "redirect:/enrollments";
        }

        int currentPage = Math.max(1, reviewPage);
        int offset = (currentPage - 1) * REVIEW_PAGE_SIZE;

        int totalReviewCount = courseRepository.countReviews(courseId);
        int totalReviewPages = (int) Math.ceil(totalReviewCount / (double) REVIEW_PAGE_SIZE);
        if (totalReviewPages == 0) totalReviewPages = 1;
        if (currentPage > totalReviewPages) {
            currentPage = totalReviewPages;
            offset = (currentPage - 1) * REVIEW_PAGE_SIZE;
        }

        int groupIndex = (currentPage - 1) / REVIEW_PAGE_GROUP;
        int startPage = groupIndex * REVIEW_PAGE_GROUP + 1;
        int endPage = Math.min(startPage + REVIEW_PAGE_GROUP - 1, totalReviewPages);

        List<CourseOutlineItem> outline = courseRepository.findOutline(courseId);
        if (outline.isEmpty()) {
            outline = List.of(
                    new CourseOutlineItem(-1L, 1, "OT 및 강의 소개", 20),
                    new CourseOutlineItem(-2L, 2, "핵심 개념 이해", 45),
                    new CourseOutlineItem(-3L, 3, "실습 프로젝트", 60),
                    new CourseOutlineItem(-4L, 4, "고급 활용", 40)
            );
        }

        List<CourseReviewItem> reviews = courseRepository.findReviews(courseId, REVIEW_PAGE_SIZE, offset);
        List<CourseCatalogItem> relatedCourses = courseRepository.findRelated(detail.category(), detail.id(), 3);

        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("course", detail);
        model.addAttribute("outline", outline);
        model.addAttribute("reviews", reviews);
        model.addAttribute("relatedCourses", relatedCourses);

        model.addAttribute("reviewCurrentPage", currentPage);
        model.addAttribute("reviewStartPage", startPage);
        model.addAttribute("reviewEndPage", endPage);
        model.addAttribute("reviewHasPrev", startPage > 1);
        model.addAttribute("reviewHasNext", endPage < totalReviewPages);
        model.addAttribute("reviewPrevPage", startPage - 1);
        model.addAttribute("reviewNextPage", endPage + 1);
        model.addAttribute("reviewCount", totalReviewCount);
        model.addAttribute("alreadyEnrolled", alreadyEnrolled);
        model.addAttribute("notOwnerDelete", notOwnerDelete);

        boolean isBusinessUser = isAuthenticated && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_INSTRUCTOR".equals(a.getAuthority()));
        boolean canDeleteCourse = isBusinessUser && courseManageRepository.isOwnedBy(courseId, authentication.getName());
        model.addAttribute("canDeleteCourse", canDeleteCourse);
        model.addAttribute("chatUserName", isAuthenticated ? authentication.getName() : "guest");

        return "courses/detail";
    }

    // 사용처: 삭제 기능 (PostMapping /courses/{courseId}/delete)
    @PostMapping("/courses/{courseId}/delete")
    public String deleteCourse(@PathVariable Long courseId, Authentication authentication) {
        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        if (!isAuthenticated) {
            return "redirect:/login";
        }

        boolean isBusinessUser = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_INSTRUCTOR".equals(a.getAuthority()));

        if (!isBusinessUser) {
            return "redirect:/courses/" + courseId + "?notOwnerDelete=true";
        }

        String username = authentication.getName();
        if (!courseManageRepository.isOwnedBy(courseId, username)) {
            return "redirect:/courses/" + courseId + "?notOwnerDelete=true";
        }

        int deleted = courseManageRepository.deleteCourseByOwner(courseId, username);
        if (deleted > 0) {
            return "redirect:/business/courses?deleted=true";
        }

        return "redirect:/courses/" + courseId + "?notOwnerDelete=true";
    }
}

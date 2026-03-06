package com.example.LMS.course.controller;

import com.example.LMS.course.model.*;
import com.example.LMS.course.repository.*;
import com.example.LMS.user.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 수강신청 목록 화면 Controller
 *
 * 왜 필요한가?
 * - 카테고리/검색/페이징이 동시에 동작하는 목록 화면의 파라미터 조합을 관리한다.
 * - Repository는 데이터 조회만, Controller는 화면 상태 계산(currentPage/startPage 등)을 담당한다.
 */
@Controller
@RequestMapping("/enrollments")
public class EnrollmentController {

    /** 한 페이지당 강의 개수 */
    private static final int PAGE_SIZE = 20;
    /** 페이지 네비게이션 묶음 크기 (앞 1 2 3 4 5 뒤) */
    private static final int PAGE_GROUP_SIZE = 5;

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseManageRepository courseManageRepository;

    public EnrollmentController(CourseRepository courseRepository,
                                EnrollmentRepository enrollmentRepository,
                                UserRepository userRepository,
                                CourseManageRepository courseManageRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.courseManageRepository = courseManageRepository;
    }

    /**
     * 수강신청 목록 조회
     *
     * 기능:
     * - category/keyword 필터 적용
     * - 페이징 계산 및 범위 보정
     * - 로그인 상태 전달 (layout 공통 UI 제어)
     */
    // 사용처: 목록/상세 조회 기능 (GetMapping 기본 경로)
    @GetMapping
    public String list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            Model model,
            Authentication authentication
    ) {
        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        // 잘못된 page 값 방어
        int currentPage = Math.max(page, 1);
        int offset = (currentPage - 1) * PAGE_SIZE;

        // 조건(category/keyword)에 맞는 전체 개수 계산
        int totalCount = courseRepository.countPaged(category, keyword);
        int totalPages = (int) Math.ceil(totalCount / (double) PAGE_SIZE);

        // 데이터가 0건이어도 View 계산 단순화를 위해 최소 1페이지로 고정
        if (totalPages == 0) totalPages = 1;

        // 범위를 넘어간 페이지 요청 보정
        if (currentPage > totalPages) {
            currentPage = totalPages;
            offset = (currentPage - 1) * PAGE_SIZE;
        }

        List<CourseCatalogItem> courses = courseRepository.findPaged(PAGE_SIZE, offset, category, keyword);
        List<String> categories = courseRepository.findCategories();

        // 페이지 네비게이션 그룹 계산 (예: 1~5, 6~10)
        int groupIndex = (currentPage - 1) / PAGE_GROUP_SIZE;
        int startPage = groupIndex * PAGE_GROUP_SIZE + 1;
        int endPage = Math.min(startPage + PAGE_GROUP_SIZE - 1, totalPages);

        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategory", category == null ? "" : category);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("courses", courses);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("hasPrevGroup", startPage > 1);
        model.addAttribute("hasNextGroup", endPage < totalPages);
        model.addAttribute("prevGroupPage", startPage - 1);
        model.addAttribute("nextGroupPage", endPage + 1);
        model.addAttribute("hasKeyword", keyword != null && !keyword.isBlank());
        model.addAttribute("isAdmin", isAdmin(authentication));

        return "courses/enrollment-list";
    }


    // 사용처: 삭제 기능 (PostMapping /{courseId}/admin-delete)
    @PostMapping("/{courseId}/admin-delete")
    public String adminDelete(@PathVariable Long courseId, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return "redirect:/enrollments";
        }
        courseManageRepository.deleteCourseByAdmin(courseId);
        return "redirect:/enrollments";
    }

    private boolean isAdmin(Authentication authentication) {
        boolean ok = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        if (!ok) return false;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
    /**
     * 강의 수강신청 처리
     * - 로그인 사용자만 가능
     * - 이미 신청된 경우 UNIQUE KEY로 중복 저장 방지
     */
    // 사용처: 수강신청 기능 (PostMapping /{courseId}/apply)
    @PostMapping("/{courseId}/apply")
    public String apply(@PathVariable Long courseId, Authentication authentication) {
        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        if (!isAuthenticated) {
            return "redirect:/login";
        }

        Long userId = userRepository.findIdByUsername(authentication.getName()).orElse(null);
        if (userId == null) {
            return "redirect:/login";
        }

        if (enrollmentRepository.existsEnrollment(courseId, userId)) {
            return "redirect:/courses/" + courseId + "?alreadyEnrolled=1";
        }

        enrollmentRepository.apply(courseId, userId);
        return "redirect:/study/enrollments";
    }
}

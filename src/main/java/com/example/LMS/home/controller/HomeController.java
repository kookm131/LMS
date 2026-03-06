package com.example.LMS.home.controller;

import com.example.LMS.course.model.Course;
import com.example.LMS.course.repository.CourseRepository;
import com.example.LMS.home.repository.HomeBoardRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * 메인 페이지 Controller
 *
 * 왜 필요한가?
 * - 메인 화면은 추천강의/공지/수강평 등 여러 도메인의 데이터를 한 번에 보여준다.
 * - 화면 조합 책임을 Controller에서 수행하면 View(HTML)는 표시 역할에 집중할 수 있다.
 */
@Controller
public class HomeController {

    private final CourseRepository courseRepository;
    private final HomeBoardRepository homeBoardRepository;

    public HomeController(CourseRepository courseRepository, HomeBoardRepository homeBoardRepository) {
        this.courseRepository = courseRepository;
        this.homeBoardRepository = homeBoardRepository;
    }

    /**
     * 메인 화면 진입
     *
     * 기능 요약:
     * 1) 로그인 여부 계산(상단 메뉴 분기용)
     * 2) 추천강의 조회
     * 3) 공지 고정 2개 + 일반 공지 5개 조회
     * 4) 최신 수강평 5개 조회
     */
    // 사용처: 목록/상세 조회 기능 (GetMapping /)
    @GetMapping("/")
    public String home(Model model, Authentication authentication) {
        // Spring Security의 익명 사용자 토큰까지 제외해 실제 로그인 상태를 판별
        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        // 메인 화면 카드 데이터 조회
        List<Course> recommendedCourses = courseRepository.findRecommended(10);
        List<HomeBoardRepository.NoticeItem> pinnedNotices = homeBoardRepository.findPinnedNotices();
        List<HomeBoardRepository.NoticeItem> notices = homeBoardRepository.findLatestNoticesExceptPinned(5);
        List<HomeBoardRepository.ReviewItem> reviews = homeBoardRepository.findLatestReviews(5);

        // 화면 렌더링에 필요한 데이터 바인딩
        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("recommendedCourses", recommendedCourses);
        model.addAttribute("pinnedNotices", pinnedNotices);
        model.addAttribute("notices", notices);
        model.addAttribute("reviews", reviews);

        return "home/index";
    }
}

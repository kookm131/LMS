package com.example.LMS.course.controller;

import com.example.LMS.course.repository.*;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/business/satisfaction")
public class BusinessSatisfactionManageController {

    private static final int PAGE_SIZE = 10;
    private static final int PAGE_GROUP_SIZE = 5;

    private final CourseRepository courseRepository;
    private final SatisfactionSurveyRepository satisfactionSurveyRepository;

    public BusinessSatisfactionManageController(CourseRepository courseRepository,
                                                SatisfactionSurveyRepository satisfactionSurveyRepository) {
        this.courseRepository = courseRepository;
        this.satisfactionSurveyRepository = satisfactionSurveyRepository;
    }

    // 사용처: 목록/상세 조회 기능 (GetMapping 기본 경로)
    @GetMapping
    public String list(@RequestParam(defaultValue = "1") int page,
                       Model model,
                       Authentication authentication) {

        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        boolean isBusinessUser = isAuthenticated && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_INSTRUCTOR".equals(a.getAuthority()));

        if (!isBusinessUser) {
            return "redirect:/enrollments";
        }

        String owner = authentication.getName();
        int currentPage = Math.max(page, 1);
        int offset = (currentPage - 1) * PAGE_SIZE;

        int totalCount = courseRepository.countBusinessSatisfaction(owner);
        int totalPages = (int) Math.ceil(totalCount / (double) PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
        if (currentPage > totalPages) {
            currentPage = totalPages;
            offset = (currentPage - 1) * PAGE_SIZE;
        }

        var items = courseRepository.findBusinessSatisfactionPaged(owner, PAGE_SIZE, offset);

        int groupIndex = (currentPage - 1) / PAGE_GROUP_SIZE;
        int startPage = groupIndex * PAGE_GROUP_SIZE + 1;
        int endPage = Math.min(startPage + PAGE_GROUP_SIZE - 1, totalPages);

        model.addAttribute("isAuthenticated", true);
        model.addAttribute("items", items);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("hasPrevGroup", startPage > 1);
        model.addAttribute("hasNextGroup", endPage < totalPages);
        model.addAttribute("prevGroupPage", startPage - 1);
        model.addAttribute("nextGroupPage", endPage + 1);

        return "courses/business-satisfaction";
    }

    // 사용처: 목록/상세 조회 기능 (GetMapping /{courseId}/survey/{surveyId})
    @GetMapping("/{courseId}/survey/{surveyId}")
    public String surveyReadonly(@PathVariable Long courseId,
                                 @PathVariable Long surveyId,
                                 Model model,
                                 Authentication authentication) {
        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        boolean isBusinessUser = isAuthenticated && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_INSTRUCTOR".equals(a.getAuthority()));
        if (!isBusinessUser) return "redirect:/enrollments";

        String owner = authentication.getName();
        var summary = satisfactionSurveyRepository.findCourseSummaryForOwner(courseId, owner);
        if (summary.isEmpty()) return "redirect:/business/satisfaction";

        var survey = satisfactionSurveyRepository.findCourseSurveyById(courseId, surveyId);
        if (survey.isEmpty()) return "redirect:/business/satisfaction/" + courseId;

        model.addAttribute("isAuthenticated", true);
        model.addAttribute("summary", summary.get());
        model.addAttribute("survey", survey.get());
        model.addAttribute("courseId", courseId);
        return "courses/business-satisfaction-survey-readonly";
    }

    // 사용처: 목록/상세 조회 기능 (GetMapping /{courseId})
    @GetMapping("/{courseId}")
    public String detail(@PathVariable Long courseId,
                         @RequestParam(defaultValue = "1") int page,
                         Model model,
                         Authentication authentication) {
        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        boolean isBusinessUser = isAuthenticated && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_INSTRUCTOR".equals(a.getAuthority()));

        if (!isBusinessUser) {
            return "redirect:/enrollments";
        }

        String owner = authentication.getName();
        var summary = satisfactionSurveyRepository.findCourseSummaryForOwner(courseId, owner);
        if (summary.isEmpty()) {
            return "redirect:/business/satisfaction";
        }

        int pageSize = 5;
        int currentPage = Math.max(page, 1);
        int offset = (currentPage - 1) * pageSize;

        int totalCount = satisfactionSurveyRepository.countCourseSurveys(courseId);
        int totalPages = (int) Math.ceil(totalCount / (double) pageSize);
        if (totalPages == 0) totalPages = 1;
        if (currentPage > totalPages) {
            currentPage = totalPages;
            offset = (currentPage - 1) * pageSize;
        }

        int groupIndex = (currentPage - 1) / PAGE_GROUP_SIZE;
        int startPage = groupIndex * PAGE_GROUP_SIZE + 1;
        int endPage = Math.min(startPage + PAGE_GROUP_SIZE - 1, totalPages);

        var surveys = satisfactionSurveyRepository.findCourseSurveysPaged(courseId, pageSize, offset);

        model.addAttribute("isAuthenticated", true);
        model.addAttribute("summary", summary.get());
        model.addAttribute("surveys", surveys);
        model.addAttribute("courseId", courseId);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("hasPrevGroup", startPage > 1);
        model.addAttribute("hasNextGroup", endPage < totalPages);
        model.addAttribute("prevGroupPage", startPage - 1);
        model.addAttribute("nextGroupPage", endPage + 1);

        return "courses/business-satisfaction-detail";
    }
}

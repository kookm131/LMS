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
import org.springframework.web.bind.annotation.PostMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/business/exams")
public class BusinessExamManageController {

    private static final int PAGE_SIZE = 10;
    private static final int PAGE_GROUP_SIZE = 5;

    private final CourseRepository courseRepository;
    private final ExamSettingRepository examSettingRepository;
    private final ExamQuestionRepository examQuestionRepository;

    public BusinessExamManageController(CourseRepository courseRepository, ExamSettingRepository examSettingRepository, ExamQuestionRepository examQuestionRepository) {
        this.courseRepository = courseRepository;
        this.examSettingRepository = examSettingRepository;
        this.examQuestionRepository = examQuestionRepository;
    }

    // 사용처: 목록/상세 조회 기능 (GetMapping 기본 경로)
    @GetMapping
    public String manageExams(
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
        java.util.Map<Long, Integer> questionCountMap = new java.util.HashMap<>();
        for (var c : courses) {
            questionCountMap.put(c.id(), examQuestionRepository.countByCourse(c.id()));
        }

        int groupIndex = (currentPage - 1) / PAGE_GROUP_SIZE;
        int startPage = groupIndex * PAGE_GROUP_SIZE + 1;
        int endPage = Math.min(startPage + PAGE_GROUP_SIZE - 1, totalPages);

        model.addAttribute("isAuthenticated", true);
        model.addAttribute("courses", courses);
        model.addAttribute("questionCountMap", questionCountMap);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategory", category == null ? "" : category);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("hasPrevGroup", startPage > 1);
        model.addAttribute("hasNextGroup", endPage < totalPages);
        model.addAttribute("prevGroupPage", startPage - 1);
        model.addAttribute("nextGroupPage", endPage + 1);

        return "courses/business-exam-manage";
    }

    // 사용처: 목록/상세 조회 기능 (GetMapping /{courseId})
    @GetMapping("/{courseId}")
    public String examDetail(
            @PathVariable Long courseId,
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
        var course = courseRepository.findInstructorCourseById(instructorUsername, courseId);
        if (course.isEmpty()) {
            return "redirect:/business/exams";
        }

        var outline = courseRepository.findOutline(courseId);

        model.addAttribute("isAuthenticated", true);
        model.addAttribute("course", course.get());
        model.addAttribute("courseId", courseId);
        model.addAttribute("outline", outline);
        model.addAttribute("questionCount", examQuestionRepository.countByCourse(courseId));
        model.addAttribute("passScore", examSettingRepository.getPassScoreOrDefault(courseId));

        return "courses/business-exam-detail";
    }



    // 사용처: 문제추가/수정 화면 기능 (GetMapping /{courseId}/questions/edit)
    @GetMapping("/{courseId}/questions/edit")
    public String editQuestions(
            @PathVariable Long courseId,
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
        var course = courseRepository.findInstructorCourseById(instructorUsername, courseId);
        if (course.isEmpty()) {
            return "redirect:/business/exams";
        }

        var questions = examQuestionRepository.findByCourse(courseId);

        model.addAttribute("isAuthenticated", true);
        model.addAttribute("course", course.get());
        model.addAttribute("courseId", courseId);
        model.addAttribute("questions", questions);

        return "courses/business-exam-question-edit";
    }

    // 사용처: 문제추가/수정 저장 기능 (PostMapping /{courseId}/questions/edit)
    @PostMapping("/{courseId}/questions/edit")
    public String saveQuestions(
            @PathVariable Long courseId,
            @RequestParam(name = "questionText", required = false) List<String> questionTextList,
            @RequestParam(name = "referenceImageUrl", required = false) List<String> referenceImageUrlList,
            @RequestParam(name = "option1", required = false) List<String> option1List,
            @RequestParam(name = "option2", required = false) List<String> option2List,
            @RequestParam(name = "option3", required = false) List<String> option3List,
            @RequestParam(name = "option4", required = false) List<String> option4List,
            @RequestParam(name = "correctOption", required = false) List<String> correctOptionList,
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
        var course = courseRepository.findInstructorCourseById(instructorUsername, courseId);
        if (course.isEmpty()) {
            return "redirect:/business/exams";
        }

        List<String> q = questionTextList == null ? List.of() : questionTextList;
        List<String> r = referenceImageUrlList == null ? List.of() : referenceImageUrlList;
        List<String> o1 = option1List == null ? List.of() : option1List;
        List<String> o2 = option2List == null ? List.of() : option2List;
        List<String> o3 = option3List == null ? List.of() : option3List;
        List<String> o4 = option4List == null ? List.of() : option4List;
        List<String> ca = correctOptionList == null ? List.of() : correctOptionList;

        int n = q.size();

        List<ExamQuestionRepository.ExamQuestionDraft> drafts = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String questionText = at(q, i);
            String ref = at(r, i);
            String a1 = at(o1, i);
            String a2 = at(o2, i);
            String a3 = at(o3, i);
            String a4 = at(o4, i);
            int correct = 0;
            try {
                correct = Integer.parseInt(at(ca, i));
            } catch (Exception ignored) {}

            if (questionText.isBlank() || a1.isBlank() || a2.isBlank() || a3.isBlank() || a4.isBlank()) {
                return "redirect:/business/exams/" + courseId + "/questions/edit?error=required";
            }
            if (correct < 1 || correct > 4) {
                return "redirect:/business/exams/" + courseId + "/questions/edit?error=answer";
            }

            drafts.add(new ExamQuestionRepository.ExamQuestionDraft(
                    questionText,
                    ref,
                    a1,
                    a2,
                    a3,
                    a4,
                    correct
            ));
        }

        if (drafts.isEmpty()) {
            return "redirect:/business/exams/" + courseId + "/questions/edit?error=required";
        }

        examQuestionRepository.replaceAll(courseId, drafts);
        return "redirect:/business/exams/" + courseId;
    }


    private String at(List<String> list, int idx) {
        if (list == null || idx < 0 || idx >= list.size() || list.get(idx) == null) return "";
        return list.get(idx).trim();
    }

    // 사용처: 합격점수 변경 기능 (PostMapping /{courseId}/pass-score)
    @PostMapping("/{courseId}/pass-score")
    public String updatePassScore(
            @PathVariable Long courseId,
            @RequestParam int passScore,
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

        if (passScore < 0 || passScore > 100 || passScore % 5 != 0) {
            return "redirect:/business/exams/" + courseId;
        }

        String instructorUsername = authentication.getName();
        var course = courseRepository.findInstructorCourseById(instructorUsername, courseId);
        if (course.isEmpty()) {
            return "redirect:/business/exams";
        }

        examSettingRepository.setPassScore(courseId, passScore);
        return "redirect:/business/exams/" + courseId;
    }

}

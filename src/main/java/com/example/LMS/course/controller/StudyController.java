package com.example.LMS.course.controller;

import com.example.LMS.course.repository.*;
import com.example.LMS.user.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/study")
public class StudyController {

    private static final int PAGE_SIZE = 10;
    private static final int PAGE_GROUP_SIZE = 5;
    private static final int REVIEW_PAGE_SIZE = 5;
    private static final int REVIEW_PAGE_GROUP_SIZE = 5;
    private static final int NOTE_PAGE_SIZE = 10;

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final StudyScheduleRepository studyScheduleRepository;
    private final UserRepository userRepository;
    private final SatisfactionSurveyRepository satisfactionSurveyRepository;
    private final StudyNoteRepository studyNoteRepository;
    private final StudyCourseNoteRepository studyCourseNoteRepository;
    private final StudyQuestionRepository studyQuestionRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final StudyAttendanceRepository studyAttendanceRepository;

    public StudyController(EnrollmentRepository enrollmentRepository,
                           CourseRepository courseRepository,
                           StudyScheduleRepository studyScheduleRepository,
                           UserRepository userRepository,
                           SatisfactionSurveyRepository satisfactionSurveyRepository,
                           StudyNoteRepository studyNoteRepository,
                           StudyCourseNoteRepository studyCourseNoteRepository,
                           StudyQuestionRepository studyQuestionRepository,
                           ExamQuestionRepository examQuestionRepository,
                           ExamAttemptRepository examAttemptRepository,
                           StudyAttendanceRepository studyAttendanceRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.studyScheduleRepository = studyScheduleRepository;
        this.userRepository = userRepository;
        this.satisfactionSurveyRepository = satisfactionSurveyRepository;
        this.studyNoteRepository = studyNoteRepository;
        this.studyCourseNoteRepository = studyCourseNoteRepository;
        this.studyQuestionRepository = studyQuestionRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.studyAttendanceRepository = studyAttendanceRepository;
    }

    // 사용처: 목록/상세 조회 기능 (GetMapping /enrollments)
    @GetMapping("/enrollments")
    public String enrollmentHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "incomplete") String completion,
            Model model,
            Authentication authentication
    ) {
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

        int currentPage = Math.max(page, 1);
        int offset = (currentPage - 1) * PAGE_SIZE;

        int totalCount = enrollmentRepository.countPaged(userId, category, completion);
        int totalPages = (int) Math.ceil(totalCount / (double) PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
        if (currentPage > totalPages) {
            currentPage = totalPages;
            offset = (currentPage - 1) * PAGE_SIZE;
        }

        List<EnrollmentRepository.EnrollmentHistoryItem> enrollments =
                enrollmentRepository.findPaged(userId, PAGE_SIZE, offset, category, completion);
        List<String> categories = enrollmentRepository.findCategories(userId);

        int groupIndex = (currentPage - 1) / PAGE_GROUP_SIZE;
        int startPage = groupIndex * PAGE_GROUP_SIZE + 1;
        int endPage = Math.min(startPage + PAGE_GROUP_SIZE - 1, totalPages);

        model.addAttribute("isAuthenticated", true);
        model.addAttribute("enrollments", enrollments);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategory", category == null ? "" : category);
        model.addAttribute("completion", completion);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("hasPrevGroup", startPage > 1);
        model.addAttribute("hasNextGroup", endPage < totalPages);
        model.addAttribute("prevGroupPage", startPage - 1);
        model.addAttribute("nextGroupPage", endPage + 1);

        return "study/enrollment-history";
    }

    /*
    마이페이지 스케줄표
     */

    // 사용처: 학습 일정 조회 기능 (GetMapping /schedule)
    @GetMapping("/schedule")
    public String studySchedule(Model model, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return "redirect:/login";

        var schedules = studyScheduleRepository.findByUser(userId);
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("schedules", schedules);
        return "study/schedule";
    }

    // 사용처: 수정 화면 진입 기능 (GetMapping /schedule/edit)
    @GetMapping("/schedule/edit")
    public String editSchedule(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String category,
            Model model,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return "redirect:/login";

        int currentPage = Math.max(page, 1);
        int offset = (currentPage - 1) * PAGE_SIZE;

        int totalCount = enrollmentRepository.countPaged(userId, category, "inprogress");
        int totalPages = (int) Math.ceil(totalCount / (double) PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
        if (currentPage > totalPages) {
            currentPage = totalPages;
            offset = (currentPage - 1) * PAGE_SIZE;
        }

        var enrollments = enrollmentRepository.findPaged(userId, PAGE_SIZE, offset, category, "inprogress");
        var categories = enrollmentRepository.findCategories(userId);
        var schedules = studyScheduleRepository.findByUser(userId);
        java.util.Map<Long, StudyScheduleRepository.ScheduleItem> scheduleMap = new java.util.HashMap<>();
        for (var sc : schedules) {
            scheduleMap.put(sc.courseId(), sc);
        }

        int groupIndex = (currentPage - 1) / PAGE_GROUP_SIZE;
        int startPage = groupIndex * PAGE_GROUP_SIZE + 1;
        int endPage = Math.min(startPage + PAGE_GROUP_SIZE - 1, totalPages);

        model.addAttribute("isAuthenticated", true);
        model.addAttribute("enrollments", enrollments);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategory", category == null ? "" : category);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("hasPrevGroup", startPage > 1);
        model.addAttribute("hasNextGroup", endPage < totalPages);
        model.addAttribute("prevGroupPage", startPage - 1);
        model.addAttribute("nextGroupPage", endPage + 1);
        java.util.List<String> alarmTimes = new java.util.ArrayList<>();
        for (int h = 0; h <= 24; h++) {
            alarmTimes.add(String.format("%02d:00", h));
        }

        model.addAttribute("schedules", schedules);
        model.addAttribute("scheduleMap", scheduleMap);
        model.addAttribute("alarmTimes", alarmTimes);

        return "study/schedule-edit";
    }

    // 사용처: 학습 일정 저장 기능 (PostMapping /schedule/save)
    @PostMapping("/schedule/save")
    public String saveSchedule(
            @RequestParam(value = "courseId", required = false) List<Long> courseIds,
            @RequestParam(value = "dayOfWeek", required = false) List<String> days,
            @RequestParam(value = "alarmTime", required = false) List<String> times,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return "redirect:/login";

        if (courseIds == null || days == null || times == null) {
            return "redirect:/study/schedule";
        }

        studyScheduleRepository.clearAll(userId);

        int size = Math.min(courseIds.size(), Math.min(days.size(), times.size()));
        for (int i = 0; i < size; i++) {
            String day = days.get(i);
            String time = times.get(i);
            if (day == null || day.isBlank() || "선택안함".equals(day)) continue;
            if (time == null || time.isBlank()) continue;
            studyScheduleRepository.save(userId, courseIds.get(i), day, time);
        }

        return "redirect:/study/schedule";
    }

    // 사용처: 학습 일정 초기화 기능 (PostMapping /schedule/clear)
    @PostMapping("/schedule/clear")
    public String clearSchedule(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return "redirect:/login";
        studyScheduleRepository.clearAll(userId);
        return "redirect:/study/schedule";
    }


    // 사용처: 노트 조회 기능 (GetMapping /notes)
    @GetMapping("/notes")
    public String noteCourseList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "incomplete") String completion,
            Model model,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return "redirect:/login";

        int currentPage = Math.max(page, 1);
        int offset = (currentPage - 1) * NOTE_PAGE_SIZE;

        int totalCount = enrollmentRepository.countPaged(userId, category, completion);
        int totalPages = (int) Math.ceil(totalCount / (double) NOTE_PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
        if (currentPage > totalPages) {
            currentPage = totalPages;
            offset = (currentPage - 1) * NOTE_PAGE_SIZE;
        }

        var enrollments = enrollmentRepository.findPaged(userId, NOTE_PAGE_SIZE, offset, category, completion);
        var categories = enrollmentRepository.findCategories(userId);

        int groupIndex = (currentPage - 1) / PAGE_GROUP_SIZE;
        int startPage = groupIndex * PAGE_GROUP_SIZE + 1;
        int endPage = Math.min(startPage + PAGE_GROUP_SIZE - 1, totalPages);

        model.addAttribute("isAuthenticated", true);
        model.addAttribute("enrollments", enrollments);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategory", category == null ? "" : category);
        model.addAttribute("completion", completion);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("hasPrevGroup", startPage > 1);
        model.addAttribute("hasNextGroup", endPage < totalPages);
        model.addAttribute("prevGroupPage", startPage - 1);
        model.addAttribute("nextGroupPage", endPage + 1);
        return "study/note-course-list";
    }

    // 사용처: 노트 조회 기능 (GetMapping /notes/courses/{courseId})
    @GetMapping("/notes/courses/{courseId}")
    public String noteWorkspace(
            @PathVariable Long courseId,
            Model model,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return "redirect:/login";

        var studyCourse = enrollmentRepository.findStudyCourse(userId, courseId);
        if (studyCourse.isEmpty()) return "redirect:/study/notes";

        var outline = courseRepository.findOutline(courseId);
        var courseNote = studyCourseNoteRepository.findByUserAndCourse(userId, courseId).orElse(null);
        var questions = studyQuestionRepository.findByCourse(userId, courseId);

        model.addAttribute("isAuthenticated", true);
        model.addAttribute("course", studyCourse.get());
        model.addAttribute("outline", outline);
        model.addAttribute("questions", questions);
        model.addAttribute("noteContent", courseNote == null ? "" : courseNote.noteContent());
        model.addAttribute("summaryLine1", courseNote == null ? "" : courseNote.summaryLine1());
        model.addAttribute("summaryLine2", courseNote == null ? "" : courseNote.summaryLine2());
        model.addAttribute("summaryLine3", courseNote == null ? "" : courseNote.summaryLine3());
        return "study/note-workspace";
    }

    // 사용처: 노트 자동저장 기능 (PostMapping /notes/courses/{courseId}/autosave)
    @PostMapping("/notes/courses/{courseId}/autosave")
    @ResponseBody
    public Map<String, Object> autosaveNote(
            @PathVariable Long courseId,
            @RequestParam String noteContent,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return Map.of("ok", false, "message", "unauthorized");

        String safe = noteContent == null ? "" : noteContent;
        if (safe.length() > 10000) safe = safe.substring(0, 10000);
        studyCourseNoteRepository.saveNote(userId, courseId, safe);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("savedAt", java.time.LocalDateTime.now().toString());
        return result;
    }

    // 사용처: 노트 요약 저장 기능 (PostMapping /notes/courses/{courseId}/summary)
    @PostMapping("/notes/courses/{courseId}/summary")
    @ResponseBody
    public Map<String, Object> saveSummary(
            @PathVariable Long courseId,
            @RequestParam(required = false) String line1,
            @RequestParam(required = false) String line2,
            @RequestParam(required = false) String line3,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return Map.of("ok", false, "message", "unauthorized");

        studyCourseNoteRepository.saveSummary(userId, courseId,
                trimTo(line1, 500), trimTo(line2, 500), trimTo(line3, 500));
        return Map.of("ok", true);
    }

    // 사용처: 질문 등록 기능 (PostMapping /notes/courses/{courseId}/questions)
    @PostMapping("/notes/courses/{courseId}/questions")
    @ResponseBody
    public Map<String, Object> addQuestion(
            @PathVariable Long courseId,
            @RequestParam String question,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return Map.of("ok", false, "message", "unauthorized");

        String safe = trimTo(question, 1000);
        if (safe == null || safe.isBlank()) return Map.of("ok", false, "message", "empty");
        studyQuestionRepository.addQuestion(userId, courseId, safe);
        return Map.of("ok", true);
    }

    // 사용처: 질문 상태 변경 기능 (PostMapping /notes/courses/{courseId}/questions/{questionId}/status)
    @PostMapping("/notes/courses/{courseId}/questions/{questionId}/status")
    @ResponseBody
    public Map<String, Object> updateQuestionStatus(
            @PathVariable Long courseId,
            @PathVariable Long questionId,
            @RequestParam String status,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return Map.of("ok", false, "message", "unauthorized");

        String normalized = "DONE".equalsIgnoreCase(status) ? "DONE" : "OPEN";
        studyQuestionRepository.updateStatus(userId, courseId, questionId, normalized);
        return Map.of("ok", true);
    }

    // 사용처: 목록/상세 조회 기능 (GetMapping /courses/{courseId})
    @GetMapping("/courses/{courseId}")
    public String studyCourse(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "1") int reviewPage,
            Model model,
            Authentication authentication
    ) {
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

        var studyCourse = enrollmentRepository.findStudyCourse(userId, courseId);
        if (studyCourse.isEmpty()) {
            return "redirect:/study/enrollments";
        }

        var outline = courseRepository.findOutline(courseId);

        var notes = studyNoteRepository.findNotesByCourse(userId, courseId);
        java.util.Map<Long, String> noteMap = new java.util.HashMap<>();
        for (var n : notes) {
            noteMap.put(n.lectureId(), n.noteContent());
        }

        double progressPercent = studyCourse.get().progressRate();
        Double examScore = examAttemptRepository.findScore(userId, courseId);
        double quizPercent = examScore != null ? examScore : enrollmentRepository.getQuizPercent(userId, courseId);
        double totalScore = Math.round((progressPercent * 0.4 + quizPercent * 0.6) * 10.0) / 10.0;

        int currentReviewPage = Math.max(reviewPage, 1);
        int reviewOffset = (currentReviewPage - 1) * REVIEW_PAGE_SIZE;
        int totalReviewCount = courseRepository.countReviews(courseId);
        int totalReviewPages = (int) Math.ceil(totalReviewCount / (double) REVIEW_PAGE_SIZE);
        if (totalReviewPages == 0) totalReviewPages = 1;
        if (currentReviewPage > totalReviewPages) {
            currentReviewPage = totalReviewPages;
            reviewOffset = (currentReviewPage - 1) * REVIEW_PAGE_SIZE;
        }

        int reviewGroupIndex = (currentReviewPage - 1) / REVIEW_PAGE_GROUP_SIZE;
        int reviewStartPage = reviewGroupIndex * REVIEW_PAGE_GROUP_SIZE + 1;
        int reviewEndPage = Math.min(reviewStartPage + REVIEW_PAGE_GROUP_SIZE - 1, totalReviewPages);

        var reviews = courseRepository.findReviews(courseId, REVIEW_PAGE_SIZE, reviewOffset);
        String ragText = courseRepository.findDetail(courseId)
                .map(com.example.LMS.course.model.CourseDetail::content)
                .orElse(studyCourse.get().title());

        model.addAttribute("isAuthenticated", true);
        model.addAttribute("chatUserName", authentication.getName());
        model.addAttribute("ragText", ragText);
        model.addAttribute("course", studyCourse.get());
        model.addAttribute("outline", outline);
        model.addAttribute("noteMap", noteMap);
        model.addAttribute("progressPercent", progressPercent);
        model.addAttribute("quizPercent", quizPercent);
        model.addAttribute("totalScore", totalScore);
        model.addAttribute("canWriteReview", progressPercent >= 20.0);
        model.addAttribute("hasMyReview", courseRepository.existsReviewByWriter(courseId, authentication.getName()));
        model.addAttribute("reviews", reviews);
        model.addAttribute("reviewCount", totalReviewCount);
        model.addAttribute("currentUsername", authentication.getName());
        model.addAttribute("reviewCurrentPage", currentReviewPage);
        model.addAttribute("reviewStartPage", reviewStartPage);
        model.addAttribute("reviewEndPage", reviewEndPage);
        model.addAttribute("reviewHasPrev", reviewStartPage > 1);
        model.addAttribute("reviewHasNext", reviewEndPage < totalReviewPages);
        model.addAttribute("reviewPrevPage", reviewStartPage - 1);
        model.addAttribute("reviewNextPage", reviewEndPage + 1);

        return "study/course-learning";
    }

    // 사용처: 노트 조회 기능 (GetMapping /courses/{courseId}/notes/{lectureId})
    @GetMapping("/courses/{courseId}/notes/{lectureId}")
    public String studyNotePopup(
            @PathVariable Long courseId,
            @PathVariable Long lectureId,
            @RequestParam(defaultValue = "false") boolean saved,
            Model model,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return "redirect:/login";

        var studyCourse = enrollmentRepository.findStudyCourse(userId, courseId);
        if (studyCourse.isEmpty()) return "redirect:/study/enrollments";

        var outline = courseRepository.findOutline(courseId);
        var lecture = outline.stream().filter(o -> o.lectureId().equals(lectureId)).findFirst();
        if (lecture.isEmpty()) {
            return "redirect:/study/courses/" + courseId;
        }

        if (!studyAttendanceRepository.canAccessLectureSequential(userId, courseId, lecture.get().orderNo())) {
            return "redirect:/study/courses/" + courseId;
        }

        // 회차 진입 시: 해당 일자 출결을 우선 지각으로 기록 (이미 출석이면 유지)
        studyAttendanceRepository.markLectureStarted(userId, courseId, lectureId);
        studyAttendanceRepository.upsertDailyLate(userId, courseId, lectureId);

        String noteContent = studyNoteRepository.findNote(userId, courseId, lectureId)
                .map(StudyNoteRepository.StudyNoteItem::noteContent)
                .orElse("");

        model.addAttribute("isAuthenticated", true);
        model.addAttribute("course", studyCourse.get());
        model.addAttribute("lecture", lecture.get());
        model.addAttribute("noteContent", noteContent);
        model.addAttribute("saved", saved);
        return "study/note-popup";
    }

    // 사용처: 노트 저장 기능 (PostMapping /courses/{courseId}/notes/{lectureId})
    @PostMapping("/courses/{courseId}/notes/{lectureId}")
    public String saveStudyNotePopup(
            @PathVariable Long courseId,
            @PathVariable Long lectureId,
            @RequestParam String noteContent,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return "redirect:/login";

        var outline = courseRepository.findOutline(courseId);
        var lecture = outline.stream().filter(o -> o.lectureId().equals(lectureId)).findFirst();
        if (lecture.isEmpty()) {
            return "redirect:/study/courses/" + courseId;
        }

        if (!studyAttendanceRepository.canAccessLectureSequential(userId, courseId, lecture.get().orderNo())) {
            return "redirect:/study/courses/" + courseId;
        }

        String safeContent = noteContent == null ? "" : noteContent;
        if (safeContent.length() > 5000) {
            safeContent = safeContent.substring(0, 5000);
        }

        studyNoteRepository.save(userId, courseId, lectureId, safeContent);

        // 동일 회차 중복 학습으로는 새 출석을 쌓지 않음(최초 완료만 인정)
        boolean newlyCompleted = studyAttendanceRepository.markLectureCompletedIfFirstTime(userId, courseId, lectureId);
        if (newlyCompleted) {
            // 지각 기록이 있던 날에도 최초 완료가 생기면 출석으로 승격
            studyAttendanceRepository.upsertDailyAttend(userId, courseId, lectureId);
        }

        return "redirect:/study/courses/" + courseId + "/notes/" + lectureId + "?saved=true";
    }

    // 사용처: 수강취소 기능 (PostMapping /courses/{courseId}/drop)
    @PostMapping("/courses/{courseId}/drop")
    public String dropCourse(@PathVariable Long courseId, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return "redirect:/login";
        enrollmentRepository.dropEnrollment(courseId, userId);
        return "redirect:/study/enrollments";
    }

    // 사용처: 후기 등록/수정 기능 (PostMapping /courses/{courseId}/reviews)
    @PostMapping("/courses/{courseId}/reviews")
    public String writeReview(
            @PathVariable Long courseId,
            @RequestParam double rating,
            @RequestParam String content,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return "redirect:/login";

        var studyCourse = enrollmentRepository.findStudyCourse(userId, courseId);
        if (studyCourse.isEmpty()) return "redirect:/study/enrollments";
        if (studyCourse.get().progressRate() < 20.0) {
            return "redirect:/study/courses/" + courseId;
        }

        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isEmpty()) return "redirect:/study/courses/" + courseId;

        double safeRating = Math.max(0.0, Math.min(5.0, Math.round(rating * 2.0) / 2.0));
        String writerName = authentication.getName();
        if (courseRepository.existsReviewByWriter(courseId, writerName)) {
            return "redirect:/study/courses/" + courseId;
        }
        courseRepository.saveReview(courseId, writerName, safeRating, trimmed);

        return "redirect:/study/courses/" + courseId;
    }


    // 사용처: 수정 화면 진입 기능 (GetMapping /courses/{courseId}/reviews/{reviewId}/edit)
    @GetMapping("/courses/{courseId}/reviews/{reviewId}/edit")
    public String editReviewPage(
            @PathVariable Long courseId,
            @PathVariable Long reviewId,
            Model model,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return "redirect:/login";

        var review = courseRepository.findReviewById(reviewId);
        if (review.isEmpty() || !review.get().writerName().equals(authentication.getName())) {
            return "redirect:/study/courses/" + courseId;
        }

        model.addAttribute("isAuthenticated", true);
        model.addAttribute("courseId", courseId);
        model.addAttribute("reviewId", reviewId);
        model.addAttribute("rating", review.get().rating());
        model.addAttribute("content", review.get().content());
        return "study/review-edit";
    }

    // 사용처: 수정 저장 기능 (PostMapping /courses/{courseId}/reviews/{reviewId}/edit)
    @PostMapping("/courses/{courseId}/reviews/{reviewId}/edit")
    public String editReview(
            @PathVariable Long courseId,
            @PathVariable Long reviewId,
            @RequestParam double rating,
            @RequestParam String content,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return "redirect:/login";

        double safeRating = Math.max(0.0, Math.min(5.0, Math.round(rating * 2.0) / 2.0));
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isEmpty()) {
            return "redirect:/study/courses/" + courseId + "/reviews/" + reviewId + "/edit";
        }

        courseRepository.updateReview(reviewId, courseId, authentication.getName(), safeRating, trimmed);
        return "redirect:/study/courses/" + courseId;
    }

    // 사용처: 삭제 기능 (PostMapping /courses/{courseId}/reviews/{reviewId}/delete)
    @PostMapping("/courses/{courseId}/reviews/{reviewId}/delete")
    public String deleteReview(
            @PathVariable Long courseId,
            @PathVariable Long reviewId,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return "redirect:/login";

        courseRepository.deleteReview(reviewId, courseId, authentication.getName());
        return "redirect:/study/courses/" + courseId;
    }


    // 사용처: 만족도 화면 조회 기능 (GetMapping /satisfaction)
    @GetMapping("/satisfaction")
    public String satisfactionList(
            @RequestParam(defaultValue = "1") int page,
            Model model,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return "redirect:/login";

        int currentPage = Math.max(page, 1);
        int offset = (currentPage - 1) * PAGE_SIZE;

        int totalCount = enrollmentRepository.countPaged(userId, null, "complete");
        int totalPages = (int) Math.ceil(totalCount / (double) PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
        if (currentPage > totalPages) {
            currentPage = totalPages;
            offset = (currentPage - 1) * PAGE_SIZE;
        }

        var enrollments = enrollmentRepository.findPaged(userId, PAGE_SIZE, offset, null, "complete");
        var surveyedCourseIds = satisfactionSurveyRepository.findSurveyedCourseIdsByUser(userId);

        int groupIndex = (currentPage - 1) / PAGE_GROUP_SIZE;
        int startPage = groupIndex * PAGE_GROUP_SIZE + 1;
        int endPage = Math.min(startPage + PAGE_GROUP_SIZE - 1, totalPages);

        model.addAttribute("isAuthenticated", true);
        model.addAttribute("enrollments", enrollments);
        model.addAttribute("surveyedCourseIds", surveyedCourseIds);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("hasPrevGroup", startPage > 1);
        model.addAttribute("hasNextGroup", endPage < totalPages);
        model.addAttribute("prevGroupPage", startPage - 1);
        model.addAttribute("nextGroupPage", endPage + 1);

        return "study/satisfaction-list";
    }

    // 사용처: 만족도 화면 조회 기능 (GetMapping /satisfaction/{courseId})
    @GetMapping("/satisfaction/{courseId}")
    public String satisfactionDetail(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "false") boolean surveyDone,
            @RequestParam(defaultValue = "false") boolean alreadySurveyed,
            Model model,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return "redirect:/login";

        var studyCourse = enrollmentRepository.findStudyCourse(userId, courseId);
        if (studyCourse.isEmpty() || studyCourse.get().progressRate() < 100.0) {
            return "redirect:/study/satisfaction";
        }

        double progressPercent = studyCourse.get().progressRate();
        Double examScore = examAttemptRepository.findScore(userId, courseId);
        double quizPercent = examScore != null ? examScore : enrollmentRepository.getQuizPercent(userId, courseId);
        double totalScore = Math.round((progressPercent * 0.4 + quizPercent * 0.6) * 10.0) / 10.0;

        model.addAttribute("isAuthenticated", true);
        model.addAttribute("course", studyCourse.get());
        model.addAttribute("progressPercent", progressPercent);
        model.addAttribute("quizPercent", quizPercent);
        model.addAttribute("totalScore", totalScore);
        model.addAttribute("surveyDone", surveyDone);
        model.addAttribute("alreadySurveyed", alreadySurveyed);
        model.addAttribute("survey", satisfactionSurveyRepository.findByUserAndCourse(userId, courseId).orElse(null));

        return "study/satisfaction-detail";
    }


    // 사용처: 만족도 화면 조회 기능 (GetMapping /satisfaction/{courseId}/survey)
    @GetMapping("/satisfaction/{courseId}/survey")
    public String satisfactionSurveyPage(
            @PathVariable Long courseId,
            Model model,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return "redirect:/login";

        var studyCourse = enrollmentRepository.findStudyCourse(userId, courseId);
        if (studyCourse.isEmpty() || studyCourse.get().progressRate() < 100.0) {
            return "redirect:/study/satisfaction";
        }

        if (satisfactionSurveyRepository.findByUserAndCourse(userId, courseId).isPresent()) {
            return "redirect:/study/satisfaction/" + courseId + "?alreadySurveyed=true";
        }

        model.addAttribute("isAuthenticated", true);
        model.addAttribute("course", studyCourse.get());
        model.addAttribute("editMode", false);
        model.addAttribute("survey", null);
        model.addAttribute("surveyFormAction", "/study/satisfaction/" + courseId + "/survey");
        return "study/satisfaction-survey";
    }


    // 사용처: 수정 화면 진입 기능 (GetMapping /satisfaction/{courseId}/survey/edit)
    @GetMapping("/satisfaction/{courseId}/survey/edit")
    public String satisfactionSurveyEditPage(
            @PathVariable Long courseId,
            Model model,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return "redirect:/login";

        var studyCourse = enrollmentRepository.findStudyCourse(userId, courseId);
        if (studyCourse.isEmpty() || studyCourse.get().progressRate() < 100.0) {
            return "redirect:/study/satisfaction";
        }

        var survey = satisfactionSurveyRepository.findDetailByUserAndCourse(userId, courseId);
        if (survey.isEmpty()) {
            return "redirect:/study/satisfaction/" + courseId;
        }

        model.addAttribute("isAuthenticated", true);
        model.addAttribute("course", studyCourse.get());
        model.addAttribute("editMode", true);
        model.addAttribute("survey", survey.get());
        model.addAttribute("surveyFormAction", "/study/satisfaction/" + courseId + "/survey/edit");
        return "study/satisfaction-survey";
    }

    // 사용처: 수정 저장 기능 (PostMapping /satisfaction/{courseId}/survey/edit)
    @PostMapping("/satisfaction/{courseId}/survey/edit")
    public String submitSatisfactionSurveyEdit(
            @PathVariable Long courseId,
            @RequestParam(required = false) Double q1,
            @RequestParam(required = false) Double q2,
            @RequestParam(required = false) Double q3,
            @RequestParam(required = false) Double q4,
            @RequestParam(required = false) Double q5,
            @RequestParam(required = false) Double q6,
            @RequestParam(required = false) Double q7,
            @RequestParam(required = false) Double q8,
            @RequestParam(required = false) Double q9,
            @RequestParam(required = false) Double q10,
            @RequestParam(required = false) String comment,
            @RequestParam(defaultValue = "5.0") double overall,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return "redirect:/login";
        satisfactionSurveyRepository.update(userId, courseId, q1,q2,q3,q4,q5,q6,q7,q8,q9,q10, comment, overall);
        return "redirect:/study/satisfaction/" + courseId + "?surveyDone=true";
    }

    // 사용처: 만족도 설문 저장/삭제 기능 (PostMapping /satisfaction/{courseId}/survey)
    @PostMapping("/satisfaction/{courseId}/survey")
    public String submitSatisfactionSurvey(
            @PathVariable Long courseId,
            @RequestParam(required = false) Double q1,
            @RequestParam(required = false) Double q2,
            @RequestParam(required = false) Double q3,
            @RequestParam(required = false) Double q4,
            @RequestParam(required = false) Double q5,
            @RequestParam(required = false) Double q6,
            @RequestParam(required = false) Double q7,
            @RequestParam(required = false) Double q8,
            @RequestParam(required = false) Double q9,
            @RequestParam(required = false) Double q10,
            @RequestParam(required = false) String comment,
            @RequestParam(defaultValue = "5.0") double overall,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return "redirect:/login";

        var studyCourse = enrollmentRepository.findStudyCourse(userId, courseId);
        if (studyCourse.isEmpty() || studyCourse.get().progressRate() < 100.0) {
            return "redirect:/study/satisfaction";
        }

        if (satisfactionSurveyRepository.findByUserAndCourse(userId, courseId).isPresent()) {
            return "redirect:/study/satisfaction/" + courseId + "?alreadySurveyed=true";
        }
        satisfactionSurveyRepository.save(userId, courseId, q1, q2, q3, q4, q5, q6, q7, q8, q9, q10, comment, overall);
        return "redirect:/study/satisfaction/" + courseId + "?surveyDone=true";
    }


    // 사용처: 삭제 기능 (PostMapping /satisfaction/{courseId}/survey/delete)
    @PostMapping("/satisfaction/{courseId}/survey/delete")
    public String deleteSatisfactionSurvey(
            @PathVariable Long courseId,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return "redirect:/login";
        satisfactionSurveyRepository.delete(userId, courseId);
        return "redirect:/study/satisfaction/" + courseId;
    }


    // 사용처: 목록/상세 조회 기능 (GetMapping /courses/{courseId}/exam)
    @GetMapping("/courses/{courseId}/exam")
    public String examPage(
            @PathVariable Long courseId,
            Model model,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return "redirect:/login";

        var studyCourse = enrollmentRepository.findStudyCourse(userId, courseId);
        if (studyCourse.isEmpty()) return "redirect:/study/enrollments";

        var questions = examQuestionRepository.findByCourse(courseId);
        if (questions.isEmpty()) {
            return "redirect:/study/courses/" + courseId + "?noExam=true";
        }

        Double prevScore = examAttemptRepository.findScore(userId, courseId);

        model.addAttribute("isAuthenticated", true);
        model.addAttribute("course", studyCourse.get());
        model.addAttribute("questions", questions);
        model.addAttribute("hasPreviousAttempt", prevScore != null);
        return "study/exam";
    }

    // 사용처: 등록/처리 기능 (PostMapping /courses/{courseId}/exam/submit)
    @PostMapping("/courses/{courseId}/exam/submit")
    public String submitExam(
            @PathVariable Long courseId,
            @RequestParam Map<String, String> params,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        if (userId == null) return "redirect:/login";

        var studyCourse = enrollmentRepository.findStudyCourse(userId, courseId);
        if (studyCourse.isEmpty()) return "redirect:/study/enrollments";

        var questions = examQuestionRepository.findByCourse(courseId);
        if (questions.isEmpty()) return "redirect:/study/courses/" + courseId + "?noExam=true";

        int correctCount = 0;
        for (var q : questions) {
            String key = "q_" + q.id();
            String val = params.get(key);
            int selected = 0;
            try { selected = Integer.parseInt(val); } catch (Exception ignored) {}
            if (selected == q.correctOption()) {
                correctCount++;
            }
        }

        double score = Math.round((correctCount * 100.0 / questions.size()) * 10.0) / 10.0;
        examAttemptRepository.saveScore(userId, courseId, score);

        return "redirect:/study/courses/" + courseId + "?examSubmitted=true&examScore=" + score;
    }


    private String trimTo(String value, int max) {
        if (value == null) return null;
        String v = value.trim();
        if (v.length() > max) return v.substring(0, max);
        return v;
    }

    private Long getCurrentUserId(Authentication authentication) {
        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        if (!isAuthenticated) return null;
        return userRepository.findIdByUsername(authentication.getName()).orElse(null);
    }
}

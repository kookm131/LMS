package com.example.LMS.home.controller;

import com.example.LMS.home.repository.*;
import com.example.LMS.user.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 공지사항(목록/상세/댓글) Controller
 *
 * 왜 분리했는가?
 * - /community/notices 관련 흐름이 늘어나면서 HomeController와 역할을 분리해 유지보수성을 높이기 위함.
 */
@Controller
@RequestMapping("/community/notices")
public class NoticeController {

    /** 공지 목록 페이지당 개수 */
    private static final int PAGE_SIZE = 10;
    /** 페이지 네비 묶음 크기 (앞 1..5 뒤) */
    private static final int PAGE_GROUP_SIZE = 5;
    /** 댓글 페이지당 개수 */
    private static final int COMMENT_PAGE_SIZE = 5;

    private final HomeBoardRepository homeBoardRepository;
    private final UserRepository userRepository;

    public NoticeController(HomeBoardRepository homeBoardRepository, UserRepository userRepository) {
        this.homeBoardRepository = homeBoardRepository;
        this.userRepository = userRepository;
    }

    /**
     * 공지사항 목록
     * - 고정 공지 2개는 항상 상단
     * - 일반 공지는 페이지네이션
     */
    // 사용처: 목록/상세 조회 기능 (GetMapping 기본 경로)
    @GetMapping
    public String list(
            @RequestParam(defaultValue = "1") int page,
            Model model,
            Authentication authentication
    ) {
        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        int currentPage = Math.max(page, 1);
        int offset = (currentPage - 1) * PAGE_SIZE;

        // 고정공지 제외 일반공지의 총 페이지 계산
        int totalCount = homeBoardRepository.countNoticesExceptPinned();
        int totalPages = (int) Math.ceil(totalCount / (double) PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;

        if (currentPage > totalPages) {
            currentPage = totalPages;
            offset = (currentPage - 1) * PAGE_SIZE;
        }

        List<HomeBoardRepository.NoticeItem> pinnedNotices = homeBoardRepository.findPinnedNotices();
        List<HomeBoardRepository.NoticeItem> notices = homeBoardRepository.findNoticesPaged(PAGE_SIZE, offset);

        int groupIndex = (currentPage - 1) / PAGE_GROUP_SIZE;
        int startPage = groupIndex * PAGE_GROUP_SIZE + 1;
        int endPage = Math.min(startPage + PAGE_GROUP_SIZE - 1, totalPages);

        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("isAdmin", isAdmin(authentication));
        model.addAttribute("pinnedNotices", pinnedNotices);
        model.addAttribute("notices", notices);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("hasPrevGroup", startPage > 1);
        model.addAttribute("hasNextGroup", endPage < totalPages);
        model.addAttribute("prevGroupPage", startPage - 1);
        model.addAttribute("nextGroupPage", endPage + 1);

        return "community/notices";
    }

    /**
     * 공지 상세 + 댓글 목록
     * - 댓글은 5개 단위 페이징
     */
    // 사용처: 목록/상세 조회 기능 (GetMapping /{noticeId})
    @GetMapping("/{noticeId}")
    public String detail(
            @PathVariable Long noticeId,
            @RequestParam(defaultValue = "1") int page,
            Model model,
            Authentication authentication
    ) {
        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        Long currentUserId = resolveAuthenticatedUserId(authentication).orElse(null);

        var notice = homeBoardRepository.findNoticeDetail(noticeId);
        if (notice.isEmpty()) {
            // 존재하지 않는 공지 접근 방어
            return "redirect:/community/notices";
        }

        int currentPage = Math.max(page, 1);
        int offset = (currentPage - 1) * COMMENT_PAGE_SIZE;

        int totalCount = homeBoardRepository.countNoticeComments(noticeId);
        int totalPages = (int) Math.ceil(totalCount / (double) COMMENT_PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;

        if (currentPage > totalPages) {
            currentPage = totalPages;
            offset = (currentPage - 1) * COMMENT_PAGE_SIZE;
        }

        List<HomeBoardRepository.NoticeCommentItem> comments =
                homeBoardRepository.findNoticeCommentsPaged(noticeId, COMMENT_PAGE_SIZE, offset);

        int groupIndex = (currentPage - 1) / PAGE_GROUP_SIZE;
        int startPage = groupIndex * PAGE_GROUP_SIZE + 1;
        int endPage = Math.min(startPage + PAGE_GROUP_SIZE - 1, totalPages);

        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("isAdmin", isAdmin(authentication));
        model.addAttribute("currentUserId", currentUserId);
        model.addAttribute("isAdmin", isAdmin(authentication));
        model.addAttribute("notice", notice.get());
        model.addAttribute("comments", comments);
        model.addAttribute("commentPage", currentPage);
        model.addAttribute("commentStartPage", startPage);
        model.addAttribute("commentEndPage", endPage);
        model.addAttribute("hasPrevCommentGroup", startPage > 1);
        model.addAttribute("hasNextCommentGroup", endPage < totalPages);
        model.addAttribute("prevCommentGroupPage", startPage - 1);
        model.addAttribute("nextCommentGroupPage", endPage + 1);

        return "community/notice-detail";
    }

    /**
     * 댓글 등록
     * - 로그인 사용자만 허용
     * - 공백 댓글 방지
     */
    // 사용처: 댓글 등록 기능 (PostMapping /{noticeId}/comments)
    @PostMapping("/{noticeId}/comments")
    public String writeComment(
            @PathVariable Long noticeId,
            @RequestParam String content,
            Authentication authentication
    ) {
        var userId = resolveAuthenticatedUserId(authentication);
        if (userId.isEmpty()) {
            return "redirect:/login";
        }

        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isEmpty()) {
            return "redirect:/community/notices/" + noticeId;
        }

        homeBoardRepository.saveNoticeComment(noticeId, userId.get(), trimmed);
        return "redirect:/community/notices/" + noticeId;
    }

    /** 댓글 수정 페이지 */
    // 사용처: 수정 화면 진입 기능 (GetMapping /{noticeId}/comments/{commentId}/edit)
    @GetMapping("/{noticeId}/comments/{commentId}/edit")
    public String editCommentPage(
            @PathVariable Long noticeId,
            @PathVariable Long commentId,
            Model model,
            Authentication authentication
    ) {
        var userId = resolveAuthenticatedUserId(authentication);
        if (userId.isEmpty()) {
            return "redirect:/login";
        }

        var comment = homeBoardRepository.findNoticeCommentById(commentId);
        if (comment.isEmpty() || !comment.get().noticeId().equals(noticeId) || !comment.get().userId().equals(userId.get())) {
            return "redirect:/community/notices/" + noticeId;
        }

        boolean isAuthenticated = true;
        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("isAdmin", isAdmin(authentication));
        model.addAttribute("noticeId", noticeId);
        model.addAttribute("commentId", commentId);
        model.addAttribute("content", comment.get().content());

        return "community/comment-edit";
    }

    /** 댓글 수정 처리 (본인 댓글만) */
    // 사용처: 수정 저장 기능 (PostMapping /{noticeId}/comments/{commentId}/edit)
    @PostMapping("/{noticeId}/comments/{commentId}/edit")
    public String editComment(
            @PathVariable Long noticeId,
            @PathVariable Long commentId,
            @RequestParam String content,
            Authentication authentication
    ) {
        var userId = resolveAuthenticatedUserId(authentication);
        if (userId.isEmpty()) {
            return "redirect:/login";
        }

        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isEmpty()) {
            return "redirect:/community/notices/" + noticeId + "/comments/" + commentId + "/edit";
        }

        homeBoardRepository.updateNoticeComment(commentId, userId.get(), trimmed);
        return "redirect:/community/notices/" + noticeId;
    }

    /** 댓글 삭제 처리 (본인 댓글만) */
    // 사용처: 삭제 기능 (PostMapping /{noticeId}/comments/{commentId}/delete)
    @PostMapping("/{noticeId}/comments/{commentId}/delete")
    public String deleteComment(
            @PathVariable Long noticeId,
            @PathVariable Long commentId,
            Authentication authentication
    ) {
        var userId = resolveAuthenticatedUserId(authentication);
        if (userId.isEmpty()) {
            return "redirect:/login";
        }

        homeBoardRepository.deleteNoticeComment(commentId, userId.get());
        return "redirect:/community/notices/" + noticeId;
    }


    // 사용처: 작성 화면 진입 기능 (GetMapping /write)
    @GetMapping("/write")
    public String writePage(Authentication authentication, Model model) {
        if (!isAdmin(authentication)) {
            return "redirect:/community/notices";
        }
        model.addAttribute("isAuthenticated", true);
        return "community/notice-write";
    }

    // 사용처: 작성 저장 기능 (PostMapping /write)
    @PostMapping("/write")
    public String write(@RequestParam String title,
                        @RequestParam String content,
                        Authentication authentication) {
        if (!isAdmin(authentication)) {
            return "redirect:/community/notices";
        }
        var userId = resolveAuthenticatedUserId(authentication);
        if (userId.isEmpty()) return "redirect:/community/notices";

        String t = title == null ? "" : title.trim();
        String c = content == null ? "" : content.trim();
        if (t.isEmpty() || c.isEmpty()) {
            return "redirect:/community/notices/write";
        }
        Long id = homeBoardRepository.saveNotice(userId.get(), t, c);
        return "redirect:/community/notices/" + id;
    }

    // 사용처: 수정 화면 진입 기능 (GetMapping /{noticeId}/edit)
    @GetMapping("/{noticeId}/edit")
    public String editPage(@PathVariable Long noticeId, Authentication authentication, Model model) {
        if (!isAdmin(authentication)) {
            return "redirect:/community/notices/" + noticeId;
        }
        var notice = homeBoardRepository.findNoticeDetail(noticeId);
        if (notice.isEmpty()) return "redirect:/community/notices";
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("notice", notice.get());
        return "community/notice-edit";
    }

    // 사용처: 수정 저장 기능 (PostMapping /{noticeId}/edit)
    @PostMapping("/{noticeId}/edit")
    public String edit(@PathVariable Long noticeId,
                       @RequestParam String title,
                       @RequestParam String content,
                       Authentication authentication) {
        if (!isAdmin(authentication)) {
            return "redirect:/community/notices/" + noticeId;
        }
        String t = title == null ? "" : title.trim();
        String c = content == null ? "" : content.trim();
        if (t.isEmpty() || c.isEmpty()) {
            return "redirect:/community/notices/" + noticeId + "/edit";
        }
        homeBoardRepository.updateNotice(noticeId, t, c);
        return "redirect:/community/notices/" + noticeId;
    }

    // 사용처: 삭제 기능 (PostMapping /{noticeId}/delete)
    @PostMapping("/{noticeId}/delete")
    public String delete(@PathVariable Long noticeId, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return "redirect:/community/notices/" + noticeId;
        }
        homeBoardRepository.deleteNotice(noticeId);
        return "redirect:/community/notices";
    }

    // 사용처: 상단고정 기능 (PostMapping /{noticeId}/pin)
    @PostMapping("/{noticeId}/pin")
    public String pin(@PathVariable Long noticeId, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return "redirect:/community/notices";
        }
        homeBoardRepository.pinNotice(noticeId);
        return "redirect:/community/notices";
    }

    // 사용처: 고정해제 기능 (PostMapping /{noticeId}/unpin)
    @PostMapping("/{noticeId}/unpin")
    public String unpin(@PathVariable Long noticeId, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return "redirect:/community/notices";
        }
        homeBoardRepository.unpinNotice(noticeId);
        return "redirect:/community/notices";
    }

    private boolean isAdmin(Authentication authentication) {
        boolean ok = authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken);
        if (!ok) return false;
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch("ROLE_ADMIN"::equals);
    }

    /** 현재 인증 사용자 id 조회(없으면 empty) */
    private java.util.Optional<Long> resolveAuthenticatedUserId(Authentication authentication) {
        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        if (!isAuthenticated) {
            return java.util.Optional.empty();
        }
        return userRepository.findIdByUsername(authentication.getName());
    }
}

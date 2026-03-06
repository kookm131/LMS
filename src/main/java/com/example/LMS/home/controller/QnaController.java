package com.example.LMS.home.controller;

import com.example.LMS.home.repository.*;
import com.example.LMS.user.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/community/qna")
public class QnaController {

    private static final int PAGE_SIZE = 10;
    private static final int PAGE_GROUP_SIZE = 5;
    private static final int COMMENT_PAGE_SIZE = 5;
    private static final int COMMENT_PAGE_GROUP_SIZE = 5;

    private final QnaRepository qnaRepository;
    private final UserRepository userRepository;

    public QnaController(QnaRepository qnaRepository, UserRepository userRepository) {
        this.qnaRepository = qnaRepository;
        this.userRepository = userRepository;
    }

    // 사용처: 목록/상세 조회 기능 (GetMapping 기본 경로)
    @GetMapping
    public String list(@RequestParam(defaultValue = "1") int page, Model model, Authentication authentication) {
        boolean isAuthenticated = isAuthenticated(authentication);

        int currentPage = Math.max(page, 1);
        int offset = (currentPage - 1) * PAGE_SIZE;

        int totalCount = qnaRepository.countQna();
        int totalPages = (int) Math.ceil(totalCount / (double) PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;

        if (currentPage > totalPages) {
            currentPage = totalPages;
            offset = (currentPage - 1) * PAGE_SIZE;
        }

        List<QnaRepository.QnaItem> faqItems = qnaRepository.findTopFaqs();
        List<QnaRepository.QnaItem> qnaItems = qnaRepository.findQnaPaged(PAGE_SIZE, offset);

        int groupIndex = (currentPage - 1) / PAGE_GROUP_SIZE;
        int startPage = groupIndex * PAGE_GROUP_SIZE + 1;
        int endPage = Math.min(startPage + PAGE_GROUP_SIZE - 1, totalPages);

        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("faqItems", faqItems);
        model.addAttribute("qnaItems", qnaItems);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("hasPrevGroup", startPage > 1);
        model.addAttribute("hasNextGroup", endPage < totalPages);
        model.addAttribute("prevGroupPage", startPage - 1);
        model.addAttribute("nextGroupPage", endPage + 1);

        return "community/qna";
    }

    // 사용처: 작성 화면 진입 기능 (GetMapping /write)
    @GetMapping("/write")
    public String writePage(Authentication authentication, Model model) {
        if (!isNormalAuthenticatedUser(authentication)) {
            return "redirect:/community/qna";
        }
        model.addAttribute("isAuthenticated", true);
        return "community/qna-write";
    }

    // 사용처: 작성 저장 기능 (PostMapping /write)
    @PostMapping("/write")
    public String write(@RequestParam String title,
                        @RequestParam String content,
                        @RequestParam(required = false, name = "attachment") MultipartFile attachment,
                        Authentication authentication) throws IOException {
        if (!isNormalAuthenticatedUser(authentication)) {
            return "redirect:/community/qna";
        }

        String trimmedTitle = title == null ? "" : title.trim();
        String trimmedContent = content == null ? "" : content.trim();
        if (trimmedTitle.isEmpty() || trimmedContent.isEmpty()) {
            return "redirect:/community/qna/write";
        }

        Long userId = userRepository.findIdByUsername(authentication.getName()).orElse(null);
        if (userId == null) {
            return "redirect:/community/qna";
        }

        String originalName = null;
        String savedName = null;
        String contentType = null;

        if (attachment != null && !attachment.isEmpty()) {
            contentType = attachment.getContentType();
            boolean isImage = contentType != null && contentType.startsWith("image/");
            boolean isVideo = contentType != null && contentType.startsWith("video/");
            if (!isImage && !isVideo) {
                return "redirect:/community/qna/write";
            }

            Path uploadDir = Path.of("/home/ubuntu/projects/LMS/uploads/qna");
            Files.createDirectories(uploadDir);

            originalName = attachment.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf('.'));
            }
            savedName = UUID.randomUUID() + ext;
            Path savePath = uploadDir.resolve(savedName);
            Files.copy(attachment.getInputStream(), savePath, StandardCopyOption.REPLACE_EXISTING);
        }

        Long qnaId = qnaRepository.saveQnaAndReturnId(userId, trimmedTitle, trimmedContent, originalName, savedName, contentType);
        if (qnaId == null) {
            return "redirect:/community/qna";
        }
        return "redirect:/community/qna/" + qnaId;
    }

    // 사용처: 목록/상세 조회 기능 (GetMapping /{qnaId})
    @GetMapping("/{qnaId}")
    public String detail(@PathVariable Long qnaId,
                         @RequestParam(defaultValue = "1") int page,
                         Model model,
                         Authentication authentication) {
        qnaRepository.increaseViewCount(qnaId);
        var detailOpt = qnaRepository.findQnaDetail(qnaId);
        if (detailOpt.isEmpty()) {
            return "redirect:/community/qna";
        }

        var detail = detailOpt.get();
        var acceptedComment = qnaRepository.findAcceptedComment(qnaId).orElse(null);

        int currentPage = Math.max(page, 1);
        int offset = (currentPage - 1) * COMMENT_PAGE_SIZE;

        int totalCount = qnaRepository.countQnaComments(qnaId);
        int totalPages = (int) Math.ceil(totalCount / (double) COMMENT_PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;

        if (currentPage > totalPages) {
            currentPage = totalPages;
            offset = (currentPage - 1) * COMMENT_PAGE_SIZE;
        }

        var comments = qnaRepository.findQnaCommentsPaged(qnaId, COMMENT_PAGE_SIZE, offset);

        int groupIndex = (currentPage - 1) / COMMENT_PAGE_GROUP_SIZE;
        int startPage = groupIndex * COMMENT_PAGE_GROUP_SIZE + 1;
        int endPage = Math.min(startPage + COMMENT_PAGE_GROUP_SIZE - 1, totalPages);

        boolean isAuthenticated = isAuthenticated(authentication);
        boolean canComment = isAdminOrBusiness(authentication);
        boolean isAuthor = isAuthenticated && authentication.getName().equals(detail.authorUsername());

        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("qna", detail);
        model.addAttribute("acceptedComment", acceptedComment);
        model.addAttribute("comments", comments);
        model.addAttribute("canComment", canComment);
        model.addAttribute("isAuthor", isAuthor);
        model.addAttribute("currentUsername", isAuthenticated ? authentication.getName() : null);

        model.addAttribute("commentPage", currentPage);
        model.addAttribute("commentStartPage", startPage);
        model.addAttribute("commentEndPage", endPage);
        model.addAttribute("hasPrevCommentGroup", startPage > 1);
        model.addAttribute("hasNextCommentGroup", endPage < totalPages);
        model.addAttribute("prevCommentGroupPage", startPage - 1);
        model.addAttribute("nextCommentGroupPage", endPage + 1);

        return "community/qna-detail";
    }

    // 사용처: 댓글 등록 기능 (PostMapping /{qnaId}/comments)
    @PostMapping("/{qnaId}/comments")
    public String writeComment(@PathVariable Long qnaId,
                               @RequestParam String content,
                               Authentication authentication) {
        if (!isAdminOrBusiness(authentication)) {
            return "redirect:/community/qna/" + qnaId;
        }
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isEmpty()) {
            return "redirect:/community/qna/" + qnaId;
        }
        qnaRepository.saveQnaComment(qnaId, authentication.getName(), trimmed);
        return "redirect:/community/qna/" + qnaId;
    }

    // 사용처: 채택 처리 기능 (PostMapping /{qnaId}/comments/{commentId}/accept)
    @PostMapping("/{qnaId}/comments/{commentId}/accept")
    public String acceptComment(@PathVariable Long qnaId,
                                @PathVariable Long commentId,
                                Authentication authentication) {
        var detailOpt = qnaRepository.findQnaDetail(qnaId);
        if (detailOpt.isEmpty()) {
            return "redirect:/community/qna";
        }
        boolean isAuthor = isAuthenticated(authentication)
                && authentication.getName().equals(detailOpt.get().authorUsername());
        if (!isAuthor) {
            return "redirect:/community/qna/" + qnaId;
        }
        if (!qnaRepository.isCommentInQna(commentId, qnaId)) {
            return "redirect:/community/qna/" + qnaId;
        }

        qnaRepository.acceptComment(qnaId, commentId);
        return "redirect:/community/qna/" + qnaId;
    }


    // 사용처: 채택 취소 기능 (PostMapping /{qnaId}/comments/unaccept)
    @PostMapping("/{qnaId}/comments/unaccept")
    public String unacceptComment(@PathVariable Long qnaId, Authentication authentication) {
        var detailOpt = qnaRepository.findQnaDetail(qnaId);
        if (detailOpt.isEmpty()) {
            return "redirect:/community/qna";
        }
        boolean isAuthor = isAuthenticated(authentication)
                && authentication.getName().equals(detailOpt.get().authorUsername());
        if (!isAuthor) {
            return "redirect:/community/qna/" + qnaId;
        }
        qnaRepository.unacceptComment(qnaId);
        return "redirect:/community/qna/" + qnaId;
    }

    // 사용처: 수정 화면 진입 기능 (GetMapping /{qnaId}/edit)
    @GetMapping("/{qnaId}/edit")
    public String editPage(@PathVariable Long qnaId, Authentication authentication, Model model) {
        var detailOpt = qnaRepository.findQnaDetail(qnaId);
        if (detailOpt.isEmpty()) {
            return "redirect:/community/qna";
        }
        var detail = detailOpt.get();
        boolean isAuthor = isAuthenticated(authentication)
                && authentication.getName().equals(detail.authorUsername());
        if (!isAuthor) {
            return "redirect:/community/qna/" + qnaId;
        }

        model.addAttribute("isAuthenticated", true);
        model.addAttribute("qna", detail);
        return "community/qna-edit";
    }

    // 사용처: 수정 저장 기능 (PostMapping /{qnaId}/edit)
    @PostMapping("/{qnaId}/edit")
    public String edit(@PathVariable Long qnaId,
                       @RequestParam String title,
                       @RequestParam String content,
                       @RequestParam(required = false, name = "attachment") MultipartFile attachment,
                       Authentication authentication) throws IOException {
        var userId = currentUserId(authentication);
        if (userId == null) {
            return "redirect:/community/qna/" + qnaId;
        }

        String t = title == null ? "" : title.trim();
        String c = content == null ? "" : content.trim();
        if (t.isEmpty() || c.isEmpty()) {
            return "redirect:/community/qna/" + qnaId + "/edit";
        }

        if (attachment != null && !attachment.isEmpty()) {
            String contentType = attachment.getContentType();
            boolean isImage = contentType != null && contentType.startsWith("image/");
            boolean isVideo = contentType != null && contentType.startsWith("video/");
            if (!isImage && !isVideo) {
                return "redirect:/community/qna/" + qnaId + "/edit";
            }

            Path uploadDir = Path.of("/home/ubuntu/projects/LMS/uploads/qna");
            Files.createDirectories(uploadDir);

            String originalName = attachment.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf('.'));
            }
            String savedName = UUID.randomUUID() + ext;
            Path savePath = uploadDir.resolve(savedName);
            Files.copy(attachment.getInputStream(), savePath, StandardCopyOption.REPLACE_EXISTING);

            qnaRepository.updateQnaWithAttachment(qnaId, userId, t, c, originalName, savedName, contentType);
        } else {
            qnaRepository.updateQna(qnaId, userId, t, c);
        }
        return "redirect:/community/qna/" + qnaId;
    }

    // 사용처: 삭제 기능 (PostMapping /{qnaId}/delete)
    @PostMapping("/{qnaId}/delete")
    public String delete(@PathVariable Long qnaId, Authentication authentication) {
        var userId = currentUserId(authentication);
        if (userId == null) {
            return "redirect:/community/qna/" + qnaId;
        }
        qnaRepository.deleteQna(qnaId, userId);
        return "redirect:/community/qna";
    }


    // 사용처: 수정 화면 진입 기능 (GetMapping /{qnaId}/comments/{commentId}/edit)
    @GetMapping("/{qnaId}/comments/{commentId}/edit")
    public String editCommentPage(@PathVariable Long qnaId,
                                  @PathVariable Long commentId,
                                  Authentication authentication,
                                  Model model) {
        if (!isAdminOrBusiness(authentication)) {
            return "redirect:/community/qna/" + qnaId;
        }
        var commentOpt = qnaRepository.findCommentById(commentId);
        if (commentOpt.isEmpty() || !commentOpt.get().qnaId().equals(qnaId)) {
            return "redirect:/community/qna/" + qnaId;
        }
        var comment = commentOpt.get();
        if (!authentication.getName().equals(comment.authorUsername())) {
            return "redirect:/community/qna/" + qnaId;
        }

        model.addAttribute("isAuthenticated", true);
        model.addAttribute("qnaId", qnaId);
        model.addAttribute("comment", comment);
        return "community/qna-comment-edit";
    }

    // 사용처: 수정 저장 기능 (PostMapping /{qnaId}/comments/{commentId}/edit)
    @PostMapping("/{qnaId}/comments/{commentId}/edit")
    public String editComment(@PathVariable Long qnaId,
                              @PathVariable Long commentId,
                              @RequestParam String content,
                              Authentication authentication) {
        if (!isAdminOrBusiness(authentication)) {
            return "redirect:/community/qna/" + qnaId;
        }
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isEmpty()) {
            return "redirect:/community/qna/" + qnaId + "/comments/" + commentId + "/edit";
        }
        qnaRepository.updateComment(commentId, authentication.getName(), trimmed);
        return "redirect:/community/qna/" + qnaId;
    }

    // 사용처: 삭제 기능 (PostMapping /{qnaId}/comments/{commentId}/delete)
    @PostMapping("/{qnaId}/comments/{commentId}/delete")
    public String deleteComment(@PathVariable Long qnaId,
                                @PathVariable Long commentId,
                                Authentication authentication) {
        if (!isAdminOrBusiness(authentication)) {
            return "redirect:/community/qna/" + qnaId;
        }
        qnaRepository.deleteComment(commentId, authentication.getName());
        return "redirect:/community/qna/" + qnaId;
    }

    private Long currentUserId(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return null;
        }
        return userRepository.findIdByUsername(authentication.getName()).orElse(null);
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private boolean isNormalAuthenticatedUser(Authentication authentication) {
        if (!isAuthenticated(authentication)) return false;
        boolean isBusiness = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_INSTRUCTOR"::equals);
        return !isBusiness;
    }

    private boolean isAdminOrBusiness(Authentication authentication) {
        if (!isAuthenticated(authentication)) return false;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_ADMIN".equals(a) || "ROLE_INSTRUCTOR".equals(a));
    }
}

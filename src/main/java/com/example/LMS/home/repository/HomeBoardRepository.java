package com.example.LMS.home.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 메인/커뮤니티 보드 데이터 접근 Repository
 *
 * 핵심 설계 포인트:
 * - "고정 공지 2개" 정책을 SQL에서 직접 분리 처리
 * - 목록에서 댓글 수를 바로 보여주기 위해 comment_count를 함께 조회
 */
@Repository
public class HomeBoardRepository {

    /** 메인용 최신 공지 조회 (고정 포함) */
    private static final String SELECT_NOTICES_SQL = """
            SELECT n.id, n.title, n.is_pinned, n.created_at,
                   (SELECT COUNT(*) FROM notice_comments nc WHERE nc.notice_id = n.id) AS comment_count
            FROM notices n
            ORDER BY n.id DESC
            LIMIT ?
            """;

    /** 메인용 최신 공지 조회 (고정 2개 제외) */
    private static final String SELECT_LATEST_NOTICES_EXCEPT_PINNED_SQL = """
            SELECT n.id, n.title, n.is_pinned, n.created_at,
                   (SELECT COUNT(*) FROM notice_comments nc WHERE nc.notice_id = n.id) AS comment_count
            FROM notices n
            WHERE n.is_pinned = FALSE
            ORDER BY n.id DESC
            LIMIT ?
            """;

    /** 메인용 최신 수강평 */
    private static final String SELECT_REVIEWS_SQL = """
            SELECT r.id, r.course_id, c.title AS course_title, r.writer_name, r.rating, r.content, r.created_at
            FROM course_reviews r
            JOIN courses c ON c.id = r.course_id
            ORDER BY r.id DESC
            LIMIT ?
            """;

    /** 공지 고정 2건 (최신순 기준) */
    private static final String SELECT_PINNED_NOTICES_SQL = """
            SELECT n.id, n.title, n.is_pinned, n.created_at,
                   (SELECT COUNT(*) FROM notice_comments nc WHERE nc.notice_id = n.id) AS comment_count
            FROM notices n
            WHERE n.is_pinned = TRUE
            ORDER BY n.id DESC
            """;

    /** 공지 목록 페이징 (고정 2건 제외) */
    private static final String SELECT_NOTICES_PAGED_SQL = """
            SELECT n.id, n.title, n.is_pinned, n.created_at,
                   (SELECT COUNT(*) FROM notice_comments nc WHERE nc.notice_id = n.id) AS comment_count
            FROM notices n
            WHERE n.is_pinned = FALSE
            ORDER BY n.id DESC
            LIMIT ? OFFSET ?
            """;

    /** 고정 제외 공지 개수 */
    private static final String COUNT_NOTICES_EXCEPT_PINNED_SQL = """
            SELECT COUNT(*)
            FROM notices n
            WHERE n.is_pinned = FALSE
            """;

    /** 공지 상세 */
    private static final String SELECT_NOTICE_DETAIL_SQL = """
            SELECT id, title, content, is_pinned, created_at
            FROM notices
            WHERE id = ?
            """;

    /** 댓글 목록 (최신순) */
    private static final String SELECT_NOTICE_COMMENTS_PAGED_SQL = """
            SELECT nc.id, nc.notice_id, nc.user_id, u.name AS user_name, nc.content, nc.created_at
            FROM notice_comments nc
            JOIN users u ON u.id = nc.user_id
            WHERE nc.notice_id = ?
            ORDER BY nc.id DESC
            LIMIT ? OFFSET ?
            """;

    /** 댓글 개수 */
    private static final String COUNT_NOTICE_COMMENTS_SQL = """
            SELECT COUNT(*)
            FROM notice_comments
            WHERE notice_id = ?
            """;

    /** 댓글 저장 */
    private static final String INSERT_NOTICE_COMMENT_SQL = """
            INSERT INTO notice_comments (notice_id, user_id, content)
            VALUES (?, ?, ?)
            """;

    /** 댓글 단건 조회 (수정 화면 진입용) */
    private static final String SELECT_NOTICE_COMMENT_BY_ID_SQL = """
            SELECT id, notice_id, user_id, content, created_at
            FROM notice_comments
            WHERE id = ?
            """;

    /** 본인 댓글만 수정 */
    private static final String UPDATE_NOTICE_COMMENT_SQL = """
            UPDATE notice_comments
            SET content = ?
            WHERE id = ? AND user_id = ?
            """;

    /** 본인 댓글만 삭제 */

    private static final String COUNT_NOTICE_PIN_COLUMN_SQL = """
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'notices'
              AND column_name = 'is_pinned'
            """;

    private static final String ALTER_NOTICE_PIN_COLUMN_SQL = """
            ALTER TABLE notices
            ADD COLUMN is_pinned BOOLEAN NOT NULL DEFAULT FALSE
            """;

    private static final String INSERT_NOTICE_SQL = """
            INSERT INTO notices (course_id, author_id, title, content, is_pinned)
            VALUES (NULL, ?, ?, ?, FALSE)
            """;

    private static final String UPDATE_NOTICE_SQL = """
            UPDATE notices
            SET title = ?, content = ?
            WHERE id = ?
            """;

    private static final String DELETE_NOTICE_SQL = """
            DELETE FROM notices
            WHERE id = ?
            """;

    private static final String PIN_NOTICE_SQL = """
            UPDATE notices
            SET is_pinned = TRUE
            WHERE id = ?
            """;

    private static final String UNPIN_NOTICE_SQL = """
            UPDATE notices
            SET is_pinned = FALSE
            WHERE id = ?
            """;
    private static final String DELETE_NOTICE_COMMENT_SQL = """
            DELETE FROM notice_comments
            WHERE id = ? AND user_id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public HomeBoardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private void ensureNoticePinnedColumn() {
        Integer count = jdbcTemplate.queryForObject(COUNT_NOTICE_PIN_COLUMN_SQL, Integer.class);
        if (count == null || count == 0) {
            jdbcTemplate.execute(ALTER_NOTICE_PIN_COLUMN_SQL);
        }
    }

    public List<NoticeItem> findLatestNotices(int limit) {
        ensureNoticePinnedColumn();
        return jdbcTemplate.query(SELECT_NOTICES_SQL, (rs, rowNum) -> new NoticeItem(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getBoolean("is_pinned"),
                rs.getInt("comment_count"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), limit);
    }

    public List<NoticeItem> findLatestNoticesExceptPinned(int limit) {
        ensureNoticePinnedColumn();
        return jdbcTemplate.query(SELECT_LATEST_NOTICES_EXCEPT_PINNED_SQL, (rs, rowNum) -> new NoticeItem(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getBoolean("is_pinned"),
                rs.getInt("comment_count"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), limit);
    }

    public List<ReviewItem> findLatestReviews(int limit) {
        return jdbcTemplate.query(SELECT_REVIEWS_SQL, (rs, rowNum) -> new ReviewItem(
                rs.getLong("id"),
                rs.getLong("course_id"),
                rs.getString("course_title"),
                rs.getString("writer_name"),
                rs.getInt("rating"),
                rs.getString("content"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), limit);
    }

    public List<NoticeItem> findPinnedNotices() {
        ensureNoticePinnedColumn();
        return jdbcTemplate.query(SELECT_PINNED_NOTICES_SQL, (rs, rowNum) -> new NoticeItem(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getBoolean("is_pinned"),
                rs.getInt("comment_count"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ));
    }

    public List<NoticeItem> findNoticesPaged(int limit, int offset) {
        ensureNoticePinnedColumn();
        return jdbcTemplate.query(SELECT_NOTICES_PAGED_SQL, (rs, rowNum) -> new NoticeItem(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getBoolean("is_pinned"),
                rs.getInt("comment_count"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), limit, offset);
    }

    public int countNoticesExceptPinned() {
        ensureNoticePinnedColumn();
        Integer count = jdbcTemplate.queryForObject(COUNT_NOTICES_EXCEPT_PINNED_SQL, Integer.class);
        return count == null ? 0 : count;
    }

    public Optional<NoticeDetail> findNoticeDetail(Long noticeId) {
        ensureNoticePinnedColumn();
        List<NoticeDetail> list = jdbcTemplate.query(SELECT_NOTICE_DETAIL_SQL, (rs, rowNum) -> new NoticeDetail(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getBoolean("is_pinned"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), noticeId);
        return list.stream().findFirst();
    }

    public List<NoticeCommentItem> findNoticeCommentsPaged(Long noticeId, int limit, int offset) {
        return jdbcTemplate.query(SELECT_NOTICE_COMMENTS_PAGED_SQL, (rs, rowNum) -> new NoticeCommentItem(
                rs.getLong("id"),
                rs.getLong("notice_id"),
                rs.getLong("user_id"),
                rs.getString("user_name"),
                rs.getString("content"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), noticeId, limit, offset);
    }

    public int countNoticeComments(Long noticeId) {
        Integer count = jdbcTemplate.queryForObject(COUNT_NOTICE_COMMENTS_SQL, Integer.class, noticeId);
        return count == null ? 0 : count;
    }

    public void saveNoticeComment(Long noticeId, Long userId, String content) {
        jdbcTemplate.update(INSERT_NOTICE_COMMENT_SQL, noticeId, userId, content);
    }

    /** 댓글 단건 조회 */
    public Optional<NoticeCommentDetail> findNoticeCommentById(Long commentId) {
        List<NoticeCommentDetail> list = jdbcTemplate.query(SELECT_NOTICE_COMMENT_BY_ID_SQL, (rs, rowNum) -> new NoticeCommentDetail(
                rs.getLong("id"),
                rs.getLong("notice_id"),
                rs.getLong("user_id"),
                rs.getString("content"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), commentId);
        return list.stream().findFirst();
    }

    /** 본인 댓글 수정. 반영 건수(0/1) 반환 */
    public int updateNoticeComment(Long commentId, Long userId, String content) {
        return jdbcTemplate.update(UPDATE_NOTICE_COMMENT_SQL, content, commentId, userId);
    }

    /** 본인 댓글 삭제. 반영 건수(0/1) 반환 */
    public int deleteNoticeComment(Long commentId, Long userId) {
        return jdbcTemplate.update(DELETE_NOTICE_COMMENT_SQL, commentId, userId);
    }


    public Long saveNotice(Long authorId, String title, String content) {
        ensureNoticePinnedColumn();
        jdbcTemplate.update(INSERT_NOTICE_SQL, authorId, title, content);
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return id;
    }

    public int updateNotice(Long noticeId, String title, String content) {
        ensureNoticePinnedColumn();
        return jdbcTemplate.update(UPDATE_NOTICE_SQL, title, content, noticeId);
    }

    public int deleteNotice(Long noticeId) {
        ensureNoticePinnedColumn();
        jdbcTemplate.update("DELETE FROM notice_comments WHERE notice_id = ?", noticeId);
        return jdbcTemplate.update(DELETE_NOTICE_SQL, noticeId);
    }

    public int pinNotice(Long noticeId) {
        ensureNoticePinnedColumn();
        return jdbcTemplate.update(PIN_NOTICE_SQL, noticeId);
    }

    public int unpinNotice(Long noticeId) {
        ensureNoticePinnedColumn();
        return jdbcTemplate.update(UNPIN_NOTICE_SQL, noticeId);
    }

    /** 공지 목록 아이템(제목 + 댓글수 + 작성일) */
    public record NoticeItem(Long id, String title, boolean pinned, int commentCount, LocalDateTime createdAt) {}

    /** 공지 상세 아이템 */
    public record NoticeDetail(Long id, String title, String content, boolean pinned, LocalDateTime createdAt) {}

    /** 댓글 목록 아이템 */
    public record NoticeCommentItem(Long id, Long noticeId, Long userId, String userName, String content, LocalDateTime createdAt) {}

    /** 댓글 수정 화면/권한 확인용 단건 아이템 */
    public record NoticeCommentDetail(Long id, Long noticeId, Long userId, String content, LocalDateTime createdAt) {}

    /** 메인 수강평 아이템 */
    public record ReviewItem(Long id, Long courseId, String courseTitle, String writerName, int rating, String content, LocalDateTime createdAt) {}
}

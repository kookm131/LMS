package com.example.LMS.home.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class QnaRepository {

    private static final String CREATE_QNA_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS qna_questions (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id BIGINT NULL,
                title VARCHAR(200) NOT NULL,
                content TEXT NOT NULL,
                view_count INT NOT NULL DEFAULT 0,
                attachment_original_name VARCHAR(255) NULL,
                attachment_saved_name VARCHAR(255) NULL,
                attachment_content_type VARCHAR(100) NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """;

    private static final String CREATE_QNA_COMMENTS_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS qna_comments (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                qna_id BIGINT NOT NULL,
                author_username VARCHAR(50) NOT NULL,
                content TEXT NOT NULL,
                is_accepted BOOLEAN NOT NULL DEFAULT FALSE,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """;

    private static final String COUNT_COLUMN_SQL = """
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'qna_questions'
              AND column_name = ?
            """;

    private static final String SELECT_TOP_FAQ_SQL = """
            SELECT id, title, view_count, created_at
            FROM qna_questions
            ORDER BY view_count DESC, id DESC
            LIMIT 5
            """;

    private static final String SELECT_QNA_PAGED_SQL = """
            SELECT id, title, view_count, created_at
            FROM qna_questions
            ORDER BY id DESC
            LIMIT ? OFFSET ?
            """;

    private static final String COUNT_QNA_SQL = """
            SELECT COUNT(*)
            FROM qna_questions
            """;

    private static final String INSERT_QNA_SQL = """
            INSERT INTO qna_questions (
                user_id, title, content, view_count,
                attachment_original_name, attachment_saved_name, attachment_content_type
            ) VALUES (?, ?, ?, 0, ?, ?, ?)
            """;

    private static final String UPDATE_VIEW_COUNT_SQL = """
            UPDATE qna_questions
            SET view_count = view_count + 1
            WHERE id = ?
            """;

    private static final String SELECT_QNA_DETAIL_SQL = """
            SELECT q.id,
                   q.user_id,
                   q.title,
                   q.content,
                   q.view_count,
                   q.attachment_original_name,
                   q.attachment_saved_name,
                   q.attachment_content_type,
                   q.created_at,
                   u.username AS author_username,
                   u.name AS author_name
            FROM qna_questions q
            LEFT JOIN users u ON u.id = q.user_id
            WHERE q.id = ?
            """;

    private static final String SELECT_ACCEPTED_COMMENT_SQL = """
            SELECT id, qna_id, author_username, content, is_accepted, created_at
            FROM qna_comments
            WHERE qna_id = ? AND is_accepted = TRUE
            ORDER BY id DESC
            LIMIT 1
            """;

    private static final String SELECT_QNA_COMMENTS_PAGED_SQL = """
            SELECT id, qna_id, author_username, content, is_accepted, created_at
            FROM qna_comments
            WHERE qna_id = ?
            ORDER BY id DESC
            LIMIT ? OFFSET ?
            """;

    private static final String COUNT_QNA_COMMENTS_SQL = """
            SELECT COUNT(*)
            FROM qna_comments
            WHERE qna_id = ?
            """;


    private static final String SELECT_QNA_COMMENT_SQL = """
            SELECT id, qna_id, author_username, content, is_accepted, created_at
            FROM qna_comments
            WHERE id = ?
            """;

    private static final String UPDATE_QNA_COMMENT_SQL = """
            UPDATE qna_comments
            SET content = ?
            WHERE id = ? AND author_username = ?
            """;

    private static final String DELETE_QNA_COMMENT_SQL = """
            DELETE FROM qna_comments
            WHERE id = ? AND author_username = ?
            """;
    private static final String INSERT_QNA_COMMENT_SQL = """
            INSERT INTO qna_comments (qna_id, author_username, content, is_accepted)
            VALUES (?, ?, ?, FALSE)
            """;

    private static final String SELECT_COMMENT_BY_ID_SQL = """
            SELECT id, qna_id
            FROM qna_comments
            WHERE id = ?
            """;

    private static final String RESET_ACCEPTED_SQL = """
            UPDATE qna_comments
            SET is_accepted = FALSE
            WHERE qna_id = ?
            """;


    private static final String UNACCEPT_ALL_SQL = """
            UPDATE qna_comments
            SET is_accepted = FALSE
            WHERE qna_id = ?
            """;

    private static final String UPDATE_QNA_SQL = """
            UPDATE qna_questions
            SET title = ?, content = ?
            WHERE id = ? AND user_id = ?
            """;

    private static final String UPDATE_QNA_WITH_ATTACHMENT_SQL = """
            UPDATE qna_questions
            SET title = ?, content = ?,
                attachment_original_name = ?,
                attachment_saved_name = ?,
                attachment_content_type = ?
            WHERE id = ? AND user_id = ?
            """;

    private static final String DELETE_QNA_COMMENTS_SQL = """
            DELETE FROM qna_comments
            WHERE qna_id = ?
            """;

    private static final String DELETE_QNA_SQL = """
            DELETE FROM qna_questions
            WHERE id = ? AND user_id = ?
            """;
    private static final String ACCEPT_COMMENT_SQL = """
            UPDATE qna_comments
            SET is_accepted = TRUE
            WHERE id = ? AND qna_id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public QnaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private void ensureTable() {
        jdbcTemplate.execute(CREATE_QNA_TABLE_SQL);
        ensureColumn("attachment_original_name", "ALTER TABLE qna_questions ADD COLUMN attachment_original_name VARCHAR(255) NULL");
        ensureColumn("attachment_saved_name", "ALTER TABLE qna_questions ADD COLUMN attachment_saved_name VARCHAR(255) NULL");
        ensureColumn("attachment_content_type", "ALTER TABLE qna_questions ADD COLUMN attachment_content_type VARCHAR(100) NULL");
        jdbcTemplate.execute(CREATE_QNA_COMMENTS_TABLE_SQL);
    }

    private void ensureColumn(String columnName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject(COUNT_COLUMN_SQL, Integer.class, columnName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(alterSql);
        }
    }

    public List<QnaItem> findTopFaqs() {
        ensureTable();
        return jdbcTemplate.query(SELECT_TOP_FAQ_SQL, (rs, rowNum) -> new QnaItem(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getInt("view_count"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ));
    }

    public List<QnaItem> findQnaPaged(int limit, int offset) {
        ensureTable();
        return jdbcTemplate.query(SELECT_QNA_PAGED_SQL, (rs, rowNum) -> new QnaItem(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getInt("view_count"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), limit, offset);
    }

    public int countQna() {
        ensureTable();
        Integer count = jdbcTemplate.queryForObject(COUNT_QNA_SQL, Integer.class);
        return count == null ? 0 : count;
    }

    public Long saveQnaAndReturnId(Long userId,
                                   String title,
                                   String content,
                                   String attachmentOriginalName,
                                   String attachmentSavedName,
                                   String attachmentContentType) {
        ensureTable();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(INSERT_QNA_SQL, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, userId);
            ps.setString(2, title);
            ps.setString(3, content);
            ps.setString(4, attachmentOriginalName);
            ps.setString(5, attachmentSavedName);
            ps.setString(6, attachmentContentType);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void increaseViewCount(Long qnaId) {
        ensureTable();
        jdbcTemplate.update(UPDATE_VIEW_COUNT_SQL, qnaId);
    }

    public Optional<QnaDetail> findQnaDetail(Long qnaId) {
        ensureTable();
        List<QnaDetail> list = jdbcTemplate.query(SELECT_QNA_DETAIL_SQL, (rs, rowNum) -> new QnaDetail(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("author_username"),
                rs.getString("author_name"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getInt("view_count"),
                rs.getString("attachment_original_name"),
                rs.getString("attachment_saved_name"),
                rs.getString("attachment_content_type"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), qnaId);
        return list.stream().findFirst();
    }

    public Optional<QnaCommentItem> findAcceptedComment(Long qnaId) {
        ensureTable();
        List<QnaCommentItem> list = jdbcTemplate.query(SELECT_ACCEPTED_COMMENT_SQL, (rs, rowNum) -> new QnaCommentItem(
                rs.getLong("id"),
                rs.getLong("qna_id"),
                rs.getString("author_username"),
                rs.getString("content"),
                rs.getBoolean("is_accepted"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), qnaId);
        return list.stream().findFirst();
    }

    public List<QnaCommentItem> findQnaCommentsPaged(Long qnaId, int limit, int offset) {
        ensureTable();
        return jdbcTemplate.query(SELECT_QNA_COMMENTS_PAGED_SQL, (rs, rowNum) -> new QnaCommentItem(
                rs.getLong("id"),
                rs.getLong("qna_id"),
                rs.getString("author_username"),
                rs.getString("content"),
                rs.getBoolean("is_accepted"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), qnaId, limit, offset);
    }

    public int countQnaComments(Long qnaId) {
        ensureTable();
        Integer count = jdbcTemplate.queryForObject(COUNT_QNA_COMMENTS_SQL, Integer.class, qnaId);
        return count == null ? 0 : count;
    }

    public void saveQnaComment(Long qnaId, String authorUsername, String content) {
        ensureTable();
        jdbcTemplate.update(INSERT_QNA_COMMENT_SQL, qnaId, authorUsername, content);
    }

    public boolean isCommentInQna(Long commentId, Long qnaId) {
        ensureTable();
        List<Long> list = jdbcTemplate.query(SELECT_COMMENT_BY_ID_SQL,
                (rs, rowNum) -> rs.getLong("qna_id"),
                commentId);
        return !list.isEmpty() && list.get(0).equals(qnaId);
    }

    public void acceptComment(Long qnaId, Long commentId) {
        ensureTable();
        jdbcTemplate.update(RESET_ACCEPTED_SQL, qnaId);
        jdbcTemplate.update(ACCEPT_COMMENT_SQL, commentId, qnaId);
    }


    public void unacceptComment(Long qnaId) {
        ensureTable();
        jdbcTemplate.update(UNACCEPT_ALL_SQL, qnaId);
    }

    public int updateQna(Long qnaId, Long userId, String title, String content) {
        ensureTable();
        return jdbcTemplate.update(UPDATE_QNA_SQL, title, content, qnaId, userId);
    }

    public int updateQnaWithAttachment(Long qnaId,
                                       Long userId,
                                       String title,
                                       String content,
                                       String attachmentOriginalName,
                                       String attachmentSavedName,
                                       String attachmentContentType) {
        ensureTable();
        return jdbcTemplate.update(UPDATE_QNA_WITH_ATTACHMENT_SQL,
                title, content,
                attachmentOriginalName, attachmentSavedName, attachmentContentType,
                qnaId, userId);
    }

    public int deleteQna(Long qnaId, Long userId) {
        ensureTable();
        jdbcTemplate.update(DELETE_QNA_COMMENTS_SQL, qnaId);
        return jdbcTemplate.update(DELETE_QNA_SQL, qnaId, userId);
    }

    public Optional<QnaCommentItem> findCommentById(Long commentId) {
        ensureTable();
        List<QnaCommentItem> list = jdbcTemplate.query(SELECT_QNA_COMMENT_SQL, (rs, rowNum) -> new QnaCommentItem(
                rs.getLong("id"),
                rs.getLong("qna_id"),
                rs.getString("author_username"),
                rs.getString("content"),
                rs.getBoolean("is_accepted"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), commentId);
        return list.stream().findFirst();
    }

    public int updateComment(Long commentId, String authorUsername, String content) {
        ensureTable();
        return jdbcTemplate.update(UPDATE_QNA_COMMENT_SQL, content, commentId, authorUsername);
    }

    public int deleteComment(Long commentId, String authorUsername) {
        ensureTable();
        return jdbcTemplate.update(DELETE_QNA_COMMENT_SQL, commentId, authorUsername);
    }
    public record QnaItem(Long id, String title, int viewCount, LocalDateTime createdAt) {}

    public record QnaDetail(Long id,
                            Long userId,
                            String authorUsername,
                            String authorName,
                            String title,
                            String content,
                            int viewCount,
                            String attachmentOriginalName,
                            String attachmentSavedName,
                            String attachmentContentType,
                            LocalDateTime createdAt) {}

    public record QnaCommentItem(Long id,
                                 Long qnaId,
                                 String authorUsername,
                                 String content,
                                 boolean accepted,
                                 LocalDateTime createdAt) {}
}

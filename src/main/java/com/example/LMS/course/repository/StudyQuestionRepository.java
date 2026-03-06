package com.example.LMS.course.repository;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StudyQuestionRepository {

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS study_questions (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id BIGINT NOT NULL,
                course_id BIGINT NOT NULL,
                question_text VARCHAR(1000) NOT NULL,
                status ENUM('OPEN','DONE') NOT NULL DEFAULT 'OPEN',
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """;

    private static final String FIND_BY_COURSE_SQL = """
            SELECT id, question_text, status, created_at
            FROM study_questions
            WHERE user_id = ? AND course_id = ?
            ORDER BY id DESC
            """;

    private static final String INSERT_SQL = """
            INSERT INTO study_questions (user_id, course_id, question_text, status)
            VALUES (?, ?, ?, 'OPEN')
            """;

    private static final String UPDATE_STATUS_SQL = """
            UPDATE study_questions
            SET status = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND user_id = ? AND course_id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public StudyQuestionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        jdbcTemplate.execute(CREATE_TABLE_SQL);
    }

    public List<StudyQuestionItem> findByCourse(Long userId, Long courseId) {
        return jdbcTemplate.query(FIND_BY_COURSE_SQL, (rs, rowNum) -> new StudyQuestionItem(
                rs.getLong("id"),
                rs.getString("question_text"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), userId, courseId);
    }

    public void addQuestion(Long userId, Long courseId, String questionText) {
        jdbcTemplate.update(INSERT_SQL, userId, courseId, questionText);
    }

    public void updateStatus(Long userId, Long courseId, Long questionId, String status) {
        jdbcTemplate.update(UPDATE_STATUS_SQL, status, questionId, userId, courseId);
    }

    public record StudyQuestionItem(Long id, String questionText, String status, java.time.LocalDateTime createdAt) {}
}

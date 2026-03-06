package com.example.LMS.course.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ExamAttemptRepository {

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS exam_attempts (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                student_id BIGINT NOT NULL,
                course_id BIGINT NOT NULL,
                score DOUBLE NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                UNIQUE KEY uq_exam_attempt_student_course (student_id, course_id)
            )
            """;

    private static final String UPSERT_SCORE_SQL = """
            INSERT INTO exam_attempts (student_id, course_id, score)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE score = VALUES(score)
            """;

    private static final String SELECT_SCORE_SQL = """
            SELECT score
            FROM exam_attempts
            WHERE student_id = ? AND course_id = ?
            LIMIT 1
            """;

    private final JdbcTemplate jdbcTemplate;

    public ExamAttemptRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private void ensureTable() {
        jdbcTemplate.execute(CREATE_TABLE_SQL);
    }

    public void saveScore(Long studentId, Long courseId, double score) {
        ensureTable();
        jdbcTemplate.update(UPSERT_SCORE_SQL, studentId, courseId, score);
    }

    public Double findScore(Long studentId, Long courseId) {
        ensureTable();
        return jdbcTemplate.query(SELECT_SCORE_SQL,
                rs -> rs.next() ? rs.getDouble("score") : null,
                studentId, courseId);
    }
}

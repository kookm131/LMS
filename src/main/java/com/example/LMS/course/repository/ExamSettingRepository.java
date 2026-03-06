package com.example.LMS.course.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;


@Repository
public class ExamSettingRepository {

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS exam_settings (
                course_id BIGINT PRIMARY KEY,
                pass_score INT NOT NULL DEFAULT 60,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """;

    private static final String SELECT_PASS_SCORE_SQL = """
            SELECT pass_score
            FROM exam_settings
            WHERE course_id = ?
            LIMIT 1
            """;

    private static final String UPSERT_PASS_SCORE_SQL = """
            INSERT INTO exam_settings (course_id, pass_score)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE pass_score = VALUES(pass_score)
            """;

    private final JdbcTemplate jdbcTemplate;

    public ExamSettingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private void ensureTable() {
        jdbcTemplate.execute(CREATE_TABLE_SQL);
    }

    public int getPassScoreOrDefault(Long courseId) {
        ensureTable();
        Integer passScore = jdbcTemplate.query(
                SELECT_PASS_SCORE_SQL,
                rs -> rs.next() ? rs.getInt("pass_score") : null,
                courseId
        );

        if (passScore == null) {
            setPassScore(courseId, 60);
            return 60;
        }
        return passScore;
    }

    public void setPassScore(Long courseId, int passScore) {
        ensureTable();
        jdbcTemplate.update(UPSERT_PASS_SCORE_SQL, courseId, passScore);
    }
}

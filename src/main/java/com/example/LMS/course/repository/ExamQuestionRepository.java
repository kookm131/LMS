package com.example.LMS.course.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ExamQuestionRepository {

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS exam_questions (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                course_id BIGINT NOT NULL,
                question_text TEXT NOT NULL,
                reference_image_url VARCHAR(1000),
                option1 VARCHAR(500) NOT NULL,
                option2 VARCHAR(500) NOT NULL,
                option3 VARCHAR(500) NOT NULL,
                option4 VARCHAR(500) NOT NULL,
                correct_option TINYINT NOT NULL,
                sort_order INT NOT NULL DEFAULT 0,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_exam_questions_course (course_id)
            )
            """;

    private static final String SELECT_BY_COURSE_SQL = """
            SELECT id, course_id, question_text, reference_image_url,
                   option1, option2, option3, option4, correct_option, sort_order
            FROM exam_questions
            WHERE course_id = ?
            ORDER BY sort_order ASC, id ASC
            """;

    private static final String DELETE_BY_COURSE_SQL = """
            DELETE FROM exam_questions
            WHERE course_id = ?
            """;

    private static final String INSERT_SQL = """
            INSERT INTO exam_questions (
                course_id, question_text, reference_image_url,
                option1, option2, option3, option4,
                correct_option, sort_order
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String COUNT_BY_COURSE_SQL = """
            SELECT COUNT(*)
            FROM exam_questions
            WHERE course_id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public ExamQuestionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private void ensureTable() {
        jdbcTemplate.execute(CREATE_TABLE_SQL);
    }

    public List<ExamQuestionItem> findByCourse(Long courseId) {
        ensureTable();
        return jdbcTemplate.query(SELECT_BY_COURSE_SQL, (rs, rowNum) -> new ExamQuestionItem(
                rs.getLong("id"),
                rs.getLong("course_id"),
                rs.getString("question_text"),
                rs.getString("reference_image_url"),
                rs.getString("option1"),
                rs.getString("option2"),
                rs.getString("option3"),
                rs.getString("option4"),
                rs.getInt("correct_option"),
                rs.getInt("sort_order")
        ), courseId);
    }

    public int countByCourse(Long courseId) {
        ensureTable();
        Integer c = jdbcTemplate.queryForObject(COUNT_BY_COURSE_SQL, Integer.class, courseId);
        return c == null ? 0 : c;
    }

    public void replaceAll(Long courseId, List<ExamQuestionDraft> drafts) {
        ensureTable();
        jdbcTemplate.update(DELETE_BY_COURSE_SQL, courseId);
        for (int i = 0; i < drafts.size(); i++) {
            ExamQuestionDraft d = drafts.get(i);
            jdbcTemplate.update(INSERT_SQL,
                    courseId,
                    d.questionText(),
                    d.referenceImageUrl(),
                    d.option1(),
                    d.option2(),
                    d.option3(),
                    d.option4(),
                    d.correctOption(),
                    i + 1
            );
        }
    }

    public record ExamQuestionItem(
            Long id,
            Long courseId,
            String questionText,
            String referenceImageUrl,
            String option1,
            String option2,
            String option3,
            String option4,
            int correctOption,
            int sortOrder
    ) {}

    public record ExamQuestionDraft(
            String questionText,
            String referenceImageUrl,
            String option1,
            String option2,
            String option3,
            String option4,
            int correctOption
    ) {}
}

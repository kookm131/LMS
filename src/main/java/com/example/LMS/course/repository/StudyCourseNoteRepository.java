package com.example.LMS.course.repository;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class StudyCourseNoteRepository {

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS study_course_notes (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id BIGINT NOT NULL,
                course_id BIGINT NOT NULL,
                note_content TEXT,
                summary_line1 VARCHAR(500),
                summary_line2 VARCHAR(500),
                summary_line3 VARCHAR(500),
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                UNIQUE KEY uq_study_course_notes_user_course (user_id, course_id)
            )
            """;

    private static final String FIND_SQL = """
            SELECT note_content, summary_line1, summary_line2, summary_line3, updated_at
            FROM study_course_notes
            WHERE user_id = ? AND course_id = ?
            LIMIT 1
            """;

    private static final String UPSERT_NOTE_SQL = """
            INSERT INTO study_course_notes (user_id, course_id, note_content)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE
                note_content = VALUES(note_content),
                updated_at = CURRENT_TIMESTAMP
            """;

    private static final String UPSERT_SUMMARY_SQL = """
            INSERT INTO study_course_notes (user_id, course_id, summary_line1, summary_line2, summary_line3)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                summary_line1 = VALUES(summary_line1),
                summary_line2 = VALUES(summary_line2),
                summary_line3 = VALUES(summary_line3),
                updated_at = CURRENT_TIMESTAMP
            """;

    private final JdbcTemplate jdbcTemplate;

    public StudyCourseNoteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        jdbcTemplate.execute(CREATE_TABLE_SQL);
    }

    public Optional<StudyCourseNoteItem> findByUserAndCourse(Long userId, Long courseId) {
        List<StudyCourseNoteItem> list = jdbcTemplate.query(FIND_SQL, (rs, rowNum) -> new StudyCourseNoteItem(
                rs.getString("note_content"),
                rs.getString("summary_line1"),
                rs.getString("summary_line2"),
                rs.getString("summary_line3"),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime()
        ), userId, courseId);
        return list.stream().findFirst();
    }

    public void saveNote(Long userId, Long courseId, String noteContent) {
        jdbcTemplate.update(UPSERT_NOTE_SQL, userId, courseId, noteContent);
    }

    public void saveSummary(Long userId, Long courseId, String line1, String line2, String line3) {
        jdbcTemplate.update(UPSERT_SUMMARY_SQL, userId, courseId, line1, line2, line3);
    }

    public record StudyCourseNoteItem(String noteContent, String summaryLine1, String summaryLine2, String summaryLine3,
                                      java.time.LocalDateTime updatedAt) {}
}

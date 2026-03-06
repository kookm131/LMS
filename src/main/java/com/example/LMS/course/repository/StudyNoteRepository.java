package com.example.LMS.course.repository;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class StudyNoteRepository {

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS study_notes (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id BIGINT NOT NULL,
                course_id BIGINT NOT NULL,
                lecture_id BIGINT NOT NULL,
                note_content TEXT,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                UNIQUE KEY uq_study_notes_user_course_lecture (user_id, course_id, lecture_id)
            )
            """;

    private static final String FIND_NOTE_SQL = """
            SELECT lecture_id, note_content, updated_at
            FROM study_notes
            WHERE user_id = ? AND course_id = ? AND lecture_id = ?
            LIMIT 1
            """;

    private static final String FIND_NOTES_BY_COURSE_SQL = """
            SELECT lecture_id, note_content, updated_at
            FROM study_notes
            WHERE user_id = ? AND course_id = ?
            ORDER BY lecture_id ASC
            """;

    private static final String UPSERT_NOTE_SQL = """
            INSERT INTO study_notes (user_id, course_id, lecture_id, note_content)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                note_content = VALUES(note_content),
                updated_at = CURRENT_TIMESTAMP
            """;

    private final JdbcTemplate jdbcTemplate;

    public StudyNoteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        ensureTable();
    }

    private void ensureTable() {
        jdbcTemplate.execute(CREATE_TABLE_SQL);
    }


    public Optional<StudyNoteItem> findNote(Long userId, Long courseId, Long lectureId) {
        return withMissingTableRecovery(() -> {
            List<StudyNoteItem> list = jdbcTemplate.query(FIND_NOTE_SQL, (rs, rowNum) -> new StudyNoteItem(
                    rs.getLong("lecture_id"),
                    rs.getString("note_content"),
                    rs.getTimestamp("updated_at").toLocalDateTime()
            ), userId, courseId, lectureId);
            return list.stream().findFirst();
        });
    }

    public List<StudyNoteItem> findNotesByCourse(Long userId, Long courseId) {
        return withMissingTableRecovery(() -> jdbcTemplate.query(FIND_NOTES_BY_COURSE_SQL, (rs, rowNum) -> new StudyNoteItem(
                rs.getLong("lecture_id"),
                rs.getString("note_content"),
                rs.getTimestamp("updated_at").toLocalDateTime()
        ), userId, courseId));
    }

    public void save(Long userId, Long courseId, Long lectureId, String noteContent) {
        withMissingTableRecovery(() -> {
            jdbcTemplate.update(UPSERT_NOTE_SQL, userId, courseId, lectureId, noteContent);
            return null;
        });
    }

    private <T> T withMissingTableRecovery(java.util.concurrent.Callable<T> action) {
        try {
            return action.call();
        } catch (BadSqlGrammarException e) {
            if (e.getMessage() != null && e.getMessage().contains("study_notes")) {
                ensureTable();
                try {
                    return action.call();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public record StudyNoteItem(Long lectureId, String noteContent, java.time.LocalDateTime updatedAt) {}
}

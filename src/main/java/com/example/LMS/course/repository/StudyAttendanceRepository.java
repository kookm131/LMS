package com.example.LMS.course.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StudyAttendanceRepository {

    private static final String CREATE_PROGRESS_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS study_lecture_progress (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id BIGINT NOT NULL,
                course_id BIGINT NOT NULL,
                lecture_id BIGINT NOT NULL,
                status ENUM('STARTED','COMPLETED') NOT NULL DEFAULT 'STARTED',
                started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                completed_at DATETIME NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                UNIQUE KEY uq_study_lecture_progress_user_course_lecture (user_id, course_id, lecture_id),
                INDEX idx_study_lecture_progress_course_user (course_id, user_id)
            )
            """;

    private static final String CREATE_ATTENDANCE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS attendance_daily (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                course_id BIGINT NOT NULL,
                student_id BIGINT NOT NULL,
                attendance_date DATE NOT NULL,
                status ENUM('ATTEND','LATE') NOT NULL DEFAULT 'LATE',
                last_checked_at DATETIME NOT NULL,
                last_lecture_id BIGINT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                UNIQUE KEY uq_attendance_daily_course_student_date (course_id, student_id, attendance_date),
                INDEX idx_attendance_daily_course_checked (course_id, last_checked_at)
            )
            """;

    private static final String COUNT_MISSING_PREVIOUS_SQL = """
            SELECT COUNT(*)
            FROM lectures prev
            LEFT JOIN study_lecture_progress p
              ON p.user_id = ?
             AND p.course_id = prev.course_id
             AND p.lecture_id = prev.id
             AND p.status = 'COMPLETED'
            WHERE prev.course_id = ?
              AND prev.sort_order < ?
              AND p.id IS NULL
            """;

    private static final String INSERT_STARTED_SQL = """
            INSERT INTO study_lecture_progress (user_id, course_id, lecture_id, status, started_at)
            VALUES (?, ?, ?, 'STARTED', NOW())
            ON DUPLICATE KEY UPDATE
                updated_at = CURRENT_TIMESTAMP
            """;

    private static final String EXISTS_COMPLETED_SQL = """
            SELECT COUNT(*)
            FROM study_lecture_progress
            WHERE user_id = ? AND course_id = ? AND lecture_id = ? AND status = 'COMPLETED'
            """;

    private static final String MARK_COMPLETED_SQL = """
            INSERT INTO study_lecture_progress (user_id, course_id, lecture_id, status, started_at, completed_at)
            VALUES (?, ?, ?, 'COMPLETED', NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                status = 'COMPLETED',
                completed_at = COALESCE(completed_at, NOW()),
                updated_at = CURRENT_TIMESTAMP
            """;

    private static final String UPSERT_DAILY_LATE_SQL = """
            INSERT INTO attendance_daily (course_id, student_id, attendance_date, status, last_checked_at, last_lecture_id)
            VALUES (?, ?, CURDATE(), 'LATE', NOW(), ?)
            ON DUPLICATE KEY UPDATE
                status = IF(status = 'ATTEND', 'ATTEND', 'LATE'),
                last_checked_at = IF(status = 'ATTEND', last_checked_at, NOW()),
                last_lecture_id = IF(status = 'ATTEND', last_lecture_id, VALUES(last_lecture_id)),
                updated_at = CURRENT_TIMESTAMP
            """;

    private static final String UPSERT_DAILY_ATTEND_SQL = """
            INSERT INTO attendance_daily (course_id, student_id, attendance_date, status, last_checked_at, last_lecture_id)
            VALUES (?, ?, CURDATE(), 'ATTEND', NOW(), ?)
            ON DUPLICATE KEY UPDATE
                status = 'ATTEND',
                last_checked_at = NOW(),
                last_lecture_id = VALUES(last_lecture_id),
                updated_at = CURRENT_TIMESTAMP
            """;

    private final JdbcTemplate jdbcTemplate;

    public StudyAttendanceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private void ensureTables() {
        jdbcTemplate.execute(CREATE_PROGRESS_TABLE_SQL);
        jdbcTemplate.execute(CREATE_ATTENDANCE_TABLE_SQL);
    }

    public boolean canAccessLectureSequential(Long userId, Long courseId, int lectureSortOrder) {
        ensureTables();
        Integer missing = jdbcTemplate.queryForObject(
                COUNT_MISSING_PREVIOUS_SQL,
                Integer.class,
                userId, courseId, lectureSortOrder
        );
        return missing == null || missing == 0;
    }

    public void markLectureStarted(Long userId, Long courseId, Long lectureId) {
        ensureTables();
        jdbcTemplate.update(INSERT_STARTED_SQL, userId, courseId, lectureId);
    }

    public boolean markLectureCompletedIfFirstTime(Long userId, Long courseId, Long lectureId) {
        ensureTables();
        Integer count = jdbcTemplate.queryForObject(EXISTS_COMPLETED_SQL, Integer.class, userId, courseId, lectureId);
        if (count != null && count > 0) {
            return false;
        }
        jdbcTemplate.update(MARK_COMPLETED_SQL, userId, courseId, lectureId);
        return true;
    }

    public void upsertDailyLate(Long userId, Long courseId, Long lectureId) {
        ensureTables();
        jdbcTemplate.update(UPSERT_DAILY_LATE_SQL, courseId, userId, lectureId);
    }

    public void upsertDailyAttend(Long userId, Long courseId, Long lectureId) {
        ensureTables();
        jdbcTemplate.update(UPSERT_DAILY_ATTEND_SQL, courseId, userId, lectureId);
    }
}

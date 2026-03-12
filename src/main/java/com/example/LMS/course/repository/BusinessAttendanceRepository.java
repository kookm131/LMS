package com.example.LMS.course.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class BusinessAttendanceRepository {

    private static final String CREATE_TABLE_SQL = """
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

    private static final String SELECT_BY_COURSE_OWNER_SQL = """
            SELECT a.id,
                   u.name AS student_name,
                   u.username AS student_username,
                   a.attendance_date,
                   a.status,
                   a.last_checked_at,
                   l.title AS lecture_title
            FROM attendance_daily a
            JOIN courses c ON c.id = a.course_id
            JOIN users u ON u.id = a.student_id
            LEFT JOIN lectures l ON l.id = a.last_lecture_id
            WHERE a.course_id = ?
              AND c.created_by_username = ?
            ORDER BY a.last_checked_at DESC, a.id DESC
            """;

    private final JdbcTemplate jdbcTemplate;

    public BusinessAttendanceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private void ensureTable() {
        jdbcTemplate.execute(CREATE_TABLE_SQL);
    }

    public List<AttendanceDetailItem> findByCourseOwner(Long courseId, String ownerUsername) {
        ensureTable();
        return jdbcTemplate.query(SELECT_BY_COURSE_OWNER_SQL, (rs, rowNum) -> new AttendanceDetailItem(
                rs.getLong("id"),
                rs.getString("student_name"),
                rs.getString("student_username"),
                rs.getDate("attendance_date").toLocalDate(),
                rs.getString("status"),
                rs.getTimestamp("last_checked_at").toLocalDateTime(),
                rs.getString("lecture_title")
        ), courseId, ownerUsername);
    }

    public record AttendanceDetailItem(Long id,
                                       String studentName,
                                       String studentUsername,
                                       LocalDate attendanceDate,
                                       String status,
                                       LocalDateTime lastCheckedAt,
                                       String lastLectureTitle) {}
}

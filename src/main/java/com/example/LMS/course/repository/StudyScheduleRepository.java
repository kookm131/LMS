package com.example.LMS.course.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class StudyScheduleRepository {

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS study_schedules (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id BIGINT NOT NULL,
                course_id BIGINT NOT NULL,
                day_of_week VARCHAR(10) NOT NULL,
                alarm_time VARCHAR(5) NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY uq_schedule_user_course (user_id, course_id)
            )
            """;

    private static final String DELETE_ALL_SQL = """
            DELETE FROM study_schedules
            WHERE user_id = ?
            """;

    private static final String INSERT_SQL = """
            INSERT INTO study_schedules (user_id, course_id, day_of_week, alarm_time)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE day_of_week = VALUES(day_of_week), alarm_time = VALUES(alarm_time)
            """;

    private static final String SELECT_BY_USER_SQL = """
            SELECT s.id, s.course_id, c.title, s.day_of_week, s.alarm_time, s.created_at
            FROM study_schedules s
            JOIN courses c ON c.id = s.course_id
            WHERE s.user_id = ?
            ORDER BY FIELD(s.day_of_week, '월','화','수','목','금','토','일'), s.alarm_time ASC, s.id DESC
            """;

    private final JdbcTemplate jdbcTemplate;

    public StudyScheduleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private void ensureTable() {
        jdbcTemplate.execute(CREATE_TABLE_SQL);
    }

    public void clearAll(Long userId) {
        ensureTable();
        jdbcTemplate.update(DELETE_ALL_SQL, userId);
    }

    public void save(Long userId, Long courseId, String dayOfWeek, String alarmTime) {
        ensureTable();
        jdbcTemplate.update(INSERT_SQL, userId, courseId, dayOfWeek, alarmTime);
    }

    public List<ScheduleItem> findByUser(Long userId) {
        ensureTable();
        return jdbcTemplate.query(SELECT_BY_USER_SQL, (rs, rowNum) -> new ScheduleItem(
                rs.getLong("id"),
                rs.getLong("course_id"),
                rs.getString("title"),
                rs.getString("day_of_week"),
                rs.getString("alarm_time"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), userId);
    }

    public record ScheduleItem(Long id, Long courseId, String courseTitle, String dayOfWeek, String alarmTime, LocalDateTime createdAt) {}
}

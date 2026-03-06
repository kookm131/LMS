package com.example.LMS.course.repository;

import com.example.LMS.course.model.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class CourseManageRepository {

    private static final String INSERT_COURSE_SQL = """
            INSERT INTO courses (category, title, instructor_name, description, content_text, created_by_username, status)
            VALUES (?, ?, ?, ?, ?, ?, 'PUBLISHED')
            """;

    private static final String INSERT_LECTURE_SQL = """
            INSERT INTO lectures (course_id, title, content_type, duration_sec, sort_order)
            VALUES (?, ?, 'VIDEO', ?, ?)
            """;

    private static final String UPDATE_COURSE_TIME_SQL = """
            UPDATE courses c
            JOIN (
                SELECT course_id,
                       SUM(duration_sec) AS total_sec,
                       COUNT(*) AS lecture_cnt
                FROM lectures
                WHERE course_id = ?
                GROUP BY course_id
            ) t ON c.id = t.course_id
            SET c.total_hours = CEIL(t.total_sec / 3600),
                c.avg_hours = ROUND((t.total_sec / 60) / t.lecture_cnt, 1)
            WHERE c.id = ?
            """;

    private static final String SELECT_OWNED_COURSE_SQL = """
            SELECT id, category, title, instructor_name, description, content_text
            FROM courses
            WHERE id = ? AND created_by_username = ?
            LIMIT 1
            """;

    private static final String SELECT_OUTLINE_SQL = """
            SELECT id, sort_order, title, IFNULL(duration_sec, 0) AS duration_sec
            FROM lectures
            WHERE course_id = ?
            ORDER BY sort_order ASC, id ASC
            """;

    private static final String UPDATE_COURSE_SQL = """
            UPDATE courses
            SET category = ?, title = ?, instructor_name = ?, description = ?, content_text = ?
            WHERE id = ? AND created_by_username = ?
            """;

    private static final String COUNT_OWNED_COURSE_SQL = """
            SELECT COUNT(*)
            FROM courses
            WHERE id = ? AND created_by_username = ?
            """;

    private static final String DELETE_ENROLLMENTS_BY_COURSE_SQL = """
            DELETE FROM enrollments WHERE course_id = ?
            """;

    private static final String DELETE_LECTURES_BY_COURSE_SQL = """
            DELETE FROM lectures WHERE course_id = ?
            """;

    private static final String DELETE_REVIEWS_BY_COURSE_SQL = """
            DELETE FROM course_reviews WHERE course_id = ?
            """;

    private static final String DELETE_COURSE_SQL = """
            DELETE FROM courses
            WHERE id = ? AND created_by_username = ?
            """;

    private static final String DELETE_COURSE_BY_ADMIN_SQL = """
            DELETE FROM courses
            WHERE id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public CourseManageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long saveCourse(String category, String title, String instructorName, String description, String contentText, String createdByUsername) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT_COURSE_SQL, new String[]{"id"});
            ps.setString(1, (category == null || category.isBlank()) ? "기타" : category);
            ps.setString(2, title);
            ps.setString(3, instructorName);
            ps.setString(4, description);
            ps.setString(5, contentText);
            ps.setString(6, createdByUsername);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void saveLectures(Long courseId, List<String> titles, List<Integer> minutes) {
        if (courseId == null || titles == null || minutes == null) return;

        int size = Math.min(titles.size(), minutes.size());
        int sortOrder = 1;
        for (int i = 0; i < size; i++) {
            String t = titles.get(i);
            Integer m = minutes.get(i);
            if (t == null || t.isBlank() || m == null || m <= 0) continue;
            jdbcTemplate.update(INSERT_LECTURE_SQL, courseId, t, m * 60, sortOrder++);
        }

        jdbcTemplate.update(UPDATE_COURSE_TIME_SQL, courseId, courseId);
    }

    public boolean isOwnedBy(Long courseId, String username) {
        Integer count = jdbcTemplate.queryForObject(COUNT_OWNED_COURSE_SQL, Integer.class, courseId, username);
        return count != null && count > 0;
    }

    public int deleteCourseByOwner(Long courseId, String username) {
        jdbcTemplate.update(DELETE_ENROLLMENTS_BY_COURSE_SQL, courseId);
        jdbcTemplate.update(DELETE_LECTURES_BY_COURSE_SQL, courseId);
        jdbcTemplate.update(DELETE_REVIEWS_BY_COURSE_SQL, courseId);
        return jdbcTemplate.update(DELETE_COURSE_SQL, courseId, username);
    }

    public int deleteCourseByAdmin(Long courseId) {
        jdbcTemplate.update(DELETE_ENROLLMENTS_BY_COURSE_SQL, courseId);
        jdbcTemplate.update(DELETE_LECTURES_BY_COURSE_SQL, courseId);
        jdbcTemplate.update(DELETE_REVIEWS_BY_COURSE_SQL, courseId);
        return jdbcTemplate.update(DELETE_COURSE_BY_ADMIN_SQL, courseId);
    }

    public record CourseEditItem(Long id, String category, String title, String instructorName, String description, String contentText) {}

    public Optional<CourseEditItem> findOwnedCourseForEdit(Long courseId, String username) {
        List<CourseEditItem> list = jdbcTemplate.query(SELECT_OWNED_COURSE_SQL, (rs, rowNum) -> new CourseEditItem(
                rs.getLong("id"),
                rs.getString("category"),
                rs.getString("title"),
                rs.getString("instructor_name"),
                rs.getString("description"),
                rs.getString("content_text")
        ), courseId, username);
        return list.stream().findFirst();
    }

    public List<CourseOutlineItem> findOutline(Long courseId) {
        return jdbcTemplate.query(SELECT_OUTLINE_SQL, (rs, rowNum) -> new CourseOutlineItem(
                rs.getLong("id"),
                rs.getInt("sort_order"),
                rs.getString("title"),
                rs.getInt("duration_sec") / 60
        ), courseId);
    }

    public int updateCourseByOwner(Long courseId, String username, String category, String title, String instructorName, String description, String contentText) {
        return jdbcTemplate.update(UPDATE_COURSE_SQL,
                (category == null || category.isBlank()) ? "기타" : category,
                title, instructorName, description, contentText,
                courseId, username);
    }

    public void replaceLectures(Long courseId, List<String> titles, List<Integer> minutes) {
        jdbcTemplate.update(DELETE_LECTURES_BY_COURSE_SQL, courseId);
        saveLectures(courseId, titles, minutes);
    }
}

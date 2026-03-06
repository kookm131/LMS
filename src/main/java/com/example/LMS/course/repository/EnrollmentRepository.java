package com.example.LMS.course.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 수강신청(내역) 전용 Repository
 */
@Repository
public class EnrollmentRepository {

    private static final String APPLY_ENROLLMENT_SQL = """
            INSERT INTO enrollments (course_id, student_id)
            VALUES (?, ?)
            """;

    private static final String EXISTS_ENROLLMENT_SQL = """
            SELECT COUNT(*)
            FROM enrollments
            WHERE course_id = ? AND student_id = ?
            """;

    private static final String SELECT_ENROLLMENTS_BASE_SQL = """
            SELECT e.id, c.id AS course_id, c.category, c.title, c.instructor_name, e.progress_rate, e.enrolled_at, e.completed_at
            FROM enrollments e
            JOIN courses c ON c.id = e.course_id
            """;

    private static final String COUNT_ENROLLMENTS_BASE_SQL = """
            SELECT COUNT(*)
            FROM enrollments e
            JOIN courses c ON c.id = e.course_id
            """;

    private static final String SELECT_ENROLLED_CATEGORIES_SQL = """
            SELECT DISTINCT c.category
            FROM enrollments e
            JOIN courses c ON c.id = e.course_id
            WHERE e.student_id = ?
            ORDER BY c.category ASC
            """;

    private static final String SELECT_STUDY_COURSE_SQL = """
            SELECT c.id AS course_id, c.title, c.category, c.instructor_name,
                   c.total_hours, c.avg_hours, e.progress_rate, e.enrolled_at
            FROM enrollments e
            JOIN courses c ON c.id = e.course_id
            WHERE e.student_id = ? AND e.course_id = ?
            LIMIT 1
            """;

    private static final String SELECT_QUIZ_PERCENT_SQL = """
            SELECT COALESCE(ROUND(AVG(COALESCE(qr.score, 0)), 1), 0)
            FROM quizzes q
            LEFT JOIN quiz_results qr
              ON qr.quiz_id = q.id
             AND qr.student_id = ?
            WHERE q.course_id = ?
            """;

    private static final String DELETE_ENROLLMENT_SQL = """
            DELETE FROM enrollments
            WHERE course_id = ? AND student_id = ?
            """;

    private static final String SYNC_PURCHASE_COUNT_SQL = """
            UPDATE courses c
            SET c.purchase_count = (
                SELECT COUNT(*)
                FROM enrollments e
                WHERE e.course_id = c.id
            )
            WHERE c.id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public EnrollmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int apply(Long courseId, Long studentId) {
        int updated = jdbcTemplate.update(APPLY_ENROLLMENT_SQL, courseId, studentId);
        jdbcTemplate.update(SYNC_PURCHASE_COUNT_SQL, courseId);
        return updated;
    }

    public boolean existsEnrollment(Long courseId, Long studentId) {
        Integer count = jdbcTemplate.queryForObject(EXISTS_ENROLLMENT_SQL, Integer.class, courseId, studentId);
        return count != null && count > 0;
    }

    public List<EnrollmentHistoryItem> findPaged(Long studentId, int pageSize, int offset, String category, String completionStatus) {
        List<Object> params = new ArrayList<>();
        String where = buildWhereClause(studentId, category, completionStatus, params);

        String sql = SELECT_ENROLLMENTS_BASE_SQL + where + " ORDER BY e.enrolled_at DESC, e.id DESC LIMIT ? OFFSET ?";
        params.add(pageSize);
        params.add(offset);

        return jdbcTemplate.query(sql, (rs, rowNum) -> new EnrollmentHistoryItem(
                rs.getLong("id"),
                rs.getLong("course_id"),
                rs.getString("category"),
                rs.getString("title"),
                rs.getString("instructor_name"),
                rs.getDouble("progress_rate"),
                rs.getTimestamp("enrolled_at").toLocalDateTime()
        ), params.toArray());
    }

    public int countPaged(Long studentId, String category, String completionStatus) {
        List<Object> params = new ArrayList<>();
        String where = buildWhereClause(studentId, category, completionStatus, params);

        Integer count = jdbcTemplate.queryForObject(COUNT_ENROLLMENTS_BASE_SQL + where, Integer.class, params.toArray());
        return count == null ? 0 : count;
    }

    private String buildWhereClause(Long studentId, String category, String completionStatus, List<Object> params) {
        StringBuilder where = new StringBuilder(" WHERE e.student_id = ?");
        params.add(studentId);

        if (category != null && !category.isBlank()) {
            where.append(" AND c.category = ?");
            params.add(category);
        }

        if ("complete".equalsIgnoreCase(completionStatus)) {
            where.append(" AND e.progress_rate >= 100.0 AND e.completed_at IS NOT NULL");
        } else if ("incomplete".equalsIgnoreCase(completionStatus)) {
            where.append(" AND (e.progress_rate < 100.0 OR e.completed_at IS NULL)");
        } else if ("inprogress".equalsIgnoreCase(completionStatus)) {
            where.append(" AND e.progress_rate < 100.0");
        }

        return where.toString();
    }

    public List<String> findCategories(Long studentId) {
        return jdbcTemplate.queryForList(SELECT_ENROLLED_CATEGORIES_SQL, String.class, studentId);
    }

    public Optional<StudyCourseItem> findStudyCourse(Long studentId, Long courseId) {
        List<StudyCourseItem> list = jdbcTemplate.query(SELECT_STUDY_COURSE_SQL, (rs, rowNum) -> new StudyCourseItem(
                rs.getLong("course_id"),
                rs.getString("title"),
                rs.getString("category"),
                rs.getString("instructor_name"),
                rs.getInt("total_hours"),
                rs.getDouble("avg_hours"),
                rs.getDouble("progress_rate"),
                rs.getTimestamp("enrolled_at").toLocalDateTime()
        ), studentId, courseId);
        return list.stream().findFirst();
    }

    public double getQuizPercent(Long studentId, Long courseId) {
        Double v = jdbcTemplate.queryForObject(SELECT_QUIZ_PERCENT_SQL, Double.class, studentId, courseId);
        return v == null ? 0.0 : v;
    }

    public int dropEnrollment(Long courseId, Long studentId) {
        int updated = jdbcTemplate.update(DELETE_ENROLLMENT_SQL, courseId, studentId);
        jdbcTemplate.update(SYNC_PURCHASE_COUNT_SQL, courseId);
        return updated;
    }

    public record EnrollmentHistoryItem(Long id, Long courseId, String category, String title, String instructorName, double progressRate, LocalDateTime enrolledAt) {}

    public record StudyCourseItem(Long courseId, String title, String category, String instructorName,
                                  int totalHours, double avgHours, double progressRate, LocalDateTime enrolledAt) {}
}

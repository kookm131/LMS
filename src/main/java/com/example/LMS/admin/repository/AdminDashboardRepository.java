package com.example.LMS.admin.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class AdminDashboardRepository {
    private final JdbcTemplate jdbcTemplate;

    public AdminDashboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long totalUsers() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
    }

    public long totalEnrollments() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM enrollments", Long.class);
    }

    public long totalCourses() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM courses", Long.class);
    }

    public double completionRate() {
        Double v = jdbcTemplate.queryForObject("""
                SELECT COALESCE(
                    100.0 * SUM(CASE WHEN (completed_at IS NOT NULL OR progress_rate >= 100) THEN 1 ELSE 0 END) / NULLIF(COUNT(*),0),
                    0
                )
                FROM enrollments
                """, Double.class);
        return v == null ? 0.0 : v;
    }

    public double examPassRate() {
        Double v = jdbcTemplate.queryForObject("""
                SELECT COALESCE(
                    100.0 * AVG(CASE WHEN ea.score >= COALESCE(es.pass_score, 60) THEN 1 ELSE 0 END),
                    0
                )
                FROM exam_attempts ea
                LEFT JOIN exam_settings es ON es.course_id = ea.course_id
                """, Double.class);
        return v == null ? 0.0 : v;
    }

    public List<TrendPoint> signupTrendDaily(LocalDate from) {
        return jdbcTemplate.query("""
                SELECT DATE(created_at) AS bucket, COUNT(*) AS cnt
                FROM users
                WHERE DATE(created_at) >= ?
                GROUP BY DATE(created_at)
                ORDER BY bucket
                """, (rs, rowNum) -> new TrendPoint(rs.getString("bucket"), rs.getLong("cnt")), from);
    }

    public List<TrendPoint> enrollmentTrendDaily(LocalDate from) {
        return jdbcTemplate.query("""
                SELECT DATE(enrolled_at) AS bucket, COUNT(*) AS cnt
                FROM enrollments
                WHERE DATE(enrolled_at) >= ?
                GROUP BY DATE(enrolled_at)
                ORDER BY bucket
                """, (rs, rowNum) -> new TrendPoint(rs.getString("bucket"), rs.getLong("cnt")), from);
    }

    public List<TrendPoint> signupTrendMonthly(String fromYm) {
        return jdbcTemplate.query("""
                SELECT DATE_FORMAT(created_at, '%Y-%m') AS bucket, COUNT(*) AS cnt
                FROM users
                WHERE DATE_FORMAT(created_at, '%Y-%m') >= ?
                GROUP BY DATE_FORMAT(created_at, '%Y-%m')
                ORDER BY bucket
                """, (rs, rowNum) -> new TrendPoint(rs.getString("bucket"), rs.getLong("cnt")), fromYm);
    }

    public List<TrendPoint> enrollmentTrendMonthly(String fromYm) {
        return jdbcTemplate.query("""
                SELECT DATE_FORMAT(enrolled_at, '%Y-%m') AS bucket, COUNT(*) AS cnt
                FROM enrollments
                WHERE DATE_FORMAT(enrolled_at, '%Y-%m') >= ?
                GROUP BY DATE_FORMAT(enrolled_at, '%Y-%m')
                ORDER BY bucket
                """, (rs, rowNum) -> new TrendPoint(rs.getString("bucket"), rs.getLong("cnt")), fromYm);
    }

    public List<TrendPoint> signupTrendYearly(int fromYear) {
        return jdbcTemplate.query("""
                SELECT YEAR(created_at) AS bucket, COUNT(*) AS cnt
                FROM users
                WHERE YEAR(created_at) >= ?
                GROUP BY YEAR(created_at)
                ORDER BY bucket
                """, (rs, rowNum) -> new TrendPoint(String.valueOf(rs.getInt("bucket")), rs.getLong("cnt")), fromYear);
    }

    public List<TrendPoint> enrollmentTrendYearly(int fromYear) {
        return jdbcTemplate.query("""
                SELECT YEAR(enrolled_at) AS bucket, COUNT(*) AS cnt
                FROM enrollments
                WHERE YEAR(enrolled_at) >= ?
                GROUP BY YEAR(enrolled_at)
                ORDER BY bucket
                """, (rs, rowNum) -> new TrendPoint(String.valueOf(rs.getInt("bucket")), rs.getLong("cnt")), fromYear);
    }

    public List<CategoryPoint> courseCategoryDistribution() {
        return jdbcTemplate.query("""
                SELECT category, COUNT(*) AS cnt
                FROM courses
                GROUP BY category
                ORDER BY cnt DESC, category ASC
                """, (rs, rowNum) -> new CategoryPoint(rs.getString("category"), rs.getLong("cnt")));
    }

    public List<CategoryPoint> enrollmentCategoryDistribution() {
        return jdbcTemplate.query("""
                SELECT c.category, COUNT(*) AS cnt
                FROM enrollments e
                JOIN courses c ON c.id = e.course_id
                GROUP BY c.category
                ORDER BY cnt DESC, c.category ASC
                """, (rs, rowNum) -> new CategoryPoint(rs.getString("category"), rs.getLong("cnt")));
    }

    public List<PopularCourseRow> popularCoursesTop10() {
        return jdbcTemplate.query("""
                SELECT
                    c.id,
                    c.title,
                    c.category,
                    COUNT(e.id) AS enrollment_count,
                    c.purchase_count,
                    COALESCE(c.rating, 0) AS rating
                FROM courses c
                LEFT JOIN enrollments e ON e.course_id = c.id
                GROUP BY c.id, c.title, c.category, c.purchase_count, c.rating
                ORDER BY enrollment_count DESC, c.purchase_count DESC, c.id DESC
                LIMIT 10
                """, (rs, rowNum) -> new PopularCourseRow(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("category"),
                rs.getLong("enrollment_count"),
                rs.getLong("purchase_count"),
                rs.getDouble("rating")
        ));
    }

    public List<LowRatingCourseRow> lowRatingCourses() {
        return jdbcTemplate.query("""
                SELECT id, title, category, rating, purchase_count
                FROM courses
                WHERE rating <= 3.5
                ORDER BY rating ASC, purchase_count DESC, id DESC
                LIMIT 10
                """, (rs, rowNum) -> new LowRatingCourseRow(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("category"),
                rs.getDouble("rating"),
                rs.getLong("purchase_count")
        ));
    }

    public long unansweredQnaCount() {
        Long v = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM qna_questions q
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM qna_comments c
                    WHERE c.qna_id = q.id
                      AND c.is_accepted = 1
                )
                """, Long.class);
        return v == null ? 0L : v;
    }

    public List<UnansweredQnaRow> unansweredQnaTop10() {
        return jdbcTemplate.query("""
                SELECT q.id, q.title, q.created_at, COUNT(c.id) AS comment_count
                FROM qna_questions q
                LEFT JOIN qna_comments c ON c.qna_id = q.id
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM qna_comments c2
                    WHERE c2.qna_id = q.id
                      AND c2.is_accepted = 1
                )
                GROUP BY q.id, q.title, q.created_at
                ORDER BY q.created_at DESC
                LIMIT 10
                """, (rs, rowNum) -> new UnansweredQnaRow(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getTimestamp("created_at").toLocalDateTime().toString().replace('T', ' '),
                rs.getLong("comment_count")
        ));
    }

    public record TrendPoint(String bucket, long count) {}
    public record CategoryPoint(String category, long count) {}
    public record PopularCourseRow(long id, String title, String category, long enrollmentCount, long purchaseCount, double rating) {}
    public record UnansweredQnaRow(long id, String title, String createdAt, long commentCount) {}
    public record LowRatingCourseRow(long id, String title, String category, double rating, long purchaseCount) {}
}

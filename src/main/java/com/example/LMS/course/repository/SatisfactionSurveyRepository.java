package com.example.LMS.course.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class SatisfactionSurveyRepository {

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS satisfaction_surveys (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id BIGINT NOT NULL,
                course_id BIGINT NOT NULL,
                q1 DECIMAL(2,1) NULL,
                q2 DECIMAL(2,1) NULL,
                q3 DECIMAL(2,1) NULL,
                q4 DECIMAL(2,1) NULL,
                q5 DECIMAL(2,1) NULL,
                q6 DECIMAL(2,1) NULL,
                q7 DECIMAL(2,1) NULL,
                q8 DECIMAL(2,1) NULL,
                q9 DECIMAL(2,1) NULL,
                q10 DECIMAL(2,1) NULL,
                comment_text TEXT NULL,
                overall_rating DECIMAL(2,1) NOT NULL DEFAULT 5.0,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY uq_survey_user_course (user_id, course_id)
            )
            """;

    private static final String SELECT_BY_USER_AND_COURSE_SQL = """
            SELECT id, user_id, course_id, overall_rating, created_at
            FROM satisfaction_surveys
            WHERE user_id = ? AND course_id = ?
            LIMIT 1
            """;

    private static final String SELECT_DETAIL_BY_USER_AND_COURSE_SQL = """
            SELECT id, user_id, course_id, q1,q2,q3,q4,q5,q6,q7,q8,q9,q10, comment_text, overall_rating, created_at
            FROM satisfaction_surveys
            WHERE user_id = ? AND course_id = ?
            LIMIT 1
            """;

    private static final String SELECT_COURSE_SURVEY_SUMMARY_SQL = """
            SELECT c.id AS course_id, c.title, c.purchase_count,
                   COALESCE(ROUND(AVG(ss.overall_rating), 1), 0.0) AS avg_rating,
                   COUNT(ss.id) AS survey_count
            FROM courses c
            LEFT JOIN satisfaction_surveys ss ON ss.course_id = c.id
            WHERE c.id = ? AND c.created_by_username = ?
            GROUP BY c.id, c.title, c.purchase_count
            """;

    private static final String SELECT_COURSE_SURVEYS_PAGED_SQL = """
            SELECT id, overall_rating, comment_text, created_at
            FROM satisfaction_surveys
            WHERE course_id = ?
            ORDER BY created_at DESC, id DESC
            LIMIT ? OFFSET ?
            """;

    private static final String SELECT_COURSE_SURVEY_BY_ID_SQL = """
            SELECT id, user_id, course_id, q1,q2,q3,q4,q5,q6,q7,q8,q9,q10, comment_text, overall_rating, created_at
            FROM satisfaction_surveys
            WHERE course_id = ? AND id = ?
            LIMIT 1
            """;

    private static final String COUNT_COURSE_SURVEYS_SQL = """
            SELECT COUNT(*)
            FROM satisfaction_surveys
            WHERE course_id = ?
            """;

    private static final String SELECT_COURSE_IDS_BY_USER_SQL = """
            SELECT course_id
            FROM satisfaction_surveys
            WHERE user_id = ?
            """;

    private static final String INSERT_SQL = """
            INSERT INTO satisfaction_surveys (
                user_id, course_id, q1, q2, q3, q4, q5, q6, q7, q8, q9, q10, comment_text, overall_rating
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_SQL = """
            UPDATE satisfaction_surveys
            SET q1=?, q2=?, q3=?, q4=?, q5=?, q6=?, q7=?, q8=?, q9=?, q10=?, comment_text=?, overall_rating=?
            WHERE user_id=? AND course_id=?
            """;

    private static final String DELETE_SQL = """
            DELETE FROM satisfaction_surveys
            WHERE user_id = ? AND course_id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public SatisfactionSurveyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private void ensureTable() {
        jdbcTemplate.execute(CREATE_TABLE_SQL);
    }

    public Optional<SurveySummary> findByUserAndCourse(Long userId, Long courseId) {
        ensureTable();
        List<SurveySummary> list = jdbcTemplate.query(SELECT_BY_USER_AND_COURSE_SQL, (rs, rowNum) -> new SurveySummary(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getLong("course_id"),
                rs.getDouble("overall_rating"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), userId, courseId);
        return list.stream().findFirst();
    }

    public Optional<SurveyDetail> findDetailByUserAndCourse(Long userId, Long courseId) {
        ensureTable();
        List<SurveyDetail> list = jdbcTemplate.query(SELECT_DETAIL_BY_USER_AND_COURSE_SQL, (rs, rowNum) -> new SurveyDetail(
                rs.getLong("id"), rs.getLong("user_id"), rs.getLong("course_id"),
                getDouble(rs, "q1"), getDouble(rs, "q2"), getDouble(rs, "q3"), getDouble(rs, "q4"), getDouble(rs, "q5"),
                getDouble(rs, "q6"), getDouble(rs, "q7"), getDouble(rs, "q8"), getDouble(rs, "q9"), getDouble(rs, "q10"),
                rs.getString("comment_text"), rs.getDouble("overall_rating"), rs.getTimestamp("created_at").toLocalDateTime()
        ), userId, courseId);
        return list.stream().findFirst();
    }

    private Double getDouble(java.sql.ResultSet rs, String col) throws java.sql.SQLException {
        double v = rs.getDouble(col);
        return rs.wasNull() ? null : v;
    }

    public List<Long> findSurveyedCourseIdsByUser(Long userId) {
        ensureTable();
        return jdbcTemplate.queryForList(SELECT_COURSE_IDS_BY_USER_SQL, Long.class, userId);
    }

    public int save(Long userId, Long courseId,
                    Double q1, Double q2, Double q3, Double q4, Double q5,
                    Double q6, Double q7, Double q8, Double q9, Double q10,
                    String comment, double overallRating) {
        ensureTable();
        return jdbcTemplate.update(INSERT_SQL,
                userId, courseId,
                q1, q2, q3, q4, q5, q6, q7, q8, q9, q10,
                comment, overallRating);
    }

    public int update(Long userId, Long courseId,
                      Double q1, Double q2, Double q3, Double q4, Double q5,
                      Double q6, Double q7, Double q8, Double q9, Double q10,
                      String comment, double overallRating) {
        ensureTable();
        return jdbcTemplate.update(UPDATE_SQL,
                q1,q2,q3,q4,q5,q6,q7,q8,q9,q10,comment,overallRating,
                userId,courseId);
    }

    public Optional<CourseSurveySummary> findCourseSummaryForOwner(Long courseId, String ownerUsername) {
        ensureTable();
        List<CourseSurveySummary> list = jdbcTemplate.query(SELECT_COURSE_SURVEY_SUMMARY_SQL, (rs, rowNum) -> new CourseSurveySummary(
                rs.getLong("course_id"),
                rs.getString("title"),
                rs.getInt("purchase_count"),
                rs.getInt("survey_count"),
                rs.getDouble("avg_rating")
        ), courseId, ownerUsername);
        return list.stream().findFirst();
    }

    public List<AnonymousSurveyItem> findCourseSurveysPaged(Long courseId, int pageSize, int offset) {
        ensureTable();
        return jdbcTemplate.query(SELECT_COURSE_SURVEYS_PAGED_SQL, (rs, rowNum) -> new AnonymousSurveyItem(
                rs.getLong("id"),
                rs.getDouble("overall_rating"),
                rs.getString("comment_text"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), courseId, pageSize, offset);
    }

    public Optional<SurveyDetail> findCourseSurveyById(Long courseId, Long surveyId) {
        ensureTable();
        List<SurveyDetail> list = jdbcTemplate.query(SELECT_COURSE_SURVEY_BY_ID_SQL, (rs, rowNum) -> new SurveyDetail(
                rs.getLong("id"), rs.getLong("user_id"), rs.getLong("course_id"),
                getDouble(rs, "q1"), getDouble(rs, "q2"), getDouble(rs, "q3"), getDouble(rs, "q4"), getDouble(rs, "q5"),
                getDouble(rs, "q6"), getDouble(rs, "q7"), getDouble(rs, "q8"), getDouble(rs, "q9"), getDouble(rs, "q10"),
                rs.getString("comment_text"), rs.getDouble("overall_rating"), rs.getTimestamp("created_at").toLocalDateTime()
        ), courseId, surveyId);
        return list.stream().findFirst();
    }

    public int countCourseSurveys(Long courseId) {
        ensureTable();
        Integer count = jdbcTemplate.queryForObject(COUNT_COURSE_SURVEYS_SQL, Integer.class, courseId);
        return count == null ? 0 : count;
    }

    public int delete(Long userId, Long courseId) {
        ensureTable();
        return jdbcTemplate.update(DELETE_SQL, userId, courseId);
    }

    public record SurveySummary(Long id, Long userId, Long courseId, double overallRating, LocalDateTime createdAt) {}
    public record CourseSurveySummary(Long courseId, String title, int purchaseCount, int surveyCount, double avgRating) {}
    public record AnonymousSurveyItem(Long id, double overallRating, String comment, LocalDateTime createdAt) {}
    public record SurveyDetail(Long id, Long userId, Long courseId,
                               Double q1, Double q2, Double q3, Double q4, Double q5,
                               Double q6, Double q7, Double q8, Double q9, Double q10,
                               String comment, double overallRating, LocalDateTime createdAt) {}
}

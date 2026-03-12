package com.example.LMS.course.repository;

import com.example.LMS.course.model.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 강의 도메인 데이터 접근 Repository (JdbcTemplate)
 *
 * 왜 JdbcTemplate?
 * - SQL을 직접 제어하면서 검색/카테고리/페이징 조건을 명확하게 관리하기 위함.
 *
 * 왜 SQL 상수 분리?
 * - 기능별 쿼리를 독립적으로 관리해 수정 범위를 줄이고 가독성을 높이기 위함.
 */
@Repository
public class CourseRepository {

    private static final String SELECT_ALL_SQL = """
            SELECT id, title, instructor_name, description, created_at
            FROM courses
            ORDER BY id DESC
            """;

    private static final String INSERT_SQL = """
            INSERT INTO courses (title, instructor_name, description)
            VALUES (?, ?, ?)
            """;

    private static final String SELECT_RECOMMENDED_SQL = """
            SELECT id, title, instructor_name, description, created_at
            FROM courses
            WHERE status = 'PUBLISHED'
            ORDER BY created_at DESC, id DESC
            LIMIT ?
            """;

    private static final String SELECT_PAGED_SQL = """
            SELECT id, category, title, instructor_name, description, rating, purchase_count, created_at
            FROM courses
            WHERE status = 'PUBLISHED'
            ORDER BY created_at DESC, id DESC
            LIMIT ? OFFSET ?
            """;

    private static final String SELECT_PAGED_BY_CATEGORY_SQL = """
            SELECT id, category, title, instructor_name, description, rating, purchase_count, created_at
            FROM courses
            WHERE status = 'PUBLISHED' AND category = ?
            ORDER BY created_at DESC, id DESC
            LIMIT ? OFFSET ?
            """;

    private static final String COUNT_PUBLISHED_SQL = """
            SELECT COUNT(*) FROM courses WHERE status = 'PUBLISHED'
            """;

    private static final String COUNT_PUBLISHED_BY_CATEGORY_SQL = """
            SELECT COUNT(*) FROM courses WHERE status = 'PUBLISHED' AND category = ?
            """;

    private static final String SELECT_PAGED_BY_INSTRUCTOR_SQL = """
            SELECT id, category, title, instructor_name, description, rating, purchase_count, created_at
            FROM courses
            WHERE status = 'PUBLISHED' AND created_by_username = ?
            ORDER BY created_at DESC, id DESC
            LIMIT ? OFFSET ?
            """;

    private static final String SELECT_PAGED_BY_INSTRUCTOR_AND_CATEGORY_SQL = """
            SELECT id, category, title, instructor_name, description, rating, purchase_count, created_at
            FROM courses
            WHERE status = 'PUBLISHED' AND created_by_username = ? AND category = ?
            ORDER BY created_at DESC, id DESC
            LIMIT ? OFFSET ?
            """;

    private static final String COUNT_PUBLISHED_BY_INSTRUCTOR_SQL = """
            SELECT COUNT(*)
            FROM courses
            WHERE status = 'PUBLISHED' AND created_by_username = ?
            """;

    private static final String COUNT_PUBLISHED_BY_INSTRUCTOR_AND_CATEGORY_SQL = """
            SELECT COUNT(*)
            FROM courses
            WHERE status = 'PUBLISHED' AND created_by_username = ? AND category = ?
            """;

    private static final String SELECT_CATEGORIES_BY_INSTRUCTOR_SQL = """
            SELECT DISTINCT category
            FROM courses
            WHERE status = 'PUBLISHED' AND instructor_name = ?
            ORDER BY category ASC
            """;


    private static final String SELECT_BUSINESS_SATISFACTION_PAGED_SQL = """
            SELECT c.id, c.title, c.category, c.instructor_name, c.purchase_count,
                   COALESCE(ROUND(AVG(ss.overall_rating), 1), 0.0) AS avg_rating,
                   MAX(ss.created_at) AS last_survey_at
            FROM courses c
            LEFT JOIN satisfaction_surveys ss ON ss.course_id = c.id
            WHERE c.status = 'PUBLISHED' AND c.created_by_username = ?
            GROUP BY c.id, c.title, c.category, c.instructor_name, c.purchase_count
            ORDER BY c.created_at DESC, c.id DESC
            LIMIT ? OFFSET ?
            """;

    private static final String COUNT_BUSINESS_SATISFACTION_SQL = """
            SELECT COUNT(*)
            FROM courses c
            WHERE c.status = 'PUBLISHED' AND c.created_by_username = ?
            """;

    private static final String SELECT_PAGED_BY_KEYWORD_SQL = """
            SELECT id, category, title, instructor_name, description, rating, purchase_count, created_at
            FROM courses
            WHERE status = 'PUBLISHED'
              AND title LIKE CONCAT('%', ?, '%')
            ORDER BY created_at DESC, id DESC
            LIMIT ? OFFSET ?
            """;

    private static final String SELECT_PAGED_BY_CATEGORY_AND_KEYWORD_SQL = """
            SELECT id, category, title, instructor_name, description, rating, purchase_count, created_at
            FROM courses
            WHERE status = 'PUBLISHED'
              AND category = ?
              AND title LIKE CONCAT('%', ?, '%')
            ORDER BY created_at DESC, id DESC
            LIMIT ? OFFSET ?
            """;

    private static final String COUNT_PUBLISHED_BY_KEYWORD_SQL = """
            SELECT COUNT(*)
            FROM courses
            WHERE status = 'PUBLISHED'
              AND title LIKE CONCAT('%', ?, '%')
            """;

    private static final String COUNT_PUBLISHED_BY_CATEGORY_AND_KEYWORD_SQL = """
            SELECT COUNT(*)
            FROM courses
            WHERE status = 'PUBLISHED'
              AND category = ?
              AND title LIKE CONCAT('%', ?, '%')
            """;

    private static final String SELECT_CATEGORIES_SQL = """
            SELECT DISTINCT category
            FROM courses
            WHERE status = 'PUBLISHED'
            ORDER BY category ASC
            """;

    private static final String SELECT_DETAIL_SQL = """
            SELECT id, category, title, content_text, instructor_name, total_hours, avg_hours, rating, created_at
            FROM courses
            WHERE id = ? AND status = 'PUBLISHED'
            """;

    private static final String SELECT_OUTLINE_SQL = """
            SELECT id, sort_order, title, IFNULL(duration_sec, 0) AS duration_sec
            FROM lectures
            WHERE course_id = ?
            ORDER BY sort_order ASC, id ASC
            """;

    private static final String SELECT_REVIEWS_SQL = """
            SELECT id, writer_name, rating, content, created_at
            FROM course_reviews
            WHERE course_id = ?
            ORDER BY id DESC
            LIMIT ? OFFSET ?
            """;

    private static final String COUNT_REVIEWS_SQL = """
            SELECT COUNT(*) FROM course_reviews WHERE course_id = ?
            """;

    private static final String INSERT_REVIEW_SQL = """
            INSERT INTO course_reviews (course_id, writer_name, rating, content)
            VALUES (?, ?, ?, ?)
            """;

    private static final String SELECT_REVIEW_BY_ID_SQL = """
            SELECT id, course_id, writer_name, rating, content, created_at
            FROM course_reviews
            WHERE id = ?
            LIMIT 1
            """;

    private static final String UPDATE_REVIEW_SQL = """
            UPDATE course_reviews
            SET rating = ?, content = ?
            WHERE id = ? AND writer_name = ?
            """;

    private static final String DELETE_REVIEW_SQL = """
            DELETE FROM course_reviews
            WHERE id = ? AND writer_name = ?
            """;

    private static final String SYNC_COURSE_RATING_SQL = """
            UPDATE courses c
            SET c.rating = COALESCE((
                SELECT ROUND(AVG(cr.rating), 1)
                FROM course_reviews cr
                WHERE cr.course_id = c.id
            ), 0.0)
            WHERE c.id = ?
            """;

    private static final String EXISTS_REVIEW_BY_WRITER_SQL = """
            SELECT COUNT(*)
            FROM course_reviews
            WHERE course_id = ? AND writer_name = ?
            """;

    private static final String SELECT_RELATED_SQL = """
            SELECT id, category, title, instructor_name, description, rating, purchase_count, created_at
            FROM courses
            WHERE status = 'PUBLISHED' AND category = ? AND id <> ?
            ORDER BY RAND()
            LIMIT ?
            """;


    private static final String SELECT_INSTRUCTOR_COURSE_BY_ID_SQL = """
            SELECT id, category, title, instructor_name, description, rating, purchase_count, created_at
            FROM courses
            WHERE status = 'PUBLISHED' AND created_by_username = ? AND id = ?
            LIMIT 1
            """;

    private static final String SELECT_ALL_BY_INSTRUCTOR_SQL = """
            SELECT id, title
            FROM courses
            WHERE status = 'PUBLISHED' AND created_by_username = ?
            ORDER BY created_at DESC, id DESC
            """;

    private final JdbcTemplate jdbcTemplate;

    public CourseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Course> findAll() {
        return jdbcTemplate.query(SELECT_ALL_SQL, (rs, rowNum) -> new Course(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("instructor_name"),
                rs.getString("description"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ));
    }

    public int save(String title, String instructorName, String description) {
        return jdbcTemplate.update(INSERT_SQL, title, instructorName, description);
    }

    public List<Course> findRecommended(int limit) {
        return jdbcTemplate.query(SELECT_RECOMMENDED_SQL, (rs, rowNum) -> new Course(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("instructor_name"),
                rs.getString("description"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), limit);
    }

    /**
     * 수강신청 목록 조회 (카테고리/검색어/페이징 조합 처리)
     *
     * 분기 이유:
     * - 카테고리만 있는 경우
     * - 검색어만 있는 경우
     * - 둘 다 있는 경우
     * - 둘 다 없는 경우
     * 를 각각 SQL로 분리해 인덱스 활용과 쿼리 단순성을 유지.
     */
    public List<CourseCatalogItem> findPaged(int pageSize, int offset, String category, String keyword) {
        boolean hasCategory = category != null && !category.isBlank();
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        if (!hasCategory && !hasKeyword) {
            return jdbcTemplate.query(SELECT_PAGED_SQL, (rs, rowNum) -> new CourseCatalogItem(
                    rs.getLong("id"),
                    rs.getString("category"),
                    rs.getString("title"),
                    rs.getString("instructor_name"),
                    rs.getString("description"),
                    rs.getDouble("rating"),
                    rs.getInt("purchase_count"),
                    rs.getTimestamp("created_at").toLocalDateTime()
            ), pageSize, offset);
        }

        if (hasCategory && hasKeyword) {
            return jdbcTemplate.query(SELECT_PAGED_BY_CATEGORY_AND_KEYWORD_SQL, (rs, rowNum) -> new CourseCatalogItem(
                    rs.getLong("id"),
                    rs.getString("category"),
                    rs.getString("title"),
                    rs.getString("instructor_name"),
                    rs.getString("description"),
                    rs.getDouble("rating"),
                    rs.getInt("purchase_count"),
                    rs.getTimestamp("created_at").toLocalDateTime()
            ), category, keyword, pageSize, offset);
        }

        if (hasCategory) {
            return jdbcTemplate.query(SELECT_PAGED_BY_CATEGORY_SQL, (rs, rowNum) -> new CourseCatalogItem(
                    rs.getLong("id"),
                    rs.getString("category"),
                    rs.getString("title"),
                    rs.getString("instructor_name"),
                    rs.getString("description"),
                    rs.getDouble("rating"),
                    rs.getInt("purchase_count"),
                    rs.getTimestamp("created_at").toLocalDateTime()
            ), category, pageSize, offset);
        }

        return jdbcTemplate.query(SELECT_PAGED_BY_KEYWORD_SQL, (rs, rowNum) -> new CourseCatalogItem(
                rs.getLong("id"),
                rs.getString("category"),
                rs.getString("title"),
                rs.getString("instructor_name"),
                rs.getString("description"),
                rs.getDouble("rating"),
                rs.getInt("purchase_count"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), keyword, pageSize, offset);
    }

    /**
     * findPaged와 동일 조건으로 전체 건수 조회.
     *
     * 왜 필요한가?
     * - 페이지 수(totalPages) 계산을 위해 목록 조회와 동일한 필터 기준의 COUNT가 필요.
     */
    public int countPaged(String category, String keyword) {
        boolean hasCategory = category != null && !category.isBlank();
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        Integer count;
        if (!hasCategory && !hasKeyword) {
            count = jdbcTemplate.queryForObject(COUNT_PUBLISHED_SQL, Integer.class);
        } else if (hasCategory && hasKeyword) {
            count = jdbcTemplate.queryForObject(COUNT_PUBLISHED_BY_CATEGORY_AND_KEYWORD_SQL, Integer.class, category, keyword);
        } else if (hasCategory) {
            count = jdbcTemplate.queryForObject(COUNT_PUBLISHED_BY_CATEGORY_SQL, Integer.class, category);
        } else {
            count = jdbcTemplate.queryForObject(COUNT_PUBLISHED_BY_KEYWORD_SQL, Integer.class, keyword);
        }

        return count == null ? 0 : count;
    }

    public List<String> findCategories() {
        return jdbcTemplate.queryForList(SELECT_CATEGORIES_SQL, String.class);
    }

    public List<CourseCatalogItem> findPagedByInstructor(String instructorName, int pageSize, int offset, String category) {
        boolean hasCategory = category != null && !category.isBlank();

        if (!hasCategory) {
            return jdbcTemplate.query(SELECT_PAGED_BY_INSTRUCTOR_SQL, (rs, rowNum) -> new CourseCatalogItem(
                    rs.getLong("id"),
                    rs.getString("category"),
                    rs.getString("title"),
                    rs.getString("instructor_name"),
                    rs.getString("description"),
                    rs.getDouble("rating"),
                    rs.getInt("purchase_count"),
                    rs.getTimestamp("created_at").toLocalDateTime()
            ), instructorName, pageSize, offset);
        }

        return jdbcTemplate.query(SELECT_PAGED_BY_INSTRUCTOR_AND_CATEGORY_SQL, (rs, rowNum) -> new CourseCatalogItem(
                rs.getLong("id"),
                rs.getString("category"),
                rs.getString("title"),
                rs.getString("instructor_name"),
                rs.getString("description"),
                rs.getDouble("rating"),
                rs.getInt("purchase_count"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), instructorName, category, pageSize, offset);
    }

    public int countPagedByInstructor(String instructorName, String category) {
        boolean hasCategory = category != null && !category.isBlank();

        Integer count;
        if (!hasCategory) {
            count = jdbcTemplate.queryForObject(COUNT_PUBLISHED_BY_INSTRUCTOR_SQL, Integer.class, instructorName);
        } else {
            count = jdbcTemplate.queryForObject(COUNT_PUBLISHED_BY_INSTRUCTOR_AND_CATEGORY_SQL, Integer.class, instructorName, category);
        }

        return count == null ? 0 : count;
    }

    public List<String> findCategoriesByInstructor(String instructorName) {
        return jdbcTemplate.queryForList(SELECT_CATEGORIES_BY_INSTRUCTOR_SQL, String.class, instructorName);
    }

    public Optional<CourseDetail> findDetail(Long courseId) {
        List<CourseDetail> list = jdbcTemplate.query(SELECT_DETAIL_SQL, (rs, rowNum) -> new CourseDetail(
                rs.getLong("id"),
                rs.getString("category"),
                rs.getString("title"),
                rs.getString("content_text"),
                rs.getString("instructor_name"),
                rs.getInt("total_hours"),
                rs.getDouble("avg_hours"),
                rs.getDouble("rating"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), courseId);
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

    public List<CourseReviewItem> findReviews(Long courseId, int limit, int offset) {
        return jdbcTemplate.query(SELECT_REVIEWS_SQL, (rs, rowNum) -> new CourseReviewItem(
                rs.getLong("id"),
                rs.getString("writer_name"),
                rs.getDouble("rating"),
                rs.getString("content"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), courseId, limit, offset);
    }

    public int countReviews(Long courseId) {
        Integer count = jdbcTemplate.queryForObject(COUNT_REVIEWS_SQL, Integer.class, courseId);
        return count == null ? 0 : count;
    }

    public void saveReview(Long courseId, String writerName, double rating, String content) {
        jdbcTemplate.update(INSERT_REVIEW_SQL, courseId, writerName, rating, content);
        jdbcTemplate.update(SYNC_COURSE_RATING_SQL, courseId);
    }

    public Optional<CourseReviewItem> findReviewById(Long reviewId) {
        List<CourseReviewItem> list = jdbcTemplate.query(SELECT_REVIEW_BY_ID_SQL, (rs, rowNum) -> new CourseReviewItem(
                rs.getLong("id"),
                rs.getString("writer_name"),
                rs.getDouble("rating"),
                rs.getString("content"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), reviewId);
        return list.stream().findFirst();
    }

    public int updateReview(Long reviewId, Long courseId, String writerName, double rating, String content) {
        int updated = jdbcTemplate.update(UPDATE_REVIEW_SQL, rating, content, reviewId, writerName);
        if (updated > 0) {
            jdbcTemplate.update(SYNC_COURSE_RATING_SQL, courseId);
        }
        return updated;
    }

    public boolean existsReviewByWriter(Long courseId, String writerName) {
        Integer count = jdbcTemplate.queryForObject(EXISTS_REVIEW_BY_WRITER_SQL, Integer.class, courseId, writerName);
        return count != null && count > 0;
    }

    public int deleteReview(Long reviewId, Long courseId, String writerName) {
        int deleted = jdbcTemplate.update(DELETE_REVIEW_SQL, reviewId, writerName);
        if (deleted > 0) {
            jdbcTemplate.update(SYNC_COURSE_RATING_SQL, courseId);
        }
        return deleted;
    }


    public Optional<CourseCatalogItem> findInstructorCourseById(String instructorName, Long courseId) {
        List<CourseCatalogItem> list = jdbcTemplate.query(SELECT_INSTRUCTOR_COURSE_BY_ID_SQL, (rs, rowNum) -> new CourseCatalogItem(
                rs.getLong("id"),
                rs.getString("category"),
                rs.getString("title"),
                rs.getString("instructor_name"),
                rs.getString("description"),
                rs.getDouble("rating"),
                rs.getInt("purchase_count"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), instructorName, courseId);
        return list.stream().findFirst();
    }

    public List<CourseOptionItem> findAllByInstructor(String instructorName) {
        return jdbcTemplate.query(SELECT_ALL_BY_INSTRUCTOR_SQL, (rs, rowNum) -> new CourseOptionItem(
                rs.getLong("id"),
                rs.getString("title")
        ), instructorName);
    }

    public List<CourseCatalogItem> findRelated(String category, Long excludeCourseId, int limit) {
        return jdbcTemplate.query(SELECT_RELATED_SQL, (rs, rowNum) -> new CourseCatalogItem(
                rs.getLong("id"),
                rs.getString("category"),
                rs.getString("title"),
                rs.getString("instructor_name"),
                rs.getString("description"),
                rs.getDouble("rating"),
                rs.getInt("purchase_count"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), category, excludeCourseId, limit);
    }

    public List<BusinessSatisfactionItem> findBusinessSatisfactionPaged(String ownerUsername, int pageSize, int offset) {
        return jdbcTemplate.query(SELECT_BUSINESS_SATISFACTION_PAGED_SQL, (rs, rowNum) -> {
            java.sql.Timestamp ts = rs.getTimestamp("last_survey_at");
            return new BusinessSatisfactionItem(
                    rs.getLong("id"),
                    rs.getString("title"),
                    rs.getString("category"),
                    rs.getString("instructor_name"),
                    rs.getInt("purchase_count"),
                    rs.getDouble("avg_rating"),
                    ts == null ? null : ts.toLocalDateTime()
            );
        }, ownerUsername, pageSize, offset);
    }

    public int countBusinessSatisfaction(String ownerUsername) {
        Integer count = jdbcTemplate.queryForObject(COUNT_BUSINESS_SATISFACTION_SQL, Integer.class, ownerUsername);
        return count == null ? 0 : count;
    }

    public record BusinessSatisfactionItem(Long courseId, String title, String category, String instructorName,
                                           int purchaseCount, double avgRating, java.time.LocalDateTime lastSurveyAt) {}

    public record CourseOptionItem(Long id, String title) {}
}


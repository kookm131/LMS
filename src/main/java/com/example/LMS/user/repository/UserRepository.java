package com.example.LMS.user.repository;

import com.example.LMS.user.dto.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자 저장/조회 Repository (JdbcTemplate)
 *
 * 왜 JdbcTemplate?
 * - 현재 프로젝트는 SQL을 직접 제어하는 학습 목적 구조를 사용 중.
 * - 단순 CRUD는 JdbcTemplate이 빠르고 직관적이다.
 */
@Repository
public class UserRepository {

    // 회원가입 저장 SQL
    private static final String INSERT_USER_SQL = """
            INSERT INTO users (
                user_type, username, password, email, name,
                birth_date, gender, phone, role
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    // 아이디 중복 체크
    private static final String COUNT_USERNAME_SQL = """
            SELECT COUNT(*) FROM users WHERE username = ?
            """;

    // 이메일 중복 체크
    private static final String COUNT_EMAIL_SQL = """
            SELECT COUNT(*) FROM users WHERE email = ?
            """;

    // 댓글 저장 시 작성자 식별을 위해 username -> user id 조회
    private static final String SELECT_ID_BY_USERNAME_SQL = """
            SELECT id FROM users WHERE username = ?
            """;

    private static final String SELECT_NAME_BY_USERNAME_SQL = """
            SELECT name FROM users WHERE username = ?
            """;


    private static final String SELECT_USER_AUTH_BY_EMAIL_SQL = """
            SELECT username, email, name, role
            FROM users
            WHERE email = ?
            LIMIT 1
            """;

    private static final String UPDATE_NAME_BY_EMAIL_SQL = """
            UPDATE users
            SET name = ?
            WHERE email = ?
            """;

    private static final String INSERT_SOCIAL_USER_SQL = """
            INSERT INTO users (
                user_type, username, password, email, name,
                birth_date, gender, phone, role
            ) VALUES ('NORMAL', ?, ?, ?, ?, '2000-01-01', 'MALE', '010-0000-0000', 'STUDENT')
            """;

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** username 중복 수 반환 (0이면 사용 가능) */
    public int countByUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(COUNT_USERNAME_SQL, Integer.class, username);
        return count == null ? 0 : count;
    }

    /** email 중복 수 반환 (0이면 사용 가능) */
    public int countByEmail(String email) {
        Integer count = jdbcTemplate.queryForObject(COUNT_EMAIL_SQL, Integer.class, email);
        return count == null ? 0 : count;
    }

    /** 회원 저장 */
    public int save(UserRegisterForm form, String encodedPassword, String role) {
        return jdbcTemplate.update(
                INSERT_USER_SQL,
                form.getUserType(),
                form.getUsername(),
                encodedPassword,
                form.getEmail(),
                form.getName(),
                form.getBirthDate(),
                form.getGender(),
                form.getPhone(),
                role
        );
    }

    /**
     * username으로 user id 조회
     *
     * 왜 Optional?
     * - 존재하지 않을 수 있는 조회 결과를 null 대신 안전하게 표현하기 위해.
     */
    public Optional<Long> findIdByUsername(String username) {
        try {
            Long id = jdbcTemplate.queryForObject(SELECT_ID_BY_USERNAME_SQL, Long.class, username);
            return Optional.ofNullable(id);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<String> findNameByUsername(String username) {
        try {
            String name = jdbcTemplate.queryForObject(SELECT_NAME_BY_USERNAME_SQL, String.class, username);
            return Optional.ofNullable(name);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }


    public Optional<UserAuthInfo> findAuthInfoByEmail(String email) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(SELECT_USER_AUTH_BY_EMAIL_SQL,
                    (rs, rowNum) -> new UserAuthInfo(
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("name"),
                            rs.getString("role")
                    ), email));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public int updateNameByEmail(String email, String name) {
        return jdbcTemplate.update(UPDATE_NAME_BY_EMAIL_SQL, name, email);
    }

    public int saveSocialUser(String username, String encodedPassword, String email, String name) {
        return jdbcTemplate.update(INSERT_SOCIAL_USER_SQL, username, encodedPassword, email, name);
    }

    public record UserAuthInfo(String username, String email, String name, String role) {}

}

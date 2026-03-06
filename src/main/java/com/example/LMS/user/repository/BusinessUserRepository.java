package com.example.LMS.user.repository;

import com.example.LMS.user.dto.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 사업자 회원 전용 저장소
 */
@Repository
public class BusinessUserRepository {

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS business_users (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(50) NOT NULL UNIQUE,
                email VARCHAR(255) NOT NULL UNIQUE,
                password VARCHAR(255) NOT NULL,
                representative_name VARCHAR(100) NOT NULL,
                business_name VARCHAR(200) NOT NULL,
                phone VARCHAR(20) NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """;

    private static final String COUNT_USERNAME_SQL = """
            SELECT COUNT(*) FROM business_users WHERE username = ?
            """;

    private static final String COUNT_EMAIL_SQL = """
            SELECT COUNT(*) FROM business_users WHERE email = ?
            """;

    private static final String INSERT_SQL = """
            INSERT INTO business_users (username, email, password, representative_name, business_name, phone)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public BusinessUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private void ensureTable() {
        jdbcTemplate.execute(CREATE_TABLE_SQL);
    }

    public int countByUsername(String username) {
        ensureTable();
        Integer count = jdbcTemplate.queryForObject(COUNT_USERNAME_SQL, Integer.class, username);
        return count == null ? 0 : count;
    }

    public int countByEmail(String email) {
        ensureTable();
        Integer count = jdbcTemplate.queryForObject(COUNT_EMAIL_SQL, Integer.class, email);
        return count == null ? 0 : count;
    }

    public void save(UserRegisterForm form, String encodedPassword) {
        ensureTable();
        jdbcTemplate.update(INSERT_SQL,
                form.getUsername(),
                form.getEmail(),
                encodedPassword,
                form.getName(),
                form.getBusinessName(),
                form.getPhone());
    }
}

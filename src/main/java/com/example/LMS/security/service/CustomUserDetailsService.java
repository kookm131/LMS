package com.example.LMS.security.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 로그인 인증 사용자 조회
 * - 일반회원: users 테이블
 * - 사업자회원: business_users 테이블
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final String SELECT_USER_SQL = """
            SELECT username, password, role
            FROM users
            WHERE username = ?
            LIMIT 1
            """;

    private static final String SELECT_BUSINESS_USER_SQL = """
            SELECT username, password
            FROM business_users
            WHERE username = ?
            LIMIT 1
            """;

    private final JdbcTemplate jdbcTemplate;

    public CustomUserDetailsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1) 일반 회원(users) 우선 조회
        UserDetails normalUser = jdbcTemplate.query(SELECT_USER_SQL, rs -> {
            if (!rs.next()) return null;

            String foundUsername = rs.getString("username");
            String password = rs.getString("password");
            String role = rs.getString("role");

            return User.withUsername(foundUsername)
                    .password(password)
                    .roles(role)
                    .build();
        }, username);

        if (normalUser != null) {
            return normalUser;
        }

        // 2) 사업자 회원(business_users) 조회
        UserDetails businessUser = jdbcTemplate.query(SELECT_BUSINESS_USER_SQL, rs -> {
            if (!rs.next()) return null;

            String foundUsername = rs.getString("username");
            String password = rs.getString("password");

            // 사업자 기본 권한은 INSTRUCTOR로 부여
            return User.withUsername(foundUsername)
                    .password(password)
                    .roles("INSTRUCTOR")
                    .build();
        }, username);

        if (businessUser != null) {
            return businessUser;
        }

        throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username);
    }
}
